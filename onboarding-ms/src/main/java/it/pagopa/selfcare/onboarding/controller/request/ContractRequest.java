package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


@Data
public class ContractRequest {
    private String version;

    @NotEmpty(message = "contract path is required")
    private String path;
}
