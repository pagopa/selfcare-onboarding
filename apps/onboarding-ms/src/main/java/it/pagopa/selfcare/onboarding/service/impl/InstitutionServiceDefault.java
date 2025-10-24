package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.util.QueryUtils.FieldNames.INSTITUTION_ID;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;


@ApplicationScoped
@Slf4j
public class InstitutionServiceDefault implements InstitutionService {

    @Override
    public Multi<InstitutionResponse> getInstitutions(List<String> institutionIds) {
        if (Objects.isNull(institutionIds) || institutionIds.isEmpty()) {
            log.error("The parameter institutionIds cannot be null or empty");
            return Multi.createFrom().empty();
        }
        Map<String, Object> queryParameterMap = new HashMap<>();
        queryParameterMap.put(INSTITUTION_ID, institutionIds);
        Document query = QueryUtils.buildQuery(queryParameterMap);
        return getDistinctOnboardings(query).onItem().transform(onboarding -> {
            InstitutionResponse institutionResponse = new InstitutionResponse();
            institutionResponse.setId(onboarding.getInstitution().getId());
            institutionResponse.setInstitutionType(onboarding.getInstitution().getInstitutionType().name());
            institutionResponse.setDescription(onboarding.getInstitution().getDescription());
            return institutionResponse;
        });
    }

    public Multi<Onboarding> getDistinctOnboardings(Document query) {
        Set<String> seenInstitutionIds = new HashSet<>();
        return Onboarding.find(query).stream().map(Onboarding.class::cast)
                .filter(onboarding -> seenInstitutionIds.add(onboarding.getInstitution().getId()));
    }
}
