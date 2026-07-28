package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.generic.GenericUiServlet;

import java.io.Serial;

/**
 * Serves the React single-page app from its own webapp context ({@code mailworkflow-app}). The
 * admin extenders in hivemodule.xml open it as
 * {@code /polarion/mailworkflow-app/ui/app/index.html?feature=<id>}; everything else about the
 * request handling comes from the generic servlet.
 */
public class MailWorkflowAppServlet extends GenericUiServlet {

    @Serial
    private static final long serialVersionUID = 6893052734118250371L;

    public MailWorkflowAppServlet() {
        super("mailworkflow-app");
    }
}
