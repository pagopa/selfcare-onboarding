package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@ApplicationScoped
public class NotificationEventResenderServiceDefault implements NotificationEventResenderService {
    private final NotificationEventService notificationEventService;
    private final OnboardingService onboardingService;

    private static final String RESEND_ENDING_LOG = "Resend notifications for page %s completed with %s notifications sent successfully and %s notifications not sent";
    private static final String RESEND_ENDING_LOG_LAST_PAGE = "There aren't more notifications to resend, page %s completed with %s notifications sent successfully and %s notifications not sent";


    public NotificationEventResenderServiceDefault(NotificationEventService notificationEventService, OnboardingService onboardingService) {
        this.notificationEventService = notificationEventService;
        this.onboardingService = onboardingService;
    }

    public ResendNotificationsFilters resendNotifications(ResendNotificationsFilters filters, ExecutionContext context) {
        context.getLogger().info("resendNotifications started with filters: " + filters.toString());

        int page = Optional.ofNullable(filters.getPage()).orElse(0);
        int pageSize = 100;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        List<Onboarding> onboardingsToResend = onboardingService.getOnboardingsToResend(filters, page, pageSize);
        context.getLogger().info(String.format("Found: %s onboardings to send for page: %s ", onboardingsToResend.size(), page));

        for (Onboarding onboarding : onboardingsToResend) {
            try {
                notificationEventService.send(context, onboarding, null);
                notificationsSent++;
            } catch (NotificationException e) {
                context.getLogger().log(Level.WARNING, e, () -> String.format("Error resending notification for onboarding with ID %s. Error: %s", onboarding.getId(), e.getMessage()));
                notificationsFailed++;
            }
        }

        if(onboardingsToResend.isEmpty() || onboardingsToResend.size() < pageSize) {
            context.getLogger().info(String.format(RESEND_ENDING_LOG_LAST_PAGE, filters.getPage(), notificationsSent, notificationsFailed));
            return null;
        }

        context.getLogger().info(String.format(RESEND_ENDING_LOG, filters.getPage(), notificationsSent, notificationsFailed));

        filters.setPage(page + 1);
        return filters;
    }
}
