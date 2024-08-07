package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class NotificationEventResenderServiceDefault implements NotificationEventResenderService {
    private final NotificationEventService notificationEventService;
    private final OnboardingService onboardingService;

    private static final String RESEND_ENDING_LOG = "Resend notifications for page %s completed";
    private static final String RESEND_ENDING_LOG_LAST_PAGE = "There aren't more notifications to resend, page %s completed";


    public NotificationEventResenderServiceDefault(NotificationEventService notificationEventService, OnboardingService onboardingService) {
        this.notificationEventService = notificationEventService;
        this.onboardingService = onboardingService;
    }

    public ResendNotificationsFilters resendNotifications(ResendNotificationsFilters filters, ExecutionContext context) {
        context.getLogger().info(() -> "resendNotifications started with filters: " + filters);

        int page = Optional.ofNullable(filters.getPage()).orElse(0);
        int pageSize = 100;

        List<Onboarding> onboardingsToResend = onboardingService.getOnboardingsToResend(filters, page, pageSize);
        context.getLogger().info(() -> String.format("Found: %s onboardings to send for page: %s ", onboardingsToResend.size(), page));
        for (Onboarding onboarding : onboardingsToResend) {
            notificationEventService.send(context, onboarding, null, filters.getNotificationEventTraceId());
        }

        if(onboardingsToResend.isEmpty() || onboardingsToResend.size() < pageSize) {
            context.getLogger().info(() -> String.format(RESEND_ENDING_LOG_LAST_PAGE, filters.getPage()));
            return null;
        }

        context.getLogger().info(() -> String.format(RESEND_ENDING_LOG, filters.getPage()));

        filters.setPage(page + 1);
        return filters;
    }
}
