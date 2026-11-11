package me.dcplugin.artifex;


import me.dcplugin.artifex.api.ItemRegistry;
import me.dcplugin.artifex.api.database.DatabaseManager;
import me.dcplugin.artifex.api.modules.ModuleManager;
import me.dcplugin.artifex.api.placeholders.PlaceholderManager;
import me.dcplugin.artifex.bootstrap.PluginBootstrap;
import me.dcplugin.artifex.handlers.CustomItemHandler;
import me.dcplugin.artifex.managers.*;
import me.dcplugin.artifex.utils.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static Main instance;
    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        instance = this;
        try {
            bootstrap = new PluginBootstrap(this);
            bootstrap.enable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error enabling plugin: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (bootstrap != null) bootstrap.disable();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error disabling plugin: " + e.getMessage(), e);
        } finally {
            // The static reference to the plugin kept the old classloader alive
            // after unloading, so reloaded plugin versions were never released
            // from memory.
            if (instance == this) instance = null;
        }
    }

    // ===== Getters =====

    public static Main getInstance() { return instance; }

    public ConfigManager getConfigManager() { return bootstrap.getConfigManager(); }
    public MessageManager getMessageManager() { return bootstrap.getMessageManager(); }
    public EffectManager getEffectManager() { return bootstrap.getEffectManager(); }
    public AttributeManager getAttributeManager() { return bootstrap.getAttributeManager(); }
    public CustomItemHandler getItemHandler() { return bootstrap.getItemHandler(); }
    public ArmorSetManager getArmorSetManager() { return bootstrap.getArmorSetManager(); }
    public ItemRegistry getApiItemRegistry() { return bootstrap.getApiItemRegistry(); }
    public DatabaseManager getDatabaseManager() { return bootstrap.getDatabaseManager(); }
    public PlaceholderManager getPlaceholderManager() { return bootstrap.getPlaceholderManager(); }
    public ModuleManager getModuleManager() { return bootstrap.getModuleManager(); }
    public UpdateChecker getUpdateChecker() { return bootstrap.getUpdateChecker(); }
    public me.dcplugin.artifex.listeners.PlayerListener getPlayerListener() { return bootstrap.getPlayerListener(); }
    public me.dcplugin.artifex.listeners.TriggerListener getTriggerListener() { return bootstrap.getTriggerListener(); }
    public me.dcplugin.artifex.handlers.EquippedItemsChecker getEquippedItemsChecker() { return bootstrap.getEquippedItemsChecker(); }
    public me.dcplugin.artifex.utils.CooldownStore getCooldownStore() { return bootstrap.getCooldownStore(); }

    // Delegate methods for backward compatibility
    public void registerCustomCommands() { bootstrap.getCommandRegistrar().registerAll(bootstrap.getApiItemRegistry()); }
    public void unregisterCustomCommands() { bootstrap.getCommandRegistrar().unregisterAll(); }
}
