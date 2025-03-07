package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import java.util.List;

public interface InstitutionService {

    Multi<InstitutionResponse> getInstitutions(List<String> institutionIds);
}
