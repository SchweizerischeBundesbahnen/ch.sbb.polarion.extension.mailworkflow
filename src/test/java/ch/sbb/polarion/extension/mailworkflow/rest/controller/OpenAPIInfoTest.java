package ch.sbb.polarion.extension.mailworkflow.rest.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OpenAPIInfoTest {

    @Test
    void testInstantiation() {
        assertDoesNotThrow(OpenAPIInfo::new);
    }

}
