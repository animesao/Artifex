package me.dcplugin.artifex.api.database;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Simple YAML storage for small configuration-like data.
 *
 * Use it for small promo lists, settings, and prototypes. For many players or
 * frequent writes, use DatabaseManager with SQLite/MySQL instead. All methods
 * are synchronized and should be called on the Bukkit main thread.
 */
public class YamlStorage {

    private final Main plugin;
    private final File file;
    private YamlConfiguration data;

    public YamlStorage(Main plugin, String fileName) {
        this.plugin = plugin;
        File directory = new File(plugin.getDataFolder(), "storage");
        if (!directory.exists()) directory.mkdirs();
        this.file = new File(directory, fileName);
        reload();
    }

    public synchronized void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized boolean contains(String path) {
        return data.contains(path);
    }

    public synchronized String getString(String path, String def) {
        return data.getString(path, def);
    }

    public synchronized int getInt(String path, int def) {
        return data.getInt(path, def);
    }

    public synchronized boolean getBoolean(String path, boolean def) {
        return data.getBoolean(path, def);
    }

    public synchronized List<String> getStringList(String path) {
        return data.getStringList(path);
    }

    public synchronized void set(String path, Object value) {
        data.set(path, value);
    }

    public synchronized boolean save() {
        try {
            data.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "Could not save YAML storage " + file.getName() + ": " + e.getMessage(), e);
            return false;
        }
    }

    public File getFile() {
        return file;
    }
}
