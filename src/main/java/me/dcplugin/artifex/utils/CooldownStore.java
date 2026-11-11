package me.dcplugin.artifex.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage of the player cooldowns (in memory + on disk in YAML).
 *
 * The key is stored as a list of entries, not as a YAML path: item IDs can
 * contain dots ("my.item"), and the path "uuid.my.item" created nested
 * sections — such cooldowns were lost after loading.
 */
public final class CooldownStore {

    private static final String SEPARATOR = "\u0000";
    private static final String LIST_PATH = "entries";

    private final Map<String, Long> endsAt = new ConcurrentHashMap<>();
    private final File file;

    public CooldownStore(File dataFolder) {
        this.file = new File(dataFolder, "cooldowns.yml");
        load();
    }

    private static String key(String player, String itemId) {
        return player + SEPARATOR + itemId;
    }

    public boolean isOnCooldown(String player, String itemId, long now) {
        Long end = endsAt.get(key(player, itemId));
        return end != null && end > now;
    }

    public long remaining(String player, String itemId, long now) {
        Long end = endsAt.get(key(player, itemId));
        if (end == null) return 0;
        return Math.max(0, end - now);
    }

    public void put(String player, String itemId, long endsAtMillis) {
        endsAt.put(key(player, itemId), endsAtMillis);
    }

    public void remove(String player, String itemId) {
        endsAt.remove(key(player, itemId));
    }

    public void save() {
        try {
            org.bukkit.configuration.file.YamlConfiguration yml = new org.bukkit.configuration.file.YamlConfiguration();
            List<Map<String, Object>> entries = new ArrayList<>(endsAt.size());
            for (Map.Entry<String, Long> e : endsAt.entrySet()) {
                String[] parts = e.getKey().split(SEPARATOR, 2);
                if (parts.length != 2) continue;
                Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("player", parts[0]);
                entry.put("item", parts[1]);
                entry.put("expires", e.getValue());
                entries.add(entry);
            }
            yml.set(LIST_PATH, entries);
            yml.save(file);
        } catch (Exception ignored) {
            // Do not break the game logic because of a save error
        }
    }

    private void load() {
        try {
            if (!file.exists()) return;
            org.bukkit.configuration.file.YamlConfiguration yml =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            long now = System.currentTimeMillis();

            // New format: a list of entries
            for (Map<?, ?> entry : yml.getMapList(LIST_PATH)) {
                Object player = entry.get("player");
                Object item = entry.get("item");
                Object expires = entry.get("expires");
                if (!(player instanceof String) || !(item instanceof String)) continue;
                if (!(expires instanceof Number)) continue;
                long end = ((Number) expires).longValue();
                if (end > now) {
                    endsAt.put(key((String) player, (String) item), end);
                }
            }

            // Old format: entries in the YAML root by the path "<uuid>.<itemId>".
            // YAML always expands such a path into nested sections,
            // so the path is reassembled: the root key is the uuid,
            // the nested sections are the itemId (it may contain dots).
            for (String rootKey : yml.getKeys(false)) {
                if (LIST_PATH.equals(rootKey)) continue;
                Object value = yml.get(rootKey);
                if (value instanceof Number number) {
                    long end = number.longValue();
                    if (end > now) {
                        int dot = rootKey.indexOf('.');
                        if (dot > 0) {
                            endsAt.put(key(rootKey.substring(0, dot), rootKey.substring(dot + 1)), end);
                        }
                    }
                    continue;
                }
                org.bukkit.configuration.ConfigurationSection section =
                        yml.getConfigurationSection(rootKey);
                if (section != null) {
                    collectLegacyEntries(section, rootKey, "", now);
                }
            }
        } catch (Exception ignored) {
            // A corrupted file must not break the startup
        }
    }

    /**
     * Recursively collects the entries of the old format.
     *
     * @param section  the current YAML section
     * @param player   the player UUID (the root key)
     * @param itemPath the already assembled part of the itemId path
     */
    private void collectLegacyEntries(org.bukkit.configuration.ConfigurationSection section,
                                      String player, String itemPath, long now) {
        for (String child : section.getKeys(false)) {
            Object value = section.get(child);
            String path = itemPath.isEmpty() ? child : itemPath + "." + child;
            if (value instanceof Number number) {
                long end = number.longValue();
                if (end > now) {
                    endsAt.put(key(player, path), end);
                }
                continue;
            }
            org.bukkit.configuration.ConfigurationSection nested = section.getConfigurationSection(child);
            if (nested != null) {
                collectLegacyEntries(nested, player, path, now);
            }
        }
    }
}
