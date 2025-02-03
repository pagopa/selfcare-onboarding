package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPgRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Authenticated
@Path("/v2/onboarding")
@Tag(name = "Onboarding Controller")
@AllArgsConstructor
@Slf4j
public class OnboardingV2Controller {

    private final OnboardingService onboardingService;
    private final OnboardingMapper onboardingMapper;
    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @Operation(
            summary = "Complete onboarding request and set status to COMPLETED.",
            description = "Perform onboarding as /onboarding but completing the onboarding request to COMPLETED phase."
    )
    @Path("/completion")
    @POST
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingCompletion(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers()));
    }

    @Operation(
            summary = "Complete PA onboarding request and set status to COMPLETED.",
            description =
                    "Perform onboarding as /onboarding/pa but completing the onboarding request to COMPLETED phase.")
    @POST
    @Path("/pa/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPaCompletion(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers()));
    }


    @Operation(
            summary = "Complete PSP onboarding request and set status to COMPLETED.",
            description =
                    "Perform onboarding as /onboarding/psp but completing the onboarding request to COMPLETED phase.")
    @POST
    @Path("/psp/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-v1")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPspCompletion(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers()));
    }

    @Operation(
            summary = "Complete PG onboarding request on PNPG domain and set status to COMPLETED.",
            description = "Perform onboarding as /onboarding/psp but completing the onboarding request to COMPLETED phase."
    )
    @POST
    @Path("/pg/completion")
    @Tag(name = "Onboarding Controller")
    @Tag(name = "internal-pnpg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPgCompletion(@Valid OnboardingPgRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(userId -> onboardingService
                        .onboardingPgCompletion(fillUserId(onboardingMapper.toEntity(onboardingRequest), userId), onboardingRequest.getUsers()));
    }


    private Uni<String> readUserIdFromToken(SecurityContext ctx) {
        return currentIdentityAssociation.getDeferredIdentity()
                .onItem().transformToUni(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        return Uni.createFrom().failure(new InternalServerErrorException("Principal and JsonWebToken names do not match"));
                    }

                    if (identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        String uid = jwtCallerPrincipal.getClaim("uid");
                        return Uni.createFrom().item(uid);
                    }

                    return Uni.createFrom().nullItem();
                });
    }


    @Operation(
            summary = "Get onboardings by institution taxCode, subunitCode, origin, or originId.",
            description = "Returns onboardings record by institution taxCode/subunitCode/origin/originId",
            operationId = "onboardingInstitutionUsingGET",
            extensions = @Extension(name = "x-legacy-api", value = "true")
    )
    @GET
    @Tag(name = "support")
    @Tag(name = "internal-v1")
    @Tag(name = "Onboarding")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/institutionOnboardings")
    public Uni<List<OnboardingResponse>> getOnboardingPending(@QueryParam(value = "taxCode") String taxCode,
                                                              @QueryParam(value = "subunitCode") String subunitCode,
                                                              @QueryParam(value = "origin") String origin,
                                                              @QueryParam(value = "originId") String originId,
                                                              @QueryParam(value = "status") OnboardingStatus status) {
        return onboardingService.institutionOnboardings(taxCode, subunitCode, origin, originId, status);
    }

    private Onboarding fillUserId(Onboarding onboarding, String userRequestUid) {
        onboarding.setUserRequestUid(userRequestUid);
        return onboarding;
    }
    
}
