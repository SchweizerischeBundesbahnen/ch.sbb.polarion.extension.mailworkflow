package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.mailworkflow.service.MailService;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the "no active transaction" branch of {@link MailWorkflow}, where it has to open a write transaction itself.
 * This case can't use {@code TransactionalExecutorExtension}: that extension always reports an active transaction
 * (currentTransaction() != null), so the transactional context is mocked directly here instead.
 */
@SuppressWarnings("unchecked")
class MailWorkflowNoActiveTransactionTest {

    @Test
    void testExecuteOpensWriteTransactionWhenNoneActive() {
        try (MockedStatic<TransactionalExecutor> txExecutor = mockStatic(TransactionalExecutor.class);
             MockedConstruction<MailService> ignored = mockConstruction(MailService.class)) {

            txExecutor.when(TransactionalExecutor::currentTransaction).thenReturn(null);
            txExecutor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction<Object> runnable = invocation.getArgument(0);
                return runnable.run(mock(WriteTransaction.class));
            });

            ICallContext<IWorkflowObject> callContext = mock(ICallContext.class);
            IWorkItem workItem = mock(IWorkItem.class);
            IArguments arguments = mock(IArguments.class);
            when(callContext.getTarget()).thenReturn(workItem);

            new MailWorkflow().execute(callContext, arguments);

            verify(workItem, times(1)).save();
        }
    }

}
