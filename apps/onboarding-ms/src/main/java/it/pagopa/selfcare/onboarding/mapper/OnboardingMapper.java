package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPaRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingPspRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingSaRequest;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    @Mapping(target = "workflowType", source = ".", qualifiedByName = "getWorkflowType")
    Onboarding toEntity(OnboardingPaRequest request);
    Onboarding toEntity(OnboardingPspRequest request);
    Onboarding toEntity(OnboardingDefaultRequest request);
    Onboarding toEntity(OnboardingSaRequest request);

    //@Mapping(source = "taxCode", target = "institution.taxCode")
    //@Mapping(source = "businessName", target = "institution.description")
    //@Mapping(source = "digitalAddress", target = "institution.digitalAddress")
    //Onboarding toEntity(OnboardingPgRequest request);

    OnboardingResponse toResponse(Onboarding onboarding);

    @Named("getWorkflowType")
    default WorkflowType getWorkflowType(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        if(InstitutionType.PT.equals(institutionType)){
            return WorkflowType.FOR_APPROVE_PT;
        }

        if(InstitutionType.PA.equals(institutionType)
                || checkIfGspProdInterop(institutionType, onboarding.getProductId())
                || InstitutionType.SA.equals(institutionType)
                || InstitutionType.AS.equals(institutionType)) {
            return WorkflowType.CONTRACT_REGISTRATION;
        }

        if(InstitutionType.PG.equals(institutionType)) {
            return WorkflowType.CONFIRMATION;
        }

        return WorkflowType.FOR_APPROVE;
    }

    private boolean checkIfGspProdInterop(InstitutionType institutionType, String productId) {
        return InstitutionType.GSP == institutionType
                && productId.equals(PROD_INTEROP.getValue());
    }
}
