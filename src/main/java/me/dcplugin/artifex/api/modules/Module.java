package me.dcplugin.artifex.api.modules;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

/**
 * Base class for a module (a plugin inside a plugin)
 * 
 * Module structure:
 * items/
 * └── shop/
 *     ├── config.yml
 *     ├── items.yml
 *     └── shop.java
 * 
 * In config.yml:
 * name: "Shop"
 * version: "1.0"
 * commands:
 *   - shop
 * permissions:
 *   - shop.admin
 */
public abstract class Module {

    protected final Main plugin;
    protected final String id;
    protected final File folder;
    protected YamlConfiguration config;
    protected YamlConfiguration itemsConfig;
    protected boolean enabled = false;
    
    protected final List<String> commands = new ArrayList<>();
    protected final List<String> permissions = new ArrayList<>();

    /**
     * Dynamically registered module commands (CommandMap).
     * They are removed on disable(), so that no "dead" commands are left in the
     * CommandMap when the module is removed or reloaded.
     */
    private final List<org.bukkit.command.Command> dynamicCommands = new ArrayList<>();

    public Module(Main plugin, String id, File folder) {
        this.plugin = plugin;
        this.id = id;
        this.folder = folder;
        
        // Load the configs
        File configFile = new File(folder, "config.yml");
        File itemsFile = new File(folder, "items.yml");
        
        this.config = configFile.exists() ? YamlConfiguration.loadConfiguration(configFile) : new YamlConfiguration();
        this.itemsConfig = itemsFile.exists() ? YamlConfiguration.loadConfiguration(itemsFile) : new YamlConfiguration();
        
        // Load the settings from the config
        loadConfig();
    }

    /**
     * Loads the settings from config.yml
     */
    protected void loadConfig() {
        commands.clear();
        permissions.clear();
        
        if (config.contains("commands")) {
            commands.addAll(config.getStringList("commands"));
        }
        if (config.contains("permissions")) {
            permissions.addAll(config.getStringList("permissions"));
        }
    }

    /**
     * Enables the module
     */
    public void enable() {
        if (enabled) return;
        enabled = true;
        onEnable();
        plugin.getLogger().info("[Module] Enabled: " + id);
    }

    /**
     * Disables the module
     */
    public void disable() {
        if (!enabled) return;
        enabled = false;
        try {
            onDisable();
        } finally {
            unregisterDynamicCommands();
        }
        plugin.getLogger().info("[Module] Disabled: " + id);
    }

    /**
     * Reloads the module
     */
    public void reload() {
        disable();
        loadConfig();
        enable();
    }

    /**
     * Opens a module menu. Menu modules may override this method.
     */
    public boolean openMenu(Player player, String menuId) {
        return false;
    }

    // ===== ABSTRACT METHODS =====

    /**
     * Called when the module is enabled
     */
    protected abstract void onEnable();

    /**
     * Called when the module is disabled
     */
    protected abstract void onDisable();

    // ===== DYNAMIC COMMAND REGISTRATION =====

    /**
     * Registers a command in the CommandMap (default permission: "dci.<name>").
     * The command is removed automatically when the module is disabled.
     */
    protected void registerDynamicCommand(String name, org.bukkit.command.CommandExecutor executor) {
        registerDynamicCommand(name, executor, "dci." + name.toLowerCase());
    }

