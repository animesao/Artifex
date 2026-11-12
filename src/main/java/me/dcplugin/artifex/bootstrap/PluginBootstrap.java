package me.dcplugin.artifex.bootstrap;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.ApiEventListener;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.api.ItemRegistry;
import me.dcplugin.artifex.api.config.MessagesConfig;
import me.dcplugin.artifex.api.database.DatabaseManager;
import me.dcplugin.artifex.api.modules.ModuleManager;
import me.dcplugin.artifex.api.placeholders.PlaceholderManager;
import me.dcplugin.artifex.commands.CustomItemsCommand;
import me.dcplugin.artifex.handlers.CustomItemHandler;
import me.dcplugin.artifex.handlers.EquippedItemsChecker;
import me.dcplugin.artifex.listeners.PlayerListener;
import me.dcplugin.artifex.listeners.TriggerListener;
import me.dcplugin.artifex.managers.*;
import me.dcplugin.artifex.utils.UpdateChecker;
import org.bukkit.Bukkit;

import java.util.logging.Level;

/**
 * Centralized initialization of all plugin components.
 * Called from Main.onEnable() and Main.onDisable().
 */
public class PluginBootstrap {

    private final Main plugin;
    private final CommandRegistrar commandRegistrar;
    private final ConfigMigrator configMigrator;

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private EffectManager effectManager;
    private AttributeManager attributeManager;
    private CustomItemHandler itemHandler;
    private ArmorSetManager armorSetManager;

    // Listeners
    private PlayerListener playerListener;
    private TriggerListener triggerListener;
    private EquippedItemsChecker equippedItemsChecker;

    // API
    private ItemRegistry apiItemRegistry;
    private ModuleManager moduleManager;
    private DatabaseManager databaseManager;
    private PlaceholderManager placeholderManager;
    private UpdateChecker updateChecker;
    private PluginLogManager pluginLogManager;
    private me.dcplugin.artifex.utils.CooldownStore cooldownStore;

    public PluginBootstrap(Main plugin) {
        this.plugin = plugin;
        this.commandRegistrar = new CommandRegistrar(plugin);
        this.configMigrator = new ConfigMigrator(plugin);
    }

    /**
     * Full plugin initialization
     */
    public void enable() {
        plugin.getLogger().info("Artifex starting...");

        plugin.saveDefaultConfig();

        // The plugin ships a single file: items/messages.java.
        // All the examples live in the repository (samples/) and are never
        // installed automatically — a fresh install stays empty and clean.
        configMigrator.createDefaultMessagesFile();
        configMigrator.checkConfigMigration();

        // File logging (optional)
        initFileLogging();

        // Manager initialization
        initManagers();

        // Placeholders are created BEFORE the Java API is loaded: on startup
        // ItemRegistry.loadAll() registers the Java placeholders — without
        // the manager they would not appear until the first /ci reload.
        initPlaceholders();

        // API initialization
        ItemAPI.init(plugin);
        apiItemRegistry = new ItemRegistry(plugin);
        moduleManager = new ModuleManager(plugin);

        // The Java API is loaded first: YAML recipes may reference
        // Java items as ingredients, and the registry must already exist
        // by that moment (it used to be null — that caused an NPE).
        apiItemRegistry.loadAll();

        // Loading of YAML items (and their recipes)
        itemHandler.loadCustomItems();

        // The recipes of Java items are re-registered after the YAML load:
        // a Java recipe may use a YAML item as an ingredient.
        apiItemRegistry.registerJavaRecipes();

        moduleManager.loadAll();

        // Command registration (delayed by 1 tick)
        registerBukkitCommand();

        // Event registration
        registerEvents();

        // Database
        initDatabase();

        // Global equipped-items check task
        equippedItemsChecker = new EquippedItemsChecker(plugin);
        equippedItemsChecker.start();

        // Dynamic commands from the API
        commandRegistrar.registerAll(apiItemRegistry);

        // Update check
        checkUpdates();

        logStartupInfo();
    }

    /**
     * Clean plugin shutdown
     */
    public void disable() {
        plugin.getLogger().info("Artifex disabling...");

        // Remove effects from players: after the plugin is disabled they would
        // otherwise linger until their duration expires (60 s).
        if (effectManager != null) {
            for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                effectManager.stopPeriodicEffects(player);
            }
        }
        if (attributeManager != null) attributeManager.cleanup();

        if (equippedItemsChecker != null) equippedItemsChecker.cancel();
        try { commandRegistrar.unregisterAll(); } catch (Exception ignored) {}
        try {
            if (placeholderManager != null) placeholderManager.unregisterExpansion();
        } catch (Exception ignored) {}
        try {
            if (apiItemRegistry != null) apiItemRegistry.shutdown();
        } catch (Exception ignored) {}

