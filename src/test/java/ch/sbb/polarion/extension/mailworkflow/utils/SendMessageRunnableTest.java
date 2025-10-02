package ch.sbb.polarion.extension.mailworkflow.utils;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.mail.Transport;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SendMessageRunnableTest {

    @Test
    void testInitialization() {
        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        props.put("announcer.smtp.user", "user");
        props.put("announcer.smtp.password", "password");
        System.setProperties(props);

        SendMessageRunnable runnable = new SendMessageRunnable();

        assertNotNull(runnable.getProperties());
        assertEquals("smtp.gmail.com", runnable.getProperties().getProperty("mail.smtp.host"));
        assertEquals("587", runnable.getProperties().getProperty("mail.smtp.port"));
        assertEquals("user", runnable.getProperties().getProperty("mail.smtp.user"));
        assertEquals("password", runnable.getProperties().getProperty("mail.smtp.password"));

        assertNotNull(runnable.getAuthenticator(runnable.getProperties()));
    }

    @Test
    void testNoUser() {
        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        props.remove("announcer.smtp.user");
        props.remove("announcer.smtp.password");
        props.remove("mail.smtp.user");
        props.remove("mail.smtp.user");
        System.setProperties(props);

        SendMessageRunnable runnable = new SendMessageRunnable();

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

        SendMessageRunnable runnable = new SendMessageRunnable();
        try (MockedStatic<Transport> mockTransport = mockStatic(Transport.class)) {
            mockTransport.when(() -> Transport.send(any(), any())).thenAnswer(invocation -> null);
            runnable.run(Map.of("workItem", workItem, "arguments", arguments));
            mockTransport.verify(() -> Transport.send(any(), any()), times(1));
        }
    }

}
