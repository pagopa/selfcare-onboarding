package it.pagopa.selfcare.azurestorage;

import java.io.File;

public interface AzureBlobClient {

    byte[] getFile(String filePath);

    String getFileAsText(String filePath);

    File getFileAsPdf(String contractTemplate);

    String uploadFile(String path, String filename, byte[] data);

    void removeFile(String fileName);
}
