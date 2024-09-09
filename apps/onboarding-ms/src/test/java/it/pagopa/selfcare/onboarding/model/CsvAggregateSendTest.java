package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvAggregateSendTest {

    @Test
    void testCsvAggregateSendConstructorAndGetters() {
        String description = "Test Description";
        String pec = "test@example.com";
        String taxCode = "TAX12345";
        String vatNumber = "VAT12345678";
        String codeSDI = "SDI123456";
        String address = "Test Address";
        String city = "Test City";
        String province = "Test Province";
        String ipaCode = "IPA12345";
        String subunitType = "Department";
        String subunitCode = "DEP001";
        String adminAggregateName = "Admin Name";
        String adminAggregateSurname = "Admin Surname";
        String adminAggregateTaxCode = "ADMIN12345";
        String adminAggregateEmail = "admin@example.com";
        Integer rowNumber = 5;

        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        csvAggregateSend.setDescription(description);
        csvAggregateSend.setPec(pec);
        csvAggregateSend.setTaxCode(taxCode);
        csvAggregateSend.setVatNumber(vatNumber);
        csvAggregateSend.setCodeSDI(codeSDI);
        csvAggregateSend.setAddress(address);
        csvAggregateSend.setCity(city);
        csvAggregateSend.setProvince(province);
        csvAggregateSend.setIpaCode(ipaCode);
        csvAggregateSend.setSubunitType(subunitType);
        csvAggregateSend.setSubunitCode(subunitCode);
        csvAggregateSend.setAdminAggregateName(adminAggregateName);
        csvAggregateSend.setAdminAggregateSurname(adminAggregateSurname);
        csvAggregateSend.setAdminAggregateTaxCode(adminAggregateTaxCode);
        csvAggregateSend.setAdminAggregateEmail(adminAggregateEmail);
        csvAggregateSend.setRowNumber(rowNumber);

        assertEquals(description, csvAggregateSend.getDescription());
        assertEquals(pec, csvAggregateSend.getPec());
        assertEquals(taxCode, csvAggregateSend.getTaxCode());
        assertEquals(vatNumber, csvAggregateSend.getVatNumber());
        assertEquals(codeSDI, csvAggregateSend.getCodeSDI());
        assertEquals(address, csvAggregateSend.getAddress());
        assertEquals(city, csvAggregateSend.getCity());
        assertEquals(province, csvAggregateSend.getProvince());
        assertEquals(ipaCode, csvAggregateSend.getIpaCode());
        assertEquals(subunitType, csvAggregateSend.getSubunitType());
        assertEquals(subunitCode, csvAggregateSend.getSubunitCode());
        assertEquals(adminAggregateName, csvAggregateSend.getAdminAggregateName());
        assertEquals(adminAggregateSurname, csvAggregateSend.getAdminAggregateSurname());
        assertEquals(adminAggregateTaxCode, csvAggregateSend.getAdminAggregateTaxCode());
        assertEquals(adminAggregateEmail, csvAggregateSend.getAdminAggregateEmail());
        assertEquals(rowNumber, csvAggregateSend.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        int expectedRowNumber = 10;

        csvAggregateSend.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregateSend.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        csvAggregateSend.setDescription("Sample Description");
        csvAggregateSend.setPec("sample@example.com");
        csvAggregateSend.setTaxCode("SAMPLETAXCODE");
        csvAggregateSend.setAdminAggregateEmail("admin@example.com");

        assertNotNull(csvAggregateSend);
        assertEquals("Sample Description", csvAggregateSend.getDescription());
        assertEquals("sample@example.com", csvAggregateSend.getPec());
        assertEquals("SAMPLETAXCODE", csvAggregateSend.getTaxCode());
        assertEquals("admin@example.com", csvAggregateSend.getAdminAggregateEmail());
    }
}
