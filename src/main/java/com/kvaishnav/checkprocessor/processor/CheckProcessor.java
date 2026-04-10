package com.kvaishnav.checkprocessor.processor;

import com.google.api.services.drive.model.File;
import com.kvaishnav.checkprocessor.config.AppConfig;
import com.kvaishnav.checkprocessor.drive.DriveService;
import com.kvaishnav.checkprocessor.email.GmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates the full check-processing pipeline for a single file:
 *
 * <ol>
 *   <li>Download the check image from Drive to a local temp file.</li>
 *   <li>Generate any supporting documents (currently stubbed).</li>
 *   <li>Email the check image + documents to the configured recipient.</li>
 *   <li>Upload the check image + documents to the dated Drive output folder
 *       ({@code ddMMM_Deposites}) under the configured parent folder.</li>
 *   <li>Clean up all local temp files.</li>
 * </ol>
 *
 * <p>If any step fails the error is logged and the file ID is <em>not</em>
 * marked as processed, so the watcher will retry it on the next poll.
 */
public class CheckProcessor {

    private static final Logger log = LoggerFactory.getLogger(CheckProcessor.class);

    /**
     * Output folder name pattern: e.g. "10Apr_Deposites" for April 10.
     * Note: "Deposites" matches the user-specified naming convention.
     */
    private static final DateTimeFormatter FOLDER_DATE_FMT =
            DateTimeFormatter.ofPattern("ddMMM");

    private final AppConfig config;
    private final DriveService driveService;
    private final GmailService gmailService;
    private final DocumentGenerator docGenerator;

    public CheckProcessor(AppConfig config,
                          DriveService driveService,
                          GmailService gmailService) {
        this.config = config;
        this.driveService = driveService;
        this.gmailService = gmailService;
        this.docGenerator = new DocumentGenerator();
    }

    /**
     * Processes a single check file from Google Drive.
     *
     * @param driveFile metadata for the Drive file to process
     * @throws Exception if a fatal error occurs that should prevent the file
     *                   from being marked as processed
     */
    public void process(File driveFile) throws Exception {
        log.info("Processing check: {}", driveFile.getName());

        Path checkImage = null;
        List<Path> generatedDocs = List.of();

        try {
            // Step 1: download
            checkImage = driveService.downloadFile(driveFile);

            // Step 2: generate documents (stubbed until types are defined)
            generatedDocs = docGenerator.generate(driveFile.getName(), checkImage);

            // Step 3: send email
            gmailService.sendCheckEmail(driveFile.getName(), checkImage, generatedDocs);

            // Step 4: archive to Drive output folder (creates folder if needed)
            String outputFolderName = LocalDate.now().format(FOLDER_DATE_FMT) + "_Deposites";
            String outputFolderId = driveService.ensureFolder(
                    config.getOutputParentFolderId(), outputFolderName);

            driveService.uploadFile(
                    checkImage, driveFile.getName(), driveFile.getMimeType(), outputFolderId);

            for (Path doc : generatedDocs) {
                driveService.uploadFile(
                        doc, doc.getFileName().toString(), "application/pdf", outputFolderId);
            }

            log.info("Successfully processed '{}'", driveFile.getName());

        } finally {
            deleteTempFile(checkImage);
            generatedDocs.forEach(this::deleteTempFile);
        }
    }

    private void deleteTempFile(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Could not delete temp file: {}", path);
        }
    }
}
