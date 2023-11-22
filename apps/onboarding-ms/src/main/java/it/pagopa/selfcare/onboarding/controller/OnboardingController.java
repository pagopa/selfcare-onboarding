package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

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
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboarding(onboardingRequest));
    }

    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPa(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboardingPa(onboardingRequest));
    }

    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboardingPsp(onboardingRequest));
    }

    @POST
    @Path("/sa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingSa(@Valid OnboardingSaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboardingSa(onboardingRequest));
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
                .onItem().transformToUni(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        return Uni.createFrom().failure(new InternalServerErrorException("Principal and JsonWebToken names do not match"));
                    }

                    if(identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        String uid = jwtCallerPrincipal.getClaim("uid");
                        return Uni.createFrom().item(uid);
                    }

                    return Uni.createFrom().nullItem();
                });
    }



    @PUT
    @Path("/{onboardingId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> complete(@PathParam(value = "onboardingId") String tokenId,
                                  @RestForm("contract") FileUpload file) {

        return onboardingService.complete(tokenId, file.uploadedFile().toFile(), file.contentType())
                .onItem().transform(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }
}
