package it.pagopa.selfcare.onboarding.service;


import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.UserApi;

@ApplicationScoped
public class UserService {

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

}
