/*
 *     XRayDetector - An advanced automatic detector to prevent X-Ray in your server
 *     Copyright (C) 2025 'AmberCode'
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ambercode.logging;

import com.ambercode.XRayDetector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The FileLogger class is a utility for buffering log messages and writing them to a file.
 * It is designed to work in conjunction with an XRayDetector instance for logging operations.
 * Thread safety is ensured through the use of a ReentrantLock for all modifications to the
 * internal buffer of log messages.
 */
public class FileLogger {

    private final XRayDetector xRayDetector;
    private final ArrayList<String> logLines;
    private final ReentrantLock lock;
    private boolean enabled;
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructs a new instance of the FileLogger class.
     *
     * @param xRayDetector The XRayDetector instance used for configuration and logging management. Must not be null.
     */
    public FileLogger(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.logLines = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.enabled = xRayDetector.getStandardConfig().isLoggerEnabled();
    }

    /**
     * Checks if the FileLogger is currently enabled.
     *
     * @return true if the FileLogger is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the FileLogger based on the provided value.
     *
     * @param enabled true to enable the FileLogger, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Adds a log message to the buffer with a timestamp
     * @param message The log message to add
     */
    public void addLogMessage(@NotNull String message) {
        if (!enabled) return;

        lock.lock();
        try {
            final String timestampedMessage = String.format("[%s] %s", LOG_DATE_FORMAT.format(new Date()), message);
            logLines.add(timestampedMessage);
            if (xRayDetector.getStandardConfig().isDebugEnabled()) {xRayDetector.getLogger().info(timestampedMessage);}
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds multiple log messages to the buffer
     * @param messages List of messages to add
     */
    public void addLogMessages(@NotNull List<String> messages) {
        if (!enabled || messages == null || messages.isEmpty()) return;

        lock.lock();
        try {
            for (String message : messages) {
                addLogMessage(message);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Writes all buffered log lines to a file with current timestamp
     * @return true if successful, false otherwise
     */
    public boolean writeLogsToFile() {
        if (!enabled || logLines.isEmpty()) {
            return false;
        }

        lock.lock();
        try {
            File pluginFolder = xRayDetector.getDataFolder();
            File logsFolder = new File(pluginFolder, "logs");
            if (!logsFolder.exists() && !logsFolder.mkdir()) {
                xRayDetector.getLogger().info("Failed to create logs directory: " + logsFolder.getAbsolutePath());
                return false;
            }

            String fileName = "xray_log_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
            File logFile = new File(logsFolder, fileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                for (String line : logLines) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();

                xRayDetector.getLogger().info("Successfully wrote " + logLines.size() +
                        " log lines to: " + logFile.getAbsolutePath());

                // Clear the buffer after successful write
                logLines.clear();
                return true;

            } catch (IOException e) {
                xRayDetector.getLogger().warning("Error writing log file: " + e.getMessage());
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the current number of buffered log lines
     * @return number of log lines in buffer
     */
    public int getBufferedLogCount() {
        lock.lock();
        try {
            return logLines.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears all buffered log lines
     */
    public void clearLogBuffer() {
        lock.lock();
        try {
            logLines.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if there are any buffered log lines
     * @return true if buffer is not empty
     */
    public boolean hasBufferedLogs() {
        lock.lock();
        try {
            return !logLines.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets a copy of the current log buffer (thread-safe)
     * @return List containing all buffered log messages
     */
    @Nullable
    public List<String> getLogBufferCopy() {
        lock.lock();
        try {
            return new ArrayList<>(logLines);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Writes logs to file and returns the file path if successful
     * @return File path of the created log file, or null if failed
     */
    @Nullable
    public String writeLogsAndGetPath() {
        if (writeLogsToFile()) {
            File pluginFolder = xRayDetector.getDataFolder();
            File logsFolder = new File(pluginFolder, "logs");
            String fileName = "xray_log_" + FILE_DATE_FORMAT.format(new Date()) + ".txt";
            return new File(logsFolder, fileName).getAbsolutePath();
        }

        return null;
    }
}