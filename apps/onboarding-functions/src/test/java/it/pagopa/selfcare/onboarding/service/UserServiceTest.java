package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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

}
