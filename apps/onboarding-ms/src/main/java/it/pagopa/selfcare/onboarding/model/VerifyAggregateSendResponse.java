package it.pagopa.selfcare.onboarding.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyAggregateSendResponse {

    private List<AggregateSend> aggregates;
    private List<RowError> errors;

    @Data
    public static class AggregateSend {
        private String description;
        private String pec;
        private String taxCode;
        private String vatNumber;
        private String codeSDI;
        private String address;
        private String city;
        private String province;
        private String subunitType;
        private String subunitCode;
        private List<AggregateUser> users;
    }

    @Data
    public static class AggregateUser {
        private String name;
        private String surname;
        private String taxCode;
        private String email;
        private String role;
    }

}
