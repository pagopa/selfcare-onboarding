package it.pagopa.selfcare.controller;

import it.pagopa.selfcare.controller.request.OnboardingRequest;
import it.pagopa.selfcare.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;

@Path("/onboarding")
@AllArgsConstructor
public class OnboardingController {

    final private OnboardingService onboardingService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String onboarding(@Valid OnboardingRequest onboardingInstitutionRequest) {
        return "Hello from RESTEasy Reactive";
    }
}
