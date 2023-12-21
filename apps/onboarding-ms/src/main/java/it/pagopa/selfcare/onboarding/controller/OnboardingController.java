package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

@Authenticated
@Path("/v1/onboarding")
@AllArgsConstructor
public class OnboardingController {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    private final OnboardingService onboardingService;

    @Operation(summary = "Perform default onboarding request, it is used for GSP/SA/AS institution type." +
            "Users data will be saved on personal data vault if it doesn't already exist." +
            "At the end, function triggers async activities related to onboarding based on institution type.")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboarding(@Valid OnboardingDefaultRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboarding(onboardingRequest));
    }


    @Operation(summary = "Perform onboarding request for PA institution type, it require billing.recipientCode in additition to default request" +
            "Users data will be saved on personal data vault if it doesn't already exist." +
            "At the end, function triggers async activities related to onboarding that consist of create contract and sending mail to institution's digital address.")
    @POST
    @Path("/pa")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPa(@Valid OnboardingPaRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboardingPa(onboardingRequest));
    }

    @Operation(summary = "Perform onboarding request for PSP institution type." +
            "Users data will be saved on personal data vault if it doesn't already exist." +
            "At the end, function triggers async activities related to onboarding that consist of sending mail to Selfcare admin for approve request.")
    @POST
    @Path("/psp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingResponse> onboardingPsp(@Valid OnboardingPspRequest onboardingRequest, @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().invoke(onboardingRequest::setUserRequestUid)
                .onItem().transformToUni(ignore -> onboardingService.onboardingPsp(onboardingRequest));
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


    @Operation(summary = "Perform complete operation of an onboarding request receiving onboarding id and contract signed by hte institution." +
            "It checks the contract's signature and upload the contract on an azure storage" +
            "At the end, function triggers async activities related to complete onboarding " +
            "that consist of create the institution, activate the onboarding and sending data to notification queue.")

    @PUT
    @Path("/{onboardingId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> complete(@PathParam(value = "onboardingId") String onboardingId, @RestForm("contract") File file) {
        return onboardingService.complete(onboardingId, file)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(summary = "The API retrieves paged onboarding using optional filter, order by descending creation date")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<OnboardingGetResponse> getOnboardingWithFilter(@QueryParam(value = "productId") String productId,
                                                              @QueryParam(value = "taxCode") String taxCode,
                                                              @QueryParam(value = "from") String from,
                                                              @QueryParam(value = "to") String to,
                                                              @QueryParam(value = "status") String status,
                                                              @QueryParam(value = "page") @DefaultValue("0") Integer page,
                                                              @QueryParam(value = "size") @DefaultValue("20") Integer size) {
        return onboardingService.onboardingGet(productId, taxCode, status, from, to, page, size);
    }

    @PUT
    @Path("/{onboardingId}/delete")
    public Uni<Response> delete(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.deleteOnboarding(onboardingId)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(summary = "Retrieve an onboarding record given its ID")
    @GET
    @Path("/{onboardingId}")
    public Uni<OnboardingGet> getById(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingGet(onboardingId);
    }

    @Operation(summary = "Retrieve an onboarding record given its ID adding to user sensitive information")
    @GET
    @Path("/{onboardingId}/withUserInfo")
    public Uni<OnboardingGet> getByIdWithUserInfo(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingGetWithUserInfo(onboardingId);
    }
    @Operation(summary = "Returns an onboarding record by its ID only if its status is PENDING. " +
            "This feature is crucial for ensuring that the onboarding process can be completed only when " +
            "the onboarding status is appropriately set to PENDING.")
    @GET
    @Path("/{onboardingId}/pending")
    public Uni<OnboardingGet> getOnboardingPending(@PathParam(value = "onboardingId") String onboardingId) {
        return onboardingService.onboardingPending(onboardingId);
    }


}
