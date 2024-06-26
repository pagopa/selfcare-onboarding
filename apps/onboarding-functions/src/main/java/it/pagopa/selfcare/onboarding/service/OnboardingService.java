package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.GenericError;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.utils.Utils.ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS;
import static it.pagopa.selfcare.onboarding.utils.Utils.CONTRACT_FILENAME_FUNC;

@ApplicationScoped
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";
    public static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USER_REQUEST_DOES_NOT_FOUND = "User request does not found for onboarding %s";
    public static final String ACTIVATED_AT_FIELD = "activatedAt";
    public static final String DELETED_AT_FIELD = "deletedAt";

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

    @Inject
    MailTemplatePathConfig mailTemplatePathConfig;

    @Inject
    MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;

    public Optional<Onboarding> getOnboarding(String onboardingId) {
        return repository.findByIdOptional(onboardingId);
    }

    public void createContract(OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String validManagerId = getValidManagerId(onboarding.getUsers());
        UserResource manager = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST,validManagerId);

        List<UserResource> delegates = onboarding.getUsers()
                .stream()
                .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
                .map(userToOnboard -> userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userToOnboard.getId())).collect(Collectors.toList());

        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.createContractPDF(onboardingWorkflow.getContractTemplatePath(product), onboarding, manager, delegates, product.getTitle(), onboardingWorkflow.getPdfFormatFilename());
    }

    public void loadContract(Onboarding onboarding) {
        Product product = productService.getProductIsValid(onboarding.getProductId());
        contractService.loadContractPDF(product.getContractTemplatePath(), onboarding.getId(), product.getTitle());
    }

    public void saveTokenWithContract(OnboardingWorkflow onboardingWorkflow) {

        Onboarding onboarding = onboardingWorkflow.getOnboarding();

        // Skip if token already exists
        Optional<Token> optToken = tokenRepository.findByOnboardingId(onboarding.getId());
        if(optToken.isPresent()) {
            log.debug("Token has already exists for onboarding {}", onboarding.getId());
            return;
        }

        Product product = productService.getProductIsValid(onboarding.getProductId());

        // Load PDF contract and create digest
        File contract = contractService.retrieveContractNotSigned(onboardingWorkflow, product.getTitle());
        DSSDocument document = new FileDocument(contract);
        String digest = document.getDigest(DigestAlgorithm.SHA256);

        saveToken(onboardingWorkflow, product, digest);
    }

    private void saveToken(OnboardingWorkflow onboardingWorkflow, Product product, String digest) {

        Onboarding onboarding = onboardingWorkflow.getOnboarding();

        log.debug("creating Token for onboarding {} ...", onboarding.getId());

        // Persist token entity
        Token token = new Token();
        token.setId(onboarding.getId());
        token.setOnboardingId(onboarding.getId());
        token.setContractTemplate(onboardingWorkflow.getContractTemplatePath(product));
        token.setContractVersion(product.getContractTemplateVersion());
        token.setContractFilename(CONTRACT_FILENAME_FUNC.apply(onboardingWorkflow.getPdfFormatFilename(), product.getTitle()));
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setProductId(onboarding.getProductId());
        token.setChecksum(digest);
        token.setType(onboardingWorkflow.getTokenType());

        tokenRepository.persist(token);
    }

    public void sendMailRegistration(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        notificationService.sendMailRegistration(onboarding.getInstitution().getDescription(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle());

    }

    public void sendMailRegistrationForContract(OnboardingWorkflow onboardingWorkflow) {

        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        final String templatePath = onboardingWorkflow.emailRegistrationPath(mailTemplatePathConfig);
        final String confirmTokenUrl = onboardingWorkflow.getConfirmTokenUrl(mailTemplatePlaceholdersConfig);

        notificationService.sendMailRegistrationForContract(onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                sendMailInput.institutionName,
                templatePath,
                confirmTokenUrl);
    }

    public void sendMailRegistrationForContractAggregator(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        notificationService.sendMailRegistrationForContractAggregator(onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle());
    }

    public void sendMailRegistrationForContractWhenApprove(OnboardingWorkflow onboardingWorkflow) {

        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        Product product = productService.getProduct(onboarding.getProductId());

        notificationService.sendMailRegistrationForContract(onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                onboarding.getInstitution().getDescription(), "",
                product.getTitle(), "description",
                onboardingWorkflow.emailRegistrationPath(mailTemplatePathConfig),
                onboardingWorkflow.getConfirmTokenUrl(mailTemplatePlaceholdersConfig));
    }

    public void sendMailRegistrationApprove(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        notificationService.sendMailRegistrationApprove(onboarding.getInstitution().getDescription(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                onboarding.getId());

    }

    public void sendMailOnboardingApprove(Onboarding onboarding) {

        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        notificationService.sendMailOnboardingApprove(onboarding.getInstitution().getDescription(),
                sendMailInput.userRequestName, sendMailInput.userRequestSurname,
                sendMailInput.product.getTitle(),
                onboarding.getId());
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

    private SendMailInput builderWithProductAndUserRequest(Onboarding onboarding) {
        SendMailInput sendMailInput = new SendMailInput();
        sendMailInput.product = productService.getProduct(onboarding.getProductId());

        // Retrieve user request name and surname
        UserResource userRequest = Optional.ofNullable(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequestUid()))
                .orElseThrow(() -> new GenericOnboardingException(String.format(USER_REQUEST_DOES_NOT_FOUND, onboarding.getId())));
        sendMailInput.userRequestName = Optional.ofNullable(userRequest.getName()).map(CertifiableFieldResourceOfstring::getValue).orElse("");
        sendMailInput.userRequestSurname = Optional.ofNullable(userRequest.getFamilyName()).map(CertifiableFieldResourceOfstring::getValue).orElse("");
        sendMailInput.institutionName = Optional.ofNullable(onboarding.getInstitution().getDescription()).orElse("");
        return sendMailInput;
    }

    public void updateOnboardingStatus(String onboardingId, OnboardingStatus status) {
        repository
                .update("status = ?1 and updatedAt = ?2", status.name(), LocalDateTime.now())
                .where("_id", onboardingId);
    }

    public void updateOnboardingStatusAndInstanceId(String onboardingId, OnboardingStatus status, String instanceId) {
        repository
                .update("status = ?1 and workflowInstanceId = ?2 and updatedAt = ?3",
                        status.name(), instanceId, LocalDateTime.now())
                .where("_id", onboardingId);
    }

    public List<NotificationCountResult> countNotifications(String productId, String from, String to, ExecutionContext context) {
        context.getLogger().info(() -> String.format("Starting countOnboarding with filters productId: %s from: %s to: %s", productId, from, to));
        return productService.getProducts(false, false)
                .stream()
                .filter(product -> Objects.isNull(productId) || product.getId().equals(productId))
                .map(product -> countNotificationsByFilters(product.getId(), from, to, context))
                .toList();
    }


    public NotificationCountResult countNotificationsByFilters(String productId, String from, String to, ExecutionContext context) {
        Document queryAddEvent = createQuery(productId, List.of(OnboardingStatus.COMPLETED, OnboardingStatus.DELETED), from, to, ACTIVATED_AT_FIELD);
        Document queryUpdateEvent = createQuery(productId, List.of(OnboardingStatus.DELETED), from, to, DELETED_AT_FIELD);

        long countAddEvents = repository.find(queryAddEvent).count();
        long countUpdateEvents= repository.find(queryUpdateEvent).count();
        long total = countUpdateEvents + countAddEvents;

        context.getLogger().info(() -> String.format("Counted onboardings for productId: %s add events: %s update events: %s", productId, countAddEvents, countUpdateEvents));
        return new NotificationCountResult(productId, total);
    }

    private Document createQuery(String productId, List<OnboardingStatus> status, String from, String to, String dateField) {
        Document query = new Document();
        query.append("productId", productId);
        query.append("status", new Document("$in", status.stream().map(OnboardingStatus::name).toList()));
        query.append("workflowType", new Document("$in", ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS.stream().map(Enum::name).toList()));

        Document dateQuery = new Document();
        Optional.ofNullable(from).ifPresent(value -> query.append(dateField, dateQuery.append("$gte", LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE))));
        Optional.ofNullable(to).ifPresent(value -> query.append(dateField, dateQuery.append("$lte", LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE))));
        if(!dateQuery.isEmpty()) {
            query.append(dateField, dateQuery);
        }
        return query;
    }

    static class SendMailInput {
        Product product;
        String userRequestName;
        String userRequestSurname;
        String institutionName;
    }
}
