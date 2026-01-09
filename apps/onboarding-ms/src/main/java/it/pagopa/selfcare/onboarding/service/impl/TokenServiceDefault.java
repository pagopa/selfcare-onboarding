package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.GENERIC_ERROR;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.ORIGINAL_DOCUMENT_NOT_FOUND;

import com.azure.storage.blob.models.BlobStorageException;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.onboarding.conf.OnboardingMsConfig;
import it.pagopa.selfcare.onboarding.conf.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.entity.SignatureInformation;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.SignatureService;
import it.pagopa.selfcare.onboarding.service.TokenService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import it.pagopa.selfcare.onboarding.util.Utils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

@Slf4j
@ApplicationScoped
public class TokenServiceDefault implements TokenService {

    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";
    private static final String ONBOARDING_NOT_FOUND_OR_ALREADY_DELETED =
            "Token with id %s not found or already deleted";
    public static final String PAGOPA_SIGNATURE_DISABLED = "disabled";

    @Inject
    SignatureService signatureService;
    private final AzureBlobClient azureBlobClient;
    private final OnboardingMsConfig onboardingMsConfig;
    private final ProductService productService;
    private final PagoPaSignatureConfig pagoPaSignatureConfig;
    private final PadesSignService padesSignService;
    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;
    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String pathContracts;

