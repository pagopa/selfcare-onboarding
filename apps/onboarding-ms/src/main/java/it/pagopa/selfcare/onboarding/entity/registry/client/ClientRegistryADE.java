package it.pagopa.selfcare.onboarding.entity.registry.client;

import static it.pagopa.selfcare.onboarding.service.OnboardingServiceDefault.USERS_FIELD_LIST;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;

public abstract class ClientRegistryADE extends BaseRegistryManager<Boolean> {

  private final NationalRegistriesApi client;
  private final UserApi userApi;

  protected ClientRegistryADE(Onboarding onboarding, NationalRegistriesApi client, UserApi userApi) {
    super(onboarding);
    this.client = client;
    this.userApi = userApi;
  }

  public Boolean retrieveInstitution() {
    return client
        .verifyLegalUsingGET(getManagerTaxCode(), onboarding.getInstitution().getTaxCode())
        .onFailure()
        .retry()
        .atMost(MAX_NUMBER_ATTEMPTS)
        .onFailure(WebApplicationException.class)
        .recoverWithUni(
            ex ->
                ((WebApplicationException) ex).getResponse().getStatus() == 404
                    ? Uni.createFrom()
                        .failure(
                            new ResourceNotFoundException(
                                String.format(
                                    "Institution with taxCode %s not found",
                                    onboarding.getInstitution().getTaxCode())))
                    : Uni.createFrom().failure(ex))
        .await()
        .atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS))
        .getVerificationResult();
  }

  private String getManagerTaxCode() {
    final String managerId =
        onboarding.getUsers().stream()
            .filter(user -> user.getRole().equals(PartyRole.MANAGER))
            .map(User::getId)
            .findFirst()
            .orElse(null);
    return userApi
        .findByIdUsingGET(USERS_FIELD_LIST, managerId)
        .await()
            .atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS))
            .getFiscalCode();
  }
}
