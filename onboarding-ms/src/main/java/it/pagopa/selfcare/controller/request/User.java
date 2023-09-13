package it.pagopa.selfcare.controller.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Valid
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @NotEmpty(message = "User internal id is required")
    private String id;
    private String taxCode;
    private String name;
    private String surname;
    private String email;
    /*private PartyRole role;
    private String productRole;
    private Env env = Env.ROOT;
    private String roleLabel;*/

    public User(String id) {
        this.id = id;
    }
}
