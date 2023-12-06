package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_INTEROP;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.utils.PdfMapper.workContactsKey;

@ApplicationScoped
public class CompletionServiceDefault implements CompletionService {


    @RestClient
    @Inject
    InstitutionApi institutionApi;
    @RestClient
    @Inject
    UserApi userRegistryApi;

    @Inject
    InstitutionMapper institutionMapper;

    @Inject
    OnboardingRepository onboardingRepository;

    @Inject
    UserMapper userMapper;

    @Override
    public String createInstitutionAndPersistInstitutionId(Onboarding onboarding) {

        Institution institution = onboarding.getInstitution();

        InstitutionsResponse institutionsResponse = institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), institution.getSubunitCode(), null, null);
        if(Objects.nonNull(institutionsResponse.getInstitutions()) && institutionsResponse.getInstitutions().size() > 1){
            throw new GenericOnboardingException("List of institutions is ambiguous, it is empty or has more than one element!!");
        }

        InstitutionResponse institutionResponse =
                Objects.isNull(institutionsResponse.getInstitutions()) || institutionsResponse.getInstitutions().isEmpty()
                    ? createInstitution(institution, onboarding.getProductId())
                    : institutionsResponse.getInstitutions().get(0);

        onboardingRepository
                .update("institution.id", institutionResponse.getId())
                .where("_id", onboarding.getOnboardingId());

        return institutionResponse.getId();
    }

    /**
     * Function that creates institution based on institution type and Origin,
     * Origin indicates which is the indexes where data come from, for ex. IPA comes from index of Pubbliche Amministrazioni
     * Look at https://pagopa.atlassian.net/wiki/spaces/SCP/pages/708804909/Glossario for more information about institution type and indexes
     */
    private InstitutionResponse createInstitution(Institution institution, String productId) {

        if(InstitutionType.SA.equals(institution.getInstitutionType())
                && Origin.ANAC.equals(institution.getOrigin())) {

            return institutionApi.createInstitutionFromAnacUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if(InstitutionType.AS.equals(institution.getInstitutionType())
                && Origin.IVASS.equals(institution.getOrigin())) {

            return institutionApi.createInstitutionFromIvassUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if(InstitutionType.PG.equals(institution.getInstitutionType()) &&
                (Origin.INFOCAMERE.equals(institution.getOrigin()) || Origin.ADE.equals(institution.getOrigin()))) {

            return institutionApi.createInstitutionFromInfocamereUsingPOST(institutionMapper.toInstitutionRequest(institution));
        }

        if(InstitutionType.PA.equals(institution.getInstitutionType()) ||
                InstitutionType.SA.equals(institution.getInstitutionType()) ||
                (isGspAndProdInterop(institution.getInstitutionType(), productId)
                        && Origin.IPA.equals(institution.getOrigin()))) {

            InstitutionFromIpaPost fromIpaPost = new InstitutionFromIpaPost();
            fromIpaPost.setTaxCode(institution.getTaxCode());
            if(Objects.nonNull(institution.getSubunitType())) {
                fromIpaPost.setSubunitCode(institution.getSubunitCode());
                fromIpaPost.setSubunitType(InstitutionFromIpaPost.SubunitTypeEnum.valueOf(institution.getSubunitType().name()));
            }
            return  institutionApi.createInstitutionFromIpaUsingPOST(fromIpaPost);
        }

        return institutionApi.createInstitutionUsingPOST1(institutionMapper.toInstitutionRequest(institution));
    }

    private boolean isGspAndProdInterop(InstitutionType institutionType, String productId) {
        return InstitutionType.GSP == institutionType
                && productId.equals(PROD_INTEROP.getValue());
    }


    @Override
    public void persistOnboarding(Onboarding onboarding) {
        //Prepare data for request
        InstitutionOnboardingRequest onboardingRequest = new InstitutionOnboardingRequest();
        onboardingRequest.setUsers(onboarding.getUsers().stream()
                .map(user -> {
                    UserResource userResource = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, user.getId());
                    String mailWork = Optional.ofNullable(userResource.getWorkContacts())
                            .map(worksContract -> worksContract.get(workContactsKey.apply(onboarding.getOnboardingId())))
                            .map(WorkContactResource::getEmail)
                            .map(CertifiableFieldResourceOfstring::getValue)
                            .orElseThrow(() -> new GenericOnboardingException("Work contract not found!"));
                    Person person = userMapper.toPerson(userResource);
                    person.setEmail(mailWork);
                    person.setProductRole(user.getProductRole());
                    person.setRole(Person.RoleEnum.valueOf(user.getRole().name()));
                    return person;
                })
                .toList()
        );
        onboardingRequest.pricingPlan(onboarding.getPricingPlan());
        onboardingRequest.productId(onboarding.getProductId());

        if(Objects.nonNull(onboarding.getBilling())) {
            BillingRequest billingRequest = new BillingRequest();
            billingRequest.recipientCode(onboarding.getBilling().getRecipientCode());
            billingRequest.publicServices(onboarding.getBilling().isPublicServices());
            billingRequest.vatNumber(onboarding.getBilling().getVatNumber());
            onboardingRequest.billing(billingRequest);
        }

        institutionApi.onboardingInstitutionUsingPOST(onboarding.getInstitution().getId(), onboardingRequest);
    }
}
