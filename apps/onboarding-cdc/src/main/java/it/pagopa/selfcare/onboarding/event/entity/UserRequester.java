package it.pagopa.selfcare.onboarding.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequester {
    private String userRequestUid;
    private String userMailUuid;
}
