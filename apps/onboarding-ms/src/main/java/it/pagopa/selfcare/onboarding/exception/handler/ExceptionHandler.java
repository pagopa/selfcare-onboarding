package it.pagopa.selfcare.onboarding.exception.handler;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.model.Problem;
import it.pagopa.selfcare.onboarding.exception.model.ProblemError;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";

    @ServerExceptionMapper
    public Response toResponse(InvalidRequestException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = problem(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode(), exception.getCode());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(problem)
                .build();
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
    @ServerExceptionMapper
    public RestResponse<String> toResponse(ResourceConflictException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.CONFLICT, exception.getMessage());
    }

    private Problem problem(String errorMessage, Integer status, String code) {
        Problem problem = new Problem();
        problem.setStatus(status);
        problem.setErrors(List.of(ProblemError.builder()
                .code(code)
                .detail(errorMessage)
                .build()));
        return problem;
    }
}
