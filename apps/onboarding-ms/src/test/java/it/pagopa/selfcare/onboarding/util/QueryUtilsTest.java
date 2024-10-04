package it.pagopa.selfcare.onboarding.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.time.LocalDateTime;
import java.util.Map;

@QuarkusTest
class QueryUtilsTest {

    @Test
    void testOnboardingUpdateMap_recipientCode() {
        Billing billing = new Billing();
        billing.setRecipientCode("recipientCode");

        Onboarding onboarding =  new Onboarding();
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setBilling(billing);
        onboarding.setCreatedAt(LocalDateTime.now());
        Map<String, Object> onboardingMap = QueryUtils.createMapForOnboardingUpdate(onboarding);

        Assert.assertNotNull(onboardingMap);
        Assert.assertNotNull(onboardingMap.get("billing.recipientCode"));
        Assert.assertTrue(onboardingMap.get("billing.recipientCode").equals("recipientCode"));
        Assert.assertNotNull(onboardingMap.get("status"));
        Assert.assertTrue(onboardingMap.get("status").equals(OnboardingStatus.COMPLETED.name()));
        Assert.assertNotNull(onboardingMap.get("createdAt"));
        Assert.assertTrue(onboardingMap.get("createdAt").equals(onboarding.getCreatedAt()));
    }
}