    /**
     * Registers a command with an explicit permission.
     */
    protected void registerDynamicCommand(String name, org.bukkit.command.CommandExecutor executor, String permission) {
        try {
            org.bukkit.command.CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            org.bukkit.command.Command command = new org.bukkit.command.Command(name.toLowerCase()) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                    // Проверка права обязательна: SimpleCommandMap.dispatch()
                    // права не проверяет (это делает только PluginCommand),
                    // а мы регистрируем «сырую» команду.
                    if (!testPermission(sender)) return true;
                    return executor.onCommand(sender, this, label, args);
                }

                @Override
                public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                    if (!testPermission(sender)) return java.util.Collections.emptyList();
                    if (executor instanceof org.bukkit.command.TabCompleter) {
                        return ((org.bukkit.command.TabCompleter) executor).onTabComplete(sender, this, alias, args);
                    }
                    return java.util.Collections.emptyList();
                }
            };
            command.setPermission(permission);
            command.setDescription("Module " + id + ": " + name);
            command.setUsage("/" + name);

            String prefix = plugin != null ? plugin.getName().toLowerCase(java.util.Locale.ROOT) : "artifex";
            commandMap.register(prefix, command);
            dynamicCommands.add(command);
            me.dcplugin.artifex.utils.CommandMapAccessor.syncCommands();
            plugin.getLogger().info("[Module " + id + "] Registered command: /" + name);
        } catch (Exception e) {
            plugin.getLogger().warning("[Module " + id + "] Failed to register /" + name + ": " + e.getMessage());
        }
    }

    /**
     * Removes all dynamic module commands from the CommandMap.
     */
    private void unregisterDynamicCommands() {
        if (dynamicCommands.isEmpty()) return;
        try {
            org.bukkit.command.CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;

            Map<String, org.bukkit.command.Command> knownCommands =
                    me.dcplugin.artifex.utils.CommandMapAccessor.getKnownCommands(commandMap);
            if (knownCommands == null) {
                plugin.getLogger().warning("[Module " + id + "] knownCommands is unavailable - commands stay in the CommandMap");
                dynamicCommands.clear();
                return;
            }
            String prefix = plugin != null ? plugin.getName().toLowerCase(java.util.Locale.ROOT) : "artifex";
            for (org.bukkit.command.Command cmd : dynamicCommands) {
                cmd.unregister(commandMap);
                knownCommands.remove(cmd.getName().toLowerCase(java.util.Locale.ROOT));
                knownCommands.remove(prefix + ":" + cmd.getName().toLowerCase(java.util.Locale.ROOT));
                knownCommands.remove("dcustomitems:" + cmd.getName().toLowerCase(java.util.Locale.ROOT));
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias.toLowerCase(java.util.Locale.ROOT));
                    knownCommands.remove(prefix + ":" + alias.toLowerCase(java.util.Locale.ROOT));
                    knownCommands.remove("dcustomitems:" + alias.toLowerCase(java.util.Locale.ROOT));
                }
            }
            dynamicCommands.clear();
            me.dcplugin.artifex.utils.CommandMapAccessor.syncCommands();
        } catch (Exception e) {
            plugin.getLogger().warning("[Module " + id + "] Error unregistering commands: " + e.getMessage());
        }
    }

    private org.bukkit.command.CommandMap getCommandMap() {
        return me.dcplugin.artifex.utils.CommandMapAccessor.getCommandMap();
    }

    // ===== UTILITIES =====

    // ===== MESSAGES =====

    /**
     * Resolves a message from the central message system.
     *
     * <p>Every message a module shows can be overridden in
     * {@code items/messages.java} — under the path you pass here.
     *
     * <pre>{@code
     * // in the module
     * registerMessage("vault.no-money", "&cNot enough money!");
     * player.sendMessage(getMessage("vault.no-money", "&cNot enough money!"));
     * }</pre>
     *
     * @param path          message path (for example "vault.no-money")
     * @param defaultMessage fallback when the path is unknown
     */
    protected String getMessage(String path, String defaultMessage) {
        return plugin.getMessageManager().getMessage(path, defaultMessage);
    }

    /**
     * Registers a message of this module under an arbitrary path, so that it
     * can be overridden from {@code items/messages.java}.
     *
     * @param path  message path (for example "vault.no-money")
     * @param value default text
     */
    protected void registerMessage(String path, String value) {
        plugin.getMessageManager().registerMessage(path, value);
    }

    /**
     * Removes a message registered by {@link #registerMessage(String, String)}.
     */
    protected void unregisterMessage(String path) {
        plugin.getMessageManager().unregisterMessage(path);
    }

    /**
     * Gets a value from config.yml
     */
    protected String getConfig(String path, String def) {
        return config.getString(path, def);
    }

    /**
     * Gets an int from config.yml
     */
    protected int getConfigInt(String path, int def) {
        return config.getInt(path, def);
    }

    /**
     * Gets a boolean from config.yml
     */
    protected boolean getConfigBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    /**
     * Gets a list from config.yml
     */
    protected List<String> getConfigList(String path) {
        return config.getStringList(path);
    }

    /**
     * Gets an item from items.yml
     */
    protected Map<String, Object> getItem(String itemId) {
        Map<String, Object> item = new HashMap<>();
        String path = "items." + itemId;
        
        if (itemsConfig.contains(path)) {
            for (String key : itemsConfig.getConfigurationSection(path).getKeys(false)) {
                item.put(key, itemsConfig.get(path + "." + key));
            }
        }
        
        return item;
    }

    /**
     * Gets all items from items.yml
     */
    protected Set<String> getAllItemIds() {
        if (itemsConfig.contains("items")) {
            return itemsConfig.getConfigurationSection("items").getKeys(false);
        }
        return Collections.emptySet();
    }

    // ===== GETTERS =====

    public String getId() { return id; }
    public File getFolder() { return folder; }
    public YamlConfiguration getConfig() { return config; }
    public YamlConfiguration getItemsConfig() { return itemsConfig; }
    public boolean isEnabled() { return enabled; }
    public List<String> getCommands() { return Collections.unmodifiableList(commands); }
    public List<String> getPermissions() { return Collections.unmodifiableList(permissions); }
}
