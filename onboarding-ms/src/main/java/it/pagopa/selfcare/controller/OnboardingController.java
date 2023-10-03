package it.pagopa.selfcare.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.controller.request.*;
import it.pagopa.selfcare.controller.response.OnboardingResponse;
import it.pagopa.selfcare.mapper.OnboardingMapper;
import it.pagopa.selfcare.service.OnboardingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;

//@RolesAllowed("USER")
@Path("/onboarding")
@AllArgsConstructor
public class OnboardingController {

    final private OnboardingService onboardingService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboarding(@Valid OnboardingDefaultRequest onboardingRequest) {
        return onboardingService.onboarding(onboardingRequest);
    }

    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPa(@Valid OnboardingPaRequest onboardingRequest) {
        return onboardingService.onboardingPa(onboardingRequest);
    }

    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest) {
        return onboardingService.onboardingPsp(onboardingRequest);
    }

    @POST
    @Path("/sa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingSa(@Valid OnboardingSaRequest onboardingRequest) {
        return onboardingService.onboardingSa(onboardingRequest);
    }

    /**
     * Onboarding pg may be excluded from the async onboarding flow
     * Institutions may be saved without passing from onboarding
     *
    @POST
    @Path("/pg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPg(@Valid OnboardingPgRequest onboardingRequest) {
        return onboardingService.onboarding(onboardingMapper.toEntity(onboardingRequest))
                .map(onboardingMapper::toResponse);
    }*/
}
