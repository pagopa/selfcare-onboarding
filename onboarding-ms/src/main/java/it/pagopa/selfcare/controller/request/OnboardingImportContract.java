package it.pagopa.selfcare.controller.request;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingImportContract {

    private String fileName;
    private String filePath;
    private String contractType;
    private OffsetDateTime createdAt;
}
