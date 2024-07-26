package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@QuarkusTest
@TestProfile(CompletionServiceDefaultTest3.UserSendMail.class)
public class CompletionServiceDefaultTest3 {

    @InjectMock
    NotificationService notificationService;

    @Inject
    CompletionServiceDefault completionServiceDefault;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;
    public static class UserSendMail implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.email.service.available", "false");
        }
    }


    @Test
    void sendCompletionEmail() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        Map<String, WorkContactResource> map = new HashMap<>();
        userResource.setWorkContacts(map);
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");

        completionServiceDefault.sendCompletedEmail(onboardingWorkflow);

        Mockito.verifyNoInteractions(notificationService);
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboarding.getId());
        onboarding.setProductId("productId");
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setUsers(List.of());
        onboarding.setInstitution(new Institution());
        onboarding.setUserRequestUid("example-uid");

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("example");
        billing.setVatNumber("example");
        onboarding.setBilling(billing);
        return onboarding;
    }
}
