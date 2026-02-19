package com.academy.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);

    @Value("${app.backup.enabled:true}")
    private boolean backupEnabled;

    @Value("${app.backup.directory:./backups}")
    private String backupDirectory;

    @Value("${app.backup.keep-days:7}")
    private int keepDays;

    @Value("${spring.datasource.url:jdbc:h2:file:./data/academy}")
    private String datasourceUrl;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Initialize backup directory on startup
     */
    @PostConstruct
    public void init() {
        if (!backupEnabled) {
            log.info("Database backup is DISABLED");
            return;
        }

        try {
            File backupDir = new File(backupDirectory);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
                log.info("Created backup directory: {}", backupDirectory);
            }
            log.info("Database backup enabled. Directory: {}", backupDirectory);
            log.info("Keeping backups for {} days", keepDays);
        } catch (Exception e) {
            log.error("Failed to initialize backup directory", e);
        }
    }

    /**
     * Backup database daily at 2 AM
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
    public void scheduledBackup() {
        if (!backupEnabled) {
            return;
        }

        log.info("Starting scheduled database backup...");
        boolean success = performBackup();

        if (success) {
            log.info("Scheduled backup completed successfully");
            cleanOldBackups();
        } else {
            log.error("Scheduled backup FAILED");
        }
    }

    /**
     * Manual backup (can be called from controller)
     */
    public boolean performBackup() {
        try {
            // Extract database file path from datasource URL
            String dbFilePath = extractDatabaseFilePath();

            if (dbFilePath == null) {
                log.error("Could not extract database file path from: {}", datasourceUrl);
                return false;
            }

            // H2 creates multiple files: .mv.db, .trace.db, .lock.db
            Path dbFile = Paths.get(dbFilePath + ".mv.db");

            if (!Files.exists(dbFile)) {
                log.error("Database file not found: {}", dbFile);
                return false;
            }

            // Create backup filename with timestamp
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            String backupFileName = "academy-backup-" + timestamp + ".mv.db";
            Path backupFile = Paths.get(backupDirectory, backupFileName);

            // Copy database file
            Files.copy(dbFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

            long fileSizeKB = Files.size(backupFile) / 1024;
            log.info("Database backed up successfully: {} ({} KB)",
                    backupFileName, fileSizeKB);

            return true;

        } catch (IOException e) {
            log.error("Failed to backup database", e);
            return false;
        }
    }

    /**
     * Extract database file path from JDBC URL
     * Example: jdbc:h2:file:./data/academy -> ./data/academy
     */
    private String extractDatabaseFilePath() {
        if (datasourceUrl == null || !datasourceUrl.contains("jdbc:h2:file:")) {
            return null;
        }

        String path = datasourceUrl.replace("jdbc:h2:file:", "");

        // Remove any additional parameters after semicolon
        int semicolonIndex = path.indexOf(';');
        if (semicolonIndex > 0) {
            path = path.substring(0, semicolonIndex);
        }

        return path;
    }

    /**
     * Delete backups older than configured days
     */
    public void cleanOldBackups() {
        try {
            File backupDir = new File(backupDirectory);
            File[] backupFiles = backupDir.listFiles((dir, name) ->
                    name.startsWith("academy-backup-") && name.endsWith(".mv.db"));

            if (backupFiles == null || backupFiles.length == 0) {
                return;
            }

            long cutoffTime = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);
            int deletedCount = 0;

            for (File file : backupFiles) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                        log.info("Deleted old backup: {}", file.getName());
                    } else {
                        log.warn("Failed to delete old backup: {}", file.getName());
                    }
                }
            }

            if (deletedCount > 0) {
                log.info("Cleaned up {} old backup(s)", deletedCount);
            }

            // Log current backup count
            int remainingBackups = backupDir.listFiles((dir, name) ->
                    name.startsWith("academy-backup-")).length;
            log.info("Current backup count: {}", remainingBackups);

        } catch (Exception e) {
            log.error("Failed to clean old backups", e);
        }
    }

    /**
     * Get list of all backups with their info
     */
    public BackupInfo[] listBackups() {
        File backupDir = new File(backupDirectory);
        File[] backupFiles = backupDir.listFiles((dir, name) ->
                name.startsWith("academy-backup-") && name.endsWith(".mv.db"));

        if (backupFiles == null || backupFiles.length == 0) {
            return new BackupInfo[0];
        }

        return Arrays.stream(backupFiles)
                .map(file -> {
                    BackupInfo info = new BackupInfo();
                    info.fileName = file.getName();
                    info.filePath = file.getAbsolutePath();
                    info.sizeKB = file.length() / 1024;
                    info.createdAt = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault()
                    );
                    return info;
                })
                .sorted(Comparator.comparing(BackupInfo::getCreatedAt).reversed())
                .toArray(BackupInfo[]::new);
    }

    /**
     * Backup info DTO
     */
    public static class BackupInfo {
        private String fileName;
        private String filePath;
        private long sizeKB;
        private LocalDateTime createdAt;

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public long getSizeKB() { return sizeKB; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}