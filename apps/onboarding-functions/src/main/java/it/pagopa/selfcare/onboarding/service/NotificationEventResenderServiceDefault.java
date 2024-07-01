package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.logging.Level;

@ApplicationScoped
public class NotificationEventResenderServiceDefault implements NotificationEventResenderService {
    private final NotificationEventService notificationEventService;
    private final OnboardingService onboardingService;

    public NotificationEventResenderServiceDefault(NotificationEventService notificationEventService, OnboardingService onboardingService) {
        this.notificationEventService = notificationEventService;
        this.onboardingService = onboardingService;
    }

    public void resendNotifications(ResendNotificationsFilters filters, ExecutionContext context) {
        context.getLogger().info("Resend notifications started with filters: " + filters.toString());
        long start = System.currentTimeMillis();
        int page = 0;
        int pageSize = 100;
        boolean thereAreNotificationsToSend = true;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        while (thereAreNotificationsToSend) {
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
                context.getLogger().info("No more notifications to resend");
                thereAreNotificationsToSend = false;
            }
            page++;
        }

        context.getLogger().info(String.format("Resend notifications completed successfully in: %s ms with %s notifications sent successfully and %s notifications not sent ", System.currentTimeMillis() - start, notificationsSent, notificationsFailed));
    }
}
