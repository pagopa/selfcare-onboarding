package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class VerifyAggregateSendResponseTest {

    @Test
    void testVerifyAggregateSendResponseConstructorAndGetters() {

        List<VerifyAggregateSendResponse.AggregateSend> aggregates = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        VerifyAggregateSendResponse.AggregateSend aggregateSend = new VerifyAggregateSendResponse.AggregateSend();
        aggregateSend.setDescription("Description");
        aggregateSend.setPec("example@pec.it");
        aggregateSend.setTaxCode("XYZ12345");
        aggregateSend.setVatNumber("IT123456789");
        aggregateSend.setCodeSDI("AB123CD");
        aggregateSend.setAddress("Street 123");
        aggregateSend.setCity("Rome");
        aggregateSend.setProvince("RM");
        aggregateSend.setSubunitType("Subunit Type");
        aggregateSend.setSubunitCode("Subunit Code");

        VerifyAggregateSendResponse.AggregateUser user = new VerifyAggregateSendResponse.AggregateUser();
        user.setName("John");
        user.setSurname("Doe");
        user.setTaxCode("JD12345");
        user.setEmail("john.doe@example.com");
        user.setRole("Admin");

        aggregateSend.setUsers(List.of(user));
        aggregates.add(aggregateSend);

        VerifyAggregateSendResponse response = new VerifyAggregateSendResponse(aggregates, errors);

        assertNotNull(response.getAggregates());
        assertEquals(1, response.getAggregates().size());
        assertEquals("Description", response.getAggregates().get(0).getDescription());
        assertEquals("example@pec.it", response.getAggregates().get(0).getPec());
        assertEquals("XYZ12345", response.getAggregates().get(0).getTaxCode());
        assertEquals("IT123456789", response.getAggregates().get(0).getVatNumber());
        assertEquals("AB123CD", response.getAggregates().get(0).getCodeSDI());
        assertEquals("Street 123", response.getAggregates().get(0).getAddress());
        assertEquals("Rome", response.getAggregates().get(0).getCity());
        assertEquals("RM", response.getAggregates().get(0).getProvince());
        assertEquals("Subunit Type", response.getAggregates().get(0).getSubunitType());
        assertEquals("Subunit Code", response.getAggregates().get(0).getSubunitCode());
        assertEquals(1, response.getAggregates().get(0).getUsers().size());
        assertEquals("John", response.getAggregates().get(0).getUsers().get(0).getName());
        assertEquals("Doe", response.getAggregates().get(0).getUsers().get(0).getSurname());
        assertEquals("JD12345", response.getAggregates().get(0).getUsers().get(0).getTaxCode());
        assertEquals("john.doe@example.com", response.getAggregates().get(0).getUsers().get(0).getEmail());
        assertEquals("Admin", response.getAggregates().get(0).getUsers().get(0).getRole());
    }

    @Test
    void testNoArgsConstructor() {
        VerifyAggregateSendResponse response = new VerifyAggregateSendResponse();

        assertNull(response.getAggregates());
        assertNull(response.getErrors());
    }
}
