package me.dcplugin.dcustomitems.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Formatter;

/**
 * Keeps DC-CustomItems logs in its own file instead of the Paper console.
 * The previous latest.log is moved to logs/archive on every plugin start.
 */
public final class PluginLogManager {

    private static final String LOG_DIRECTORY = "logs";
    private static final String ARCHIVE_DIRECTORY = "archive";
    private static final String LATEST_FILE = "latest.log";

    private final JavaPlugin plugin;
    private FileLogHandler fileHandler;

    public PluginLogManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Logger logger = plugin.getLogger();
        FileLogHandler newHandler;

        try {
            Path logsDirectory = plugin.getDataFolder().toPath().resolve(LOG_DIRECTORY);
            Path archiveDirectory = logsDirectory.resolve(ARCHIVE_DIRECTORY);
            Files.createDirectories(archiveDirectory);

            Path latestFile = logsDirectory.resolve(LATEST_FILE);
            archivePreviousLog(latestFile, archiveDirectory);

            newHandler = new FileLogHandler(latestFile);
            newHandler.setLevel(Level.ALL);
        } catch (IOException exception) {
            // Keep the normal Bukkit handler if the plugin cannot create its file.
            // This makes a permissions/disk failure visible instead of dropping all logs.
            Bukkit.getLogger().log(
                Level.SEVERE,
                "[DC-CustomItems] Cannot initialize file logging at "
                    + plugin.getDataFolder().getAbsolutePath(),
                exception
            );
            return;
        }

        removeExistingHandlers(logger);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        fileHandler = newHandler;
        logger.addHandler(fileHandler);
    }

    public void stop() {
        Logger logger = plugin.getLogger();
        if (fileHandler != null) {
            logger.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
        }
        logger.setUseParentHandlers(true);
    }

    private void removeExistingHandlers(Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            handler.close();
        }
    }

    private void archivePreviousLog(Path latestFile, Path archiveDirectory) throws IOException {
        if (!Files.exists(latestFile)) {
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(
            new Date(Files.getLastModifiedTime(latestFile).toMillis())
        );
        Path archiveFile = archiveDirectory.resolve(timestamp + ".log");
        int suffix = 1;
        while (Files.exists(archiveFile)) {
            archiveFile = archiveDirectory.resolve(timestamp + "-" + suffix + ".log");
            suffix++;
        }

        try {
            Files.move(latestFile, archiveFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(latestFile, archiveFile);
        }
    }

    private static final class FileLogHandler extends Handler {

        private final BufferedWriter writer;
        private final Path file;

        private FileLogHandler(Path file) throws IOException {
            this.file = file;
            this.writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            setFormatter(new PluginLogFormatter());
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }

            try {
                writer.write(getFormatter().format(record));
                // Keep latest.log suitable for live tailing in a panel/file browser.
                writer.flush();
            } catch (IOException exception) {
                // The plugin logger must not recurse into itself if the disk fails.
                Bukkit.getLogger().log(
                    Level.SEVERE,
                    "[DC-CustomItems] Cannot write log file " + file,
                    exception
                );
            }
        }

        @Override
        public synchronized void flush() {
            try {
                writer.flush();
            } catch (IOException exception) {
                Bukkit.getLogger().log(
                    Level.SEVERE,
                    "[DC-CustomItems] Cannot flush log file " + file,
                    exception
                );
            }
        }

        @Override
        public synchronized void close() {
            try {
                writer.flush();
                writer.close();
            } catch (IOException exception) {
                Bukkit.getLogger().log(
                    Level.SEVERE,
                    "[DC-CustomItems] Cannot close log file " + file,
                    exception
                );
            }
        }
    }

    private static final class PluginLogFormatter extends Formatter {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public synchronized String format(LogRecord record) {
            StringBuilder output = new StringBuilder()
                .append('[')
                .append(dateFormat.format(new Date(record.getMillis())))
                .append("] [")
                .append(record.getLevel().getName())
                .append("] ")
                .append(formatMessage(record))
                .append(System.lineSeparator());

            if (record.getThrown() != null) {
                StringWriter stackTrace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                output.append(stackTrace).append(System.lineSeparator());
            }

            return output.toString();
        }
    }
}
