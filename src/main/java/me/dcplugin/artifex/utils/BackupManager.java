package me.dcplugin.artifex.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public final class BackupManager {

    private BackupManager() {}

    public static Path backup(File dataFolder, int keep) throws IOException {
        Path items = dataFolder.toPath().resolve("items");
        Path backups = dataFolder.toPath().resolve("backups");
        Files.createDirectories(backups);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path dest = backups.resolve("items-" + stamp);
        Files.createDirectories(dest);
        if (Files.exists(items)) {
            try (Stream<Path> walk = Files.walk(items)) {
                for (Path src : (Iterable<Path>) walk::iterator) {
                    Path rel = items.relativize(src);
                    Path target = dest.resolve(rel.toString());
                    if (Files.isDirectory(src)) Files.createDirectories(target);
                    else Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        for (String extra : new String[]{"config.yml", "data.db"}) {
            Path src = dataFolder.toPath().resolve(extra);
            if (Files.exists(src) && !Files.isDirectory(src)) {
                Files.copy(src, dest.resolve(extra), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        prune(backups, Math.max(1, keep));
        return dest;
    }

    private static void prune(Path backups, int keep) throws IOException {
        List<Path> dirs = new ArrayList<>();
        try (Stream<Path> s = Files.list(backups)) {
            s.filter(Files::isDirectory).sorted(Comparator.comparing(p -> p.getFileName().toString())).forEach(dirs::add);
        }
        while (dirs.size() > keep) {
            Path oldest = dirs.remove(0);
            try (Stream<Path> walk = Files.walk(oldest)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
    }
}
