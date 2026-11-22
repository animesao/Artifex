package me.dcplugin.artifex.api.integration;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LinkConfigReader {
    private static final String DISCORD_ID_PATTERN = "\\d{17,20}";

    private LinkConfigReader() {
    }

    public static Map<String, UUID> read(File file) {
        Map<String, UUID> result = new HashMap<>();
        if (file == null || !file.isFile()) return result;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            if (key == null || !key.matches(DISCORD_ID_PATTERN)) continue;
            String value = yaml.getString(key);
            if (value == null) continue;
            try {
                result.put(key, UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }
}
