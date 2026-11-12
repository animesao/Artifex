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

    private static final java.util.regex.Pattern SECTION_HEX_PATTERN =
            java.util.regex.Pattern.compile("§x§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])");

    /**
     * Converts formatted Minecraft text back into config format (& color codes and &#RRGGBB).
     */
    public static String decolorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        java.util.regex.Matcher matcher = SECTION_HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "&#" + matcher.group(1) + matcher.group(2) + matcher.group(3)
                    + matcher.group(4) + matcher.group(5) + matcher.group(6));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace('§', '&');
    }

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
    public static boolean isPlaceholderApiAvailable() {
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

    /**
     * Parses a string representation of color (hex "#RRGGBB", RGB "r,g,b", or color name).
     */
    public static org.bukkit.Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isBlank()) return null;
        String s = colorStr.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("&#")) {
            s = s.substring(2);
        }
        if (s.length() == 6) {
            try {
                int rgb = Integer.parseInt(s, 16);
                return org.bukkit.Color.fromRGB(rgb);
            } catch (NumberFormatException ignored) {}
        }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            if (parts.length == 3) {
                try {
                    int r = Math.max(0, Math.min(255, Integer.parseInt(parts[0].trim())));
                    int g = Math.max(0, Math.min(255, Integer.parseInt(parts[1].trim())));
                    int b = Math.max(0, Math.min(255, Integer.parseInt(parts[2].trim())));
                    return org.bukkit.Color.fromRGB(r, g, b);
                } catch (NumberFormatException ignored) {}
            }
        }
        return switch (s.toUpperCase(java.util.Locale.ROOT)) {
            case "RED" -> org.bukkit.Color.RED;
            case "BLUE" -> org.bukkit.Color.BLUE;
            case "GREEN" -> org.bukkit.Color.GREEN;
            case "YELLOW" -> org.bukkit.Color.YELLOW;
            case "PURPLE" -> org.bukkit.Color.PURPLE;
            case "ORANGE" -> org.bukkit.Color.ORANGE;
            case "WHITE" -> org.bukkit.Color.WHITE;
            case "BLACK" -> org.bukkit.Color.BLACK;
            case "AQUA" -> org.bukkit.Color.AQUA;
            case "FUCHSIA" -> org.bukkit.Color.FUCHSIA;
            case "GRAY", "GREY" -> org.bukkit.Color.GRAY;
            case "LIME" -> org.bukkit.Color.LIME;
            case "MAROON" -> org.bukkit.Color.MAROON;
            case "NAVY" -> org.bukkit.Color.NAVY;
            case "OLIVE" -> org.bukkit.Color.OLIVE;
            case "SILVER" -> org.bukkit.Color.SILVER;
            case "TEAL" -> org.bukkit.Color.TEAL;
            default -> null;
        };
    }
}
