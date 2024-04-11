package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.ProductMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.user_json.api.UserControllerApi;
import org.openapi.quarkus.user_json.model.AddUserRoleDto;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

@ApplicationScoped
public class CompletionServiceDefault implements CompletionService {

    @RestClient
    @Inject
    InstitutionApi institutionApi;
    @RestClient
    @Inject
    UserControllerApi userApi;
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
    ProductMapper productMapper;

    @Inject
    NotificationService notificationService;
    @Inject
    ProductService productService;

    @ConfigProperty(name = "onboarding-functions.persist-users.active")
    private boolean isUserMSActive;

    @Override
    public String createInstitutionAndPersistInstitutionId(Onboarding onboarding) {

        Institution institution = onboarding.getInstitution();

        InstitutionsResponse institutionsResponse = institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), institution.getSubunitCode(), null, null);
        if(Objects.nonNull(institutionsResponse.getInstitutions()) && institutionsResponse.getInstitutions().size() > 1){
            throw new GenericOnboardingException("List of institutions is ambiguous, it is empty or has more than one element!!");
        }

        InstitutionResponse institutionResponse =
                Objects.isNull(institutionsResponse.getInstitutions()) || institutionsResponse.getInstitutions().isEmpty()
                    ? createInstitution(institution)
                    : institutionsResponse.getInstitutions().get(0);

        onboardingRepository
                .update("institution.id = ?1 and updatedAt = ?2 ", institutionResponse.getId(), LocalDateTime.now())
                .where("_id", onboarding.getId());

        return institutionResponse.getId();
    }

    /**
     * Function that creates institution based on institution type and Origin,
     * Origin indicates which is the indexes where data come from, for ex. IPA comes from index of Pubbliche Amministrazioni
     * Look at https://pagopa.atlassian.net/wiki/spaces/SCP/pages/708804909/Glossario for more information about institution type and indexes
     */
    private InstitutionResponse createInstitution(Institution institution) {

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
            fromIpaPost.setInstitutionType(InstitutionFromIpaPost.InstitutionTypeEnum.valueOf(institution.getInstitutionType().name()));
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
            if (InstitutionPaSubunitType.AOO.equals(institution.getSubunitType())) {
                aooApi.findByUnicodeUsingGET(institution.getSubunitCode(), null);
            } else if (InstitutionPaSubunitType.UO.equals(institution.getSubunitType())) {
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

        List<String> destinationMails = onboarding.getUsers().stream()
                .filter(userToOnboard -> MANAGER.equals(userToOnboard.getRole()))
                .map(userToOnboard -> Optional.ofNullable(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId()))
                        .filter(userResource -> Objects.nonNull(userResource.getWorkContacts())
                                && userResource.getWorkContacts().containsKey(userToOnboard.getUserMailUuid()))
                        .map(user -> user.getWorkContacts().get(userToOnboard.getUserMailUuid()))
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(workContract -> StringUtils.isNotBlank(workContract.getEmail().getValue()))
                .map(workContract -> workContract.getEmail().getValue())
                .collect(Collectors.toList());

        destinationMails.add(onboarding.getInstitution().getDigitalAddress());

        Product product = productService.getProductIsValid(onboarding.getProductId());

        notificationService.sendCompletedEmail(onboarding.getInstitution().getDescription(),
                destinationMails, product, onboarding.getInstitution().getInstitutionType());
    }

    @Override
    public void persistUsers(Onboarding onboarding) {
        if(isUserMSActive) {
            List<User> users = onboarding.getUsers();
            users.forEach(user -> {
                AddUserRoleDto userRoleDto = userMapper.toUserRole(onboarding);
                userRoleDto.setUserMailUuid(user.getUserMailUuid());
                userRoleDto.setProduct(productMapper.toProduct(onboarding, user));
                userRoleDto.getProduct().setTokenId(onboarding.getId());
                Response response = userApi.usersUserIdPost(user.getId(), userRoleDto);
                if(!SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                    throw new RuntimeException("Impossible to create or update role for user with ID: " + user.getId());
                }
            });
        }
    }

    @Override
    public void sendMailRejection(Onboarding onboarding) {

        List<String> destinationMails = new ArrayList<>();
        destinationMails.add(onboarding.getInstitution().getDigitalAddress());

        Product product = productService.getProductIsValid(onboarding.getProductId());
        notificationService.sendMailRejection(destinationMails, product, onboarding.getReasonForReject());
    }


    @Override
    public void persistOnboarding(Onboarding onboarding) {
        //Prepare data for request
        InstitutionOnboardingRequest onboardingRequest = new InstitutionOnboardingRequest();
        onboardingRequest.setUsers(onboarding.getUsers().stream()
                .map(user -> {
                    UserResource userResource = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, user.getId());
                    Person person = userMapper.toPerson(userResource);
                    person.setProductRole(user.getProductRole());
                    person.setRole(Person.RoleEnum.valueOf(user.getRole().name()));

                    //Retrieve mail if exists (for PNPG is not stored)
                    if(Objects.nonNull(user.getUserMailUuid())) {
                        String mailWork = Optional.ofNullable(userResource.getWorkContacts())
                                .map(worksContract -> worksContract.get(user.getUserMailUuid()))
                                .map(workContactResource -> workContactResource.getEmail())
                                .map(certifiable -> certifiable.getValue())
                                .orElse(null);

                        person.setEmail(mailWork);
                    }

                    return person;
                })
                .toList()
        );
        onboardingRequest.pricingPlan(onboarding.getPricingPlan());
        onboardingRequest.productId(onboarding.getProductId());
        onboardingRequest.setTokenId(onboarding.getId());
        Optional.ofNullable(onboarding.getActivatedAt())
                .ifPresent(date -> onboardingRequest.setActivatedAt(date.atZone(ZoneId.systemDefault()).toOffsetDateTime()));

        if(Objects.nonNull(onboarding.getBilling())) {
            BillingRequest billingRequest = new BillingRequest();
            billingRequest.recipientCode(onboarding.getBilling().getRecipientCode());
            billingRequest.publicServices(onboarding.getBilling().isPublicServices());
            billingRequest.vatNumber(onboarding.getBilling().getVatNumber());
            onboardingRequest.billing(billingRequest);
        }

        //If contract exists we send the path of the contract
        Optional<Token> optToken = tokenRepository.findByOnboardingId(onboarding.getId());
        optToken.ifPresent(token -> onboardingRequest.setContractPath(token.getContractSigned()));

        institutionApi.onboardingInstitutionUsingPOST(onboarding.getInstitution().getId(), onboardingRequest);
    }
}
