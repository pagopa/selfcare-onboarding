package it.pagopa.selfcare.onboarding.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RowError {
    @NotNull
    private Integer row;
    @NotNull
    private String cf;
    private String reason;

}
