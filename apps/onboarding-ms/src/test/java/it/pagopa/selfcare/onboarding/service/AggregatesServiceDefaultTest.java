package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@QuarkusTest
class AggregatesServiceDefaultTest {

    @Inject
    AggregatesServiceDefault aggregatesServiceDefault;

    @Inject
    OnboardingMapper onboardingMapper;

    @RestClient
    @InjectMock
    GeographicTaxonomiesApi geographicTaxonomiesApi;

    @RestClient
    @InjectMock
    AooApi aooApi;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @Inject
    CsvService csvService;

    @RestClient
    @InjectMock
    UoApi uoApi;

    @Test
    @RunOnVertxContext
    void testValidateAppIoAggregatesCsv() {

        File file = new File("src/test/resources/aggregates-appio.csv");

        UOResource uoResource = new UOResource();
        uoResource.setMail1("pec@Pec");
        uoResource.setTipoMail1("Pec");
        uoResource.setCodiceUniAoo("18SU3S");
        uoResource.setCap("00100");
        uoResource.setDenominazioneEnte("denominazione");
        uoResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        uoResource.setCodiceComuneISTAT("123");

        AOOResource aooResource = new AOOResource();
        aooResource.setTipoMail1("Altro");
        aooResource.setMail1("pec@Pec");
        aooResource.setCodiceUniAoo("18SU3S");
        aooResource.setDenominazioneEnte("denominazione");
        aooResource.setCap("00100");
        aooResource.setCodiceFiscaleEnte("1307110484");
        aooResource.setIndirizzo("Palazzo Vecchio Piazza Della Signoria");
        aooResource.setCodiceComuneISTAT("123");

        InstitutionResource institutionResource = mock(InstitutionResource.class);
        when(institutionResource.getIstatCode()).thenReturn("123");
        when(institutionResource.getOriginId()).thenReturn("test");
        when(institutionResource.getDigitalAddress()).thenReturn("pec@Pec");

        GeographicTaxonomyResource geographicTaxonomyResource = new GeographicTaxonomyResource();
        geographicTaxonomyResource.setCode("123");
        geographicTaxonomyResource.setDesc("città");
        geographicTaxonomyResource.setProvinceAbbreviation("Provincia");

        WebClientApplicationException webClientApplicationException = mock(WebClientApplicationException.class);
        Response response = mock(Response.class);
        when(webClientApplicationException.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(404);
        when(aooApi.findByUnicodeUsingGET("18SU3S", null)).thenReturn(Uni.createFrom().item(aooResource));
        when(aooApi.findByUnicodeUsingGET("18SU3R", null)).thenReturn(Uni.createFrom().failure(webClientApplicationException));
        when(institutionApi.findInstitutionUsingGET("1307110484", null, null)).thenReturn(Uni.createFrom().item(institutionResource));
        when(uoApi.findByUnicodeUsingGET1("18SU3R", null)).thenReturn(Uni.createFrom().item(uoResource));
        when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET("123")).thenReturn(Uni.createFrom().item(geographicTaxonomyResource));

        VerifyAggregateResponse verifyAggregateResponse = mockResponseForIO();

        UniAssertSubscriber<VerifyAggregateResponse> resp = aggregatesServiceDefault.validateAppIoAggregatesCsv(file)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        Assertions.assertEquals(3, resp.getItem().getAggregates().size());
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(0), resp.getItem().getAggregates().get(0));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(1), resp.getItem().getAggregates().get(1));
        Assertions.assertEquals(verifyAggregateResponse.getAggregates().get(2), resp.getItem().getAggregates().get(2));
        Assertions.assertEquals(6, resp.getItem().getErrors().size());
        Assertions.assertEquals(verifyAggregateResponse.getErrors().get(0), resp.getItem().getErrors().get(0));
        Assertions.assertEquals(verifyAggregateResponse.getErrors().get(1), resp.getItem().getErrors().get(1));
        Assertions.assertEquals(verifyAggregateResponse.getErrors().get(2), resp.getItem().getErrors().get(2));
        Assertions.assertEquals(verifyAggregateResponse.getErrors().get(3), resp.getItem().getErrors().get(3));

        verify(geographicTaxonomiesApi,times(1)).retrieveGeoTaxonomiesByCodeUsingGET("123");
        verify(aooApi,times(1)).findByUnicodeUsingGET("18SU3R", null);
        verify(aooApi,times(1)).findByUnicodeUsingGET("18SU3S", null);
        verify(institutionApi,times(2)).findInstitutionUsingGET("1307110484", null, null);

    }

    private static VerifyAggregateResponse mockResponseForIO() {
        VerifyAggregateResponse verifyAggregateResponse = new VerifyAggregateResponse();
        Aggregate aggregateUO = new Aggregate();
        aggregateUO.setSubunitCode("18SU3R");
        aggregateUO.setSubunitType("UO");
        aggregateUO.setDescription("denominazione");
        aggregateUO.setDigitalAddress("pec@Pec");
        aggregateUO.setTaxCode("1307110484");
        aggregateUO.setVatNumber("1307110484");
        aggregateUO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateUO.setCity("città");
        aggregateUO.setCounty("Provincia");
        aggregateUO.setZipCode("00100");
        aggregateUO.setOrigin("IPA");
        aggregateUO.setRowNumber(1);

        Aggregate aggregateAOO = new Aggregate();
        aggregateAOO.setSubunitCode("18SU3S");
        aggregateAOO.setSubunitType("AOO");
        aggregateAOO.setDescription("denominazione");
        aggregateAOO.setDigitalAddress("pec@Pec");
        aggregateAOO.setTaxCode("1307110484");
        aggregateAOO.setVatNumber("1307110484");
        aggregateAOO.setAddress("Palazzo Vecchio Piazza Della Signoria");
        aggregateAOO.setCity("città");
        aggregateAOO.setCounty("Provincia");
        aggregateAOO.setZipCode("00100");
        aggregateAOO.setOrigin("IPA");
        aggregateAOO.setRowNumber(5);

        Aggregate aggregate = new Aggregate();
        aggregate.setSubunitCode(null);
        aggregate.setSubunitType(null);
        aggregate.setDescription(null);
        aggregate.setDigitalAddress("pec@Pec");
        aggregate.setTaxCode("1307110484");
        aggregate.setVatNumber("1307110484");
        aggregate.setAddress(null);
        aggregate.setCity("città");
        aggregate.setCounty("Provincia");
        aggregate.setZipCode(null);
        aggregate.setOriginId(null);
        aggregate.setOrigin("IPA");
        aggregate.setOriginId("test");
        aggregate.setRowNumber(6);

        verifyAggregateResponse.setAggregates(List.of(aggregateUO, aggregateAOO, aggregate));

        RowError error0 = new RowError(2,"1307110484","SubunitType non valido");
        RowError error1 = new RowError(3,"1307110484","La partita IVA è obbligatoria");
        RowError error2 = new RowError(7,null,"Il codice fiscale è obbligatorio");
        RowError error4 = new RowError(4,"1307110484","Codice fiscale non presente su IPA");
        RowError error3 = new RowError(8,"1307110484","In caso di AOO/UO è necessario specificare la tipologia e il codice univoco IPA AOO/UO");
        verifyAggregateResponse.setErrors(List.of(error0, error1,error4,error2, error3));
        return verifyAggregateResponse;
    }
}
