package me.dcplugin.dcustomitems.commands;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.config.MessagesConfig;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The only built-in plugin command: /ci reload
 *
 * All other commands (give, buy, sell, etc.)
 * are registered by modules through items/<module>/
 */
public class CustomItemsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public CustomItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            return handleReloadCommand(sender);
        }
        if (args[0].equalsIgnoreCase("validate")) return handleValidate(sender);
        if (args[0].equalsIgnoreCase("debug")) return handleDebug(sender, args);
        if (args[0].equalsIgnoreCase("backup")) return handleBackup(sender);

        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HEADER));
        sender.sendMessage(MessagesConfig.colorize(MessagesConfig.CI_HELP_RELOAD));
        sender.sendMessage(msg("ci.help.validate", "&7/ci validate &8- validate YAML recipes"));
        sender.sendMessage(msg("ci.help.debug", "&7/ci debug <id> &8- dump item + PDC"));
        sender.sendMessage(msg("ci.help.backup", "&7/ci backup &8- back up items/"));
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("dcustomitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }

        try {
            if (plugin.getConfig().getBoolean("backup.on-reload", true)) {
                try {
                    me.dcplugin.dcustomitems.utils.BackupManager.backup(
                        plugin.getDataFolder(), plugin.getConfig().getInt("backup.keep", 5));
                } catch (Exception e) {
                    plugin.getLogger().warning("[Backup] not created before reload: " + e.getMessage());
                }
            }
            // Reload the config
            plugin.getConfigManager().reloadConfig();

            // Reload the YAML items
            plugin.getItemHandler().reloadItems();

            // The item definitions changed — the equipment cache is stale
            if (plugin.getEquippedItemsChecker() != null) {
                plugin.getEquippedItemsChecker().invalidateAll();
            }

            // Reload the set bonuses
            plugin.getArmorSetManager().reload();

            // Reload the Java API (items, commands, placeholders)
            if (plugin.getApiItemRegistry() != null) {
                plugin.getApiItemRegistry().reload();

                int items = plugin.getApiItemRegistry().getItemCount();
                int cmds = plugin.getApiItemRegistry().getCommandCount();
                int phs = plugin.getApiItemRegistry().getPlaceholderCount();

                // The commands are re-registered inside registry.reload()
                // (reloadSynchronously calls registerCustomCommands) —
                // registering them again here would create duplicates
                // in the CommandMap.

                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_YAML,
                    "{count}", String.valueOf(plugin.getItemHandler().getAllCustomItems().size())));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_ITEMS,
                    "{count}", String.valueOf(items)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_COMMANDS,
                    "{count}", String.valueOf(cmds)));
                sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_JAVA_PLACEHOLDERS,
                    "{count}", String.valueOf(phs)));
            } else {
                sender.sendMessage(MessagesConfig.colorize(MessagesConfig.RELOAD_SUCCESS));
            }

            // Reload the modules (if API registry didn't already reload them)
            if (plugin.getModuleManager() != null) {
                if (plugin.getApiItemRegistry() == null) {
                    plugin.getModuleManager().reloadAll();
                }
                sender.sendMessage(msg("ci.modules", "&7Modules: &e{count}")
                    .replace("{count}", String.valueOf(plugin.getModuleManager().getModuleCount())));
            }

            // EnumCache statistics (for debugging)
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                Map<String, Integer> cacheStats = EnumCache.getCacheStats();
                sender.sendMessage(MessagesConfig.colorize("&8[&6DCI&8] &7EnumCache:"));
                for (Map.Entry<String, Integer> entry : cacheStats.entrySet()) {
                    sender.sendMessage(MessagesConfig.colorize(
                        "&8  &7" + entry.getKey() + ": &e" + entry.getValue()));
                }
            }

        } catch (Exception e) {
            sender.sendMessage(MessagesConfig.format(MessagesConfig.RELOAD_ERROR,
                "{error}", e.getMessage()));
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "Reload error: " + e.getMessage(), e);
        }

        return true;
    }

    /**
     * Resolves a message from the central message system
     * (overridable in items/messages.java).
     */
    private String msg(String path, String defaultMessage) {
        return plugin.getMessageManager().getMessage(path, defaultMessage);
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : new String[]{"reload", "validate", "debug", "backup"}) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            String prefix = args[1].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String id : plugin.getItemHandler().getAllCustomItems().keySet()) {
                if (id.toLowerCase().startsWith(prefix)) out.add(id);
            }
            return out;
        }
        return new ArrayList<>();
    }

    private boolean handleValidate(CommandSender sender) {
        if (!sender.hasPermission("dcustomitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }
        int problems = 0;
        java.util.Set<String> mats = new java.util.HashSet<>();
        for (org.bukkit.Material m : org.bukkit.Material.values()) mats.add(m.name());
        java.util.Set<String> ids = new java.util.HashSet<>(plugin.getItemHandler().getAllCustomItems().keySet());
        if (plugin.getApiItemRegistry() != null) {
            ids.addAll(plugin.getApiItemRegistry().getAllIds());
        }
        for (var entry : plugin.getItemHandler().getAllCustomItems().entrySet()) {
            var item = entry.getValue();
            if (item == null || !item.hasRecipes()) continue;
            Map<String, Object> recipes = item.getRecipes();
            Object shaped = recipes.get("shaped");
            if (shaped instanceof List) {
                int i = 0;
                for (Object o : (List<?>) shaped) {
                    if (!(o instanceof Map)) { i++; continue; }
                    Map<?, ?> m = (Map<?, ?>) o;
                    List<String> pattern = new ArrayList<>();
                    Object p = m.get("pattern");
                    if (p instanceof List) for (Object row : (List<?>) p) pattern.add(String.valueOf(row));
                    Object keys = m.get("keys");
                    Map<?, ?> keysMap = keys instanceof Map ? (Map<?, ?>) keys : Map.of();
                    var issues = me.dcplugin.dcustomitems.utils.ItemValidator.validateShaped(item.getId() + "#" + i, pattern, keysMap, mats, ids);
                    for (var issue : issues) { sender.sendMessage(MessagesConfig.colorize("&c" + issue)); problems++; }
                    i++;
                }
            }
            Object shapeless = recipes.get("shapeless");
            if (shapeless instanceof List) {
                for (Object o : (List<?>) shapeless) {
                    if (!(o instanceof Map)) continue;
                    Object ing = ((Map<?, ?>) o).get("ingredients");
                    List<?> list = ing instanceof List ? (List<?>) ing : List.of();
                    for (var issue : me.dcplugin.dcustomitems.utils.ItemValidator.validateShapeless(item.getId(), list, mats, ids)) {
                        sender.sendMessage(MessagesConfig.colorize("&c" + issue)); problems++;
                    }
                }
            }
        }
        sender.sendMessage(problems == 0
                ? msg("ci.validation.ok", "&aValidation OK: no problems found.")
                : msg("ci.validation.problems", "&eValidation: problems found: {count}")
                        .replace("{count}", String.valueOf(problems)));
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dcustomitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("ci.debug.usage", "&cUsage: /ci debug <id>"));
            return false;
        }
        String id = args[1].toLowerCase();
        var item = plugin.getItemHandler().getCustomItem(id);
        if (item == null) {
            sender.sendMessage(msg("ci.debug.item-not-found", "&cItem not found: {item}")
                .replace("{item}", args[1]));
            return false;
        }
        sender.sendMessage(MessagesConfig.colorize("&6" + item.getId() + " &8type=" + item.getType() + " slot=" + item.getActivationSlot()));
        sender.sendMessage(MessagesConfig.colorize("&7material=" + item.getItemStack().getType() + " uses=" + item.getMaxUses() + " cd=" + item.getClickCooldown()));
        sender.sendMessage(MessagesConfig.colorize("&7recipes=" + (item.hasRecipes() ? item.getRecipes().keySet().toString() : "none")));
        var meta = item.getItemStack().getItemMeta();
        if (meta != null) {
            var c = meta.getPersistentDataContainer();
            sender.sendMessage(MessagesConfig.colorize("&7pdc-keys=" + c.getKeys().size()));
        }
        return true;
    }

    private boolean handleBackup(CommandSender sender) {
        if (!sender.hasPermission("dcustomitems.reload")) {
            sender.sendMessage(MessagesConfig.colorize(MessagesConfig.NO_PERMISSION));
            return false;
        }
        try {
            int keep = plugin.getConfig().getInt("backup.keep", 5);
            var dest = me.dcplugin.dcustomitems.utils.BackupManager.backup(plugin.getDataFolder(), keep);
            sender.sendMessage(msg("ci.backup.created", "&aBackup created: &e{file}")
                .replace("{file}", String.valueOf(dest.getFileName())));
        } catch (Exception e) {
            sender.sendMessage(msg("ci.backup.failed", "&cBackup failed: {error}")
                .replace("{error}", String.valueOf(e.getMessage())));
        }
        return true;
    }
}
