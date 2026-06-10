package ch.sbb.polarion.extension.mailworkflow.rest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MailWorkflowRestApplicationTest {

    @Test
    void testInstantiation() {
        assertDoesNotThrow(MailWorkflowRestApplication::new);
    }

}
