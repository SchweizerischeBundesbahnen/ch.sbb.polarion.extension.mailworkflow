package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.mailworkflow.service.MailService;
import com.polarion.alm.shared.api.transaction.internal.InternalWriteTransaction;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
@SuppressWarnings("unchecked")
class MailWorkflowTest {

    // The active transaction the TransactionalExecutorExtension mocks into the context and returns from
    // TransactionalExecutor.currentTransaction(). With it present, MailWorkflow saves directly (no new write transaction).
    @CustomExtensionMock
    private InternalWriteTransaction activeTransaction;

    private MockedConstruction<MailService> mailServiceMockedConstruction;

    @BeforeEach
    void beforeEach() {
        this.mailServiceMockedConstruction = Mockito.mockConstruction(MailService.class,
                (mock, context) -> doNothing().when(mock).sendWorkflowMessage(any(), any()));
    }

    @AfterEach
    void afterEach() {
        this.mailServiceMockedConstruction.close();
    }

    @Test
    void testExecuteWithWorkItem() {
        MailWorkflow mailWorkflow = new MailWorkflow();

        ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
        IWorkItem workItem = mock(IWorkItem.class);
        IArguments arguments = mock(IArguments.class);

        when(callContext.getTarget()).thenReturn(workItem);

        clearInvocations(activeTransaction); // drop interactions recorded by the extension while stubbing
        mailWorkflow.execute(callContext, arguments);

        // A transaction is already active -> the work item is saved directly, the active transaction is not touched
        verify(workItem, times(1)).save();
        verifyNoMoreInteractions(activeTransaction);
    }

    @Test
    void testExecuteWithNonWorkItem() {
        MailWorkflow mailWorkflow = new MailWorkflow();

        ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
        IWorkflowObject workflowObject = mock(IWorkflowObject.class);
        IArguments arguments = mock(IArguments.class);

        when(callContext.getTarget()).thenReturn(workflowObject);

        assertDoesNotThrow(() -> mailWorkflow.execute(callContext, arguments));
    }

    @Test
    void testExecuteLogsErrorWhenSendFails() {
        mailServiceMockedConstruction.close();
        mailServiceMockedConstruction = Mockito.mockConstruction(MailService.class,
                (mock, context) -> doThrow(new RuntimeException("SMTP unavailable")).when(mock).sendWorkflowMessage(any(), any()));

        MailWorkflow mailWorkflow = new MailWorkflow();

        ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
        IWorkItem workItem = mock(IWorkItem.class);
        IArguments arguments = mock(IArguments.class);

        when(callContext.getTarget()).thenReturn(workItem);

        assertDoesNotThrow(() -> mailWorkflow.execute(callContext, arguments));
        verify(workItem, never()).save();
    }

}
