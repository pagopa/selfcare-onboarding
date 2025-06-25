package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.service.ContractService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.openapi.quarkus.openapi_onboarding_json.api.InternalV1Api;

import java.util.Optional;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class TokenFunctionsTest {

  @Inject
  TokenFunctions function;

  @InjectMock
  OnboardingService onboardingService;

  @InjectMock
  ContractService contractService;

  @InjectMock
  @RestClient
  InternalV1Api internalV1Api;

  @Inject
  ObjectMapper objectMapper;

  static ExecutionContext executionContext;

  static {
    executionContext = mock(ExecutionContext.class);
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void deleteContract() throws JsonProcessingException {
//    Token tokenOriginal = new Token();
//    tokenOriginal.setContractSigned("parties/docs/123/file.pdf");
//    Token tokenDeleted = new Token();
//    tokenDeleted.setContractSigned("parties/deleted/123/file.pdf");

    EntityFilter entity = EntityFilter.builder().value("123").build();
    String params = objectMapper.writeValueAsString(entity);
    function.deleteContract(params, executionContext);

    when(internalV1Api.removeDocument(any())).thenReturn(1l);
//
//
//
//    when(onboardingService.getToken(anyString())).thenReturn(Optional.of(tokenOriginal));
//    when(contractService.deleteContract(any())).thenReturn(tokenDeleted);
//    doNothing().when(onboardingService).updateTokenContractSigned(any());



    verify(onboardingService, times(1)).getToken(any());
  }
}
