package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.*;

import java.io.File;

public interface AggregatesService {

    Uni<VerifiyAggregateResponseInterface<AggregateAppIo>> validateAppIoAggregatesCsv (File file);

    Uni<VerifyAggregateSendResponse> validateSendAggregatesCsv (File file);

    Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv (File file);
}
