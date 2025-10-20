package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.service.impl.OrchestrationServiceDefault;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

@QuarkusTest
class OrchestrationServiceDefaultTest {

    @InjectMock
    @RestClient
    OrchestrationApi orchestrationApi;

    OrchestrationServiceDefault service;

    @BeforeEach
    void setUp() throws Exception {
        service = new OrchestrationServiceDefault();
        Field f = OrchestrationServiceDefault.class.getDeclaredField("orchestrationApi");
        f.setAccessible(true);
        f.set(service, orchestrationApi);
    }

    @Test
    void triggerOrchestration_success() {
        // given
        String onboardingId = "onb-123";
        OrchestrationResponse response = mock(OrchestrationResponse.class);
        when(orchestrationApi.apiStartOnboardingOrchestrationGet(
                onboardingId, OrchestrationServiceDefault.TIMEOUT_ORCHESTRATION_RESPONSE))
                .thenReturn(Uni.createFrom().item(response));

        // when
        Uni<OrchestrationResponse> uni = service.triggerOrchestration(onboardingId);

        // then
        UniAssertSubscriber<OrchestrationResponse> sub = uni.subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        sub.assertCompleted().assertItem(response);
        verify(orchestrationApi).apiStartOnboardingOrchestrationGet(
                onboardingId, OrchestrationServiceDefault.TIMEOUT_ORCHESTRATION_RESPONSE);
        verifyNoMoreInteractions(orchestrationApi);
    }

    @Test
    void triggerOrchestration_failure() {
        // given
        String onboardingId = "onb-err";
        RuntimeException boom = new RuntimeException("boom");
        when(orchestrationApi.apiStartOnboardingOrchestrationGet(
                onboardingId, OrchestrationServiceDefault.TIMEOUT_ORCHESTRATION_RESPONSE))
                .thenReturn(Uni.createFrom().failure(boom));

        // when
        Uni<OrchestrationResponse> uni = service.triggerOrchestration(onboardingId);

        // then
        UniAssertSubscriber<OrchestrationResponse> sub = uni.subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        sub.assertFailedWith(RuntimeException.class);
        verify(orchestrationApi).apiStartOnboardingOrchestrationGet(
                onboardingId, OrchestrationServiceDefault.TIMEOUT_ORCHESTRATION_RESPONSE);
        verifyNoMoreInteractions(orchestrationApi);
    }

    @Test
    void triggerOrchestrationDeleteInstitutionAndUser_success() {
        // given
        String onboardingId = "onb-del-123";
        OrchestrationResponse response = mock(OrchestrationResponse.class);
        when(orchestrationApi.apiTriggerDeleteInstitutionAndUserGet(onboardingId))
                .thenReturn(Uni.createFrom().item(response));

        // when
        Uni<OrchestrationResponse> uni = service.triggerOrchestrationDeleteInstitutionAndUser(onboardingId);

        // then
        UniAssertSubscriber<OrchestrationResponse> sub = uni.subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        sub.assertCompleted().assertItem(response);
        verify(orchestrationApi).apiTriggerDeleteInstitutionAndUserGet(onboardingId);
        verifyNoMoreInteractions(orchestrationApi);
    }

    @Test
    void triggerOrchestrationDeleteInstitutionAndUser_failure() {
        // given
        String onboardingId = "onb-del-err";
        IllegalStateException failure = new IllegalStateException("cannot delete");
        when(orchestrationApi.apiTriggerDeleteInstitutionAndUserGet(onboardingId))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        Uni<OrchestrationResponse> uni = service.triggerOrchestrationDeleteInstitutionAndUser(onboardingId);

        // then
        UniAssertSubscriber<OrchestrationResponse> sub = uni.subscribe()
                .withSubscriber(UniAssertSubscriber.create());
        sub.assertFailedWith(IllegalStateException.class);
        verify(orchestrationApi).apiTriggerDeleteInstitutionAndUserGet(onboardingId);
        verifyNoMoreInteractions(orchestrationApi);
    }
}

