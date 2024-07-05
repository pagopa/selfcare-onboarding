package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;

import java.io.File;

public interface AggregatesService {

    Uni<VerifyAggregateResponse> validateAggregatesCsv(File file);
}