    public TokenServiceDefault(AzureBlobClient azureBlobClient,
                               OnboardingMsConfig onboardingMsConfig,
                               ProductService productService,
                               PagoPaSignatureConfig pagoPaSignatureConfig,
                               PadesSignService padesSignService) {
        this.azureBlobClient = azureBlobClient;
        this.onboardingMsConfig = onboardingMsConfig;
        this.productService = productService;
        this.pagoPaSignatureConfig = pagoPaSignatureConfig;
        this.padesSignService = padesSignService;
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
        return findOnboardingById(onboardingId)
                .onItem().transformToUni(onboarding -> {

                    Product product = productService.getProductIsValid(onboarding.getProductId());
                    AttachmentTemplate attachment = getAttachmentTemplate(attachmentName, onboarding, product);

                    if (!attachment.isGenerated()) {
                        return Uni.createFrom()
                                .item(() -> azureBlobClient.getFileAsPdf(attachment.getTemplatePath()))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> RestResponse.ResponseBuilder
                                        .ok(contract, MediaType.APPLICATION_OCTET_STREAM)
                                        .header(
                                                HTTP_HEADER_CONTENT_DISPOSITION,
                                                HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + attachmentName
                                        )
                                        .build());
                    }

                    return Token
                            .find(
                                    "onboardingId = ?1 and type = ?2 and name = ?3",
                                    onboardingId,
                                    ATTACHMENT.name(),
                                    attachmentName
                            )
                            .firstResult()
                            .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Token with id %s not found", onboardingId)))
                            .map(Token.class::cast)
                            .onItem().transformToUni(token ->
                                    Uni.createFrom()
                                            .item(() -> azureBlobClient.getFileAsPdf(
                                                    getAttachmentByOnboarding(
                                                            onboardingId,
                                                            token.getContractFilename()
                                                    )
                                            ))
                                            .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                            .onItem().transform(contract -> RestResponse.ResponseBuilder
                                                    .ok(contract, MediaType.APPLICATION_OCTET_STREAM)
                                                    .header(
                                                            HTTP_HEADER_CONTENT_DISPOSITION,
                                                            HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + token.getContractFilename()
                                                    )
                                                    .build())
                            );
                });
    }

    @Override
    public Uni<Void> uploadAttachment(String onboardingId, FormItem file, String attachmentName) {
        return findOnboardingById(onboardingId)
                .onItem()
                .transformToUni(
                        onboarding -> {
                            Product product = productService.getProductIsValid(onboarding.getProductId());
                            AttachmentTemplate attachment = this.getAttachmentTemplate(attachmentName, onboarding, product);
                            if (Boolean.TRUE.equals(isVerifyEnabled)) {
                                signatureService.verifySignature(file.getFile());
                            }
                            // verify if document has not been changed
                            String digest = getAndVerifyDigest(file, attachment);
                            File signedFile;
                            try {
                                signedFile = signPdf(file.getFile(), onboarding.getInstitution().getDescription(), product.getId());
                            } catch (IOException e) {
                                throw new IllegalArgumentException("Impossible to sign pdf. Error: " + e.getMessage(), e);
                            }
                            return persistTokenAttachment(onboardingId, file, attachment, digest)
                                    .onItem().invoke(token ->
                                            uploadFileToAzure(
                                                    token,
                                                    onboardingId,
                                                    signedFile
                                            )
                                    );
                        })
                .replaceWithVoid();
    }

    private String getAndVerifyDigest(FormItem file, AttachmentTemplate attachment) {
        return getAndVerifyDigestByTemplate(file, attachment.getTemplatePath(), false);
    }

    public String getAndVerifyDigest(FormItem file, ContractTemplate contract, boolean skipDigestCheck) {
        return getAndVerifyDigestByTemplate(file, contract.getContractTemplatePath(), skipDigestCheck);
    }

    private String getAndVerifyDigestByTemplate(FormItem file, String templatePath, boolean skipDigestCheck) {
        DSSDocument document = new FileDocument(file.getFile());
        String digest = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        File originalFile = azureBlobClient.getFileAsPdf(templatePath);
        DSSDocument originalDocument = new FileDocument(originalFile);
        String originalDigest = originalDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value();
        if (!digest.equals(originalDigest) && !skipDigestCheck) {
            throw new InvalidRequestException(
                    "File has been changed. It's not possible to complete upload");
        }
        return digest;
    }

    private File signPdf(File pdf, String institutionDescription, String productId)
            throws IOException {
        if (PAGOPA_SIGNATURE_DISABLED.equals(pagoPaSignatureConfig.source())) {
            log.info("Skipping PagoPA contract pdf sign due to global disabling");
            return pdf;
        }

        String signReason =
                pagoPaSignatureConfig
                        .applyOnboardingTemplateReason()
                        .replace("${institutionName}", institutionDescription)
                        .replace("${productName}", productId);

        log.info("Signing input file {} using reason {}", pdf.getName(), signReason);
        Path signedPdf = Files.createTempFile("signed", ".pdf");
        padesSignService.padesSign(pdf, signedPdf.toFile(), buildSignatureInfo(signReason));
        return signedPdf.toFile();
    }

    private SignatureInformation buildSignatureInfo(String signReason) {
        return new SignatureInformation(
                pagoPaSignatureConfig.signer(), pagoPaSignatureConfig.location(), signReason);
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

    public String getContractPathByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s", onboardingMsConfig.getContractPath(), onboardingId, "/" + filename);
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

    private Uni<Token> persistTokenAttachment(String onboardingId, FormItem file, AttachmentTemplate attachment, String digest) {
        Token token = new Token();
        token.setCreatedAt(LocalDateTime.now());
        token.setActivatedAt(LocalDateTime.now());
        token.setType(ATTACHMENT);
        token.setOnboardingId(onboardingId);
        token.setContractVersion(attachment.getTemplateVersion());
        token.setContractTemplate(attachment.getTemplatePath());
        token.setContractFilename(file.getFileName());
        token.setContractSigned(getAttachmentByOnboarding(
                onboardingId,
                token.getContractFilename()
        ));
        token.setChecksum(digest);
        return Token.persist(token).replaceWith(token);
    }

    private Uni<Onboarding> findOnboardingById(String onboardingId) {
        return Onboarding.findById(onboardingId)
                .onItem().ifNull().failWith(() ->
                        new ResourceNotFoundException(String.format("Onboarding with id %S not found", onboardingId))
                )
                .map(Onboarding.class::cast);
    }

    private void uploadFileToAzure(Token token, String onboardingId, File signedFile) throws OnboardingNotAllowedException {
        final String path = String.format("%s%s", pathContracts, onboardingId).concat("/attachments");
        final String signedContractFileName = Utils.extractFileName(token.getContractTemplate());
        final String filename = String.format("signed_%s", signedContractFileName);
        try {
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new OnboardingNotAllowedException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
    }

    public Uni<Boolean> attachmentExists(String onboardingId, String attachmentName) {
        return findOnboardingById(onboardingId)
                .onItem().transformToUni(onboarding ->
                        {
                            String id = onboarding.getId();
                            return Token.find(
                                            "onboardingId = ?1 and type = ?2 and name = ?3",
                                            id,
                                            ATTACHMENT.name(),
                                            attachmentName
                                    )
                                    .firstResult()
                                    .onItem().ifNull().failWith(() ->
                                            new ResourceNotFoundException(String.format("Token with id %s not found", id))
                                    )
                                    .map(Token.class::cast)
                                    .onItem().transformToUni(token -> {
                                        String blobPath = getAttachmentByOnboarding(
                                                id,
                                                token.getContractFilename()
                                        );
                                        log.debug("Blob path: {}", blobPath);

                                        return Uni.createFrom()
                                                .item(() -> {
                                                    try {
                                                        azureBlobClient.getProperties(blobPath);

                                                        log.info(
                                                                "Attachment found in storage onboardingId={}, attachmentName={}",
                                                                id,
                                                                attachmentName
                                                        );

                                                        return true;
                                                    } catch (SelfcareAzureStorageException e) {
                                                            log.info(
                                                                    "Attachment not found in storage onboardingId={}, attachmentName={}",
                                                                    id,
                                                                    attachmentName
                                                            );
                                                            return false;
                                                        }
                                                })
                                                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
                                    });
                        }
                );
    }
}
