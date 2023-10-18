package it.pagopa.selfcare.onboarding.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
