package me.dcplugin.artifex.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utilities for message processing.
 * Supports color codes and PlaceholderAPI substitutions.
 */
public class ColorUtils {

    private static final java.util.regex.Pattern HEX_PATTERN =
            java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})|<#([A-Fa-f0-9]{6})>|\\{#([A-Fa-f0-9]{6})\\}");

    /**
     * Replaces color codes (& -> §), hex color formats (&#RRGGBB, <#RRGGBB>, {#RRGGBB}),
     * and Adventure MiniMessage tags (<gradient:...>, <rainbow>, <color:...>).
     */
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        if (message.contains("<") && message.contains(">")) {
            try {
                net.kyori.adventure.text.Component component =
                        net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message);
                message = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
            } catch (Throwable ignored) {
                // If MiniMessage parsing fails (e.g. raw mathematical < >), proceed to HEX and legacy formatting
            }
        }
        try {
            java.util.regex.Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group(1);
                if (hex == null) hex = matcher.group(2);
                if (hex == null) hex = matcher.group(3);
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
            }
            matcher.appendTail(buffer);
            message = buffer.toString();
        } catch (Throwable ignored) {
            // Older Bukkit without ChatColor.of
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Processes the message: color codes + PlaceholderAPI placeholders.
 * The central method for processing all plugin messages.
     *
     * @param player The player (for the PAPI placeholders, may be null)
     * @param message The message with color codes and placeholders
     * @return The processed message
     */
    public static String processMessage(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 1. PlaceholderAPI substitutions (if available and there is a player)
        if (player != null && isPlaceholderApiAvailable()) {
            try {
                message = PlaceholderAPI.setPlaceholders(player, message);
            } catch (Exception ignored) {
                // If PAPI crashed — continue with the color codes
            }
        }

        // 2. Color codes
        return colorize(message);
    }

    /**
     * Processes the title and subtitle for sendTitle.
     */
    public static String[] processTitle(Player player, String title, String subtitle) {
        return new String[]{
            processMessage(player, title),
            processMessage(player, subtitle)
        };
    }

    /**
     * Removes the color codes from the text.
     */
    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text);
    }

    /**
     * Formats the time into a readable form.
     */
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
    }

    // ===== PlaceholderAPI detection =====

    private static Boolean placeholderApiAvailable = null;

    /**
     * Checks for the presence of PlaceholderAPI (the result is cached).
     */
    private static boolean isPlaceholderApiAvailable() {
        if (placeholderApiAvailable == null) {
            try {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                placeholderApiAvailable = true;
            } catch (ClassNotFoundException e) {
                placeholderApiAvailable = false;
            }
        }
        return placeholderApiAvailable;
    }

    /**
     * Reset the PAPI cache (call on /reload if needed).
     */
    public static void resetCache() {
        placeholderApiAvailable = null;
    }
}
