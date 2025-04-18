package it.pagopa.selfcare.onboarding.service;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
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

    private final OnboardingRepository onboardingRepository;

    public UserService(
            OnboardingRepository onboardingRepository) {
        this.onboardingRepository = onboardingRepository;
    }

    public List<String> findByInstitutionAndProduct(String institutionId, String productId) {
        var onboardings = onboardingRepository.findByOnboardingUsers(institutionId, productId);
        if (onboardings.isEmpty()) {
            return List.of();
        }
        return onboardings.stream()
                .flatMap(onboarding -> onboarding.getUsers().stream())
                .map(User::getId)
                .collect(Collectors.toSet()) // Usa un Set per evitare duplicati
                .stream()
                .toList();
    }

    public void deleteByIdAndInstitutionIdAndProductId(String userId, String institutionId, String productId) {
        log.debug("Deleting user {} for institution {} and product {}", userId, institutionId, productId);
        try (Response response =  userApi.deleteProducts(institutionId, productId, userId)) {
            if (!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                log.error("Error during user deletion: {}", response);
                throw new GenericOnboardingException("Impossible to delete user with ID: " + userId);
            }
        }
    }
}
