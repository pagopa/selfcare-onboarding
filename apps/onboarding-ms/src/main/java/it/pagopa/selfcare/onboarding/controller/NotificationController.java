package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.NotificationService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Path("/v1/notification")
@Tag(name = "Notification Controller")
@AllArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    @Operation(
            summary = "Resend onboarding notifications for onboarding which are retrieved given a set of filters",
            description = "Resends notifications for all onboarding records that match the specified filter criteria. This allows administrators to trigger notification processes again for specific onboardings based on parameters such as product ID, tax code, institution ID, and more."
    )
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/resend")
    public Uni<String> resendOnboardingNotifications(OnboardingGetFilters filters) {
        return notificationService.resendOnboardingNotifications(filters)
                .replaceWith("Request taken charge");
    }
}
