package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.*;
import jakarta.inject.Inject;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Unit test for Institution Function class.
 */
@QuarkusTest
public class InstitutionFunctionsTest {

    @Inject
    InstitutionFunctions function;

    @InjectMock
    InstitutionService institutionService;

    @InjectMock
    UserService userService;

    final String institutionUserFilters = "{\"userId\":\"userId\",\"productId\":\"productId\",\"institutionId\":\"institutionId\"}";

    static ExecutionContext executionContext;

    static {
        executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    }

    @Test
    void deleteInstitutionAndUser_validBody_returnsAccepted() {
        // Mock HttpRequestMessage with valid body
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        doReturn(Optional.of(institutionUserFilters)).when(req).getBody();

        doAnswer(
                (Answer<HttpResponseMessage.Builder>)
                        invocation -> {
                            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                                    .status(status);
                        })
                .when(req)
                .createResponseBuilder(any(HttpStatus.class));

        final DurableClientContext durableContext = mock(DurableClientContext.class);
        final DurableTaskClient client = mock(DurableTaskClient.class);
        final String instanceId = "instanceId123";

        doReturn(client).when(durableContext).getClient();
        doReturn(instanceId)
                .when(client)
                .scheduleNewOrchestrationInstance("DeleteInstitutionAndUser", institutionUserFilters);
        when(durableContext.createCheckStatusResponse(any(), any()))
                .thenReturn(
                        new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                                .status(HttpStatus.ACCEPTED)
                                .build());

        // Invoke
        HttpResponseMessage responseMessage =
                function.deleteInstitutionAndUserTrigger(req, durableContext, executionContext);

        // Verify
        assertEquals(HttpStatus.ACCEPTED.value(), responseMessage.getStatusCode());
    }

    @Test
    void deleteInstitutionAndUser_emptyBody_returnsBadRequest() {
        // Mock HttpRequestMessage with empty body
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        doReturn(Optional.empty()).when(req).getBody();

        doAnswer(
                (Answer<HttpResponseMessage.Builder>)
                        invocation -> {
                            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
                            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                                    .status(status);
                        })
                .when(req)
                .createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);

        // Invoke
        HttpResponseMessage responseMessage =
                function.deleteInstitutionAndUserTrigger(req, durableContext, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
        assertEquals("Body can't be empty", responseMessage.getBody());
    }

    @Test
    void deleteInstitutionAndUser_invokeActivity() throws JsonProcessingException {
        // given
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        when(orchestrationContext.getInput(String.class)).thenReturn(institutionUserFilters);

        Task task = mock(Task.class);
        when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
        when(task.await()).thenReturn("false");
        when(orchestrationContext.allOf(anyList())).thenReturn(task);

        // when
        function.deleteInstitutionAndUser(orchestrationContext, executionContext);

        // then
        Mockito.verify(orchestrationContext, times(2)).callActivity(any(), any(), any(), any());
    }

    @Test
    void deleteInstitution() throws JsonProcessingException {

        doNothing().when(institutionService).deleteByIdAndProductId(any(), any());

        function.deleteInstitution(institutionUserFilters, executionContext);

        verify(institutionService, times(1)).deleteByIdAndProductId(any(), any());
    }

    @Test
    void deleteUser() throws JsonProcessingException {

        doNothing().when(userService).deleteByIdAndInstitutionIdAndProductId(any(), any(), any());

        function.deleteUser(institutionUserFilters, executionContext);

        verify(userService, times(1)).deleteByIdAndInstitutionIdAndProductId(any(), any(), any());
    }
}

