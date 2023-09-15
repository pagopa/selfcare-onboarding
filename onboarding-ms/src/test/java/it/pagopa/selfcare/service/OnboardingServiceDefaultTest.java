package it.pagopa.selfcare.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;


@QuarkusTest
public class OnboardingServiceDefaultTest {

    @Inject
    OnboardingServiceDefault onboardingService;

    @InjectMock
    OnboardingRepository onboardingRepository;


    @Test
    void onboarding() {
        Mockito.when(onboardingRepository.persistOrUpdate(any()))
                .thenAnswer(arg -> Uni.createFrom().item(arg.getArguments()[0]));

        UniAssertSubscriber<Onboarding> subscriber = onboardingService.onboarding(new Onboarding())
                .subscribe().withSubscriber(UniAssertSubscriber.create()).awaitItem();

        Onboarding actual = subscriber.assertCompleted().getItem();
        assertNotNull(actual);

    }
}
