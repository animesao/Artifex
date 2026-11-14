package me.dcplugin.artifex.api.config;

import me.dcplugin.artifex.utils.ColorUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Message configuration of the plugin.
 *
 * <p>This is the single place where every player-facing message lives.
 * An administrator customises them in {@code items/messages.java}
 * (see {@code samples/EXAMPLE-messages.java}) and changes take effect after
 * {@code /ci reload}.
 *
 * <p>Three layers, in priority order:
 * <ol>
 *   <li><b>Custom messages</b> — registered at runtime by
 *       {@link #set(String, String)}: modules and third-party plugins can
 *       plug their own messages in under any path they like.</li>
 *   <li><b>Built-in messages</b> — the constants below, addressed by path
 *       (for example {@code "item.world-blocked"}).</li>
 *   <li><b>Fallback</b> — the default text passed by the caller.</li>
 * </ol>
 *
 * <p>Modules use {@code plugin.getMessageManager().getMessage(path, default)}
 * or the {@code getMessage(path, default)} helper inherited from
 * {@code Module}.
 */
public class MessagesConfig {

    // ══════════════════════════════════════════════════════════════
    // PREFIX
    // ══════════════════════════════════════════════════════════════
    public static String PREFIX = "&8[&6Artifex&8] &r";

    // ══════════════════════════════════════════════════════════════
    // COMMON MESSAGES
    // ══════════════════════════════════════════════════════════════
    public static String NO_PERMISSION = PREFIX + "&cYou don't have permission!";
    public static String PLAYER_NOT_FOUND = PREFIX + "&cPlayer not found!";
    public static String ITEM_NOT_FOUND = PREFIX + "&cItem '{item}' not found!";
    public static String UNKNOWN_COMMAND = PREFIX + "&cUnknown command!";
    public static String ONLY_FOR_PLAYERS = PREFIX + "&cPlayers only!";

    // ══════════════════════════════════════════════════════════════
    // /artifex reload COMMAND
    // ══════════════════════════════════════════════════════════════
    public static String CI_HEADER = "&6=== Artifex ===";
    public static String CI_HELP_RELOAD = "&e/artifex reload &7- Reload the plugin";
    public static String RELOAD_SUCCESS = PREFIX + "&a&lPlugin reloaded!";
    public static String RELOAD_YAML = PREFIX + "&7YAML items: &e{count}";
    public static String RELOAD_JAVA_ITEMS = PREFIX + "&7Java items: &e{count}";
    public static String RELOAD_JAVA_COMMANDS = PREFIX + "&7Java commands: &e{count}";
    public static String RELOAD_JAVA_PLACEHOLDERS = PREFIX + "&7Java placeholders: &e{count}";
    public static String RELOAD_ERROR = PREFIX + "&cError: {error}";

    // ══════════════════════════════════════════════════════════════
    // ITEMS
    // ══════════════════════════════════════════════════════════════
    /** Item ran out of uses. Placeholders: {item} */
    public static String ITEM_USES_DEPLETED = "&cThe item was used up and is gone!";
    /** Item outlived its duration. Placeholders: {item} */
    public static String ITEM_EXPIRED = "&cItem {item} has expired!";
    /** Item is not allowed in the current world. Placeholders: {item} */
    public static String ITEM_WORLD_BLOCKED = "&cItem {item} is banned in this world!";
    /** No permission to use the item. */
    public static String ITEM_NO_PERMISSION = "&cYou don't have permission to use this item.";

    // ══════════════════════════════════════════════════════════════
    // COMMANDS (custom commands registered from items/*.java)
    // ══════════════════════════════════════════════════════════════
    /** Unknown command. Placeholders: {label} */
    public static String COMMAND_NOT_FOUND = "&cCommand not found: {label}";
    /** Command threw an exception. Placeholders: {error} */
    public static String COMMAND_ERROR = "&cError executing command: {error}";
    /** Permission missing. */
    public static String COMMAND_NO_PERMISSION = "&cYou don't have permission!";
    /** Target player does not exist. Placeholders: {player} */
    public static String COMMAND_PLAYER_NOT_FOUND = "&cPlayer {player} not found!";
    /** A player argument is required. Placeholders: {usage} */
    public static String COMMAND_SPECIFY_PLAYER = "&cSpecify a player: {usage}";

    // ══════════════════════════════════════════════════════════════
    // /artifex COMMAND OUTPUT
    // ══════════════════════════════════════════════════════════════
    public static String CI_HELP_VALIDATE = "&7/artifex validate &8- validate YAML recipes";
    public static String CI_HELP_DEBUG = "&7/artifex debug <id> &8- dump item + PDC";
    public static String CI_HELP_BACKUP = "&7/artifex backup &8- back up items/";
    public static String CI_MODULES = "&7Modules: &e{count}";
    public static String CI_VALIDATION_OK = "&aValidation OK: no problems found.";
    public static String CI_VALIDATION_PROBLEMS = "&eValidation: problems found: {count}";
    public static String CI_DEBUG_USAGE = "&cUsage: /artifex debug <id>";
    public static String CI_DEBUG_ITEM_NOT_FOUND = "&cItem not found: {item}";
    public static String CI_BACKUP_CREATED = "&aBackup created: &e{file}";
    public static String CI_BACKUP_FAILED = "&cBackup failed: {error}";

    // ══════════════════════════════════════════════════════════════
    // UPDATE CHECKER
    // ══════════════════════════════════════════════════════════════
    public static String UPDATE_AVAILABLE = "&e[Artifex] &fA new version is available: &b{version}";
    public static String UPDATE_DOWNLOAD = "&e[Artifex] &fDownload the update from the plugin page";
    public static String UPDATE_NOTIFICATION_HEADER = "&8&m                                                    ";
    public static String UPDATE_NOTIFICATION_TITLE = "&6&lArtifex &7- &aUpdate available!";
    public static String UPDATE_NOTIFICATION_CURRENT = "&7Current version: &e{version}";
    public static String UPDATE_NOTIFICATION_LATEST = "&7New version: &a{version}";
    public static String UPDATE_NOTIFICATION_DOWNLOAD = "&7Download: &b{url}";

    // ══════════════════════════════════════════════════════════════
    // RUNTIME REGISTRY (modules, third-party plugins)
    // ══════════════════════════════════════════════════════════════

    /** Messages registered at runtime, keyed by path. */
    private static final Map<String, String> CUSTOM = new ConcurrentHashMap<>();

    /**
     * Registers (or overrides) a message under an arbitrary path.
     *
     * <p>Modules can use this to plug their own texts into the central
     * message system, e.g. {@code MessagesConfig.set("vault.no-money",
     * "&cNot enough money!")}. An administrator can then override it from
     * {@code items/messages.java} the same way.
     */
    public static void set(String path, String value) {
        if (path == null || path.isEmpty()) return;
        if (value == null) {
            CUSTOM.remove(path);
            return;
        }
        CUSTOM.put(path, value);
    }

    /**
     * Returns a message registered at runtime (admin override), or null.
     */
    public static String getCustom(String path) {
        return path == null ? null : CUSTOM.get(path);
    }

    /**
     * Returns a built-in message by path, or null.
     */
    public static String getBuiltIn(String path) {
        if (path == null) return null;
        Supplier<String> builtIn = BUILT_IN.get(path);
        return builtIn != null ? builtIn.get() : null;
    }

    /**
     * Returns a message by path: runtime override first, built-in second.
     */
    public static String get(String path, String defaultValue) {
        String custom = getCustom(path);
        if (custom != null) return custom;
        String builtIn = getBuiltIn(path);
        return builtIn != null ? builtIn : defaultValue;
    }

    /**
     * All messages registered at runtime (read-only).
     */
    public static Map<String, String> getCustom() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(CUSTOM));
    }

    /**
     * Removes all runtime-registered messages.
     */
    public static void clearCustom() {
        CUSTOM.clear();
    }

    /**
     * Built-in messages addressed by path.
     *
     * <p>The values are suppliers so that overwriting a constant from
     * {@code items/messages.java} takes effect immediately.
     */
    private static final Map<String, Supplier<String>> BUILT_IN = new LinkedHashMap<>();

    static {
        BUILT_IN.put("plugin.no-permission", () -> NO_PERMISSION);
        BUILT_IN.put("plugin.player-not-found", () -> PLAYER_NOT_FOUND);
        BUILT_IN.put("plugin.item-not-found", () -> ITEM_NOT_FOUND);
        BUILT_IN.put("plugin.unknown-command", () -> UNKNOWN_COMMAND);
        BUILT_IN.put("plugin.only-for-players", () -> ONLY_FOR_PLAYERS);
        BUILT_IN.put("plugin.update-available", () -> UPDATE_AVAILABLE);
        BUILT_IN.put("plugin.update-download", () -> UPDATE_DOWNLOAD);

        BUILT_IN.put("items.uses-depleted", () -> ITEM_USES_DEPLETED);
        BUILT_IN.put("items.expired", () -> ITEM_EXPIRED);
        BUILT_IN.put("items.world-blocked", () -> ITEM_WORLD_BLOCKED);
        BUILT_IN.put("items.no-permission", () -> ITEM_NO_PERMISSION);

        BUILT_IN.put("commands.not-found", () -> COMMAND_NOT_FOUND);
        BUILT_IN.put("commands.error", () -> COMMAND_ERROR);
        BUILT_IN.put("commands.no-permission", () -> COMMAND_NO_PERMISSION);
        BUILT_IN.put("commands.player-not-found", () -> COMMAND_PLAYER_NOT_FOUND);
        BUILT_IN.put("commands.specify-player", () -> COMMAND_SPECIFY_PLAYER);

        BUILT_IN.put("ci.header", () -> CI_HEADER);
        BUILT_IN.put("ci.help.reload", () -> CI_HELP_RELOAD);
        BUILT_IN.put("ci.help.validate", () -> CI_HELP_VALIDATE);
        BUILT_IN.put("ci.help.debug", () -> CI_HELP_DEBUG);
        BUILT_IN.put("ci.help.backup", () -> CI_HELP_BACKUP);
        BUILT_IN.put("ci.reload.success", () -> RELOAD_SUCCESS);
        BUILT_IN.put("ci.reload.yaml", () -> RELOAD_YAML);
        BUILT_IN.put("ci.reload.java-items", () -> RELOAD_JAVA_ITEMS);
        BUILT_IN.put("ci.reload.java-commands", () -> RELOAD_JAVA_COMMANDS);
        BUILT_IN.put("ci.reload.java-placeholders", () -> RELOAD_JAVA_PLACEHOLDERS);
        BUILT_IN.put("ci.reload.error", () -> RELOAD_ERROR);
        BUILT_IN.put("ci.modules", () -> CI_MODULES);
        BUILT_IN.put("ci.validation.ok", () -> CI_VALIDATION_OK);
        BUILT_IN.put("ci.validation.problems", () -> CI_VALIDATION_PROBLEMS);
        BUILT_IN.put("ci.debug.usage", () -> CI_DEBUG_USAGE);
        BUILT_IN.put("ci.debug.item-not-found", () -> CI_DEBUG_ITEM_NOT_FOUND);
        BUILT_IN.put("ci.backup.created", () -> CI_BACKUP_CREATED);
        BUILT_IN.put("ci.backup.failed", () -> CI_BACKUP_FAILED);

        BUILT_IN.put("artifex.header", () -> CI_HEADER);
        BUILT_IN.put("artifex.help.reload", () -> CI_HELP_RELOAD);
        BUILT_IN.put("artifex.help.validate", () -> CI_HELP_VALIDATE);
        BUILT_IN.put("artifex.help.debug", () -> CI_HELP_DEBUG);
        BUILT_IN.put("artifex.help.backup", () -> CI_HELP_BACKUP);
        BUILT_IN.put("artifex.reload.success", () -> RELOAD_SUCCESS);
        BUILT_IN.put("artifex.reload.yaml", () -> RELOAD_YAML);
        BUILT_IN.put("artifex.reload.java-items", () -> RELOAD_JAVA_ITEMS);
        BUILT_IN.put("artifex.reload.java-commands", () -> RELOAD_JAVA_COMMANDS);
        BUILT_IN.put("artifex.reload.java-placeholders", () -> RELOAD_JAVA_PLACEHOLDERS);
        BUILT_IN.put("artifex.reload.error", () -> RELOAD_ERROR);
        BUILT_IN.put("artifex.modules", () -> CI_MODULES);
        BUILT_IN.put("artifex.validation.ok", () -> CI_VALIDATION_OK);
        BUILT_IN.put("artifex.validation.problems", () -> CI_VALIDATION_PROBLEMS);
        BUILT_IN.put("artifex.debug.usage", () -> CI_DEBUG_USAGE);
        BUILT_IN.put("artifex.debug.item-not-found", () -> CI_DEBUG_ITEM_NOT_FOUND);
        BUILT_IN.put("artifex.backup.created", () -> CI_BACKUP_CREATED);
        BUILT_IN.put("artifex.backup.failed", () -> CI_BACKUP_FAILED);

        BUILT_IN.put("update.header", () -> UPDATE_NOTIFICATION_HEADER);
        BUILT_IN.put("update.title", () -> UPDATE_NOTIFICATION_TITLE);
        BUILT_IN.put("update.current-version", () -> UPDATE_NOTIFICATION_CURRENT);
        BUILT_IN.put("update.new-version", () -> UPDATE_NOTIFICATION_LATEST);
        BUILT_IN.put("update.download", () -> UPDATE_NOTIFICATION_DOWNLOAD);
    }

    // ══════════════════════════════════════════════════════════════
    // FORMATTING
    // ══════════════════════════════════════════════════════════════

    /**
     * Formats a message with placeholders.
     *
     * Example:
     *   format("Hello, {player}!", "{player}", "Steve")
     *   → "Hello, Steve!"
     */
    public static String format(String message, String... replacements) {
        if (message == null) return "";
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtils.colorize(message);
    }

    /**
     * Replaces colour codes (& -> §, &#RRGGBB, <#RRGGBB>, MiniMessage tags).
     */
    public static String colorize(String message) {
        return message == null ? "" : ColorUtils.colorize(message);
    }
}
