package it.pagopa.selfcare.onboarding.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.pagopa.selfcare.onboarding.common.PartyRole;
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
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
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
    public static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String TOKEN_DOES_NOT_EXISTS_FOR_ONBOARDING_S = "Token does not exists for onboarding %s";
    public static final String USER_REQUEST_DOES_NOT_FOUND = "User request does not found for onboarding %s";

    @RestClient
    @Inject
    UserApi userRegistryApi;
    @Inject
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
        UserResource manager = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST,validManagerId);

        List<UserResource> delegates = onboarding.getUsers()
                .stream()
                .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
                .map(userToOnboard -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userToOnboard.getId())).collect(Collectors.toList());

        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.createContractPDF(product.getContractTemplatePath(), onboarding, manager, delegates, product.getTitle());
    }

    public void loadContract(Onboarding onboarding) {
        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.loadContractPDF(product.getContractTemplatePath(), onboarding.getId().toHexString(), product.getTitle());
    }
    public void saveTokenWithContract(Onboarding onboarding) {

        // Skip if token already exists
        Optional<Token> optToken = tokenRepository.findByOnboardingId(onboarding.getOnboardingId());
        if(optToken.isPresent()) {
            log.debug("Token has already exists for onboarding {}", onboarding.getId());
            return;
        }

        Product product = productService.getProductIsValid(onboarding.getProductId());

        // Load PDF contract and create digest
        File contract = contractService.retrieveContractNotSigned(onboarding.getOnboardingId(), product.getTitle());
        DSSDocument document = new FileDocument(contract);
        String digest = document.getDigest(DigestAlgorithm.SHA256);

        saveToken(onboarding, product, digest);
    }

    private void saveToken(Onboarding onboarding, Product product, String digest) {

        log.debug("creating Token for onboarding {} ...", onboarding.getId());

        // Persist token entity
        Token token = new Token();
        token.setOnboardingId(onboarding.getOnboardingId());
        token.setContractTemplate(product.getContractTemplatePath());
        token.setContractVersion(product.getContractTemplateVersion());
        token.setContractFilename(ContractServiceDefault.contractFilename.apply(product.getTitle()));
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setProductId(onboarding.getProductId());
        token.setChecksum(digest);
        token.setType(TokenType.INSTITUTION);

        tokenRepository.persist(token);
    }

    public void sendMailRegistration(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        notificationService.sendMailRegistration(onboarding.getInstitution().getDescription(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle());

    }

    public void sendMailRegistrationWithContract(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithTokenAndProductAndUserRequest(onboarding);

        notificationService.sendMailRegistrationWithContract(onboarding.getOnboardingId(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                sendMailInput.token.getId().toHexString());
    }

    public void sendMailRegistrationApprove(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithTokenAndProductAndUserRequest(onboarding);

        notificationService.sendMailRegistrationApprove(onboarding.getInstitution().getDescription(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                sendMailInput.token.getId().toHexString());

    }

    public void sendMailOnboardingApprove(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithTokenAndProductAndUserRequest(onboarding);

        notificationService.sendMailOnboardingApprove(onboarding.getInstitution().getDescription(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                sendMailInput.token.getId().toHexString());
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

    private SendMailInput builderWithTokenAndProductAndUserRequest(Onboarding onboarding) {
        Token token = tokenRepository.findByOnboardingId(onboarding.getOnboardingId())
                .orElseThrow(() -> new GenericOnboardingException(String.format(TOKEN_DOES_NOT_EXISTS_FOR_ONBOARDING_S, onboarding.getId())));

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);
        sendMailInput.token = token;
        return sendMailInput;
    }

    private SendMailInput builderWithProductAndUserRequest(Onboarding onboarding) {
        SendMailInput sendMailInput = new SendMailInput();
        sendMailInput.product = productService.getProduct(onboarding.getProductId());

        // Retrieve user request name and surname
        UserResource userRequest = Optional.ofNullable(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .orElseThrow(() -> new GenericOnboardingException(String.format(USER_REQUEST_DOES_NOT_FOUND, onboarding.getId())));
        sendMailInput.userRequestName = Optional.ofNullable(userRequest.getName()).map(CertifiableFieldResourceOfstring::getValue).orElse("");
        sendMailInput.userRequestSurname = Optional.ofNullable(userRequest.getFamilyName()).map(CertifiableFieldResourceOfstring::getValue).orElse("");
        return sendMailInput;
    }

    static class SendMailInput {
        Product product;
        Token token;
        String userRequestName;
        String userRequestSurname;
    }
}
