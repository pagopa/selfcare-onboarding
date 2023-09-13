package it.pagopa.selfcare.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupportContact {
    private String supportEmail;
    private String supportPhone;
}
