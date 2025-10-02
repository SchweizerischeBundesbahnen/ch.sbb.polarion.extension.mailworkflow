package ch.sbb.polarion.extension.mailworkflow.utils;

import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.internal.model.ApprovalStruct;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import com.polarion.alm.ui.shared.CollectionUtils;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.duration.DurationTime;
import lombok.experimental.UtilityClass;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.model.property.immutable.ImmutableMethod;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import net.fortuna.ical4j.util.RandomUidGenerator;
import org.jetbrains.annotations.NotNull;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.*;

@UtilityClass
public final class MessageConfigurator {
    private static final Logger LOGGER = Logger.getLogger(MessageConfigurator.class);

    private static final int START_OF_DAY = 9;

    public static final String SENDER = "sender";

    public static final String RECIPIENTS_FIELD = "recipientsField";
    public static final String ASSIGNEES = "assignees";
    public static final String APPROVALS = "approvals";
    public static final String AUTHOR = "author";
    public static final String DEFAULT_RECIPIENTS_FIELD = ASSIGNEES;

    public static final String EMAIL_SUBJECT = "emailSubject";
    public static final String DEFAULT_EMAIL_SUBJECT = "Deadline Reminder";

    public static final String DATE_FIELD = "dateField";
    public static final String DEFAULT_DATE_FIELD = "dueDate";

    public static final String EVENT_DURATION_FIELD = "eventDurationField";
    public static final String TEAMS_MEETING_URL_FIELD = "teamsMeetingUrlField";

    public static final String EVENT_SUMMARY = "eventSummary";
    public static final String EVENT_DESCRIPTION = "eventDescription";
    public static final String EVENT_PRIORITY = "eventPriority";
    public static final String EVENT_CATEGORY = "eventCategory";
    public static final String EVENT_LOCATION = "eventLocation";

    private static final String EVENT_UID = "eventUid";
    private static final String EVENT_SEQUENCE = "eventSequence";

    public static MimeMessage configureWorkflowMessage(@NotNull MimeMessage message, @NotNull IWorkItem workItem, @NotNull IArguments arguments) throws MessagingException, URISyntaxException {
        message.setSentDate(new Date());
        message.addHeaderLine("X-MS-TNEF-Correlator");
        message.addHeader("Method", workItem.getValue(EVENT_UID) instanceof String ? Method.VALUE_REQUEST : Method.VALUE_PUBLISH);
        message.addHeader("Component", Component.VEVENT);

        String sender = arguments.getAsString(SENDER);
        if (StringUtils.isEmpty(sender)) {
            throw new IllegalStateException("Missing required parameter: " + SENDER);
        }
        message.setFrom(new InternetAddress(sender));

        String recipientsField = Objects.requireNonNull(arguments.getAsString(RECIPIENTS_FIELD, DEFAULT_RECIPIENTS_FIELD));
        List<String> recipients = getRecipients(workItem, recipientsField);
        if (recipients.isEmpty()) {
            throw new IllegalStateException("No recipients specified");
        }
        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }

        message.setSubject(arguments.getAsString(EMAIL_SUBJECT, DEFAULT_EMAIL_SUBJECT));

        String mimeType = "text/calendar; method=REQUEST; charset=UTF-8";
        MimeBodyPart calendarPart = new MimeBodyPart();
        calendarPart.addHeader("Content-Class", "urn:content-classes:calendarmessage");
        calendarPart.addHeader("Content-ID", "calendar_message");
        calendarPart.addHeader("Content-Type", mimeType);

        net.fortuna.ical4j.model.Calendar calendarEvent = getCalendarEvent(workItem, sender, recipients, arguments);
        calendarPart.setDataHandler(new DataHandler(new ByteArrayDataSource(calendarEvent.toString().getBytes(StandardCharsets.UTF_8), mimeType)));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(calendarPart);

        message.setContent(multipart);

