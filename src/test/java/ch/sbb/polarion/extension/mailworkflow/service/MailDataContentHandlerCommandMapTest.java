package ch.sbb.polarion.extension.mailworkflow.service;

import jakarta.activation.CommandInfo;
import jakarta.activation.CommandMap;
import jakarta.activation.DataContentHandler;
import org.eclipse.angus.mail.handlers.message_rfc822;
import org.eclipse.angus.mail.handlers.multipart_mixed;
import org.eclipse.angus.mail.handlers.text_html;
import org.eclipse.angus.mail.handlers.text_plain;
import org.eclipse.angus.mail.handlers.text_xml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MailDataContentHandlerCommandMapTest {

    @Test
    void testReturnsAngusHandlersForMailTypes() {
        CommandMap delegate = mock(CommandMap.class);
        MailDataContentHandlerCommandMap commandMap = new MailDataContentHandlerCommandMap(delegate);

        assertInstanceOf(multipart_mixed.class, commandMap.createDataContentHandler("multipart/mixed; boundary=\"x\""));
        assertInstanceOf(multipart_mixed.class, commandMap.createDataContentHandler("MULTIPART/RELATED"));
        assertInstanceOf(text_plain.class, commandMap.createDataContentHandler("text/plain"));
        assertInstanceOf(text_html.class, commandMap.createDataContentHandler("text/html"));
        assertInstanceOf(text_xml.class, commandMap.createDataContentHandler("text/xml"));
        assertInstanceOf(message_rfc822.class, commandMap.createDataContentHandler("message/rfc822"));

        verifyNoInteractions(delegate);
    }

    @Test
    void testDelegatesUnknownMimeType() {
        CommandMap delegate = mock(CommandMap.class);
        DataContentHandler delegated = mock(DataContentHandler.class);
        when(delegate.createDataContentHandler("text/calendar")).thenReturn(delegated);

        MailDataContentHandlerCommandMap commandMap = new MailDataContentHandlerCommandMap(delegate);

        assertSame(delegated, commandMap.createDataContentHandler("text/calendar"));
    }

    @Test
    void testDelegatesCommandQueries() {
        CommandMap delegate = mock(CommandMap.class);
        CommandInfo[] preferred = {new CommandInfo("view", "Viewer")};
        CommandInfo[] all = {new CommandInfo("edit", "Editor")};
        CommandInfo command = new CommandInfo("view", "Viewer");
        when(delegate.getPreferredCommands("text/plain")).thenReturn(preferred);
        when(delegate.getAllCommands("text/plain")).thenReturn(all);
        when(delegate.getCommand("text/plain", "view")).thenReturn(command);

        MailDataContentHandlerCommandMap commandMap = new MailDataContentHandlerCommandMap(delegate);

        assertSame(preferred, commandMap.getPreferredCommands("text/plain"));
        assertSame(all, commandMap.getAllCommands("text/plain"));
        assertSame(command, commandMap.getCommand("text/plain", "view"));
    }

    @Test
    void testNullDelegate() {
        MailDataContentHandlerCommandMap commandMap = new MailDataContentHandlerCommandMap(null);

        assertNull(commandMap.createDataContentHandler("text/calendar"));
        assertEquals(0, commandMap.getPreferredCommands("text/calendar").length);
        assertEquals(0, commandMap.getAllCommands("text/calendar").length);
        assertNull(commandMap.getCommand("text/calendar", "view"));
    }
}
