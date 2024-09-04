package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.service.AggregatesService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

@Authenticated
@Path("/v1/aggregates")
@Tag(name = "Aggregates Controller")
@AllArgsConstructor
public class AggregatesController {

    @Inject
    AggregatesService aggregatesService;

    @Operation(summary = "Validate the data related to the aggregated entities present in a CSV file")
    @POST
    @Path("/verification/appio")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifyAppIoAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validateAppIoAggregatesCsv(file);
    }

    @Operation(summary = "Validate the data related to the aggregated entities present in a CSV file")
    @POST
    @Path("/verification/send")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifySendAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validateSendAggregatesCsv(file);
    }

    @Operation(summary = "Validate the data related to the aggregated entities present in a CSV file")
    @POST
    @Path("/verification/pagopa")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<VerifyAggregateResponse> verifyPagoPaAggregatesCsv(@NotNull @RestForm("aggregates") File file){

        return aggregatesService.validatePagoPaAggregatesCsv(file);
    }


}
