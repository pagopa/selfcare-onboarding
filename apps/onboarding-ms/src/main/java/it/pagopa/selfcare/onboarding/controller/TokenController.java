package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Authenticated
@Path("/tokens")
@AllArgsConstructor
public class TokenController {
    
    @PUT
    @Path("/{tokenId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<Response> complete(@PathParam(value = "tokenId") String tokenId,
                                            @RestForm("contract") FileUpload file) {

        return Uni.createFrom().item(Response
                .status(HttpStatus.SC_NO_CONTENT)
                .build());
    }
}
