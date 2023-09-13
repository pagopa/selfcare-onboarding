package it.pagopa.selfcare.controller;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.controller.request.OnboardingPgRequest;
import it.pagopa.selfcare.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.mapper.OnboardingMapper;
import it.pagopa.selfcare.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;

@Path("/onboarding")
@AllArgsConstructor
public class OnboardingController {

    final private OnboardingService onboardingService;
    final private OnboardingMapper onboardingMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> onboarding(@Valid OnboardingDefaultRequest onboardingRequest) {
        onboardingService.onboarding(onboardingMapper.toEntity(onboardingRequest));
        return Uni.createFrom().item("Hello from RESTEasy Reactive");
    }

    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> onboardingPa(@Valid OnboardingPaRequest onboardingRequest) {
        onboardingService.onboarding(onboardingMapper.toEntity(onboardingRequest));
        return Uni.createFrom().item("Hello from RESTEasy Reactive");
    }

    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest) {
        onboardingService.onboarding(onboardingMapper.toEntity(onboardingRequest));
        return Uni.createFrom().item("Hello from RESTEasy Reactive");
    }

    @POST
    @Path("/pg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> onboardingPg(@Valid OnboardingPgRequest onboardingRequest) {
        onboardingService.onboarding(onboardingMapper.toEntity(onboardingRequest));
        return Uni.createFrom().item("Hello from RESTEasy Reactive");
    }
}
