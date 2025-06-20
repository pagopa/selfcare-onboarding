package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.dto.EntityFilter;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.service.ContractService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.DELETE_TOKEN_CONTRACT_ACTIVITY_NAME;

public class TokenFunctions {
  private static final Logger logger = LoggerFactory.getLogger(InstitutionFunctions.class.getName());
  private static final String FORMAT_LOGGER_INSTITUTION_STRING = "%s: %s";
  private final OnboardingService onboardingService;
  private final ContractService contractService;
  private final ObjectMapper objectMapper;

  public TokenFunctions(ObjectMapper objectMapper, OnboardingService onboardingService, ContractService contractService) {
    this.objectMapper = objectMapper;
    this.onboardingService = onboardingService;
    this.contractService = contractService;
  }


  /**
   * Deletion contract from document storage after copy this blob to deleted contract storage
   */
  @FunctionName(DELETE_TOKEN_CONTRACT_ACTIVITY_NAME)
  public void deleteContract(
    @DurableOrchestrationTrigger(name = "filtersString") String filtersString,
    ExecutionContext functionContext) throws JsonProcessingException {

    functionContext
      .getLogger()
      .info(() -> String.format(FORMAT_LOGGER_INSTITUTION_STRING, DELETE_TOKEN_CONTRACT_ACTIVITY_NAME, filtersString));

    EntityFilter entityFilter = objectMapper.readValue(filtersString, EntityFilter.class);
    Optional<Token> token = onboardingService.getToken(entityFilter.getValue());
    token.ifPresent(t -> {
      Token newToken = contractService.deleteContract(entityFilter.getValue(), t);
      onboardingService.updateTokenContractSigned(newToken);
    });
    functionContext.getLogger().info("DeleteInstitutionAndUser orchestration completed");
  }
}
