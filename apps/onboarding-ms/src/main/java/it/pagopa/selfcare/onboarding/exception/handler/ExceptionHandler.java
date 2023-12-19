package it.pagopa.selfcare.onboarding.exception.handler;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";

    @ServerExceptionMapper
    public RestResponse<String> toResponse(InvalidRequestException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }
    @ServerExceptionMapper
    public RestResponse<String> toResponse(OnboardingNotAllowedException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> toResponse(Exception exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
    }
    @ServerExceptionMapper
    public RestResponse<String> toResponse(ResourceNotFoundException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.NOT_FOUND, exception.getMessage());
    }
}
