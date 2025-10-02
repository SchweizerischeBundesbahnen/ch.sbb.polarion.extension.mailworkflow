package ch.sbb.polarion.extension.mailworkflow.utils;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendMessageRunnableTest {

    private final SendMessageRunnable runnable = new SendMessageRunnable();

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

        assertNotNull(runnable.getProperties());
        assertEquals(4, runnable.getProperties().size());
        assertEquals("smtp.gmail.com", runnable.getProperties().getProperty("mail.smtp.host"));
        assertEquals("587", runnable.getProperties().getProperty("mail.smtp.port"));
        assertEquals("user42", runnable.getProperties().getProperty("mail.smtp.user"));
        assertEquals("password42", runnable.getProperties().getProperty("mail.smtp.password"));

        Authenticator authenticator = runnable.getAuthenticator(runnable.getProperties());
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
        assertNull(runnable.getAuthenticator(runnable.getProperties()));

        props.put("announcer.smtp.user", "someUser");
        props.remove("announcer.smtp.password");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.password");
        System.setProperties(props);
        assertNull(runnable.getAuthenticator(runnable.getProperties()));

        props.remove("announcer.smtp.user");
        props.remove("announcer.smtp.password");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.password");
        System.setProperties(props);
        assertNull(runnable.getAuthenticator(runnable.getProperties()));
    }

    @Test
    @SneakyThrows
    void testSendWorkflowMessage() {

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        PObjectListStub<IUser> assignees = new PObjectListStub<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        assignees.add(user);
        when(workItem.getAssignees()).thenReturn(assignees);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");

        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        System.setProperties(props);

        try (MockedStatic<Transport> mockTransport = mockStatic(Transport.class)) {
            mockTransport.when(() -> Transport.send(any(), any())).thenAnswer(invocation -> null);
            runnable.run(Map.of("workItem", workItem, "arguments", arguments));
            mockTransport.verify(() -> Transport.send(any(), any()), times(1));
        }
    }

    @Test
    void testRunWithTransportException() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        PObjectListStub<IUser> assignees = new PObjectListStub<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        assignees.add(user);
        when(workItem.getAssignees()).thenReturn(assignees);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");

        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        System.setProperties(props);

        try (MockedStatic<Transport> mockTransport = mockStatic(Transport.class)) {
            mockTransport.when(() -> Transport.send(any(), any())).thenThrow(new RuntimeException("Transport error"));

            assertThrows(RuntimeException.class, () -> runnable.run(Map.of("workItem", workItem, "arguments", arguments)));
        }
    }

}
