package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.TokenResponse;
import it.pagopa.selfcare.onboarding.mapper.TokenMapper;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Authenticated
@Path("/v1/tokens")
@AllArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final TokenMapper tokenMapper;

    /**
     * Retrieves the token for a given onboarding
     *
     * @param onboardingId onboarding's unique identifier
     * @return The token
     * * Code: 200, Message: successful operation, DataType: TokenId
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Token not found, DataType: Problem
     */

    @Operation(summary = "Retrieves the token for a given onboarding")
    @GET
    public Uni<List<TokenResponse>> getToken(@QueryParam(value = "onboardingId") String onboardingId){
        return tokenService.getToken(onboardingId)
                .map(tokens -> tokens.stream()
                        .map(tokenMapper::toResponse)
                        .toList());
    }

    @Operation(summary = "Retrieves the token for a given onboarding")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{onboardingId}/contract")
    public Uni<Response> getContract(@PathParam(value = "onboardingId") String onboardingId){
        return tokenService.retrieveContractNotSigned(onboardingId);
    }
}
