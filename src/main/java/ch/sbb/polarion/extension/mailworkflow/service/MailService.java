package ch.sbb.polarion.extension.mailworkflow.service;

import ch.sbb.polarion.extension.mailworkflow.utils.MessageConfigurator;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.Getter;
import org.jetbrains.annotations.VisibleForTesting;

import jakarta.activation.CommandMap;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import java.net.URISyntaxException;
import java.util.Properties;

@Getter
public class MailService {
    private static final String ANNOUNCER_PREFIX = "announcer.";
    private static final String MAIL_PREFIX = "mail.";
    private static final String USER_PROPERTY = "mail.smtp.user";
    private static final String PASSWORD_PROPERTY = "mail.smtp.password";

    static {
        registerMailDataContentHandlers();
    }

    private final Properties props;
    private final Authenticator authenticator;

    public MailService() {
        props = new Properties();

        // Here we replace 'announcer' prefix of Polarion specific parameters for its AnnouncerService like 'announcer.smtp.host'
        // by 'mail' prefix to comply standard JavaMail notation and to reuse Polarion's announcer configuration
        int announcerPrefixLength = ANNOUNCER_PREFIX.length();
        System.getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().toString().startsWith(ANNOUNCER_PREFIX))
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
        // We rely on the jakarta.mail bundle provided by Polarion's OSGi runtime (declared as a Require-Bundle of this
        // extension), so the message is sent directly without re-defining the context class loader to our own classpath.
        Transport.send(message, message.getAllRecipients());
    }

    // Polarion 2606 has no Jakarta Activation SPI implementation, so JAF can't discover Angus Mail's content handlers
    // on its own (see MailDataContentHandlerCommandMap). We register them once by wrapping the default command map.
    @VisibleForTesting
    static void registerMailDataContentHandlers() {
        CommandMap current = CommandMap.getDefaultCommandMap();
        if (!(current instanceof MailDataContentHandlerCommandMap)) {
            CommandMap.setDefaultCommandMap(new MailDataContentHandlerCommandMap(current));
        }
    }
}
