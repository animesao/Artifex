package me.dcplugin.artifex.utils;

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
                "[Artifex] Could not obtain the CommandMap - dynamic commands will not be registered");
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

    private static final java.util.concurrent.atomic.AtomicBoolean syncScheduled = new java.util.concurrent.atomic.AtomicBoolean(false);

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

        // 1) The standard field SimpleCommandMap#knownCommands (walking up the class hierarchy for Paper/Purpur subclasses)
        Class<?> current = map.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField("knownCommands");
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
                // Try superclass
            }
            current = current.getSuperclass();
        }

        // 2) Enumeration of all fields across hierarchy in case Paper renames the field
        current = map.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
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
                    // Next field
                }
            }
            current = current.getSuperclass();
        }

        Bukkit.getLogger().log(Level.WARNING,
                "[Artifex] Could not obtain knownCommands - commands will not be removed on reload");
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> castKnownCommands(Map<?, ?> raw) {
        if (raw == null) return null;
        if (raw.isEmpty()) return (Map<String, Command>) raw;
        Map.Entry<?, ?> first = raw.entrySet().iterator().next();
        if (!(first.getKey() instanceof String) || !(first.getValue() instanceof Command)) {
            return null;
        }
        return (Map<String, Command>) raw;
    }

    /**
     * Synchronizes commands with players and Brigadier if supported (e.g. Paper).
     * Automatically debounced to the next tick to prevent racing with Paper's
     * async command builder thread pool.
     */
    public static void syncCommands() {
        Object server = Bukkit.getServer();
        if (server == null) return;

        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
            if (plugin != null && plugin.isEnabled() && Bukkit.isPrimaryThread()) {
                if (syncScheduled.compareAndSet(false, true)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        syncScheduled.set(false);
                        try {
                            Method syncMethod = server.getClass().getMethod("syncCommands");
                            syncMethod.invoke(server);
                        } catch (Throwable ignored) {}
                    });
                }
                return;
            }
        } catch (Throwable ignored) {
            // Unit tests without full Bukkit scheduler mock
        }

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
