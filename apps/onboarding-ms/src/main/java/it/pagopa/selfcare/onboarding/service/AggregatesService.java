package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.*;

import java.io.File;

public interface AggregatesService {

    Uni<VerifyAggregateResponse> validateAppIoAggregatesCsv (File file);

    Uni<VerifyAggregateResponse> validateSendAggregatesCsv (File file);

}
