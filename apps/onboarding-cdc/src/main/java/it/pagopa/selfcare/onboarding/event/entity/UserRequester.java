package it.pagopa.selfcare.onboarding.event.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequester {
    private String userRequestUid;
    private String userMailUuid;
}
