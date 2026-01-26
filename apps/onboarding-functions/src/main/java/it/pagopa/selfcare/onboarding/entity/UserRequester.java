package it.pagopa.selfcare.onboarding.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequester {

    private String userRequestUid;
    private String userMailUuid;
}
