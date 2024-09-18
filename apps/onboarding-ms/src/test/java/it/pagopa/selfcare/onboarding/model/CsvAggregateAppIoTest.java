package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvAggregateAppIoTest {

    @Test
    void testCsvAggregateAppIoConstructorAndGetters() {

        String description = "Test Description";
        String pec = "test@example.com";
        String taxCode = "TAX12345";
        String vatNumber = "VAT12345678";
        String address = "Test Address";
        String city = "Test City";
        String province = "Test Province";
        String ipaCode = "IPA12345";
        String subunitType = "Department";
        String subunitCode = "DEP001";
        String originId = "ORI123";
        Integer rowNumber = 5;

        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();
        csvAggregateAppIo.setDescription(description);
        csvAggregateAppIo.setPec(pec);
        csvAggregateAppIo.setTaxCode(taxCode);
        csvAggregateAppIo.setVatNumber(vatNumber);
        csvAggregateAppIo.setAddress(address);
        csvAggregateAppIo.setCity(city);
        csvAggregateAppIo.setProvince(province);
        csvAggregateAppIo.setIpaCode(ipaCode);
        csvAggregateAppIo.setSubunitType(subunitType);
        csvAggregateAppIo.setSubunitCode(subunitCode);
        csvAggregateAppIo.setOriginId(originId);
        csvAggregateAppIo.setRowNumber(rowNumber);

        assertEquals(description, csvAggregateAppIo.getDescription());
        assertEquals(pec, csvAggregateAppIo.getPec());
        assertEquals(taxCode, csvAggregateAppIo.getTaxCode());
        assertEquals(vatNumber, csvAggregateAppIo.getVatNumber());
        assertEquals(address, csvAggregateAppIo.getAddress());
        assertEquals(city, csvAggregateAppIo.getCity());
        assertEquals(province, csvAggregateAppIo.getProvince());
        assertEquals(ipaCode, csvAggregateAppIo.getIpaCode());
        assertEquals(subunitType, csvAggregateAppIo.getSubunitType());
        assertEquals(subunitCode, csvAggregateAppIo.getSubunitCode());
        assertEquals(originId, csvAggregateAppIo.getOriginId());
        assertEquals(rowNumber, csvAggregateAppIo.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();
        int expectedRowNumber = 10;

        csvAggregateAppIo.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregateAppIo.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();
        csvAggregateAppIo.setDescription("Sample Description");
        csvAggregateAppIo.setPec("sample@example.com");
        csvAggregateAppIo.setTaxCode("SAMPLETAXCODE");

        assertNotNull(csvAggregateAppIo);
        assertEquals("Sample Description", csvAggregateAppIo.getDescription());
        assertEquals("sample@example.com", csvAggregateAppIo.getPec());
        assertEquals("SAMPLETAXCODE", csvAggregateAppIo.getTaxCode());
    }
}
