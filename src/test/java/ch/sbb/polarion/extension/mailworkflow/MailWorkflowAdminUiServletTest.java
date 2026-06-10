package ch.sbb.polarion.extension.mailworkflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MailWorkflowAdminUiServletTest {

    @Test
    void testInstantiation() {
        assertDoesNotThrow(MailWorkflowAdminUiServlet::new);
    }

}
