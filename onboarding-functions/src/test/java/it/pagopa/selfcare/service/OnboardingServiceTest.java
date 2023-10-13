package it.pagopa.selfcare.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.config.OnboardingFunctionConfig;
import it.pagopa.selfcare.entity.Onboarding;
import it.pagopa.selfcare.entity.User;
import it.pagopa.selfcare.exception.GenericOnboardingException;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.repository.OnboardingRepository;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.List;
import java.util.UUID;

import static it.pagopa.selfcare.service.OnboardingService.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
public class OnboardingServiceTest {

    @InjectMock
    OnboardingRepository onboardingRepository;
    @RestClient
    @InjectMock
    UserApi userRegistryApi;
    @InjectMock
    NotificationService notificationService;
    @InjectMock
    ContractService contractService;
    @InjectMock
    ProductService productService;

    @Inject
    OnboardingService onboardingService;

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(ObjectId.get());
        onboarding.setProductId("productId");
        onboarding.setUsers(List.of());
        return onboarding;
    }

    @Test
    void getOnboarding() {
        Onboarding onboarding = createOnboarding();
        Mockito.when(onboardingRepository.findById(onboarding.getId())).thenReturn(onboarding);

        Onboarding actual = onboardingService.getOnboarding(onboarding.getId().toHexString());
        assertEquals(onboarding.getId().toHexString(), actual.getId().toHexString());
    }

    @Test
    void createContract_shouldThrowIfManagerNotfound() {
        Onboarding onboarding = createOnboarding();
        assertThrows(GenericOnboardingException.class, () -> onboardingService.createContract(onboarding));
    }

    @Test
    void createContract() {

        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());

        UserResource delegateResource = new UserResource();
        delegateResource.setId(UUID.randomUUID());

        Onboarding onboarding = createOnboarding();
        User manager = new User();
        manager.setId(userResource.getId().toString());
        manager.setRole(PartyRole.MANAGER);
        User delegate = new User();
        delegate.setId(delegateResource.getId().toString());
        delegate.setRole(PartyRole.DELEGATE);
        onboarding.setUsers(List.of(manager, delegate));

        Mockito.when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,manager.getId()))
                        .thenReturn(userResource);

        Mockito.when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST,delegate.getId()))
                .thenReturn(delegateResource);

        Mockito.when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(new Product());

        onboardingService.createContract(onboarding);

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_FIELD_LIST,manager.getId());

        Mockito.verify(userRegistryApi, Mockito.times(1))
                .findByIdUsingGET(USERS_FIELD_LIST,delegate.getId());

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());
    }



    @Test
    void loadContract() {

        Onboarding onboarding = createOnboarding();

        Mockito.when(productService.getProductIsValid(onboarding.getProductId()))
                .thenReturn(new Product());

        onboardingService.loadContract(onboarding);

        Mockito.verify(productService, Mockito.times(1))
                .getProductIsValid(onboarding.getProductId());
    }
}
