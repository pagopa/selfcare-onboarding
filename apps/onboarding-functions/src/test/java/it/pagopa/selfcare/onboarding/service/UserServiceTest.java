package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_json.api.UserApi;

@QuarkusTest
class UserServiceTest {

  @Inject
  UserService userService;
  @RestClient @InjectMock
  UserApi userApi;
  @InjectMock
  OnboardingRepository onboardingRepository;

  private final String productId = "productId";
  private final String userId = "userId";
  private final String institutionId = "institutionId";

  @Test
  void deleteUser() {
    Response response = new ServerResponse(null, 200, null);
    when(userApi.deleteProducts(any(), any(), any())).thenReturn(response);
    userService.deleteByIdAndInstitutionIdAndProductId("userId", institutionId, productId );

    Mockito.verify(userApi, times(1))
            .deleteProducts(any(), any(), any());
  }

  @Test
  void deleteUserWithException() {
    Response response = new ServerResponse(null, 500, null);
    when(userApi.deleteProducts(any(), any(), any())).thenReturn(response);
    assertThrows(RuntimeException.class, () -> userService.deleteByIdAndInstitutionIdAndProductId(userId, institutionId, productId));
  }

  @Test
  void findByInstitutionAndProduct() {
    // given
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setId(institutionId);
    onboarding.setInstitution(institution);
    onboarding.setProductId(productId);
    onboarding.setUsers(List.of());
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
        .thenReturn(List.of(onboarding));
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertTrue(onboardings.isEmpty());

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

  @Test
  void findByInstitutionAndProduct_NotEmptyList() {
    // given
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setId(institutionId);
    onboarding.setInstitution(institution);
    onboarding.setProductId(productId);
    User user = new User();
    user.setId(userId);
    onboarding.setUsers(List.of(user));
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
            .thenReturn(List.of(onboarding));
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertFalse(onboardings.isEmpty());
    assertEquals(1, onboardings.size());
    assertEquals(userId, onboardings.get(0));

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

  @Test
  void findByInstitutionAndProduct_EmptyList() {
    // when
    when(onboardingRepository.findByOnboardingUsers(institutionId, productId))
            .thenReturn(List.of());
    // then
    List<String> onboardings = userService.findByInstitutionAndProduct(institutionId, productId);
    assertNotNull(onboardings);
    assertTrue(onboardings.isEmpty());

    Mockito.verify(onboardingRepository, times(1))
            .findByOnboardingUsers(institutionId, productId);

  }

}
