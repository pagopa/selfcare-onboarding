package it.pagopa.selfcare.onboarding.service;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.UserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @RestClient
    @Inject
    UserApi userApi;

    public void deleteByIdAndInstitutionIdAndProductId(String userId, String institutionId, String productId) {
        log.debug("Deleting user {} for institution {} and product {}", userId, institutionId, productId);
        try (Response response =  userApi.deleteProducts(institutionId, productId, userId)) {
            if (!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                throw new GenericOnboardingException("Impossible to delete user with ID: " + userId);
            }
        }
    }
}
