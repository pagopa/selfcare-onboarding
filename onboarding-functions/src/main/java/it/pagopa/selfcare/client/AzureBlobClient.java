package it.pagopa.selfcare.client;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;

import io.smallrye.config.ConfigMapping;
import it.pagopa.selfcare.config.AzureStorageConfig;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.exception.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static it.pagopa.selfcare.utils.GenericError.*;


@ApplicationScoped
public class AzureBlobClient  {


    private static final Logger log = LoggerFactory.getLogger(AzureBlobClient.class);

    private final BlobServiceClient blobClient;
    private final AzureStorageConfig azureStorageConfig;

    AzureBlobClient(AzureStorageConfig azureStorageConfig) {
        log.trace("AzureBlobClient.AzureBlobClient");;
        this.azureStorageConfig = azureStorageConfig;
        this.blobClient = new BlobServiceClientBuilder()
                .endpoint(azureStorageConfig.connectionString())
                .buildClient();
    }

    public byte[] getFile(String fileName) {
        log.info("START - getFile for path: {}", fileName);
        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.container());
            final BlobClient blob = blobContainer.getBlobClient(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BlobProperties properties = blob.getProperties();
            blob.downloadStream(outputStream);
            log.info("END - getFile - path {}", fileName);
            //response.setData(outputStream.toByteArray());
            //response.setFileName(blob.getName());
            //response.setMimetype(properties.getContentType());

            return outputStream.toByteArray();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new ResourceNotFoundException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                        ERROR_DURING_DOWNLOAD_FILE.getCode());
            }
            throw new GenericOnboardingException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), fileName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

    public String getTemplateFile(String templateName) {
        log.info("START - getTemplateFile for template: {}", templateName);
        try {

            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.container());
            final BlobClient blob = blobContainer.getBlobClient(templateName);

            BinaryData content = blob.downloadContent();
            log.info("END - getTemplateFile - Downloaded {}", templateName);
            return content.toObject(String.class);
        } catch (BlobStorageException e) {
            log.error(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName), e);
            throw new GenericOnboardingException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), templateName),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

    public File getFileAsPdf(String contractTemplate){
        log.info("START - getFileAsPdf for template: {}", contractTemplate);

        final BlobContainerClient blobContainer;
        final BlobClient blob;
        final File downloadedFile;

        try {
            blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.container());
            blob = blobContainer.getBlobClient(contractTemplate);
            String fileName = Paths.get(contractTemplate).getFileName().toString();
            downloadedFile = File.createTempFile(fileName, ".pdf");
            blob.downloadToFile(downloadedFile.getAbsolutePath(), true);
        } catch (BlobStorageException | IOException e) {
            log.error(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), contractTemplate), e);
            throw new GenericOnboardingException(String.format(ERROR_DURING_DOWNLOAD_FILE.getMessage(), contractTemplate),
                    ERROR_DURING_DOWNLOAD_FILE.getCode());
        }

        log.info("END - getFileAsPdf");
        return downloadedFile;
    }

    public String uploadContract(String id, MultipartFile contract) {
        log.info("START - uploadContract for token: {}", id);
        String fileName = Paths.get(azureStorageConfig.contractPath(), id, contract.getOriginalFilename()).toString();
        log.debug("uploadContract fileName = {}, contentType = {}", fileName, contract.getContentType());
        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.container());
            final BlobClient blob = blobContainer.getBlobClient(fileName);
            //blob.getProperties().setContentType(contract.getContentType());
            blob.upload(contract.getInputStream(), contract.getInputStream().available());
            log.info("Uploaded {}", fileName);
            return fileName;
        } catch (BlobStorageException | IOException e) {
            log.error(String.format(ERROR_DURING_UPLOAD_FILE.getMessage(), fileName), e);
            throw new GenericOnboardingException(String.format(ERROR_DURING_UPLOAD_FILE.getMessage(), fileName),
                    ERROR_DURING_UPLOAD_FILE.getCode());
        }
    }

    public void removeContract(String fileName, String tokenId) {
        log.info("START - deleteContract for token: {}", tokenId);

        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(azureStorageConfig.container());
            final BlobClient blob = blobContainer.getBlobClient(fileName);
            blob.deleteIfExists();
            log.info("Deleted {}", fileName);
        } catch (BlobStorageException e) {
            log.error(String.format(ERROR_DURING_DELETED_FILE.getMessage(), fileName), e);
            throw new GenericOnboardingException(String.format(ERROR_DURING_DELETED_FILE.getMessage(), fileName),
                    ERROR_DURING_DELETED_FILE.getCode());
        }
    }

}
