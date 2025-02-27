package ch.sbb.polarion.extension.mailworkflow.service;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailServiceTest {

    @Test
    void testInitialization() {
        Properties props = System.getProperties();
        props.put("announcer.smtp.host", "smtp.gmail.com");
        props.put("announcer.smtp.port", "587");
        props.put("announcer.smtp.user", "user");
        props.put("announcer.smtp.password", "password");
        System.setProperties(props);

        MailService mailService = new MailService();

        assertNotNull(mailService.getProps());
        assertEquals("smtp.gmail.com", mailService.getProps().getProperty("mail.smtp.host"));
        assertEquals("587", mailService.getProps().getProperty("mail.smtp.port"));
        assertEquals("user", mailService.getProps().getProperty("mail.smtp.user"));
        assertEquals("password", mailService.getProps().getProperty("mail.smtp.password"));

        assertNotNull(mailService.getAuthenticator());
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

        MailService mailService = new MailService();

        assertNull(mailService.getAuthenticator());
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

        MailService mailService = spy(MailService.class);
        doNothing().when(mailService).send(any());
        mailService.sendWorkflowMessage(workItem, arguments);
        verify(mailService).send(any());
    }
}
