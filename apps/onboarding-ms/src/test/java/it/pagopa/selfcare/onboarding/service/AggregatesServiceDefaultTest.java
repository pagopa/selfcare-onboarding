package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.*;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
public class AggregatesServiceDefaultTest {

    @Inject
    AggregatesServiceDefault aggregatesServiceDefault;

    @Inject
    OnboardingMapper onboardingMapper;

    @RestClient
    @InjectMock
    AooApi aooApi;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @RestClient
    @InjectMock
    UoApi uoApi;

    @Test
    @RunOnVertxContext
    void validateAggregates(){
        File testFile = new File("src/test/resources/aggregates.csv");

        when(aooApi.findByUnicodeUsingGET("1437190414", null)).thenReturn(Uni.createFrom().item(new AOOResource()));
        when(institutionApi.findInstitutionUsingGET("00297110389", null, null)).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1("4551120274", null)).thenReturn(Uni.createFrom().item(new UOResource()));

        when(aooApi.findByUnicodeUsingGET("AQ66",null)).thenThrow(ResourceNotFoundException.class);
        when(institutionApi.findInstitutionUsingGET("345645", null, null)).thenThrow(ResourceNotFoundException.class);
        when(uoApi.findByUnicodeUsingGET1("AQ66",null)).thenThrow(ResourceNotFoundException.class);

