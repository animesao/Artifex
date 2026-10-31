package me.dcplugin.dcustomitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;

/**
 * Access to the Bukkit CommandMap and its map of known commands.
 *
 * Bukkit provides no public API for this, so reflection is used.
 * Previously every implementation looked for the commandMap field in the
 * {@code Bukkit} class, where it does not exist (the field lives in
 * CraftServer) — the module and Java API commands simply were not registered.
 *
 * The result is cached: the reflection is only needed once.
 */
public final class CommandMapAccessor {

    private CommandMapAccessor() {}

    private static CommandMap commandMap;
    private static Map<String, Command> knownCommands;
    private static boolean resolved;

    /**
     * The server's CommandMap (null if it could not be obtained).
     */
    public static synchronized CommandMap getCommandMap() {
        if (resolved) return commandMap;
        resolved = true;
        commandMap = resolveCommandMap();
        return commandMap;
    }

    private static CommandMap resolveCommandMap() {
        Object server = Bukkit.getServer();

        // 1) Paper 1.20.6+: CraftServer#getCommandMap()
        if (server != null) {
            try {
                Method method = server.getClass().getMethod("getCommandMap");
                Object result = method.invoke(server);
                if (result instanceof CommandMap map) {
                    return map;
                }
            } catch (Exception ignored) {
                // Fall through to the next approach
            }
        }

        // 2) The commandMap field in the server class (Spigot/CraftServer).
        //    The field is not static — it has to be read on the server instance.
        if (server != null) {
            CommandMap map = readCommandMapField(server.getClass(), server);
            if (map != null) return map;
        }

        // 3) Historical variant: a static field in the Bukkit class itself
        try {
            CommandMap map = readCommandMapField(Bukkit.class, null);
            if (map != null) return map;
        } catch (Exception ignored) {
            // Fall through to the next approach
        }

        Bukkit.getLogger().log(Level.WARNING,
                "[DC-CustomItems] Could not obtain the CommandMap - dynamic commands will not be registered");
        return null;
    }

    private static CommandMap readCommandMapField(Class<?> owner, Object instance) {
        try {
            Field field = owner.getDeclaredField("commandMap");
            field.setAccessible(true);
            Object value = field.get(instance);
            return value instanceof CommandMap map ? map : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * The map of the "known" commands of the CommandMap.
     *
     * Returns null if it could not be obtained — previously an empty copy was
     * returned in that case, and the module commands were formally "removed"
     * but stayed in the CommandMap forever.
     */
    public static synchronized Map<String, Command> getKnownCommands(CommandMap map) {
        if (map == null) return null;
        if (knownCommands != null) return knownCommands;

        // 1) The standard field SimpleCommandMap#knownCommands
        try {
            Field field = map.getClass().getDeclaredField("knownCommands");
            field.setAccessible(true);
            Object value = field.get(map);
            if (value instanceof Map<?, ?> raw) {
                knownCommands = castKnownCommands(raw);
                return knownCommands;
            }
        } catch (Exception ignored) {
            // Fall through to the field enumeration
        }

        // 2) Enumeration of all fields: Paper/Purpur may rename the field
        for (Field field : map.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(map);
                if (value instanceof Map<?, ?> raw) {
                    Map<String, Command> commands = castKnownCommands(raw);
                    if (commands != null) {
                        knownCommands = commands;
                        return knownCommands;
                    }
                }
            } catch (Exception ignored) {
                // The next field
            }
        }

        Bukkit.getLogger().log(Level.WARNING,
                "[DC-CustomItems] Could not obtain knownCommands - commands will not be removed on reload");
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> castKnownCommands(Map<?, ?> raw) {
        if (raw.isEmpty()) return null;
        Map.Entry<?, ?> first = raw.entrySet().iterator().next();
        if (!(first.getKey() instanceof String) || !(first.getValue() instanceof Command)) {
            return null;
        }
        return (Map<String, Command>) raw;
    }

    /**
     * Synchronizes commands with players and Brigadier if supported (e.g. Paper).
     */
    public static void syncCommands() {
        Object server = Bukkit.getServer();
        if (server == null) return;
        try {
            Method syncMethod = server.getClass().getMethod("syncCommands");
            syncMethod.invoke(server);
        } catch (Throwable ignored) {
            // Not supported on this server software or version
        }
    }

    /**
     * Reset the cache (in case the server is swapped in the tests).
     */
    public static synchronized void reset() {
        commandMap = null;
        knownCommands = null;
        resolved = false;
    }
}
