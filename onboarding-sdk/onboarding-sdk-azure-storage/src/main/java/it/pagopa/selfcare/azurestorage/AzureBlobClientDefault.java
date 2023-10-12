package it.pagopa.selfcare.azurestorage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;

import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageError;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;


public class AzureBlobClientDefault implements AzureBlobClient {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobClientDefault.class);

    private final BlobServiceClient blobClient;
    private final String containerName;

    public AzureBlobClientDefault(String connectionString, String containerName) {
        log.trace("it.pagopa.selfcare.azurestorage.AzureBlobClient.it.pagopa.selfcare.azurestorage.AzureBlobClient");
        this.containerName = containerName;
        this.blobClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    @Override
    public byte[] getFile(String filePath) {
        log.info("START - getFile for path: {}", filePath);
        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(containerName);
            final BlobClient blob = blobContainer.getBlobClient(filePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //BlobProperties properties = blob.getProperties();
            blob.downloadStream(outputStream);
            log.info("END - getFile - path {}", filePath);
            //response.setData(outputStream.toByteArray());
            //response.setFileName(blob.getName());
            //response.setMimetype(properties.getContentType());

            return outputStream.toByteArray();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), filePath),
                        SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getCode());
            }
            throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), filePath),
                    SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

    @Override
    public String getFileAsText(String filePath) {
        log.info("START - getTemplateFile for template: {}", filePath);
        try {

            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(containerName);
            final BlobClient blob = blobContainer.getBlobClient(filePath);

            BinaryData content = blob.downloadContent();
            log.info("END - getTemplateFile - Downloaded {}", filePath);
            return content.toString();
        } catch (BlobStorageException e) {
            log.error(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), filePath), e);
            throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), filePath),
                    SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getCode());
        }
    }

    @Override
    public File getFileAsPdf(String contractTemplate){
        log.info("START - getFileAsPdf for template: {}", contractTemplate);

        final BlobContainerClient blobContainer;
        final BlobClient blob;
        final File downloadedFile;

        try {
            blobContainer = blobClient.getBlobContainerClient(containerName);
            blob = blobContainer.getBlobClient(contractTemplate);
            String fileName = Paths.get(contractTemplate).getFileName().toString();
            downloadedFile = File.createTempFile(fileName, ".pdf");
            blob.downloadToFile(downloadedFile.getAbsolutePath(), true);
        } catch (BlobStorageException | IOException e) {
            log.error(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), contractTemplate), e);
            throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getMessage(), contractTemplate),
                    SelfcareAzureStorageError.ERROR_DURING_DOWNLOAD_FILE.getCode());
        }

        log.info("END - getFileAsPdf");
        return downloadedFile;
    }

    @Override
    public String uploadFile(String path, String filename, byte[] data) {
        log.debug("START - uploadFile for path: {}, filename: {}", path, filename);
        String filepath = Paths.get(path, filename).toString();
        log.debug("uploadContract fileName = {}", filepath);
        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(containerName);
            final BlobClient blob = blobContainer.getBlobClient(filepath);
            blob.upload(BinaryData.fromBytes(data), true);
            log.info("Uploaded {}", filepath);
            return filepath;
        } catch (BlobStorageException e) {
            log.error(String.format(SelfcareAzureStorageError.ERROR_DURING_UPLOAD_FILE.getMessage(), filepath), e);
            throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_UPLOAD_FILE.getMessage(), filepath),
                    SelfcareAzureStorageError.ERROR_DURING_UPLOAD_FILE.getCode());
        }
    }

    @Override
    public void removeFile(String fileName) {
        log.debug("START - delete file for fileName: {}", fileName);

        try {
            final BlobContainerClient blobContainer = blobClient.getBlobContainerClient(containerName);
            final BlobClient blob = blobContainer.getBlobClient(fileName);
            blob.deleteIfExists();
            log.debug("Deleted {}", fileName);
        } catch (BlobStorageException e) {
            log.error(String.format(SelfcareAzureStorageError.ERROR_DURING_DELETED_FILE.getMessage(), fileName), e);
            throw new SelfcareAzureStorageException(String.format(SelfcareAzureStorageError.ERROR_DURING_DELETED_FILE.getMessage(), fileName),
                    SelfcareAzureStorageError.ERROR_DURING_DELETED_FILE.getCode());
        }
    }

}
