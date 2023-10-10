package it.pagopa.selfcare;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ApplicationScoped
public class OnboardingService {

    @Inject
    OnboardingRepository repository;
    public Onboarding getOnboarding(String onboardingId) {
        return repository.findById(new ObjectId(onboardingId));
    }
}
