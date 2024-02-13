package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class OnboardingImportContract {

    @NotEmpty(message = "fileName is required")
    private String fileName;
    @NotEmpty(message = "filePath is required")
    private String filePath;
    private String contractType;
    @NotNull(message = "createdAt is required")
    private OffsetDateTime createdAt;
}
