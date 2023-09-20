package it.pagopa.selfcare.exception.handler;

import it.pagopa.selfcare.exception.InvalidRequestException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";

    @ServerExceptionMapper
    public RestResponse<String> toResponse(InvalidRequestException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> toResponse(Exception exception) {
        exception.printStackTrace();
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
    }
}
