package me.dcplugin.artifex.api.commands;

import me.dcplugin.artifex.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Base class for creating custom commands
 */
public abstract class CustomCommand {

    private String name;
    private String description;
    private String usage;
    private String permission;
    private String permissionMessage;
    private List<String> aliases;
    private Main plugin;

    protected CustomCommand() {
        // null = use the central message from items/messages.java
    }

    protected CustomCommand(String name, String description, String usage, String permission) {
        this();
        this.name = name.toLowerCase();
        this.description = description;
        this.usage = usage;
        this.permission = permission;
        this.aliases = new ArrayList<>();
    }

    protected CustomCommand(String name, String description, String usage, String permission, String... aliases) {
        this(name, description, usage, permission);
        this.aliases = Arrays.asList(aliases);
    }

    public void setPlugin(Main plugin) { this.plugin = plugin; }
    public Main getPlugin() { return plugin; }

    public abstract boolean execute(CommandSender sender, String[] args);

    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }

    public void onRegister() {}
    public void onUnregister() {}

    public boolean hasPermission(CommandSender sender) {
        if (permission == null || permission.isEmpty()) return true;
        return sender.hasPermission(permission);
    }

    public String getPermissionMessage() {
        if (permissionMessage != null) return permissionMessage;
        return message("commands.no-permission", "&cYou don't have permission!");
    }
    public void setPermissionMessage(String msg) { this.permissionMessage = msg; }

    /**
     * Resolves a message from the central message system.
     *
     * <p>Every message a command shows can be overridden in
     * {@code items/messages.java} — under the path passed here. A command
     * may also plug in its own texts with
     * {@link me.dcplugin.artifex.managers.MessageManager#registerMessage}.
     *
     * @param path          message path (for example "commands.no-permission")
     * @param defaultMessage fallback when the path is unknown
     */
    protected String message(String path, String defaultMessage) {
        if (plugin == null) return defaultMessage;
        return plugin.getMessageManager().getMessage(path, defaultMessage);
    }

    protected Player getTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player target = sender.getServer().getPlayer(args[index]);
            if (target == null) {
                msg(sender, message("commands.player-not-found", "&cPlayer {player} not found!")
                        .replace("{player}", args[index]));
                return null;
            }
            return target;
        }
        if (sender instanceof Player) return (Player) sender;
        msg(sender, message("commands.specify-player", "&cSpecify a player: {usage}")
                .replace("{usage}", usage == null ? "" : usage));
        return null;
    }

    protected int getInt(String[] args, int index, int def) {
        if (args.length <= index) return def;
        try { return Integer.parseInt(args[index]); } catch (Exception e) { return def; }
    }

    protected double getDouble(String[] args, int index, double def) {
        if (args.length <= index) return def;
        try { return Double.parseDouble(args[index]); } catch (Exception e) { return def; }
    }

    protected String joinArgs(String[] args, int start) {
        if (args.length <= start) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    protected void msg(CommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    protected void msg(Player player, String message) {
        player.sendMessage(colorize(message));
    }

    protected void title(Player player, String title, String subtitle) {
        player.sendTitle(colorize(title), colorize(subtitle), 10, 40, 10);
    }

    protected String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "\u00a7");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name.toLowerCase(); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public List<String> getAliases() { return aliases != null ? aliases : Collections.emptyList(); }
    public void setAliases(String... aliases) { this.aliases = Arrays.asList(aliases); }
}
