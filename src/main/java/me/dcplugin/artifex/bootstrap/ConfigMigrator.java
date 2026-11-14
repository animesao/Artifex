package me.dcplugin.artifex.bootstrap;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.config.MessagesConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Configuration migration and creation of default files.
 * Extracted from Main to separate responsibilities.
 */
public class ConfigMigrator {

    private final Main plugin;

    public ConfigMigrator(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Create items/messages.java if it does not exist
     */
    public void createDefaultMessagesFile() {
        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();

        File messagesFile = new File(itemsDir, "messages.java");
        if (messagesFile.exists()) return;

        try {
            // Explicit UTF-8: FileWriter used the system encoding,
            // and on Windows with cp1251 the Russian texts in messages.java
            // turned into garbled characters.
            java.nio.file.Files.write(messagesFile.toPath(),
                    generateDefaultMessagesContent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            plugin.getLogger().info("[Config] Created default messages.java");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create messages.java: " + e.getMessage(), e);
        }
    }

    /**
     * Check for items in config.yml and suggest a migration
     */
    public void checkConfigMigration() {
        Set<String> configKeys = plugin.getConfig().getKeys(false);
        Set<String> settingKeys = Set.of(
                "debug-mode", "database", "set-bonuses",
                "update-available", "latest-version",
                "disable-worlds", "whitelist-blocks",
                "java-compilation", "backup", "extract-default-modules",
                "file-logging", "crafting", "items", "cooldowns", "security"
        );
        List<String> itemKeys = new ArrayList<>();
        for (String key : configKeys) {
            if (!settingKeys.contains(key)) {
                itemKeys.add(key);
            }
        }
        if (!itemKeys.isEmpty()) {
            plugin.getLogger().info("=========================================");
            plugin.getLogger().info("[Migration] Found " + itemKeys.size() + " item(s) in config.yml:");
            for (String key : itemKeys) {
                plugin.getLogger().info("  - " + key);
            }
            plugin.getLogger().info("");
            plugin.getLogger().info("[Migration] Items are now loaded from items/ folder.");
            plugin.getLogger().info("[Migration] To migrate, move each item to its own YAML file:");
            plugin.getLogger().info("  1. Create plugins/Artifex/items/<item-name>.yml");
            plugin.getLogger().info("  2. Copy the item definition from config.yml");
            plugin.getLogger().info("  3. Remove the item from config.yml");
            plugin.getLogger().info("[Migration] Items in config.yml will still work for now.");
            plugin.getLogger().info("=========================================");
        }
    }

    /**
     * The default content of the messages.java file
     */
    private String generateDefaultMessagesContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("import me.dcplugin.artifex.api.config.MessagesConfig;\n");
        sb.append("\n");
        sb.append("/**\n");
        sb.append(" * Artifex - Plugin messages\n");
        sb.append(" *\n");
        sb.append(" * This is the only file the plugin creates on install. Everything the\n");
        sb.append(" * player sees is defined here - change a value and run /artifex reload.\n");
        sb.append(" *\n");
        sb.append(" * Placeholders: {item}, {player}, {error}, {count}, {version}, {label},\n");
        sb.append(" *              {usage}, {file}, {url}\n");
        sb.append(" *\n");
        sb.append(" * Your own module can add texts here too:\n");
        sb.append(" *   MessagesConfig.set(\"my-module.some-text\", \"&aHello!\");\n");
        sb.append(" */\n");
        sb.append("public class messages {\n");
        sb.append("\n");
        sb.append("    public static void load() {\n");
        sb.append("        MessagesConfig.PREFIX = \"&8[&6Artifex&8] &r\";\n");
        sb.append("\n");
        sb.append("        // --- Common ---\n");
        sb.append("        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + \"&cNo permission!\";\n");
        sb.append("        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + \"&cPlayer not found!\";\n");
        sb.append("        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + \"&cItem '{item}' not found!\";\n");
        sb.append("        MessagesConfig.UNKNOWN_COMMAND = MessagesConfig.PREFIX + \"&cUnknown command!\";\n");
        sb.append("        MessagesConfig.ONLY_FOR_PLAYERS = MessagesConfig.PREFIX + \"&cPlayers only!\";\n");
        sb.append("\n");
        sb.append("        // --- Items ---\n");
        sb.append("        MessagesConfig.ITEM_USES_DEPLETED = \"&cThe item was used up and is gone!\";\n");
        sb.append("        MessagesConfig.ITEM_EXPIRED = \"&cItem {item} has expired!\";\n");
        sb.append("        MessagesConfig.ITEM_WORLD_BLOCKED = \"&cItem {item} is banned in this world!\";\n");
        sb.append("        MessagesConfig.ITEM_NO_PERMISSION = \"&cYou don't have permission to use this item.\";\n");
        sb.append("\n");
        sb.append("        // --- Custom commands ---\n");
        sb.append("        MessagesConfig.COMMAND_NOT_FOUND = \"&cCommand not found: {label}\";\n");
        sb.append("        MessagesConfig.COMMAND_ERROR = \"&cError executing command: {error}\";\n");
        sb.append("        MessagesConfig.COMMAND_NO_PERMISSION = \"&cYou don't have permission!\";\n");
        sb.append("        MessagesConfig.COMMAND_PLAYER_NOT_FOUND = \"&cPlayer {player} not found!\";\n");
        sb.append("        MessagesConfig.COMMAND_SPECIFY_PLAYER = \"&cSpecify a player: {usage}\";\n");
        sb.append("\n");
        sb.append("        // --- /artifex command ---\n");
        sb.append("        MessagesConfig.CI_HEADER = \"&#279FF5=== &#9CD6FFArtifex &#279FF5===\";\n");
        sb.append("        MessagesConfig.CI_HELP_RELOAD = \"&#9CD6FF/artifex reload &7- Перезагрузить плагин\";\n");
        sb.append("        MessagesConfig.CI_HELP_VALIDATE = \"&#9CD6FF/artifex validate &7- Проверить YAML рецепты\";\n");
        sb.append("        MessagesConfig.CI_HELP_DEBUG = \"&#9CD6FF/artifex debug <id> &7- Отладка предмета\";\n");
        sb.append("        MessagesConfig.CI_HELP_BACKUP = \"&#9CD6FF/artifex backup &7- Создать бэкап items/\";\n");
        sb.append("        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + \"&#9CD6FFПлагин успешно перезагружен!\";\n");
        sb.append("        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + \"&7Предметы YAML: &f{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + \"&7Предметы Java: &a{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + \"&7Команды Java: &f{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + \"&7Плейсхолдеры Java: &f{count}\";\n");
        sb.append("        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + \"&cОшибка: {error}\";\n");
        sb.append("        MessagesConfig.CI_MODULES = MessagesConfig.PREFIX + \"&7Модули: &f{count}\";\n");
        sb.append("        MessagesConfig.CI_VALIDATION_OK = \"&aValidation OK: no problems found.\";\n");
        sb.append("        MessagesConfig.CI_VALIDATION_PROBLEMS = \"&eValidation: problems found: {count}\";\n");
        sb.append("        MessagesConfig.CI_DEBUG_USAGE = \"&cUsage: /artifex debug <id>\";\n");
        sb.append("        MessagesConfig.CI_DEBUG_ITEM_NOT_FOUND = \"&cItem not found: {item}\";\n");
        sb.append("        MessagesConfig.CI_BACKUP_CREATED = \"&aBackup created: &e{file}\";\n");
        sb.append("        MessagesConfig.CI_BACKUP_FAILED = \"&cBackup failed: {error}\";\n");
        sb.append("\n");
        sb.append("        // --- Update checker ---\n");
        sb.append("        MessagesConfig.UPDATE_AVAILABLE = \"&e[Artifex] &fA new version is available: &b{version}\";\n");
        sb.append("        MessagesConfig.UPDATE_DOWNLOAD = \"&e[Artifex] &fDownload the update from the plugin page\";\n");
        sb.append("        MessagesConfig.UPDATE_NOTIFICATION_HEADER = \"&8&m                                                    \";\n");
        sb.append("        MessagesConfig.UPDATE_NOTIFICATION_TITLE = \"&6&lArtifex &7- &aUpdate available!\";\n");
        sb.append("        MessagesConfig.UPDATE_NOTIFICATION_CURRENT = \"&7Current version: &e{version}\";\n");
        sb.append("        MessagesConfig.UPDATE_NOTIFICATION_LATEST = \"&7New version: &a{version}\";\n");
        sb.append("        MessagesConfig.UPDATE_NOTIFICATION_DOWNLOAD = \"&7Download: &b{url}\";\n");
        sb.append("\n");
        sb.append("        // --- Your own texts (modules, plugins) ---\n");
        sb.append("        // MessagesConfig.set(\"my-module.some-text\", \"&aHello!\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
