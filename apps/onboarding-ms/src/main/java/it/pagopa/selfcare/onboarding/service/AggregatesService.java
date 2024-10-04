package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateAppIoResponse;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateSendResponse;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;

public interface AggregatesService {

    Uni<VerifyAggregateAppIoResponse> validateAppIoAggregatesCsv (File file);

    Uni<VerifyAggregateSendResponse> validateSendAggregatesCsv (File file);

    Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv (File file);

    Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId);

}
