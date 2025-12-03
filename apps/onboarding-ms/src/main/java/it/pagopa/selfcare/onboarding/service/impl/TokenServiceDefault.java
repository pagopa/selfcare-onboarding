package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.SignatureService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.ORIGINAL_DOCUMENT_NOT_FOUND;

@Slf4j
@ApplicationScoped
public class TokenServiceDefault implements TokenService {

    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED =
            "Token with id %s not found or already deleted";

    @Inject
    SignatureService signatureService;

    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;
    private final ProductService productService;
    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;

    public TokenServiceDefault(AzureBlobClient azureBlobClient,
                               OnboardingMsConfig onboardingMsConfig,
                               ProductService productService) {
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
        this.productService = productService;

    }

    @Override
    public Uni<List<Token>> getToken(String onboardingId) {
        return Token.find("onboardingId", onboardingId)
                .list();
    }

    @Override
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(isSigned ? token.getContractSigned() : getContractNotSigned(onboardingId, token)))
                                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                .onItem().transform(contract -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header(HTTP_HEADER_CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + getCurrentContractName(token, isSigned));
                                    return response.build();
                                }));
    }

    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token -> Uni.createFrom().item(() -> azureBlobClient.retrieveFile(token.getContractSigned()))
                        .runSubscriptionOn(Executors.newSingleThreadExecutor())
                        .onItem().transform(contract -> {
                            if (token.getContractSigned().endsWith(".pdf")) {
                                isPdfValid(contract);
                            } else {
                                isP7mValid(contract, signatureService);
                                File original = signatureService.extractFile(contract);
                                isPdfValid(original);
                            }
                            RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                            response.header(HTTP_HEADER_CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + getCurrentContractName(token, true));
                            return response.build();
                        }).onFailure().recoverWithUni(() -> Uni.createFrom().item(RestResponse.ResponseBuilder.<File>notFound().build())));
    }

    public static void isP7mValid(File contract, SignatureService signatureService) {
        signatureService.verifySignature(contract);
    }

    public static void isPdfValid(File contract) {
        try (PDDocument document = Loader.loadPDF(contract)) {
            document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.getText(document);
        } catch (IOException e) {
            throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
        }
    }

    private String getContractNotSigned(String onboardingId, Token token) {
        return String.format("%s%s/%s", onboardingMsConfig.getContractPath(), onboardingId,
                token.getContractFilename());
    }

    private static String getCurrentContractName(Token token, boolean isSigned) {
        return isSigned ? getContractSignedName(token) : token.getContractFilename();
    }

    private static String getContractSignedName(Token token) {
        File file = new File(token.getContractSigned());
        return file.getName();
    }

    @Override
    public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName) {
        return Token.find("onboardingId = ?1 and type = ?2 and name = ?3", onboardingId, ATTACHMENT.name(), attachmentName)
                .firstResult()
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(getAttachmentByOnboarding(onboardingId, token.getContractFilename())))
                                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                .onItem().transform(contract -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header(HTTP_HEADER_CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + token.getContractFilename());
                                    return response.build();
                                }));
    }

    @Override
    public Uni<Token> uploadAttachment(String onboardingId, FormItem file) {
        return Onboarding.findById(onboardingId).map(Onboarding.class::cast)
                .onItem().transformToUni(
                        onboarding -> {
                            Product product = productService.getProductIsValid(onboarding.getProductId());
                            AttachmentTemplate attachment = this.getAttachmentTemplate(file.getFileName(), onboarding, product);
                            if (Boolean.TRUE.equals(isVerifyEnabled)) {

                            }else {

                            }
                            Token token = new Token();
                            token.setCreatedAt(LocalDateTime.now());
                            token.setActivatedAt(LocalDateTime.now());
                            token.setType(ATTACHMENT);
                            token.setOnboardingId(onboardingId);
                            token.setContractVersion(attachment.getTemplateVersion());
                            token.setContractTemplate(attachment.getTemplatePath());
                            token.setContractFilename(file.getFileName());
                            token.setChecksum();
                            Token.persist(token);
                        });
    }

    private AttachmentTemplate getAttachmentTemplate(String attachmentName, Onboarding onboarding, Product product) {
        return product
                .getInstitutionContractMappings()
                .get(onboarding.getInstitution().getInstitutionType().name())
                .getAttachments()
                .stream()
                .filter(a -> a.getName().equals(attachmentName))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                String.format("Attachment with name %s not found", attachmentName)
                        )
                );
    }

    @Override
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {

        Map<String, Object> queryParameter = new HashMap<>();
        queryParameter.put("contractSigned", documentSignedPath);
        Document query = QueryUtils.buildUpdateDocument(queryParameter);

        return Token.update(query)
                .where("_id", onboardingId)
                .onItem()
                .transformToUni(
                        updateItemCount -> {
                            if (updateItemCount == 0) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        String.format(ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED, onboardingId)));
                            }
                            return Uni.createFrom().item(updateItemCount);
                        });
    }

    public Uni<List<String>> getAttachments(String onboardingId) {
        return Token.find("onboardingId = ?1 and type = ?2", onboardingId, ATTACHMENT.name())
                .stream().onItem().transform(Token.class::cast)
                .map(Token::getName)
                .collect().asList();
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", onboardingMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

    @Override
    public Uni<ContractSignedReport> reportContractSigned(String onboardingId) {
        return Token.findById(onboardingId)
                .map(Token.class::cast)
                .onItem().transformToUni(token ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(token.getContractSigned()))
                                .runSubscriptionOn(Executors.newSingleThreadExecutor())
                                .onItem().transform(contract -> {
                                    signatureService.verifySignature(contract);
                                    return ContractSignedReport.cades(true);
                                })).onFailure().recoverWithUni(() -> Uni.createFrom().item(ContractSignedReport.cades(false)));
    }
}
