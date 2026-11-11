package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.config.MessagesConfig;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of the plugin messages.
 *
 * <p>Every player-facing text of the plugin goes through this manager, so
 * an administrator can override all of them in {@code items/messages.java}.
 *
 * <p>Modules and third-party plugins can plug their own texts into the same
 * system:
 * <pre>{@code
 * plugin.getMessageManager().registerMessage("vault.no-money", "&cNot enough money!");
 * String text = plugin.getMessageManager().getMessage("vault.no-money", "&cNot enough money!");
 * }</pre>
 */
public class MessageManager {

    private final Main plugin;

    /** Messages registered at runtime (modules, third-party plugins). */
    private final Map<String, String> registeredMessages = new ConcurrentHashMap<>();

    public MessageManager(Main plugin) {
        this.plugin = plugin;
    }

    public void reloadMessages() {
        // Messages are managed via MessagesConfig and items/messages.java
    }

    /**
     * Message by path.
     *
     * <p>Priority: admin override (items/messages.java) → module registration
     * → built-in message → the fallback passed by the caller.
     *
     * @param path          message path (for example "items.world-blocked")
     * @param defaultMessage fallback used when the path is unknown
     */
    public String getMessage(String path, String defaultMessage) {
        String admin = MessagesConfig.getCustom(path);
        if (admin != null) return ColorUtils.colorize(admin);

        String registered = registeredMessages.get(path);
        if (registered != null) return ColorUtils.colorize(registered);

        String builtIn = MessagesConfig.getBuiltIn(path);
        if (builtIn != null) return ColorUtils.colorize(builtIn);

        return ColorUtils.colorize(defaultMessage);
    }

    /**
     * Message by path with PlaceholderAPI substitutions for a player.
     */
    public String getMessage(String path, String defaultMessage, org.bukkit.entity.Player player) {
        String admin = MessagesConfig.getCustom(path);
        if (admin != null) return ColorUtils.processMessage(player, admin);

        String registered = registeredMessages.get(path);
        if (registered != null) return ColorUtils.processMessage(player, registered);

        String builtIn = MessagesConfig.getBuiltIn(path);
        if (builtIn != null) return ColorUtils.processMessage(player, builtIn);

        return ColorUtils.processMessage(player, defaultMessage);
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    /**
     * Registers a message under an arbitrary path.
     *
     * <p>Intended for modules and third-party plugins that want their texts
     * to be overridable from {@code items/messages.java}.
     */
    public void registerMessage(String path, String value) {
        if (path == null || path.isEmpty()) return;
        if (value == null) {
            registeredMessages.remove(path);
            return;
        }
        registeredMessages.put(path, value);
    }

    /**
     * Removes a runtime-registered message.
     */
    public void unregisterMessage(String path) {
        if (path != null) registeredMessages.remove(path);
    }

    /**
     * All runtime-registered messages (read-only).
     */
    public Map<String, String> getRegisteredMessages() {
        return Collections.unmodifiableMap(registeredMessages);
    }

    public void saveMessages() {
        // No longer needed
    }
}
