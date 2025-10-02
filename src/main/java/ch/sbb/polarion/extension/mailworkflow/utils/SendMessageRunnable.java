package ch.sbb.polarion.extension.mailworkflow.utils;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.workflow.IArguments;
import lombok.SneakyThrows;
import org.jetbrains.annotations.VisibleForTesting;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

public class SendMessageRunnable implements BundleJarsPrioritizingRunnable {

    public static final String PARAM_WORKITEM = "workItem";
    public static final String PARAM_ARGUMENTS = "arguments";

    private static final String ANNOUNCER_PREFIX = "announcer.";
    private static final String MAIL_PREFIX = "mail.";
    private static final String USER_PROPERTY = "mail.smtp.user";
    private static final String PASSWORD_PROPERTY = "mail.smtp.password";

    @Override
    @SneakyThrows
    public Map<String, Object> run(Map<String, Object> params) {

        IWorkItem workItem = (IWorkItem) params.get(PARAM_WORKITEM);
        IArguments arguments = (IArguments) params.get(PARAM_ARGUMENTS);

        Properties props = getProperties();
        Authenticator authenticator = getAuthenticator(props);
        Message message = MessageConfigurator.configureWorkflowMessage(new MimeMessage(Session.getInstance(props, authenticator)), workItem, arguments);
        Transport.send(message, message.getAllRecipients());

        return Map.of();
    }

    @VisibleForTesting
    Properties getProperties() {
        Properties properties = new Properties();
        // Here we replace 'announcer' prefix of Polarion specific parameters for its AnnouncerService like 'announcer.smtp.host'
        // by 'mail' prefix to comply standard JavaMail notation and to reuse Polarion's announcer configuration
        int announcerPrefixLength = ANNOUNCER_PREFIX.length();
        System.getProperties().entrySet().stream()
                .filter(entry -> entry.getKey().toString().matches(ANNOUNCER_PREFIX + "*"))
                .forEach(entry -> properties.put(MAIL_PREFIX + entry.getKey().toString().substring(announcerPrefixLength), entry.getValue()));
        return  properties;
    }

    @VisibleForTesting
    Authenticator getAuthenticator(Properties props) {
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
}
