package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final Main plugin;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }

    public List<String> getDisableWorlds() {
        return plugin.getConfig().getStringList("disable-worlds");
    }

    public List<String> getWhitelistBlocks() {
        return plugin.getConfig().getStringList("whitelist-blocks");
    }

    public boolean isWorldDisabled(String worldName) {
        return getDisableWorlds().contains(worldName);
    }

    public boolean isBlockWhitelisted(String blockMaterial) {
        List<String> whitelist = getWhitelistBlocks();
        return whitelist.isEmpty() || whitelist.contains(blockMaterial);
    }
}
