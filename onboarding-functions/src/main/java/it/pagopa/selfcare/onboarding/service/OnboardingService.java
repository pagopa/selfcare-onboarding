package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.GenericError;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";

    @RestClient
    @Inject
    UserApi userRegistryApi;
    //@Inject
    NotificationService notificationService;
    @Inject
    ContractService contractService;
    @Inject
    ProductService productService;

    @Inject
    OnboardingRepository repository;

    @Inject
    TokenRepository tokenRepository;

    public Optional<Onboarding> getOnboarding(String onboardingId) {
        return repository.findByIdOptional(new ObjectId(onboardingId))
                .map(onboarding -> {
                    onboarding.setOnboardingId(onboarding.getId().toString());
                    return onboarding;
                });
    }

    public void createContract(Onboarding onboarding) {
        String validManagerId = getValidManagerId(onboarding.getUsers());
        UserResource manager = userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,validManagerId);

        List<UserResource> delegates = onboarding.getUsers()
                .stream()
                .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
                .map(userToOnboard -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId())).collect(Collectors.toList());

        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.createContractPDF(product.getContractTemplatePath(), onboarding, manager, delegates, List.of());
    }

    public void loadContract(Onboarding onboarding) {
        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.loadContractPDF(product.getContractTemplatePath(), onboarding.getId().toHexString());
    }

    public void saveToken(Onboarding onboarding) {
        /* create digest */
        File contract = contractService.retrieveContractNotSigned(onboarding.getOnboardingId());
        DSSDocument document = new FileDocument(contract);
        String digest = document.getDigest(DigestAlgorithm.SHA256);

        log.debug("createToken for onboarding {}", onboarding.getId());

        /* persist token entity */
        Product product = productService.getProductIsValid(onboarding.getProductId());
        Token token = new Token();
        token.setContractTemplate(product.getContractTemplatePath());
        token.setContractVersion(product.getContractTemplateVersion());
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setProductId(onboarding.getProductId());
        token.setChecksum(digest);
        token.setType(TokenType.INSTITUTION);

        tokenRepository.persist(token);
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
