package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static it.pagopa.selfcare.onboarding.controller.OnboardingControllerTest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(OnboardingV2Controller.class)
@QuarkusTestResource(MongoTestResource.class)
class OnboardingV2ControllerTest {

    static final UserRequest userDTO;
    @InjectMock
    OnboardingService onboardingService;

    static {
        userDTO = new UserRequest();
        userDTO.setTaxCode("taxCode");
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletion() {

        OnboardingDefaultRequest onboardingDefaultRequest = dummyOnboardingDefaultRequest();

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingDefaultRequest)
                .contentType(ContentType.JSON)
                .post("/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any());
        assertEquals(InstitutionType.PRV, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPaCompletion() {

        OnboardingPaRequest onboardingPaRequest = dummyOnboardingPa();

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPaRequest)
                .contentType(ContentType.JSON)
                .post("/pa/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any());
        assertEquals(InstitutionType.PA, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingCompletePg() {

        OnboardingPgRequest onboardingPgRequest = new OnboardingPgRequest();
        onboardingPgRequest.setProductId("productId");
        onboardingPgRequest.setUsers(List.of(userDTO));
        onboardingPgRequest.setTaxCode("taxCode");
        onboardingPgRequest.setDigitalAddress("digital@address.it");
        onboardingPgRequest.setOrigin(Origin.INFOCAMERE);

        Mockito.when(onboardingService.onboardingPgCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPgRequest)
                .contentType(ContentType.JSON)
                .post("/pg/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingPgCompletion(captor.capture(), any());
        assertEquals(InstitutionType.PG, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void onboardingPspCompletion() {

        OnboardingPspRequest onboardingPspRequest = getOnboardingPspRequest();

        Mockito.when(onboardingService.onboardingCompletion(any(), any()))
                .thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        given()
                .when()
                .body(onboardingPspRequest)
                .contentType(ContentType.JSON)
                .post("/psp/completion")
                .then()
                .statusCode(200);

        ArgumentCaptor<Onboarding> captor = ArgumentCaptor.forClass(Onboarding.class);
        Mockito.verify(onboardingService, times(1)).onboardingCompletion(captor.capture(), any());
        assertEquals(InstitutionType.PSP, captor.getValue().getInstitution().getInstitutionType());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionOnboardings() {
        // given
        OnboardingResponse onboardingResponse = dummyOnboardingResponse();
        List<OnboardingResponse> onboardingResponses = new ArrayList<>();
        onboardingResponses.add(onboardingResponse);
        when(onboardingService.institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING))
                .thenReturn(Uni.createFrom().item(onboardingResponses));

        Map<String, String> queryParameterMap = getStringStringMapOnboardings();

        // when
        given()
                .when()
                .queryParams(queryParameterMap)
                .get("/institutionOnboardings")
                .then()
                .statusCode(200);

        // then
        verify(onboardingService, times(1))
                .institutionOnboardings("taxCode", "subunitCode", "origin", "originId", OnboardingStatus.PENDING);
        assertNotNull(onboardingResponses);
    }


}