package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.PricingPlan;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class SapNotificationFilter implements SendNotificationFilter {
    private final Set<String> allowedInstitutionType;

    private final Set<String> allowedOrigins;

    public SapNotificationFilter(NotificationConfig notificationConfig) {
        this.allowedInstitutionType = notificationConfig.filter().sapAllowedInstitutionTypes();
        this.allowedOrigins = notificationConfig.filter().sapAllowedOrigins();
    }

    @Override
    public boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution) {
        return isProductAllowed(onboarding) && isAllowedInstitutionType(institution) && isAllowedOrigin(institution.getOrigin());
    }

    private boolean isProductAllowed(Onboarding onboarding) {
        // If the product is prodIo we can allow only Io Fast, and to do so we need to check pricing plan
        boolean isProdIo = ProductId.PROD_IO.name().equals(onboarding.getProductId());
        return !isProdIo || PricingPlan.FA.name().equals(onboarding.getPricingPlan());
    }

    private boolean isAllowedInstitutionType(InstitutionResponse institution) {
        return isNullOrEmpty(allowedInstitutionType) || allowedInstitutionType.contains(institution.getInstitutionType().name());
    }

    private boolean isAllowedOrigin(String origin) {
        return isNullOrEmpty(allowedOrigins) || allowedOrigins.contains(origin);
    }

    private boolean isNullOrEmpty(Set<String> set) {
        return Objects.isNull(set) || set.isEmpty();
    }
}
