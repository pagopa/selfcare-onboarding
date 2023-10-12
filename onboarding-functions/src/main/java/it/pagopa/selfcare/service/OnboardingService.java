package it.pagopa.selfcare.service;

import it.pagopa.selfcare.client.AzureBlobClient;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.repository.OnboardingRepository;
import it.pagopa.selfcare.utils.GenericError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @RestClient
    @Inject
    ProductApi productApi;
    @Inject
    NotificationService notificationService;
    @Inject
    ContractService contractService;

    @Inject
    OnboardingRepository repository;
    public Onboarding getOnboarding(String onboardingId) {
        return repository.findById(new ObjectId(onboardingId));
    }

    public void createContract(Onboarding onboarding) {
        String validManagerId = getValidManagerId(onboarding.getUsers());
        UserResource manager = userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,validManagerId);

        List<UserResource> delegates = onboarding.getUsers()
                .stream()
                .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
                .map(userToOnboard -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId())).collect(Collectors.toList());

        //ProductResource productResource = productApi.getProductIsValidUsingGET(onboarding.getProductId());

        contractService.createContractPDF("parties/docs/651692a7e98f3b1fac40a05d/io-accordo_di_adesione-v.2.4.2.html", onboarding, manager, delegates, List.of());
    }

    public void loadContract(Onboarding onboarding) {

        //File pdf = fileStorageConnector.getFileAsPdf(strategyInput.getOnboardingRequest().getContract().getPath());

    }

    public String getValidManagerId(List<User> users) {
        log.debug("START - getOnboardingValidManager for users list size: {}", users.size());

        return users.stream()
                .filter(userToOnboard -> PartyRole.MANAGER == userToOnboard.getRole())
                .map(User::getId)
                .findAny()
                .orElseThrow(() -> new GenericOnboardingException(GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getMessage(),
                        GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getCode()));
    }
}
