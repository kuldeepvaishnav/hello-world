package com.kvaishnav.checkprocessor.email;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.kvaishnav.checkprocessor.config.AppConfig;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Sends notification emails via the Gmail API.
 *
 * <p>Each email includes:
 * <ul>
 *   <li>The check image as an attachment</li>
 *   <li>Any generated documents as attachments (none until document types are specified)</li>
 * </ul>
 *
 * <p>The "From" address is determined by the authenticated Google account —
 * no explicit From header is needed.
 */
public class GmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Gmail gmail;
    private final AppConfig config;

    public GmailService(AppConfig config,
                        NetHttpTransport transport,
                        GsonFactory jsonFactory,
                        Credential credential) {
        this.config = config;
        this.gmail = new Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName("Check Processor")
                .build();
    }

    /**
     * Sends an email to the configured recipient with the check image and any
     * generated documents attached.
     *
     * @param checkFileName  original filename of the check (used in subject/body)
     * @param checkImagePath path to the downloaded check image temp file
     * @param documentPaths  paths to any generated document temp files
     */
    public void sendCheckEmail(String checkFileName,
                               Path checkImagePath,
                               List<Path> documentPaths) throws Exception {
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);

        email.addRecipient(
                jakarta.mail.Message.RecipientType.TO,
                new InternetAddress(config.getRecipientEmail()));

        email.setSubject("Check Deposit — " + checkFileName
                + " [" + LocalDate.now().format(DATE_FMT) + "]");

        MimeMultipart multipart = new MimeMultipart();

        // Plain-text body
        MimeBodyPart body = new MimeBodyPart();
        String docsNote = documentPaths.isEmpty()
                ? ""
                : "\nGenerated documents: " + documentPaths.size() + " file(s) attached.";
        body.setText(
                "A new check has been received and processed.\n\n" +
                "File: " + checkFileName + "\n" +
                "Date: " + LocalDate.now().format(DATE_FMT) + docsNote + "\n\n" +
                "The check image" + (documentPaths.isEmpty() ? " is" : " and documents are") +
                " attached to this email and have been archived to your Google Drive.");
        multipart.addBodyPart(body);

        // Attach check image
        multipart.addBodyPart(buildAttachment(checkImagePath));

        // Attach generated documents (currently none — populated once doc types are defined)
        for (Path doc : documentPaths) {
            multipart.addBodyPart(buildAttachment(doc));
        }

        email.setContent(multipart);

        // Encode message as base64url and send via Gmail API
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        String encoded = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());

        com.google.api.services.gmail.model.Message message =
                new com.google.api.services.gmail.model.Message();
        message.setRaw(encoded);
        gmail.users().messages().send("me", message).execute();

        log.info("Email sent to {} for check '{}'",
                config.getRecipientEmail(), checkFileName);
    }

    private static MimeBodyPart buildAttachment(Path filePath) throws Exception {
        MimeBodyPart part = new MimeBodyPart();
        part.setDataHandler(new DataHandler(new FileDataSource(filePath.toFile())));
        part.setFileName(filePath.getFileName().toString());
        return part;
    }
}
