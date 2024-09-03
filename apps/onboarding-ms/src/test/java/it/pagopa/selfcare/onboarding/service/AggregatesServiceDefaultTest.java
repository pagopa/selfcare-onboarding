package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.model.VerifyAggregateResponse;
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
import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Mockito.when;

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

}
