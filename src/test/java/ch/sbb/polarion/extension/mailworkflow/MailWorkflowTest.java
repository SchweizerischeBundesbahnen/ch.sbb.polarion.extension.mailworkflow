package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TransactionalExecutorExtension.class})
@SuppressWarnings("unchecked")
class MailWorkflowTest {

    private MockedStatic<BundleJarsPrioritizingRunnable> prioritizingRunnableMockedStatic;

    @BeforeEach
    public void beforeEach() {
        this.prioritizingRunnableMockedStatic = Mockito.mockStatic(BundleJarsPrioritizingRunnable.class, Mockito.RETURNS_DEEP_STUBS);
        prioritizingRunnableMockedStatic.when(() -> BundleJarsPrioritizingRunnable.execute(any(), any(), anyBoolean())).thenAnswer(invocation -> null);
    }

    @AfterEach
    public void afterEach() {
        this.prioritizingRunnableMockedStatic.close();
    }

    @Test
    void testExecuteWithWorkItem() {
        MailWorkflow mailWorkflow = new MailWorkflow();

        ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
        IWorkItem workItem = mock(IWorkItem.class);
        IArguments arguments = mock(IArguments.class);

        when(callContext.getTarget()).thenReturn(workItem);

        mailWorkflow.execute(callContext, arguments);

        verify(workItem, times(1)).save();
    }

    @Test
    void testExecuteWithNonWorkItem() {
        MailWorkflow mailWorkflow = new MailWorkflow();

        ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
        IWorkflowObject workflowObject = mock(IWorkflowObject.class);
        IArguments arguments = mock(IArguments.class);

        when(callContext.getTarget()).thenReturn(workflowObject);

        mailWorkflow.execute(callContext, arguments);
    }

}
