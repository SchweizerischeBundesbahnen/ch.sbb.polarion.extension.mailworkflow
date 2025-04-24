package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.mailworkflow.service.MailService;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkflowObject;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.tracker.workflow.ICallContext;
import com.polarion.alm.tracker.workflow.IFunction;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class MailWorkflow implements IFunction<IWorkflowObject> {
    private static final Logger logger = Logger.getLogger(MailWorkflow.class);

    @Override
    public void execute(@NotNull ICallContext<IWorkflowObject> callContext, @NotNull IArguments arguments) {
        IWorkflowObject workflowObject = callContext.getTarget();

        if (workflowObject instanceof IWorkItem workItem) {
            try {
                new MailService().sendWorkflowMessage(workItem, arguments);

                TransactionalExecutor.executeInWriteTransaction(transaction -> {
                    workItem.save();
                    return null;
                });
            } catch (Exception e) {
                logger.error("Email has not been sent!", e);
            }
        } else {
            logger.error("Caller is not a WorkItem");
        }
    }

}
