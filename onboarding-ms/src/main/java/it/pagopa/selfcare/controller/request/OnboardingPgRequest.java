package it.pagopa.selfcare.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingPgRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotEmpty(message = "at least one user is required")
    private List<UserRequest> users;

    @NotNull(message = "taxCode is required")
    private String taxCode;
    private String businessName;
    @NotNull
    private boolean certified;
    @NotBlank
    @Email
    private String digitalAddress;

}
