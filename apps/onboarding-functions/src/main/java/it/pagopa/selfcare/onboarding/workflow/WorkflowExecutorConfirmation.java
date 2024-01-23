package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.entity.Onboarding;

public class WorkflowExecutorConfirmation implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;

    public WorkflowExecutorConfirmation(ObjectMapper objectMapper, TaskOptions optionsRetry) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
    }
    @Override
    public void executeRequestState(TaskOrchestrationContext ctx, Onboarding onboarding) {
    }

    @Override
    public void executeToBeValidatedState(TaskOrchestrationContext ctx, Onboarding onboarding) {
    }

    @Override
    public ObjectMapper objectMapper() {
        return this.objectMapper;
    }

    @Override
    public TaskOptions optionsRetry() {
        return this.optionsRetry;
    }
}