        return message;
    }

    private static @NotNull List<String> getRecipients(@NotNull IWorkItem workItem, @NotNull String recipientsField) {
        return switch (recipientsField) {
            case ASSIGNEES -> getAssigneeEmails(workItem);
            case APPROVALS -> getApprovalEmails(workItem);
            case AUTHOR -> getAuthorEmails(workItem);
            default -> getCustomRecipientsFieldEmails(workItem, recipientsField);
        };
    }

    private static @NotNull List<String> getAssigneeEmails(@NotNull IWorkItem workItem) {
        List<String> assigneeEmails = new ArrayList<>();
        if (!CollectionUtils.isEmpty(workItem.getAssignees())) {
            for (Object assignee : workItem.getAssignees()) {
                if (assignee instanceof IUser user) {
                    if (!StringUtils.isEmptyTrimmed(user.getEmail())) {
                        assigneeEmails.add(user.getEmail());
                    }
                } else {
                    LOGGER.error("Assignee is not a User");
                }
            }
        }
        return assigneeEmails;
    }

    private static @NotNull List<String> getApprovalEmails(@NotNull IWorkItem workItem) {
        List<String> approvalEmails = new ArrayList<>();
        if (!CollectionUtils.isEmpty(workItem.getApprovals())) {
            for (Object approval : workItem.getApprovals()) {
                if (approval instanceof ApprovalStruct approvalStruct && approvalStruct.getUser() != null) {
                    if (!StringUtils.isEmptyTrimmed(approvalStruct.getUser().getEmail())) {
                        approvalEmails.add(approvalStruct.getUser().getEmail());
                    }
                } else {
                    LOGGER.error("Approval is not a User");
                }
            }
        }
        return approvalEmails;
    }

    private static @NotNull List<String> getAuthorEmails(@NotNull IWorkItem workItem) {
        IUser user = workItem.getAuthor();
        if (user != null && !StringUtils.isEmptyTrimmed(user.getEmail())) {
            return Collections.singletonList(user.getEmail());
        } else {
            return Collections.emptyList();
        }
    }

    private static @NotNull List<String> getCustomRecipientsFieldEmails(@NotNull IWorkItem workItem, @NotNull String recipientsField) {
        List<String> recipientEmails = new ArrayList<>();

        Object recipients = workItem.getCustomField(recipientsField);
        if (recipients instanceof Collection<?> recipientsList) {
            for (Object recipient : recipientsList) {
                if (recipient instanceof IUser user) {
                    if (!StringUtils.isEmptyTrimmed(user.getEmail())) {
                        recipientEmails.add(user.getEmail());
                    }
                } else {
                    LOGGER.error("Recipient is not a User");
                }
            }
        } else {
            LOGGER.error("Recipients field is not a Collection");
        }

        return recipientEmails;
    }

    private static net.fortuna.ical4j.model.Calendar getCalendarEvent(@NotNull IWorkItem workItem, @NotNull String sender, @NotNull List<String> recipients, @NotNull IArguments arguments) throws URISyntaxException {
        net.fortuna.ical4j.model.Calendar calendarEvent = new net.fortuna.ical4j.model.Calendar();
        calendarEvent.add(ImmutableVersion.VERSION_2_0);
        calendarEvent.add(ImmutableMethod.REQUEST);
        calendarEvent.add(new ProdId("-//Microsoft Corporation//Outlook 16.0 MIMEDIR//EN"));

        ZonedDateTime startTime = getEventStartTime(workItem, arguments);
        Duration duration = getEventDuration(workItem, arguments);

        String eventSummary = arguments.getAsString(EVENT_SUMMARY, null);
        eventSummary = eventSummary != null ? eventSummary : String.format("WorkItem %s Deadline", workItem.getId());

        final VEvent event;
        if (duration != null) {
            event = new VEvent(startTime, duration, eventSummary);
        } else {
            event = new VEvent(startTime, eventSummary);
        }

        Uid eventUid = getEventUid(workItem);
        event.add(eventUid);

        workItem.setValue(EVENT_UID, eventUid.getValue());
        workItem.setValue(EVENT_SEQUENCE, String.valueOf(getEventSequence(workItem) + 1));

        event.add(new Organizer(sender));

        for (String recipient : recipients) {
            event.add(new Attendee(recipient));
        }

        String eventCategory = arguments.getAsString(EVENT_CATEGORY, null);
        if (eventCategory != null) {
            event.add(new Categories(eventCategory));
        }

        String eventDescription = arguments.getAsString(EVENT_DESCRIPTION, null);
        String eventLocation = arguments.getAsString(EVENT_LOCATION, null);

        String teamsMeetingUrlField = arguments.getAsString(TEAMS_MEETING_URL_FIELD, null);
        Object teamsMeetingUrlObject = teamsMeetingUrlField != null ? workItem.getValue(teamsMeetingUrlField) : null;
        if (teamsMeetingUrlObject instanceof String teamsMeetingUrl) {
            URI teamsUri = new URI(teamsMeetingUrl);
            Url url = new Url(teamsUri);
            event.add(url);

            eventDescription = Objects.requireNonNullElse(eventDescription, "");
            eventDescription += System.lineSeparator() + System.lineSeparator() + "Join Microsoft Teams Meeting: " + teamsUri;

            eventLocation = "Microsoft Teams Meeting";

            event.add(new XProperty("X-MICROSOFT-SKYPETEAMSMEETINGURL", teamsUri.toString()));
            event.add(new XProperty("X-MICROSOFT-DONOTFORWARDMEETING", "FALSE"));
        }

        if (eventDescription != null) {
            event.add(new Description(eventDescription));
        }
        if (eventLocation != null) {
            event.add(new Location(eventLocation));
        }

        event.add(new Priority(arguments.getAsInt(EVENT_PRIORITY, 0)));

        calendarEvent.add(event);

        return calendarEvent;
    }

    private ZonedDateTime getEventStartTime(@NotNull IWorkItem workItem, @NotNull IArguments arguments) {
        Calendar calendar = Calendar.getInstance();
        String dateField = arguments.getAsString(DATE_FIELD, DEFAULT_DATE_FIELD);
        Object dateValue = workItem.getValue(dateField);
        if (dateValue instanceof Date eventDate) {
            calendar.setTime(eventDate);
        } else if (dateValue instanceof DateOnly eventDateOnly) {
            calendar.setTime(eventDateOnly.getDate());
            calendar.set(Calendar.HOUR_OF_DAY, START_OF_DAY);
        } else {
            throw new IllegalStateException("Wrong date field specified");
        }

        ZoneId zoneId = ZoneId.systemDefault();
        return ZonedDateTime.of(calendar.getTime().toInstant().atZone(zoneId).toLocalDateTime(), zoneId);
    }

    private Duration getEventDuration(@NotNull IWorkItem workItem, @NotNull IArguments arguments) {
        String eventDurationField = arguments.getAsString(EVENT_DURATION_FIELD, null);
        Object eventDuration = eventDurationField != null ? workItem.getValue(eventDurationField) : null;
        if (eventDuration instanceof DurationTime polarionDuration) {
            return Duration.ZERO.plus(polarionDuration.getMillis(), ChronoUnit.MILLIS);
        } else {
            return null;
        }
    }

    private Uid getEventUid(IWorkItem workItem) {
        Object eventUidObject = workItem.getValue(EVENT_UID);
        if (eventUidObject instanceof String) {
            return new Uid(eventUidObject.toString());
        } else {
            return new RandomUidGenerator().generateUid();
        }
    }

    private Integer getEventSequence(IWorkItem workItem) {
        Object eventSequenceObject = workItem.getValue(EVENT_SEQUENCE);
        if (eventSequenceObject instanceof String eventSequence) {
            try {
                return Integer.parseInt(eventSequence);
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        return 0; // Fallback, if no sequence or malformed value
    }

}
