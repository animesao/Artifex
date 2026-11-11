import me.dcplugin.artifex.api.config.MessagesConfig;

/**
 * 🔧 CONFIGURATION OF ALL PLUGIN MESSAGES (SAMPLE)
 *
 * The plugin creates plugins/Artifex/items/messages.java on the first
 * start - this file is the documented version of it.
 * To use it, copy it over items/messages.java (or just edit the file the
 * plugin created) and run /ci reload.
 *
 * This is the ONLY place where the texts the player sees are defined.
 * Everything below can be changed freely.
 *
 * Format:
 * - &0-9, &a-f - colors
 * - &l - bold
 * - &n - underlined
 * - &o - italic
 * - &r - reset
 *
 * Placeholders:
 * - {item}     - item id
 * - {player}   - player name
 * - {error}    - error text
 * - {count}    - a number
 * - {version}  - plugin version
 * - {label}    - command label
 * - {usage}    - command usage
 * - {file}     - file name
 * - {url}      - link
 */
public class EXAMPLEmessages {

    public static void load() {
        // ══════════════════════════════════════════════════════════════
        // PREFIX
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.PREFIX = "&8[&6DCI&8] &r";

        // ══════════════════════════════════════════════════════════════
        // GENERAL
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.NO_PERMISSION = MessagesConfig.PREFIX + "&cNo permission!";
        MessagesConfig.PLAYER_NOT_FOUND = MessagesConfig.PREFIX + "&cPlayer not found!";
        MessagesConfig.ITEM_NOT_FOUND = MessagesConfig.PREFIX + "&cItem '{item}' not found!";
        MessagesConfig.UNKNOWN_COMMAND = MessagesConfig.PREFIX + "&cUnknown command!";
        MessagesConfig.ONLY_FOR_PLAYERS = MessagesConfig.PREFIX + "&cPlayers only!";

        // ══════════════════════════════════════════════════════════════
        // ITEMS
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.ITEM_USES_DEPLETED = "&cThe item was used up and is gone!";
        MessagesConfig.ITEM_EXPIRED = "&cItem {item} has expired!";
        MessagesConfig.ITEM_WORLD_BLOCKED = "&cItem {item} is banned in this world!";
        MessagesConfig.ITEM_NO_PERMISSION = "&cYou don't have permission to use this item.";

        // ══════════════════════════════════════════════════════════════
        // CUSTOM COMMANDS (items/*.java, extends CustomCommand)
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.COMMAND_NOT_FOUND = "&cCommand not found: {label}";
        MessagesConfig.COMMAND_ERROR = "&cError executing command: {error}";
        MessagesConfig.COMMAND_NO_PERMISSION = "&cYou don't have permission!";
        MessagesConfig.COMMAND_PLAYER_NOT_FOUND = "&cPlayer {player} not found!";
        MessagesConfig.COMMAND_SPECIFY_PLAYER = "&cSpecify a player: {usage}";

        // ══════════════════════════════════════════════════════════════
        // /ci COMMAND
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.CI_HEADER = "&6=== Artifex ===";
        MessagesConfig.CI_HELP_RELOAD = "&e/ci reload &7- Reload the plugin";
        MessagesConfig.CI_HELP_VALIDATE = "&7/ci validate &8- validate YAML recipes";
        MessagesConfig.CI_HELP_DEBUG = "&7/ci debug <id> &8- dump item + PDC";
        MessagesConfig.CI_HELP_BACKUP = "&7/ci backup &8- back up items/";

        MessagesConfig.RELOAD_SUCCESS = MessagesConfig.PREFIX + "&a&lReloaded!";
        MessagesConfig.RELOAD_YAML = MessagesConfig.PREFIX + "&7YAML items: &e{count}";
        MessagesConfig.RELOAD_JAVA_ITEMS = MessagesConfig.PREFIX + "&7Java items: &e{count}";
        MessagesConfig.RELOAD_JAVA_COMMANDS = MessagesConfig.PREFIX + "&7Java commands: &e{count}";
        MessagesConfig.RELOAD_JAVA_PLACEHOLDERS = MessagesConfig.PREFIX + "&7Java placeholders: &e{count}";
        MessagesConfig.RELOAD_ERROR = MessagesConfig.PREFIX + "&cError: {error}";

        MessagesConfig.CI_MODULES = "&7Modules: &e{count}";
        MessagesConfig.CI_VALIDATION_OK = "&aValidation OK: no problems found.";
        MessagesConfig.CI_VALIDATION_PROBLEMS = "&eValidation: problems found: {count}";
        MessagesConfig.CI_DEBUG_USAGE = "&cUsage: /ci debug <id>";
        MessagesConfig.CI_DEBUG_ITEM_NOT_FOUND = "&cItem not found: {item}";
        MessagesConfig.CI_BACKUP_CREATED = "&aBackup created: &e{file}";
        MessagesConfig.CI_BACKUP_FAILED = "&cBackup failed: {error}";

        // ══════════════════════════════════════════════════════════════
        // UPDATE CHECKER
        // ══════════════════════════════════════════════════════════════
        MessagesConfig.UPDATE_AVAILABLE = "&e[CustomItems] &fA new version is available: &b{version}";
        MessagesConfig.UPDATE_DOWNLOAD = "&e[CustomItems] &fDownload the update from the plugin page";
        MessagesConfig.UPDATE_NOTIFICATION_HEADER = "&8&m                                                    ";
        MessagesConfig.UPDATE_NOTIFICATION_TITLE = "&6&lArtifex &7- &aUpdate available!";
        MessagesConfig.UPDATE_NOTIFICATION_CURRENT = "&7Current version: &e{version}";
        MessagesConfig.UPDATE_NOTIFICATION_LATEST = "&7New version: &a{version}";
        MessagesConfig.UPDATE_NOTIFICATION_DOWNLOAD = "&7Download: &b{url}";

        // ══════════════════════════════════════════════════════════════
        // YOUR OWN TEXTS
        // ══════════════════════════════════════════════════════════════
        // Any path works. A module reads it with getMessage("my.text", "...").
        //   MessagesConfig.set("my-module.no-money", "&cNot enough money!");
        //   MessagesConfig.set("my-module.purchased", "&aBought {item} for {count} coins!");
    }
}
