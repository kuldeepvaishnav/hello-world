package com.kvaishnav.checkprocessor;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.kvaishnav.checkprocessor.auth.GoogleAuthService;
import com.kvaishnav.checkprocessor.config.AppConfig;
import com.kvaishnav.checkprocessor.drive.DriveService;
import com.kvaishnav.checkprocessor.drive.DriveWatcher;
import com.kvaishnav.checkprocessor.email.GmailService;
import com.kvaishnav.checkprocessor.processor.CheckProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point.
 *
 * <p>Start-up sequence:
 * <ol>
 *   <li>Load {@code config.properties}</li>
 *   <li>Perform Google OAuth (opens browser on first run; uses cached token thereafter)</li>
 *   <li>Wire up Drive, Gmail, and processing components</li>
 *   <li>Start the Drive folder watcher (runs indefinitely until Ctrl-C)</li>
 * </ol>
 *
 * <p>Build and run:
 * <pre>
 *   mvn package
 *   java -jar target/check-processor-1.0.0.jar
 * </pre>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("=== Check Processor starting ===");

        AppConfig config = AppConfig.load();

        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        // Single OAuth flow covering both Drive and Gmail scopes
        log.info("Authorising with Google (browser will open on first run)...");
        var auth = new GoogleAuthService(config, transport, jsonFactory);
        var credential = auth.authorize();
        log.info("Google authorisation complete.");

        DriveService driveService = new DriveService(config, transport, jsonFactory, credential);
        GmailService gmailService = new GmailService(config, transport, jsonFactory, credential);
        CheckProcessor processor = new CheckProcessor(config, driveService, gmailService);
        DriveWatcher watcher = new DriveWatcher(config, driveService, processor);

        watcher.start();
    }
}
