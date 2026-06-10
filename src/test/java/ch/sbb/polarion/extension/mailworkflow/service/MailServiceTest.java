package ch.sbb.polarion.extension.mailworkflow.service;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.activation.CommandMap;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MailServiceTest {

    @Test
    void testRegistersMailDataContentHandlerCommandMap() {
        // Loading/constructing MailService must install the command map that resolves Angus Mail content handlers,
        // since Polarion 2606 lacks the Jakarta Activation SPI implementation needed for mailcap-based discovery.
        new MailService();
        assertInstanceOf(MailDataContentHandlerCommandMap.class, CommandMap.getDefaultCommandMap());

        // Idempotent: registering again must not wrap our command map into another one.
        CommandMap afterFirst = CommandMap.getDefaultCommandMap();
        MailService.registerMailDataContentHandlers();
        assertSame(afterFirst, CommandMap.getDefaultCommandMap());
    }

    @Test
    @SneakyThrows
    void testInitialization() {
        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        props.put("announcer.smtp.user", "user42");
        props.put("announcer.smtp.password", "password42");
        props.put("announcers.some.param", "someValue"); // This should be ignored
        System.setProperties(props);

        MailService mailService = new MailService();

        assertNotNull(mailService.getProps());
        assertEquals(4, mailService.getProps().size());
        assertEquals("smtp.gmail.com", mailService.getProps().getProperty("mail.smtp.host"));
        assertEquals("587", mailService.getProps().getProperty("mail.smtp.port"));
        assertEquals("user42", mailService.getProps().getProperty("mail.smtp.user"));
        assertEquals("password42", mailService.getProps().getProperty("mail.smtp.password"));

        Authenticator authenticator = mailService.getAuthenticator();
        assertNotNull(authenticator);
        Method getPasswordAuthenticationMethod = Authenticator.class.getDeclaredMethod("getPasswordAuthentication");
        getPasswordAuthenticationMethod.setAccessible(true);
        PasswordAuthentication passwordAuthentication = (PasswordAuthentication) getPasswordAuthenticationMethod.invoke(authenticator);
        assertNotNull(passwordAuthentication);
        assertEquals("user42", passwordAuthentication.getUserName());
        assertEquals("password42", passwordAuthentication.getPassword());
    }

    @Test
    void testPartialUserData() {
        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        props.put("announcer.smtp.password", "42");
        props.remove("announcer.smtp.user");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.password");
        System.setProperties(props);
        assertNull(new MailService().getAuthenticator());

        props.put("announcer.smtp.user", "someUser");
        props.remove("announcer.smtp.password");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.password");
        System.setProperties(props);
        assertNull(new MailService().getAuthenticator());

        props.remove("announcer.smtp.user");
        props.remove("announcer.smtp.password");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.password");
        System.setProperties(props);
        assertNull(new MailService().getAuthenticator());
    }

    @Test
    @SneakyThrows
    void testSendWorkflowMessage() {
        IWorkItem workItem = mockWorkItem();
        IArguments arguments = mockArguments();

        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        System.setProperties(props);

        try (MockedStatic<Transport> mockTransport = mockStatic(Transport.class)) {
            mockTransport.when(() -> Transport.send(any(), any())).thenAnswer(invocation -> null);
            new MailService().sendWorkflowMessage(workItem, arguments);
            mockTransport.verify(() -> Transport.send(any(), any()), times(1));
        }
    }

    @Test
    void testSendWorkflowMessageWithTransportException() {
        IWorkItem workItem = mockWorkItem();
        IArguments arguments = mockArguments();

        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        System.setProperties(props);

        try (MockedStatic<Transport> mockTransport = mockStatic(Transport.class)) {
            mockTransport.when(() -> Transport.send(any(), any())).thenThrow(new RuntimeException("Transport error"));
            MailService mailService = new MailService();
            assertThrows(RuntimeException.class, () -> mailService.sendWorkflowMessage(workItem, arguments));
        }
    }

    private IWorkItem mockWorkItem() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        PObjectListStub<IUser> assignees = new PObjectListStub<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        assignees.add(user);
        when(workItem.getAssignees()).thenReturn(assignees);
        return workItem;
    }

    private IArguments mockArguments() {
        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");
        return arguments;
    }
}
