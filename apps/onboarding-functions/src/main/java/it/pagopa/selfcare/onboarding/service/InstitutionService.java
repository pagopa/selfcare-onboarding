package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@ApplicationScoped
public class InstitutionService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionService.class);

    @RestClient
    @Inject
    InstitutionApi institutionApi;

    public void deleteByIdAndProductId(String id, String productId) {
        log.debug("Deleting institution {} for product {}", id, productId);
        DeletedUserCountResponse response =  institutionApi.deleteUserInstitutionProductUsers(id, productId);
        if (Objects.nonNull(response) && response.getDeletedUserCount() < 1) {
            log.error("Error during institution deletion: {}", response);
            throw new GenericOnboardingException("Impossible to delete institution with ID: " + id);
        }
    }
}