        if (databaseManager != null) {
            try { databaseManager.shutdownAsync(); } catch (Exception ignored) {}
            try { databaseManager.disconnect(); } catch (Exception ignored) {}
        }

        if (pluginLogManager != null) {
            try { pluginLogManager.stop(); } catch (Exception ignored) {}
            pluginLogManager = null;
        }

        if (cooldownStore != null) {
            try { cooldownStore.save(); } catch (Exception ignored) {}
        }

        plugin.getLogger().info("Artifex disabled!");
    }

    // ===== Internal init methods =====

    private void initManagers() {
        configManager = new ConfigManager(plugin);
        cooldownStore = new me.dcplugin.artifex.utils.CooldownStore(plugin.getDataFolder());
        messageManager = new MessageManager(plugin);
        effectManager = new EffectManager(plugin);
        attributeManager = new AttributeManager(plugin);
        itemHandler = new CustomItemHandler(plugin);
        armorSetManager = new ArmorSetManager(plugin);
    }

    private void registerBukkitCommand() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            var cmd = plugin.getCommand("artifex");
            if (cmd == null) {
                cmd = plugin.getCommand("customitems");
            }
            if (cmd == null) {
                plugin.getLogger().severe("[FATAL] Command 'artifex' not found in plugin.yml!");
                return;
            }
            CustomItemsCommand command = new CustomItemsCommand(plugin);
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);

            var saveCmd = plugin.getCommand("saveitem");
            if (saveCmd != null) {
                saveCmd.setExecutor(command);
                saveCmd.setTabCompleter(command);
            }
            plugin.getLogger().info("[Command] /artifex commands registered successfully");
        }, 1L);
    }

    private void registerEvents() {
        playerListener = new PlayerListener(plugin);
        triggerListener = new TriggerListener(plugin);

        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(playerListener, plugin);
        pm.registerEvents(new me.dcplugin.artifex.listeners.BlockPlaceListener(plugin), plugin);
        pm.registerEvents(triggerListener, plugin);
        pm.registerEvents(new ApiEventListener(plugin, apiItemRegistry), plugin);
        pm.registerEvents(new me.dcplugin.artifex.listeners.CraftItemListener(plugin), plugin);
    }

    private void initDatabase() {
        databaseManager = new DatabaseManager(plugin);
        databaseManager.connect();
    }

    private void initPlaceholders() {
        placeholderManager = new PlaceholderManager(plugin);
        placeholderManager.registerExpansion(plugin);
    }

    /**
     * Plugin logging into plugins/Artifex/logs/latest.log.
     *
     * Disabled by default (file-logging: false) — otherwise the plugin
     * messages would disappear from the server console.
     */
    private void initFileLogging() {
        if (!plugin.getConfig().getBoolean("file-logging", false)) return;
        pluginLogManager = new PluginLogManager(plugin);
        pluginLogManager.start();
    }

    private void checkUpdates() {
        updateChecker = new UpdateChecker(plugin);
        updateChecker.checkForUpdates(message -> {
            String cleanMessage = message.replaceAll("\u00a7[0-9a-fk-or]", "");
            if (cleanMessage.contains("latest version")) {
                plugin.getLogger().info(cleanMessage);
            } else if (cleanMessage.contains("new version")) {
                plugin.getLogger().warning(cleanMessage);
            }
        });
    }

    private void logStartupInfo() {
        plugin.getLogger().info("CustomItems enabled!");
        plugin.getLogger().info("[API] Database connected");
        plugin.getLogger().info("[API] PlaceholderManager initialized");
        plugin.getLogger().info("[API] Custom commands registered: " + apiItemRegistry.getCommandCount());
        plugin.getLogger().info("Java API: " + apiItemRegistry.getCount() + " items, " +
                apiItemRegistry.getCommandCount() + " commands, " +
                apiItemRegistry.getPlaceholderCount() + " placeholders");
    }

    // ===== Getters =====

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public AttributeManager getAttributeManager() { return attributeManager; }
    public CustomItemHandler getItemHandler() { return itemHandler; }
    public ArmorSetManager getArmorSetManager() { return armorSetManager; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public TriggerListener getTriggerListener() { return triggerListener; }
    public ItemRegistry getApiItemRegistry() { return apiItemRegistry; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlaceholderManager getPlaceholderManager() { return placeholderManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public EquippedItemsChecker getEquippedItemsChecker() { return equippedItemsChecker; }
    public CommandRegistrar getCommandRegistrar() { return commandRegistrar; }
    public ConfigMigrator getConfigMigrator() { return configMigrator; }
    public me.dcplugin.artifex.utils.CooldownStore getCooldownStore() { return cooldownStore; }
}
