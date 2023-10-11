package it.pagopa.selfcare;

import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.repository.OnboardingRepository;
import it.pagopa.selfcare.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

@ApplicationScoped
public class OnboardingService {

    @Inject
    NotificationService notificationService;

    @Inject
    OnboardingRepository repository;
    public Onboarding getOnboarding(String onboardingId) {
        return repository.findById(new ObjectId(onboardingId));
    }
    
}