        aggregatesServiceDefault.validateAppIoAggregatesCsv(testFile)
                        .subscribe().withSubscriber(UniAssertSubscriber.create())
                        .assertCompleted();
    }

    @Test
    @RunOnVertxContext
    void validatePagoPaAggregates(){
        File testFile = new File("src/test/resources/aggregates-pagopa.csv");

        when(aooApi.findByUnicodeUsingGET("1437190414", null)).thenReturn(Uni.createFrom().item(new AOOResource()));
        when(institutionApi.findInstitutionUsingGET("00297110389", null, null)).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1("4551120274", null)).thenReturn(Uni.createFrom().item(new UOResource()));

        when(aooApi.findByUnicodeUsingGET("AQ66",null)).thenThrow(ResourceNotFoundException.class);
        when(institutionApi.findInstitutionUsingGET("345645", null, null)).thenThrow(ResourceNotFoundException.class);
        when(uoApi.findByUnicodeUsingGET1("AQ66",null)).thenThrow(ResourceNotFoundException.class);

        aggregatesServiceDefault.validatePagoPaAggregatesCsv(testFile)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }

    @Test
    @RunOnVertxContext
    void validateSendAggregates(){
        File testFile = new File("src/test/resources/aggregates-send.csv");

        when(aooApi.findByUnicodeUsingGET("1437190414", null)).thenReturn(Uni.createFrom().item(new AOOResource()));
        when(institutionApi.findInstitutionUsingGET("00297110389", null, null)).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1("4551120274", null)).thenReturn(Uni.createFrom().item(new UOResource()));

        when(aooApi.findByUnicodeUsingGET("AQ66",null)).thenThrow(ResourceNotFoundException.class);
        when(institutionApi.findInstitutionUsingGET("345645", null, null)).thenThrow(ResourceNotFoundException.class);
        when(uoApi.findByUnicodeUsingGET1("AQ66",null)).thenThrow(ResourceNotFoundException.class);

        aggregatesServiceDefault.validateSendAggregatesCsv(testFile)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();
    }


    @Test
    void testValidateAppIoAggregatesCsv() {

        File file = new File("src/test/resources/aggregates-appio.csv");

        CsvAggregateAppIo csvAggregate = new CsvAggregateAppIo();
        csvAggregate.setSubunitType("AOO");
        csvAggregate.setSubunitCode("AOO_CODE");

        List<CsvAggregateAppIo> csvAggregateList = new ArrayList<>();
        csvAggregateList.add(csvAggregate);

        AggregatesCsvResponse aggregatesCsvResponse = aggregatesServiceDefault.readItemsFromCsv(file, CsvAggregateAppIo.class);
        when(aooApi.findByUnicodeUsingGET("1437190414", null)).thenReturn(Uni.createFrom().item(new AOOResource()));
        when(institutionApi.findInstitutionUsingGET("00297110389", null, null)).thenReturn(Uni.createFrom().item(new InstitutionResource()));

        Uni<VerifyAggregateResponse> result = aggregatesServiceDefault.validateAppIoAggregatesCsv(file);

        VerifyAggregateResponse verifyAggregateResponse = result.await().indefinitely();
        assertNotNull(verifyAggregateResponse.getAggregates());
        assertNotNull(aggregatesCsvResponse.getValidAggregates());

        assertTrue(!aggregatesCsvResponse.getCsvAggregateList().isEmpty());
        assertTrue(aggregatesCsvResponse.getRowErrorList().isEmpty());

        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getAddress());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getCity());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getPec());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getDescription());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getRowNumber());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getSubunitCode());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getTaxCode());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getProvince());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getSubunitType());
        assertNotNull(((CsvAggregateAppIo) aggregatesCsvResponse.getCsvAggregateList().get(0)).getVatNumber());

        verify(aooApi,times(0)).findByUnicodeUsingGET("1437190414", null);
        verify(institutionApi,times(0)).findInstitutionUsingGET("00297110389", null, null);

    }

    @Test
    void testValidateSendAggregatesCsv() {

        File file = new File("src/test/resources/aggregates-send.csv");

        CsvAggregateSend csvAggregate = new CsvAggregateSend();
        csvAggregate.setSubunitType("AOO");
        csvAggregate.setSubunitCode("AOO_CODE");

        List<CsvAggregateSend> csvAggregateList = new ArrayList<>();
        csvAggregateList.add(csvAggregate);

        AggregatesCsvResponse aggregatesCsvResponse = aggregatesServiceDefault.readItemsFromCsv(file, CsvAggregateSend.class);
        when(aooApi.findByUnicodeUsingGET("1437190414", null)).thenReturn(Uni.createFrom().item(new AOOResource()));
        when(institutionApi.findInstitutionUsingGET("00297110389", null, null)).thenReturn(Uni.createFrom().item(new InstitutionResource()));
        when(uoApi.findByUnicodeUsingGET1("4551120274", null)).thenReturn(Uni.createFrom().item(new UOResource()));

        Uni<VerifyAggregateResponse> result = aggregatesServiceDefault.validateAppIoAggregatesCsv(file);

        VerifyAggregateResponse verifyAggregateResponse = result.await().indefinitely();
        assertNotNull(verifyAggregateResponse);
        assertNotNull(aggregatesCsvResponse.getValidAggregates());

        assertTrue(!aggregatesCsvResponse.getCsvAggregateList().isEmpty());
        assertTrue(aggregatesCsvResponse.getRowErrorList().isEmpty());

        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getAddress());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getCity());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getPec());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getDescription());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getRowNumber());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getSubunitCode());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getTaxCode());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getProvince());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getSubunitType());
        assertNotNull(((CsvAggregateSend) aggregatesCsvResponse.getCsvAggregateList().get(0)).getVatNumber());

        verify(aooApi,times(0)).findByUnicodeUsingGET("1437190414", null);
        verify(institutionApi,times(0)).findInstitutionUsingGET("00297110389", null, null);
        verify(uoApi,times(0)).findByUnicodeUsingGET1("4551120274", null);

    }

    @Test
    void testValidatePagoPaAggregatesCsv() {

        File file = new File("src/test/resources/aggregates-pagopa.csv");

        CsvAggregatePagoPa csvAggregate = new CsvAggregatePagoPa();

        List<CsvAggregatePagoPa> csvAggregateList = new ArrayList<>();
        csvAggregateList.add(csvAggregate);

        AggregatesCsvResponse aggregatesCsvResponse = aggregatesServiceDefault.readItemsFromCsv(file, CsvAggregatePagoPa.class);
        Uni<VerifyAggregateResponse> result = aggregatesServiceDefault.validateAppIoAggregatesCsv(file);

        VerifyAggregateResponse verifyAggregateResponse = result.await().indefinitely();
        assertNotNull(verifyAggregateResponse);
        assertNotNull(aggregatesCsvResponse.getValidAggregates());

        assertTrue(!aggregatesCsvResponse.getCsvAggregateList().isEmpty());
        assertTrue(aggregatesCsvResponse.getRowErrorList().isEmpty());

        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getAddress());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getCity());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getPec());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getDescription());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getRowNumber());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getTaxCode());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getProvince());
        assertNotNull(((CsvAggregatePagoPa) aggregatesCsvResponse.getCsvAggregateList().get(0)).getVatNumber());

    }

}
