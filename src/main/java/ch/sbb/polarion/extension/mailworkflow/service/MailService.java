package ch.sbb.polarion.extension.mailworkflow.service;

import ch.sbb.polarion.extension.mailworkflow.utils.MessageConfigurator;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.Getter;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.net.URISyntaxException;
import java.util.Properties;

@Getter
public class MailService {
    private static final String ANNOUNCER_PREFIX = "announcer.";
    private static final String MAIL_PREFIX = "mail.";
    private static final String USER_PROPERTY = "mail.smtp.user";
    private static final String PASSWORD_PROPERTY = "mail.smtp.password";

    private final Properties props;
    private final Authenticator authenticator;

    public MailService() {
        props = new Properties();

        // Here we replace 'announcer' prefix of Polarion specific parameters for its AnnouncerService like 'announcer.smtp.host'
        // by 'mail' prefix to comply standard JavaMail notation and to reuse Polarion's announcer configuration
        int announcerPrefixLength = ANNOUNCER_PREFIX.length();
        System.getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().toString().matches(ANNOUNCER_PREFIX + "*"))
                .forEach(entry -> props.put(MAIL_PREFIX + entry.getKey().toString().substring(announcerPrefixLength), entry.getValue()));

        this.authenticator = getAuthenticator(props);
    }

    private Authenticator getAuthenticator(Properties props) {
        if (props.containsKey(USER_PROPERTY) && props.containsKey(PASSWORD_PROPERTY)) {
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(props.getProperty(USER_PROPERTY), props.getProperty(PASSWORD_PROPERTY));
                }
            };
        } else {
            return null;
        }
    }

    public void sendWorkflowMessage(IWorkItem workItem, IArguments arguments) throws MessagingException, URISyntaxException {
        MimeMessage message = new MimeMessage(Session.getInstance(props, authenticator));
        send(MessageConfigurator.configureWorkflowMessage(message, workItem, arguments));
    }

    public void send(Message message) throws MessagingException {
        // To use most recent version of javax.mail library and to get rid of conflicts in class path because Polarion also contains
        // this library but of older version, we temporarily re-define context class loader and then rollback it to initial value
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            Transport.send(message, message.getAllRecipients());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
