package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private String id;
    private PartyRole role;
    private String ProductRole;
}
