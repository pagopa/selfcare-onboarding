package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
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
    @RestClient
    @Inject
    AooApi aooApi;
    @RestClient
    @Inject
    UoApi uoApi;
    @RestClient
    @Inject
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;

    @Inject
    InstitutionMapper institutionMapper;

    @Inject
    OnboardingRepository onboardingRepository;

    @Inject
    TokenRepository tokenRepository;

    @Inject
    UserMapper userMapper;
    @Inject
    NotificationService notificationService;
    @Inject
    ProductService productService;

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

        if(isInstitutionPresentOnIpa(institution)) {

            InstitutionFromIpaPost fromIpaPost = new InstitutionFromIpaPost();
            fromIpaPost.setTaxCode(institution.getTaxCode());
            fromIpaPost.setGeographicTaxonomies(Optional.ofNullable(institution.getGeographicTaxonomies())
                    .map(geographicTaxonomies -> geographicTaxonomies.stream().map(institutionMapper::toGeographicTaxonomy).toList())
                    .orElse(List.of()));
            if(Objects.nonNull(institution.getSubunitType())) {
                fromIpaPost.setSubunitCode(institution.getSubunitCode());
                fromIpaPost.setSubunitType(InstitutionFromIpaPost.SubunitTypeEnum.valueOf(institution.getSubunitType().name()));
            }
            return  institutionApi.createInstitutionFromIpaUsingPOST(fromIpaPost);
        }

        return institutionApi.createInstitutionUsingPOST1(institutionMapper.toInstitutionRequest(institution));
    }

    private boolean isInstitutionPresentOnIpa(Institution institution) {
        try {
            if (institution.getSubunitType() != null && institution.getSubunitType() == InstitutionPaSubunitType.AOO) {
                aooApi.findByUnicodeUsingGET(institution.getSubunitCode(), null);
            } else if (institution.getSubunitType() != null && institution.getSubunitType() == InstitutionPaSubunitType.UO) {
                uoApi.findByUnicodeUsingGET1(institution.getSubunitCode(), null);
            } else {
                institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null, null);
            }
            return true;
        } catch (WebApplicationException e) {
            if(e.getResponse().getStatus() == 404) {
                return false;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendCompletedEmail(Onboarding onboarding) {

        String workContractId = workContactsKey.apply(onboarding.getOnboardingId());

        List<String> destinationMails = onboarding.getUsers().stream()
                .filter(user -> MANAGER.equals(user.getRole()))
                .map(userToOnboard -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId()))
                .filter(user -> Objects.nonNull(user.getWorkContacts())
                        && user.getWorkContacts().containsKey(workContractId))
                .map(user -> user.getWorkContacts().get(workContractId))
                .filter(workContract -> StringUtils.isNotBlank(workContract.getEmail().getValue()))
                .map(workContract -> workContract.getEmail().getValue())
                .collect(Collectors.toList());

        destinationMails.add(onboarding.getInstitution().getDigitalAddress());

        Product product = productService.getProductIsValid(onboarding.getProductId());

        notificationService.sendCompletedEmail(destinationMails, product, onboarding.getInstitution().getInstitutionType());
    }

    @Override
    public void sendMailRejection(Onboarding onboarding) {

        List<String> destinationMails = new ArrayList<>();
        destinationMails.add(onboarding.getInstitution().getDigitalAddress());

        Product product = productService.getProductIsValid(onboarding.getProductId());
        notificationService.sendMailRejection(destinationMails, product);
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
        onboardingRequest.setTokenId(onboarding.getOnboardingId());

        if(Objects.nonNull(onboarding.getBilling())) {
            BillingRequest billingRequest = new BillingRequest();
            billingRequest.recipientCode(onboarding.getBilling().getRecipientCode());
            billingRequest.publicServices(onboarding.getBilling().isPublicServices());
            billingRequest.vatNumber(onboarding.getBilling().getVatNumber());
            onboardingRequest.billing(billingRequest);
        }

        //If contract exists we send the path of the contract
        Optional<Token> optToken = tokenRepository.findByOnboardingId(onboarding.getOnboardingId());
        optToken.ifPresent(token -> onboardingRequest.setContractPath(token.getContractSigned()));

        institutionApi.onboardingInstitutionUsingPOST(onboarding.getInstitution().getId(), onboardingRequest);
    }
}
