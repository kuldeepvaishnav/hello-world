package com.kvaishnav.checkprocessor.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and exposes application configuration from {@code config.properties}
 * on the classpath.
 */
public class AppConfig {

    private final Properties props;

    private AppConfig(Properties props) {
        this.props = props;
    }

    public static AppConfig load() throws IOException {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IOException(
                        "config.properties not found on classpath. " +
                        "Copy config.properties.example to config.properties and fill in your values.");
            }
            props.load(in);
        }
        return new AppConfig(props);
    }

    /** Drive folder ID to watch for incoming check images. */
    public String getInputFolderId() {
        return require("drive.input.folder.id");
    }

    /**
     * Drive folder ID under which dated output folders (ddMMM_Deposites) are created.
     * This is the *parent* — the utility creates the daily sub-folder automatically.
     */
    public String getOutputParentFolderId() {
        return require("drive.output.parent.folder.id");
    }

    /** Email address to send processed check notifications to. */
    public String getRecipientEmail() {
        return require("email.recipient");
    }

    /** Path to the Google OAuth credentials JSON file (from Google Cloud Console). */
    public String getCredentialsPath() {
        return props.getProperty("google.credentials.path", "credentials.json");
    }

    /** Directory where OAuth tokens are cached after the first browser login. */
    public String getTokensDirectory() {
        return props.getProperty("google.tokens.directory", "tokens");
    }

    /** How often to poll the Drive input folder, in seconds. */
    public long getPollIntervalSeconds() {
        return Long.parseLong(props.getProperty("drive.poll.interval.seconds", "30"));
    }

    /**
     * Path to the flat file used to remember which Drive file IDs have already
     * been processed, so the watcher survives restarts without re-processing.
     */
    public String getProcessedFilesStore() {
        return props.getProperty("processed.files.store", "processed_files.txt");
    }

    private String require(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required config property is missing or empty: " + key);
        }
        return value.trim();
    }
}
