package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.PricingPlan;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class SapNotificationFilterTest {
    @Inject
    private SapNotificationFilter sapNotificationFilter;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should allow notification for allowed institution type and origin")
    public void shouldAllowNotificationForAllowedInstitutionTypeAndOrigin() {

        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.name());
        onboarding.setPricingPlan(PricingPlan.FA.name());
        InstitutionResponse institution = new InstitutionResponse();
        institution.setInstitutionType(InstitutionResponse.InstitutionTypeEnum.PA);
        institution.setOrigin("IPA");

        assertTrue(sapNotificationFilter.shouldSendNotification(onboarding, institution));
    }

    @Test
    @DisplayName("Should not allow notification for disallowed institution type")
    public void shouldNotAllowNotificationForDisallowedInstitutionType() {

        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.name());
        onboarding.setPricingPlan(PricingPlan.FA.name());
        InstitutionResponse institution = new InstitutionResponse();
        institution.setInstitutionType(InstitutionResponse.InstitutionTypeEnum.AS);

        assertFalse(sapNotificationFilter.shouldSendNotification(onboarding, institution));
    }

    @Test
    @DisplayName("Should not allow notification for disallowed origin")
    public void shouldNotAllowNotificationForDisallowedOrigin() {
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.name());
        onboarding.setPricingPlan(PricingPlan.FA.name());
        InstitutionResponse institution = new InstitutionResponse();
        institution.setInstitutionType(InstitutionResponse.InstitutionTypeEnum.PA);
        institution.setOrigin("INFOCAMERE");

        assertFalse(sapNotificationFilter.shouldSendNotification(onboarding, institution));
    }

    @Test
    @DisplayName("Should not allow notification for disallowed product (prodIo not Io Fast)")
    public void shouldNotAllowNotificationForDisallowedProduct() {
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(ProductId.PROD_IO.name());
        onboarding.setPricingPlan(PricingPlan.BASE.name());
        InstitutionResponse institution = new InstitutionResponse();

        assertFalse(sapNotificationFilter.shouldSendNotification(onboarding, institution));
    }
}