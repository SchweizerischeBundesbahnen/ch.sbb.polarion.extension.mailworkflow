package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import ch.sbb.polarion.extension.mailworkflow.utils.SendMessageRunnable;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static ch.sbb.polarion.extension.mailworkflow.utils.SendMessageRunnable.PARAM_ARGUMENTS;
import static ch.sbb.polarion.extension.mailworkflow.utils.SendMessageRunnable.PARAM_WORKITEM;

public class MailWorkflow implements IFunction<IWorkflowObject> {
    private static final Logger logger = Logger.getLogger(MailWorkflow.class);

    @Override
    public void execute(@NotNull ICallContext<IWorkflowObject> callContext, @NotNull IArguments arguments) {
        IWorkflowObject workflowObject = callContext.getTarget();

        if (workflowObject instanceof IWorkItem workItem) {
            try {
                BundleJarsPrioritizingRunnable.execute(SendMessageRunnable.class, Map.of(PARAM_WORKITEM, workItem, PARAM_ARGUMENTS, arguments), true);

                if (TransactionalExecutor.currentTransaction() == null) {
                    TransactionalExecutor.executeInWriteTransaction(transaction -> {
                        workItem.save();
                        return null;
                    });
                } else {
                    workItem.save();
                }
            } catch (Exception e) {
                logger.error("Email has not been sent!", e);
            }
        } else {
            logger.error("Caller is not a WorkItem");
        }
    }

}
