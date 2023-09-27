package it.pagopa.selfcare.controller.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

    private String taxCode;
    private String name;
    private String surname;
    private String email;
    private PartyRole role;
    private String productRole;
    //private Env env = Env.ROOT;

}
