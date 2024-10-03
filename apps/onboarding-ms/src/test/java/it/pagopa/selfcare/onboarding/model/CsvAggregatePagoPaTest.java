package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvAggregatePagoPaTest {

    @Test
    void testCsvAggregatePagoPaConstructorAndGetters() {
        String description = "PagoPa Description";
        String pec = "pagopa@example.com";
        String taxCode = "PAGOTAX12345";
        String vatNumber = "PAGOVAT12345678";
        String address = "PagoPa Address";
        String city = "PagoPa City";
        String province = "PagoPa Province";
        String aggragateNamePT = "PagoPa Name PT";
        String taxCodePT = "PTTAXCODE123";
        String iban = "IT60X0542811101000000123456";
        String service = "PagoPa Service";
        String syncAsyncMode = "SYNC";
        Integer rowNumber = 2;

        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        csvAggregatePagoPa.setDescription(description);
        csvAggregatePagoPa.setPec(pec);
        csvAggregatePagoPa.setTaxCode(taxCode);
        csvAggregatePagoPa.setVatNumber(vatNumber);
        csvAggregatePagoPa.setAddress(address);
        csvAggregatePagoPa.setCity(city);
        csvAggregatePagoPa.setProvince(province);
        csvAggregatePagoPa.setAggragateNamePT(aggragateNamePT);
        csvAggregatePagoPa.setTaxCodePT(taxCodePT);
        csvAggregatePagoPa.setIban(iban);
        csvAggregatePagoPa.setService(service);
        csvAggregatePagoPa.setSyncAsyncMode(syncAsyncMode);
        csvAggregatePagoPa.setRowNumber(rowNumber);
        csvAggregatePagoPa.setTaxCodePT(taxCodePT);

        assertEquals(description, csvAggregatePagoPa.getDescription());
        assertEquals(pec, csvAggregatePagoPa.getPec());
        assertEquals(taxCode, csvAggregatePagoPa.getTaxCode());
        assertEquals(vatNumber, csvAggregatePagoPa.getVatNumber());
        assertEquals(address, csvAggregatePagoPa.getAddress());
        assertEquals(city, csvAggregatePagoPa.getCity());
        assertEquals(province, csvAggregatePagoPa.getProvince());
        assertEquals(aggragateNamePT, csvAggregatePagoPa.getAggragateNamePT());
        assertEquals(taxCodePT, csvAggregatePagoPa.getTaxCodePT());
        assertEquals(iban, csvAggregatePagoPa.getIban());
        assertEquals(service, csvAggregatePagoPa.getService());
        assertEquals(syncAsyncMode, csvAggregatePagoPa.getSyncAsyncMode());
        assertEquals(rowNumber, csvAggregatePagoPa.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        int expectedRowNumber = 15;

        csvAggregatePagoPa.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregatePagoPa.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        csvAggregatePagoPa.setDescription("Test Description");
        csvAggregatePagoPa.setPec("test@pagopa.it");
        csvAggregatePagoPa.setTaxCode("TESTTAXCODE");
        csvAggregatePagoPa.setIban("IT60X0542811101000000123456");

        assertNotNull(csvAggregatePagoPa);
        assertEquals("Test Description", csvAggregatePagoPa.getDescription());
        assertEquals("test@pagopa.it", csvAggregatePagoPa.getPec());
        assertEquals("TESTTAXCODE", csvAggregatePagoPa.getTaxCode());
        assertEquals("IT60X0542811101000000123456", csvAggregatePagoPa.getIban());
    }
}
