package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.model.AggregatesCsv;
import it.pagopa.selfcare.onboarding.model.CsvAggregateAppIo;
import it.pagopa.selfcare.onboarding.service.profile.OnboardingTestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(OnboardingTestProfile.class)
class CsvServiceTest {

    @Inject
    CsvService csvService;

    @Test
    void readItemsFromCsv_withValidCsvFile_returnsAggregatesCsv() {
        File file = new File("src/test/resources/test-read.csv");

        AggregatesCsv<CsvAggregateAppIo> result = csvService.readItemsFromCsv(file, CsvAggregateAppIo.class);

        assertNotNull(result);
        assertEquals(1, result.getCsvAggregateList().size());
        assertTrue(result.getRowErrorList().isEmpty());
    }
}