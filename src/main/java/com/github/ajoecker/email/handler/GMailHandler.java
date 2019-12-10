package com.github.ajoecker.email.handler;

import com.github.ajoecker.email.Email;
import com.github.ajoecker.email.EmailHandler;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.apache.commons.codec.binary.Base64;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.tinylog.Logger;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jsoup.Jsoup.parse;

// based on https://developers.google.com/gmail/api/quickstart/java
public class GMailHandler implements EmailHandler {
    private static final String USER = "me";
    private static final String ZUGANGSDATEN_BESTAETIGEN = "zugangsdaten-bestaetigen";
    private static final String PASSWORT_ZURUECKSETZEN = "passwort-zuruecksetzen";
    private Gmail gmail;

    public GMailHandler(Path tokenPath, String applicationName) {
        this.gmail = new GMailInitializer(tokenPath).service(applicationName).orElseThrow();
    }

    public GMailHandler() {
        this(Paths.get(System.getenv("GAUGE_PROJECT_ROOT"), "tokens"), System.getenv("email.application"));
    }

    @Override
    public boolean delete(String messageId) {
        try {
            gmail.users().messages().delete(USER, messageId).execute();
            return true;
        } catch (IOException e) {
            return failed(e, "failed to delete email " + messageId);
        }
    }

    @Override
    public boolean markRead(String messageId) {
        try {
            ModifyMessageRequest mmr = new ModifyMessageRequest().setRemoveLabelIds(List.of("UNREAD"));
            gmail.users().messages().modify(USER, messageId, mmr).execute();
            return true;
        } catch (IOException e) {
            return failed(e, "failed to mark email unread " + messageId);
        }
    }

    private Boolean failed(Exception e, String s) {
        Logger.error(s, e);
        return false;
    }

    @Override
    public List<String> getMessages(String query) {
        try {
            Logger.info("querying gmail with '{}'", query);
            return Awaitility.with().pollInterval(Duration.FIVE_SECONDS).pollDelay(Duration.TEN_SECONDS)
                    .await().atMost(Duration.TWO_MINUTES)
                    .until(() -> queryMessages(query), emails -> !emails.isEmpty());
        } catch (Exception e) {
            Logger.warn("no messages found in time periode", e);
            return List.of();
        }
    }

    @Override
    public String getLinkFromEmail(String messageId, String linkSubText) {
        Logger.info("retrieving link with text '{}' from {}", linkSubText, messageId);
        String rawEmail = getRawEmail(gmail, messageId);
        return parseLink(rawEmail, linkSubText).trim();
    }

    private String parseLink(String text, String linkSubText) {
        String regex = "(https.+" + linkSubText + ".+)\\s";
        Logger.info("parse link from with {}", regex);
        Pattern compile = Pattern.compile(regex);
        return compile.matcher(text).results().findFirst().map(MatchResult::group).orElse("");
    }

    @Override
    public Email getMessage(String messageId) {
        String rawEmail = getRawEmail(gmail, messageId);
        return new Email(messageId, rawEmail);
    }

    private String getRawEmail(Gmail gmail, String messageId) {
        try {
            Message message = gmail.users().messages().get(USER, messageId).setFormat("raw").execute();
            Logger.info("retrieving html email from {}", message.getId());
            Base64 base64 = new Base64(true);
            byte[] bytes = base64.decode(message.getRaw());
            Session session = Session.getDefaultInstance(new Properties(), null);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                MimeMessage mimeMessage = new MimeMessage(session, bais);
                return parseEmail((MimeMultipart) mimeMessage.getContent());
            }
        } catch (IOException | MessagingException e) {
            Logger.error("failed to retrieve email " + messageId, e);
            return "";
        }
    }

    private String parseEmail(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);
        if (bodyPart.isMimeType("text/plain")) {
            return bodyPart.getContent().toString();
        } else if (bodyPart.isMimeType("text/html")) {
            return parse(bodyPart.getContent().toString()).text();
        } else if (bodyPart.isMimeType("application/pdf")) {
            Logger.info("found attachement pdf");
        } else if (bodyPart.getContent() instanceof MimeMultipart) {
            return parseEmail((MimeMultipart) bodyPart.getContent());
        }
        Logger.warn("type {} is unknown to handle", bodyPart.getContentType());
        return "";
    }

    private List<String> queryMessages(String query) throws IOException {
        try {
            Logger.info("querying gmail with '{}'", query);
            List<Message> messages = gmail.users().messages().list(USER).setQ(query).execute().getMessages();
            if (messages == null) {
                return List.of();
            }
            return messages.stream().map(Message::getId).collect(Collectors.toList());
        } catch (IOException e) {
            Logger.warn("querying service for '" + query + "' failed", e);
            throw e;
        }
    }
}