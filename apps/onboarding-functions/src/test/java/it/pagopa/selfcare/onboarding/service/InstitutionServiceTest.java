package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.DeletedUserCountResponse;

@QuarkusTest
class InstitutionServiceTest {

  @Inject
  InstitutionService institutionService;

  @RestClient @InjectMock
  InstitutionApi institutionApi;

  private final String productId = "productId";
  private final String institutionId = "institutionId";

  @Test
  void deleteInstitution() {
    DeletedUserCountResponse response = new DeletedUserCountResponse();
    response.setDeletedUserCount(2L);
    when(institutionApi.deleteUserInstitutionProductUsers(any(), any())).thenReturn(response);
    institutionService.deleteByIdAndProductId(institutionId, productId);

    Mockito.verify(institutionApi, times(1))
            .deleteUserInstitutionProductUsers(any(), any());
  }

  @Test
  void deleteInstitutionWithException() {
    DeletedUserCountResponse response = new DeletedUserCountResponse();
    response.setInstitutionId(institutionId);
    response.setProductId(productId);
    response.setDeletedUserCount(0L);
    when(institutionApi.deleteUserInstitutionProductUsers(any(), any())).thenReturn(response);
    assertThrows(RuntimeException.class, () -> institutionService.deleteByIdAndProductId(institutionId, productId));
  }

}
