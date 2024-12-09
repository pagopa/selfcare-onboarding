package it.pagopa.selfcare.onboarding.controller;


import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Path("/v1/document")
@Tag(name = "Document Controller")
@AllArgsConstructor
@Slf4j
public class DocumentController {

    @Inject
    AzureBlobClient blobClient;

    /**
     * Retrieves the list of files by the path on the blob
     *
     * @param path path of the folder into azure storage
     * @return List of files on the storage * Code: 200, Message: successful operation * Code: 400, Message: Invalid ID supplied * Code: 404, Message:
     * Path not found
     */

    @Operation(
        summary = "Retrieves the list of files on the azure storage on the given path",
        description = "Fetches a list of files associated with the specified path on the storage."
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list-file")
    public Uni<List<String>> getFiles(@QueryParam(value = "path") String path) {
        return Uni.createFrom().item(blobClient.getFiles(path));
    }

}
