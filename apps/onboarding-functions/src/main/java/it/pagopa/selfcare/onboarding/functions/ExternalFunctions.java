package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.selfcare.onboarding.service.CheckOrganizationService;

import java.util.Optional;

public class ExternalFunctions {
    private final CheckOrganizationService checkOrganizationService;

    public ExternalFunctions(CheckOrganizationService checkOrganizationService) {
        this.checkOrganizationService = checkOrganizationService;
    }

    @FunctionName("TestToken")
    public HttpResponseMessage testToken(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("testToken trigger processed a request");

        String accessToken = checkOrganizationService.testToken(context);

        return request.createResponseBuilder(HttpStatus.OK).body(accessToken).build();
    }

    @FunctionName("CheckOrganization")
    public HttpResponseMessage checkOrganization(
            @HttpTrigger(name = "req", methods = {HttpMethod.HEAD}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("testToken trigger processed a request");

        String fiscalCode = request.getQueryParameters().get("fiscalCode");
        String vatNumber = request.getQueryParameters().get("vatNumber");

        if (fiscalCode == null || vatNumber == null) {
            context.getLogger().warning("fiscalCode, vatNumber are required.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("fiscalCode, vatNumber are required.")
                    .build();
        }

        boolean alreadyRegistered = checkOrganizationService.checkOrganization(context, fiscalCode, vatNumber);

        if (alreadyRegistered) {
            return request.createResponseBuilder(HttpStatus.OK).build();
        } else {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        }
    }
}
