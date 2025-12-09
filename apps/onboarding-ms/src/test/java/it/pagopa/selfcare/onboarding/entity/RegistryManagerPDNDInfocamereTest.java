package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.registry.RegistryManagerPDNDInfocamere;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.onboarding.common.ProductId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class RegistryManagerPDNDInfocamereTest {

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    @InjectMock
    @RestClient
    InfocamerePdndApi infocamerePdndApi;

    private RegistryManagerPDNDInfocamere manager;
    private Onboarding baseOnboarding;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        reset(userRegistryApi, infocamerePdndApi);

        baseOnboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PRV_PF);
        institution.setTaxCode("RSSMRA80A01H501U");
        institution.setDigitalAddress("test@pec.it");
        institution.setDescription("Test Business Name");
        baseOnboarding.setInstitution(institution);

        manager = new RegistryManagerPDNDInfocamere(baseOnboarding, infocamerePdndApi, userRegistryApi, Optional.of("01.11.00,01.12.00,01.13.00"));

        PDNDBusinessResource resource = new PDNDBusinessResource();
        resource.setDigitalAddress("test@pec.it");
        resource.setBusinessName("Test Business Name");
        manager.setRegistryResource(resource);
    }

    @Test
    void customValidation_shouldUpdateOnboarding_whenUserExists() {
        UUID existingUserId = UUID.randomUUID();
        UserResource existingUser = new UserResource();
        existingUser.setId(existingUserId);

        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().item(existingUser));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertEquals(existingUserId.toString(), result.getInstitution().getTaxCode());
        assertEquals(existingUserId.toString(), result.getInstitution().getOriginId());

        verify(userRegistryApi, never()).saveUsingPATCH(any());
        verify(userRegistryApi, times(1)).searchUsingPOST(any(), any());
    }

    @Test
    void customValidation_shouldCreateUser_whenUserNotFound404() {
        UUID newUserIdValue = UUID.randomUUID();
        UserId userIdResponse = new UserId();
        userIdResponse.setId(newUserIdValue);

        WebApplicationException notFoundException = new WebApplicationException(Response.Status.NOT_FOUND);
        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().failure(notFoundException));

        when(userRegistryApi.saveUsingPATCH(any(SaveUserDto.class)))
                .thenReturn(Uni.createFrom().item(userIdResponse));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertEquals(newUserIdValue.toString(), result.getInstitution().getTaxCode());
        assertEquals(newUserIdValue.toString(), result.getInstitution().getOriginId());

        ArgumentCaptor<SaveUserDto> captor = ArgumentCaptor.forClass(SaveUserDto.class);
        verify(userRegistryApi, times(1)).saveUsingPATCH(captor.capture());
        assertNotEquals(baseOnboarding.getInstitution().getTaxCode(), captor.getValue().getFiscalCode());
    }

    @Test
    void customValidation_shouldFail_whenSearchFailsWithNon404Error() {
        WebApplicationException serverErrorException = new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().failure(serverErrorException));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        verify(userRegistryApi, never()).saveUsingPATCH(any());
    }

    @Test
    void customValidation_shouldDoNothing_whenInstitutionTypeIsNotPrvPf() {

        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        Onboarding result = subscriber.awaitItem().getItem();

        assertSame(baseOnboarding, result);
        assertEquals("RSSMRA80A01H501U", result.getInstitution().getTaxCode());

        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void isValid_shouldReturnTrue_whenDataMatches() {
        UniAssertSubscriber<Boolean> subscriber = manager.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        assertTrue(subscriber.awaitItem().getItem());
    }

    /*@Test
    void isValid_shouldFail_whenDataDoesNotMatch() {
        manager.getRegistryResource().setDigitalAddress("another@pec.it");

        UniAssertSubscriber<Boolean> subscriber = manager.isValid()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Field digitalAddress or description are not valid", failure.getMessage());
    }*/

    @Test
    void validateAtecoCodes_shouldThrowException_whenAtecosAreNull() {
        baseOnboarding.getInstitution().setAtecoCodes(null);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Institution must have at least one ATECO code", failure.getMessage());
    }

    @Test
    void validateAtecoCodes_shouldThrowException_whenAtecosAreEmpty() {
        baseOnboarding.getInstitution().setAtecoCodes(new ArrayList<>());
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Institution must have at least one ATECO code", failure.getMessage());
    }

    @Test
    void validateAtecoCodes_shouldSucceed_whenAtecosContainValidCode() {
        baseOnboarding.getInstitution().setAtecoCodes(List.of("01.11.00"));
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertNotNull(result);
        assertEquals("01.11.00", result.getInstitution().getAtecoCodes().get(0));
    }

    @Test
    void validateAtecoCodes_shouldThrowException_whenAtecosContainInvalidCode() {
        baseOnboarding.getInstitution().setAtecoCodes(List.of("99.99.99"));
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Institution ATECO codes are not allowed for this product", failure.getMessage());
    }

    @Test
    void validateAtecoCodes_shouldSucceed_whenAtecosContainMultipleCodesWithValidOne() {
        baseOnboarding.getInstitution().setAtecoCodes(List.of("99.99.99", "01.12.00", "88.88.88"));
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertNotNull(result);
        assertTrue(result.getInstitution().getAtecoCodes().contains("01.12.00"));
    }

    @Test
    void validateAtecoCodes_shouldThrowException_whenAtecosContainMultipleCodesButNoneValid() {
        baseOnboarding.getInstitution().setAtecoCodes(List.of("99.99.99", "88.88.88", "77.77.77"));
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        
        Throwable failure = subscriber.awaitFailure().getFailure();
        assertInstanceOf(InvalidRequestException.class, failure);
        assertEquals("Institution ATECO codes are not allowed for this product", failure.getMessage());
    }

    @Test
    void validateAtecoCodes_shouldSucceed_whenAtecosContainValidCodeWithSpaces() {
        baseOnboarding.getInstitution().setAtecoCodes(List.of(" 01.13.00 "));
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_IDPAY_MERCHANT.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertNotNull(result);
        assertEquals(" 01.13.00 ", result.getInstitution().getAtecoCodes().get(0));
    }

    @Test
    void validateAtecoCodes_shouldNotBeCalledWhenProductIsNull() {
        baseOnboarding.getInstitution().setAtecoCodes(null);
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);

        UUID existingUserId = UUID.randomUUID();
        UserResource existingUser = new UserResource();
        existingUser.setId(existingUserId);

        when(userRegistryApi.searchUsingPOST(anyString(), any(UserSearchDto.class)))
                .thenReturn(Uni.createFrom().item(existingUser));

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertNotNull(result);
        assertEquals(existingUserId.toString(), result.getInstitution().getTaxCode());
    }

    @Test
    void validateAtecoCodes_shouldNotBeCalledWhenProductIdIsDifferent() {
        baseOnboarding.getInstitution().setAtecoCodes(null);
        baseOnboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        Product product = new Product();
        product.setId(ProductId.PROD_INTEROP.getValue());

        UniAssertSubscriber<Onboarding> subscriber = manager.customValidation(product)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        Onboarding result = subscriber.awaitItem().getItem();
        assertNotNull(result);
    }
}