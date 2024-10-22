package it.pagopa.selfcare.onboarding.entity;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

public class RegistryManagerIPA extends RegistryManagerIPAUo {

    public RegistryManagerIPA(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        super.originIdEC = onboarding.getInstitution().getOriginId();
        return IPAEntity.builder().build();
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}
