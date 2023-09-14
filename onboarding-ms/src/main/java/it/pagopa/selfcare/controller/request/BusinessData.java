package it.pagopa.selfcare.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessData {
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
}
