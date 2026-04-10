package com.kvaishnav.checkprocessor.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.kvaishnav.checkprocessor.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Thin wrapper around the Google Drive API v3.
 * Handles listing, downloading, folder creation, and file uploads.
 */
public class DriveService {

    private static final Logger log = LoggerFactory.getLogger(DriveService.class);
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private final Drive drive;

    public DriveService(AppConfig config,
                        NetHttpTransport transport,
                        GsonFactory jsonFactory,
                        Credential credential) {
        this.drive = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("Check Processor")
                .build();
    }

    /**
     * Lists image files (JPEG, PNG, TIFF, GIF, WebP) and PDFs inside
     * {@code folderId}, ordered oldest-first.
     */
    public List<File> listCheckFiles(String folderId) throws IOException {
        String query = String.format(
                "'%s' in parents and trashed = false " +
                "and (mimeType contains 'image/' or mimeType = 'application/pdf')",
                folderId);

        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id, name, mimeType, createdTime)")
                .setOrderBy("createdTime")
                .execute();

        return result.getFiles() != null ? result.getFiles() : Collections.emptyList();
    }

    /**
     * Downloads a Drive file to a local temp file.
     *
     * @return path to the temporary file (caller is responsible for deletion)
     */
    public Path downloadFile(File driveFile) throws IOException {
        String ext = extensionFor(driveFile.getMimeType());
        Path tmp = Files.createTempFile("check_", ext);
        try (OutputStream out = Files.newOutputStream(tmp)) {
            drive.files().get(driveFile.getId()).executeMediaAndDownloadTo(out);
        }
        log.info("Downloaded '{}' → {}", driveFile.getName(), tmp);
        return tmp;
    }

    /**
     * Ensures a child folder named {@code folderName} exists under
     * {@code parentId}. Returns the folder's Drive ID. If the folder already
     * exists it is reused — useful for the daily ddMMM_Deposites folder.
     */
    public String ensureFolder(String parentId, String folderName) throws IOException {
        String query = String.format(
                "'%s' in parents and name = '%s' " +
                "and mimeType = '%s' and trashed = false",
                parentId, folderName, FOLDER_MIME);

        FileList existing = drive.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        if (existing.getFiles() != null && !existing.getFiles().isEmpty()) {
            String id = existing.getFiles().get(0).getId();
            log.debug("Reusing existing Drive folder '{}' (id={})", folderName, id);
            return id;
        }

        File meta = new File();
        meta.setName(folderName);
        meta.setMimeType(FOLDER_MIME);
        meta.setParents(List.of(parentId));

        File created = drive.files().create(meta).setFields("id").execute();
        log.info("Created Drive folder '{}' (id={})", folderName, created.getId());
        return created.getId();
    }

    /**
     * Uploads a local file to the given Drive folder.
     *
     * @param localFile  path to the file to upload
     * @param fileName   name the file should have in Drive
     * @param mimeType   MIME type for the upload
     * @param folderId   destination Drive folder ID
     */
    public void uploadFile(Path localFile, String fileName, String mimeType,
                           String folderId) throws IOException {
        File meta = new File();
        meta.setName(fileName);
        meta.setParents(List.of(folderId));

        try (var content = Files.newInputStream(localFile)) {
            drive.files()
                    .create(meta, new InputStreamContent(mimeType, content))
                    .setFields("id, name")
                    .execute();
        }
        log.info("Uploaded '{}' to Drive folder {}", fileName, folderId);
    }

    private static String extensionFor(String mimeType) {
        if (mimeType == null) return ".bin";
        return switch (mimeType) {
            case "image/jpeg"      -> ".jpg";
            case "image/png"       -> ".png";
            case "image/tiff"      -> ".tif";
            case "image/gif"       -> ".gif";
            case "image/webp"      -> ".webp";
            case "application/pdf" -> ".pdf";
            default                -> ".bin";
        };
    }
}
