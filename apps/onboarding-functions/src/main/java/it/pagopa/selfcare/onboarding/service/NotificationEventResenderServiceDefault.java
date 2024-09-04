package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.runtime.util.ExceptionUtil;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            try {
                if(onboardingHasBeenDeletedInRange(onboarding, filters.getFrom(), filters.getTo())) {
                    notificationEventService.send(context, onboarding, QueueEvent.UPDATE);
                }

                if(onboardingHasBeenActivatedInRange(onboarding, filters.getFrom(), filters.getTo())) {
                    onboarding.setStatus(OnboardingStatus.COMPLETED);
                    notificationEventService.send(context, onboarding, QueueEvent.ADD);
                }
            } catch (Exception e) {
                context.getLogger().severe(() -> String.format("ERROR: Sending onboarding %s error: %s ", onboarding.getId(), ExceptionUtil.generateStackTrace(e)));
            }
        }

        if(onboardingsToResend.isEmpty() || onboardingsToResend.size() < pageSize) {
            context.getLogger().info(() -> String.format(RESEND_ENDING_LOG_LAST_PAGE, filters.getPage()));
            return null;
        }

        context.getLogger().info(() -> String.format(RESEND_ENDING_LOG, filters.getPage()));

        filters.setPage(page + 1);
        return filters;
    }

    private boolean onboardingHasBeenDeletedInRange(Onboarding onboarding, String from, String to) {
        if(onboarding.getStatus() != OnboardingStatus.DELETED) {
            return false;
        }

        return doesDateFallInRange(onboarding.getDeletedAt(), from, to);
    }

    private boolean onboardingHasBeenActivatedInRange(Onboarding onboarding, String from, String to) {
        return doesDateFallInRange(onboarding.getActivatedAt(), from, to);
    }

    private static boolean doesDateFallInRange(LocalDateTime date, String from, String to) {
        if(date == null) {
            return false;
        }

        LocalDate fromDate = StringUtils.isNotBlank(from) ? LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE) : null;
        LocalDate toDate = StringUtils.isNotBlank(to) ? LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE) : null;
        return (fromDate == null || date.isEqual(fromDate.atStartOfDay()) || date.isAfter(fromDate.atStartOfDay())) &&
                (toDate == null || date.isEqual(toDate.atStartOfDay()) || date.isBefore(toDate.plusDays(1).atStartOfDay()));
    }


}
