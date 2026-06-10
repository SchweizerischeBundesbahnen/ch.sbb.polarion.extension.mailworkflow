package ch.sbb.polarion.extension.mailworkflow.service;

import jakarta.activation.CommandInfo;
import jakarta.activation.CommandMap;
import jakarta.activation.DataContentHandler;
import org.eclipse.angus.mail.handlers.message_rfc822;
import org.eclipse.angus.mail.handlers.multipart_mixed;
import org.eclipse.angus.mail.handlers.text_html;
import org.eclipse.angus.mail.handlers.text_plain;
import org.eclipse.angus.mail.handlers.text_xml;

import java.util.Locale;

/**
 * Polarion 2606 ships the Jakarta Activation API (jakarta.activation-api) and Angus Mail (jakarta.mail), but NOT the
 * Jakarta Activation SPI implementation (angus-activation / MailcapRegistryProvider). Without that provider
 * {@link jakarta.activation.MailcapCommandMap} cannot parse any {@code META-INF/mailcap}, so Angus Mail's
 * DataContentHandlers (multipart/mixed, text/plain, ...) are never registered and sending a multipart message fails
 * with "no object DCH for MIME type multipart/mixed".
 *
 * <p>This CommandMap registers those handlers explicitly. The handler classes come from Polarion's {@code jakarta.mail}
 * bundle (loaded by this extension via {@code Require-Bundle: jakarta.mail}), so nothing is bundled and no thread
 * context class loader is re-defined. Any MIME type we don't handle is delegated to the wrapped default command map, so
 * the rest of JAF behaves exactly as before.
 */
public class MailDataContentHandlerCommandMap extends CommandMap {

    private final CommandMap delegate;

    public MailDataContentHandlerCommandMap(CommandMap delegate) {
        this.delegate = delegate;
    }

    @Override
    public DataContentHandler createDataContentHandler(String mimeType) {
        DataContentHandler handler = createMailHandler(baseType(mimeType));
        if (handler != null) {
            return handler;
        }
        return delegate == null ? null : delegate.createDataContentHandler(mimeType);
    }

    private DataContentHandler createMailHandler(String baseType) {
        if (baseType.startsWith("multipart/")) {
            return new multipart_mixed();
        }
        return switch (baseType) {
            case "text/plain" -> new text_plain();
            case "text/html" -> new text_html();
            case "text/xml" -> new text_xml();
            case "message/rfc822" -> new message_rfc822();
            default -> null;
        };
    }

    private static String baseType(String mimeType) {
        int semicolon = mimeType.indexOf(';');
        return (semicolon < 0 ? mimeType : mimeType.substring(0, semicolon)).trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public CommandInfo[] getPreferredCommands(String mimeType) {
        return delegate == null ? new CommandInfo[0] : delegate.getPreferredCommands(mimeType);
    }

    @Override
    public CommandInfo[] getAllCommands(String mimeType) {
        return delegate == null ? new CommandInfo[0] : delegate.getAllCommands(mimeType);
    }

    @Override
    public CommandInfo getCommand(String mimeType, String cmdName) {
        return delegate == null ? null : delegate.getCommand(mimeType, cmdName);
    }
}
