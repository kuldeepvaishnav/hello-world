package com.kvaishnav.checkprocessor.drive;

import com.google.api.services.drive.model.File;
import com.kvaishnav.checkprocessor.config.AppConfig;
import com.kvaishnav.checkprocessor.processor.CheckProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls a Google Drive folder on a fixed interval and hands off any newly
 * discovered check images to {@link CheckProcessor}.
 *
 * <p>Processed Drive file IDs are persisted to a plain-text file so the
 * watcher skips already-handled files across restarts.
 */
public class DriveWatcher {

    private static final Logger log = LoggerFactory.getLogger(DriveWatcher.class);

    private final AppConfig config;
    private final DriveService driveService;
    private final CheckProcessor processor;

    /** In-memory set of Drive file IDs that have already been processed. */
    private final Set<String> processedIds = new HashSet<>();

    /** Flat file used to persist {@link #processedIds} across restarts. */
    private final Path processedStore;

    public DriveWatcher(AppConfig config,
                        DriveService driveService,
                        CheckProcessor processor) throws IOException {
        this.config = config;
        this.driveService = driveService;
        this.processor = processor;
        this.processedStore = Path.of(config.getProcessedFilesStore());
        loadProcessedIds();
    }

    /**
     * Starts the polling loop. This method returns immediately; the scheduler
     * runs on a background thread. The JVM is kept alive by the non-daemon
     * scheduler thread until the process is stopped (Ctrl-C).
     */
    public void start() {
        long intervalSeconds = config.getPollIntervalSeconds();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "drive-watcher");
            t.setDaemon(false);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::poll, 0, intervalSeconds, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.shutdown();
        }, "shutdown-hook"));

        log.info("Polling Drive folder {} every {} seconds. Press Ctrl+C to stop.",
                config.getInputFolderId(), intervalSeconds);
    }

    private void poll() {
        try {
            List<File> files = driveService.listCheckFiles(config.getInputFolderId());
            for (File file : files) {
                if (!processedIds.contains(file.getId())) {
                    log.info("New check file detected: {}", file.getName());
                    processor.process(file);
                    markProcessed(file.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error during Drive poll — will retry next interval", e);
        }
    }

    private void loadProcessedIds() throws IOException {
        if (Files.exists(processedStore)) {
            processedIds.addAll(Files.readAllLines(processedStore));
            log.info("Loaded {} previously processed file ID(s) from {}",
                    processedIds.size(), processedStore);
        }
    }

    private void markProcessed(String fileId) throws IOException {
        processedIds.add(fileId);
        Files.writeString(processedStore, fileId + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
