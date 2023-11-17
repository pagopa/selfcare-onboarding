package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/onboarding")
@AllArgsConstructor
public class OnboardingController {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    final private OnboardingService onboardingService;
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboarding(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService.onboarding(onboardingRequest));
    }

    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPa(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService.onboardingPa(onboardingRequest));
    }

    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService.onboardingPsp(onboardingRequest));
    }

    @POST
    @Path("/sa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingSa(@Valid OnboardingSaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService.onboardingSa(onboardingRequest));
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


    private Uni<String> readUserIdFromToken(SecurityContext ctx) {

        return currentIdentityAssociation.getDeferredIdentity()
                .onItem().transform(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        throw new InternalServerErrorException("Principal and JsonWebToken names do not match");
                    }

                    if(identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        return jwtCallerPrincipal.getClaim("uid");
                    }

                    return null;
                });
    }
}
