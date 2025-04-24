package ch.sbb.polarion.extension.mailworkflow.utils;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.ApprovalStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.duration.DurationTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageConfiguratorTest {

    @Test
    @SneakyThrows
    void testBasicInformation() {
        IWorkItem workItem = mockWorkItem(new Date());

        IArguments arguments = mockArguments();

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertNotNull(message.getSentDate());
        assertTrue(Collections.list(message.getAllHeaderLines()).contains("X-MS-TNEF-Correlator"));
        boolean containsMethodHeader = false;
        boolean containsComponentHeader = false;
        for (Header header : Collections.list(message.getAllHeaders())) {
            if (header.getName().equalsIgnoreCase("Method")) {
                containsMethodHeader = true;
                assertEquals("REQUEST", header.getValue());
            }
            if (header.getName().equalsIgnoreCase("Component")) {
                containsComponentHeader = true;
                assertEquals("VEVENT", header.getValue());
            }
        }
        assertTrue(containsMethodHeader);
        assertTrue(containsComponentHeader);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
        boolean containsContentClassHeader = false;
        boolean containsContentIdHeader = false;
        for (Header header : Collections.list(mimeBodyPart.getAllHeaders())) {
            if (header.getName().equalsIgnoreCase("Content-Class")) {
                containsContentClassHeader = true;
                assertEquals("urn:content-classes:calendarmessage", header.getValue());
            }
            if (header.getName().equalsIgnoreCase("Content-ID")) {
                containsContentIdHeader = true;
                assertEquals("calendar_message", header.getValue());
            }
        }
        assertTrue(containsContentClassHeader);
        assertTrue(containsContentIdHeader);
    }

    @Test
    @SneakyThrows
    void testSenderMissing() {
        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn(null);

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        IWorkItem workItem = mock(IWorkItem.class);
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "Missing required parameter: sender");
    }

    @Test
    @SneakyThrows
    void testSender() {
        IWorkItem workItem = mockWorkItem(new Date());

        IArguments arguments = mockArguments();
        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertEquals(new InternetAddress("sender@company.com"), message.getFrom()[0]);
    }

    @Test
    @SneakyThrows
    void testAssigneesNull() {
        IWorkItem workItem = mock(IWorkItem.class);

        when(workItem.getAssignees()).thenReturn(null);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testAssigneesEmpty() {
        IWorkItem workItem = mock(IWorkItem.class);

        PObjectListStub<IUser> assignees = new PObjectListStub<>();
        when(workItem.getAssignees()).thenReturn(assignees);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testAssigneesWithNoEmail() {
        IWorkItem workItem = mock(IWorkItem.class);

        PObjectListStub<IUser> assignees = new PObjectListStub<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn(" ");
        assignees.add(user);
        when(workItem.getAssignees()).thenReturn(assignees);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("assignees");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testValidAssignees() {
        IWorkItem workItem = mockWorkItem(new Date());

        IArguments arguments = mockArguments();
        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertEquals(new InternetAddress("recipient@company.com"), message.getAllRecipients()[0]);
    }

    @Test
    @SneakyThrows
    void testApprovalsNull() {
        IWorkItem workItem = mock(IWorkItem.class);

        when(workItem.getApprovals()).thenReturn(null);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("approvals");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testApprovalsEmpty() {
        IWorkItem workItem = mock(IWorkItem.class);

        Collection<IUser> approvals = new ArrayList<>();
        when(workItem.getApprovals()).thenReturn(approvals);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("approvals");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testApprovalsWithNoEmail() {
        IWorkItem workItem = mock(IWorkItem.class);

        Collection<ApprovalStruct> approvals = new ArrayList<>();
        ApprovalStruct approvalStruct = mock(ApprovalStruct.class);
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn(" ");
        when(approvalStruct.getUser()).thenReturn(user);
        approvals.add(approvalStruct);
        when(workItem.getApprovals()).thenReturn(approvals);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("approvals");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testValidApprovals() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        Collection<ApprovalStruct> approvals = new ArrayList<>();
        ApprovalStruct approvalStruct = mock(ApprovalStruct.class);
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        when(approvalStruct.getUser()).thenReturn(user);
        approvals.add(approvalStruct);
        when(workItem.getApprovals()).thenReturn(approvals);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("approvals");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");
        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertEquals(new InternetAddress("recipient@company.com"), message.getAllRecipients()[0]);
    }

    @Test
    @SneakyThrows
    void testAuthorNull() {
        IWorkItem workItem = mock(IWorkItem.class);

        when(workItem.getAuthor()).thenReturn(null);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("author");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testAuthorWithNoEmail() {
        IWorkItem workItem = mock(IWorkItem.class);

        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn(" ");
        when(workItem.getAuthor()).thenReturn(user);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("author");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testValidAuthor() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        when(workItem.getAuthor()).thenReturn(user);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("author");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");
        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertEquals(new InternetAddress("recipient@company.com"), message.getAllRecipients()[0]);
    }

    @Test
    @SneakyThrows
    void testCustomRecipientFieldNull() {
        IWorkItem workItem = mock(IWorkItem.class);

        when(workItem.getCustomField("recipients")).thenReturn(null);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("recipients");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testCustomRecipientFieldEmpty() {
        IWorkItem workItem = mock(IWorkItem.class);

        Collection<IUser> recipients = new ArrayList<>();
        when(workItem.getCustomField("recipients")).thenReturn(recipients);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("recipients");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testCustomRecipientFieldWithNoEmail() {
        IWorkItem workItem = mock(IWorkItem.class);

        Collection<IUser> recipients = new ArrayList<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn(" ");
        recipients.add(user);
        when(workItem.getCustomField("recipients")).thenReturn(recipients);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("recipients");

        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));
        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "No recipients specified");
    }

    @Test
    @SneakyThrows
    void testValidCustomRecipientField() {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(new Date());
        when(workItem.getId()).thenReturn("WI-1");

        Collection<IUser> recipients = new ArrayList<>();
        IUser user = mock(IUser.class);
        when(user.getEmail()).thenReturn("recipient@company.com");
        recipients.add(user);
        when(workItem.getCustomField("recipients")).thenReturn(recipients);

        IArguments arguments = mock(IArguments.class);
        when(arguments.getAsString("sender")).thenReturn("sender@company.com");
        when(arguments.getAsString(eq("recipientsField"), anyString())).thenReturn("recipients");
        when(arguments.getAsString(eq("dateField"), anyString())).thenReturn("dueDate");
        when(arguments.getAsString(eq("emailSubject"), anyString())).thenReturn("Deadline Reminder");
        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        assertEquals(new InternetAddress("recipient@company.com"), message.getAllRecipients()[0]);
    }

    @Test
    @SneakyThrows
    void testCalendarEvent() {
        Date date = new Date();
        IWorkItem workItem = mockWorkItem(date);

        IArguments arguments = mockArguments();
        when(arguments.getAsString(eq("eventSummary"), isNull())).thenReturn(null);
        when(arguments.getAsString(eq("eventDescription"), isNull())).thenReturn("eventDescription");
        when(arguments.getAsString(eq("eventCategory"), isNull())).thenReturn("eventCategory");
        when(arguments.getAsString(eq("eventLocation"), isNull())).thenReturn("eventLocation");
        when(arguments.getAsString(eq("eventDurationField"), isNull())).thenReturn("eventDuration");
        when(arguments.getAsString(eq("teamsMeetingUrlField"), isNull())).thenReturn(null);

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;

        assertNotNull(mimeBodyPart.getContent());
        String calendarEventContent = mimeBodyPart.getContent().toString();
        assertTrue(calendarEventContent.contains("VERSION:2.0"));
        assertTrue(calendarEventContent.contains("METHOD:REQUEST"));
        assertTrue(calendarEventContent.contains("PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN"));
        assertTrue(calendarEventContent.contains("SUMMARY:WorkItem WI-1 Deadline"));
        assertTrue(calendarEventContent.contains("DTSTART;TZID=%s:%s".formatted(ZoneId.systemDefault().getId(), new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(date))));
        assertTrue(calendarEventContent.contains("ORGANIZER:sender@company.com"));
        assertTrue(calendarEventContent.contains("ATTENDEE:recipient@company.com"));
        assertTrue(calendarEventContent.contains("PRIORITY:0"));
        assertTrue(calendarEventContent.contains("DESCRIPTION:eventDescription"));
        assertTrue(calendarEventContent.contains("CATEGORIES:eventCategory"));
        assertTrue(calendarEventContent.contains("LOCATION:eventLocation"));
    }

    @Test
    @SneakyThrows
    void testCalendarEventWithDateOnly() {
        Date date = new Date();
        DateOnly dateOnly = new DateOnly(date);
        IWorkItem workItem = mockWorkItem(dateOnly);

        IArguments arguments = mockArguments();

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 9);

        assertNotNull(mimeBodyPart.getContent());
        String calendarEventContent = mimeBodyPart.getContent().toString();
        assertTrue(calendarEventContent.contains("DTSTART;TZID=%s:%s".formatted(ZoneId.systemDefault().getId(), new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(calendar.getTime()))));
    }

    @Test
    @SneakyThrows
    void testCalendarEventWithNoDateProvided() {
        IWorkItem workItem = mock(IWorkItem.class);
        IArguments arguments = mock(IArguments.class);
        Properties props = getProperties();
        MimeMessage message = new MimeMessage(Session.getInstance(props, getAuthenticator(props)));

        assertThrows(IllegalStateException.class, () -> MessageConfigurator.configureWorkflowMessage(message, workItem, arguments), "Wrong date field specified");
    }

    @Test
    @SneakyThrows
    void testCalendarEventWithDaysDuration() {
        Date date = new Date();
        IWorkItem workItem = mockWorkItem(date);
        when(workItem.getValue("eventDuration")).thenReturn(DurationTime.fromString("48h"));

        IArguments arguments = mockArguments();
        when(arguments.getAsString(eq("eventSummary"), isNull())).thenReturn(null);
        when(arguments.getAsString(eq("eventDescription"), isNull())).thenReturn("eventDescription");
        when(arguments.getAsString(eq("eventCategory"), isNull())).thenReturn("eventCategory");
        when(arguments.getAsString(eq("eventLocation"), isNull())).thenReturn("eventLocation");
        when(arguments.getAsString(eq("eventDurationField"), isNull())).thenReturn("eventDuration");

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;

        assertNotNull(mimeBodyPart.getContent());
        String calendarEventContent = mimeBodyPart.getContent().toString();
        assertTrue(calendarEventContent.contains("DURATION:P2D"));
    }

    @Test
    @SneakyThrows
    void testCalendarEventWithHoursDuration() {
        Date date = new Date();
        IWorkItem workItem = mockWorkItem(date);
        when(workItem.getValue("eventDuration")).thenReturn(DurationTime.fromString("2 1/2h"));

        IArguments arguments = mockArguments();
        when(arguments.getAsString(eq("eventSummary"), isNull())).thenReturn(null);
        when(arguments.getAsString(eq("eventDescription"), isNull())).thenReturn("eventDescription");
        when(arguments.getAsString(eq("eventCategory"), isNull())).thenReturn("eventCategory");
        when(arguments.getAsString(eq("eventLocation"), isNull())).thenReturn("eventLocation");
        when(arguments.getAsString(eq("eventDurationField"), isNull())).thenReturn("eventDuration");

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;

        assertNotNull(mimeBodyPart.getContent());
        String calendarEventContent = mimeBodyPart.getContent().toString();
        assertTrue(calendarEventContent.contains("DURATION:PT2H30M"));
    }

    @Test
    @SneakyThrows
    void testTeamsMeeting() {
        Date date = new Date();
        IWorkItem workItem = mockWorkItem(date);
        when(workItem.getValue("teamsMeetingUrl")).thenReturn("https://microsoft.teams.meeting.url");

        IArguments arguments = mockArguments();
        when(arguments.getAsString(eq("eventSummary"), isNull())).thenReturn(null);
        when(arguments.getAsString(eq("eventDescription"), isNull())).thenReturn("eventDescription");
        when(arguments.getAsString(eq("eventCategory"), isNull())).thenReturn("eventCategory");
        when(arguments.getAsString(eq("eventLocation"), isNull())).thenReturn("eventLocation");
        when(arguments.getAsString(eq("teamsMeetingUrlField"), isNull())).thenReturn("teamsMeetingUrl");
        when(arguments.getAsString(eq("eventDurationField"), isNull())).thenReturn(null);

        Properties props = getProperties();
        MimeMessage message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, getAuthenticator(props))), workItem, arguments);

        Object content = message.getContent();
        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;
        BodyPart bodyPart = multipart.getBodyPart(0);
        assertInstanceOf(MimeBodyPart.class, bodyPart);

        MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;

        assertNotNull(mimeBodyPart.getContent());
        String calendarEventContent = mimeBodyPart.getContent().toString();

        assertTrue(calendarEventContent.contains("VERSION:2.0"));
        assertTrue(calendarEventContent.contains("METHOD:REQUEST"));
        assertTrue(calendarEventContent.contains("PRODID:-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN"));
        assertTrue(calendarEventContent.contains("SUMMARY:WorkItem WI-1 Deadline"));
        assertTrue(calendarEventContent.contains("DTSTART;TZID=%s:%s".formatted(ZoneId.systemDefault().getId(), new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(date))));
        assertTrue(calendarEventContent.contains("ORGANIZER:sender@company.com"));
        assertTrue(calendarEventContent.contains("ATTENDEE:recipient@company.com"));
        assertTrue(calendarEventContent.contains("PRIORITY:0"));
        assertTrue(calendarEventContent.contains("DESCRIPTION:eventDescription\\n\\nJoin Microsoft Teams Meeting: https://microsoft.teams.meeting.url"));
        assertTrue(calendarEventContent.contains("CATEGORIES:eventCategory"));
        assertTrue(calendarEventContent.contains("LOCATION:Microsoft Teams Meeting"));
        assertTrue(calendarEventContent.contains("X-MICROSOFT-SKYPETEAMSMEETINGURL:https://microsoft.teams.meeting.url"));
        assertTrue(calendarEventContent.contains("X-MICROSOFT-DONOTFORWARDMEETING:FALSE"));
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.user", "user");
        properties.put("mail.smtp.password", "password");
        return properties;
    }

    private Authenticator getAuthenticator(Properties props) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(props.getProperty("mail.smtp.user"), props.getProperty("mail.smtp.password"));
            }
        };
    }

    private IWorkItem mockWorkItem(Object date) {
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("dueDate")).thenReturn(date);
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
