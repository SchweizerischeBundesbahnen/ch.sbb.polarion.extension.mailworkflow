package ch.sbb.polarion.extension.mailworkflow;

import ch.sbb.polarion.extension.generic.GenericUiServlet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailWorkflowAppServletTest {

    @Test
    void instantiatesAsGenericUiServlet() {
        MailWorkflowAppServlet servlet = new MailWorkflowAppServlet();

        assertThat(servlet).isInstanceOf(GenericUiServlet.class);
    }
}
