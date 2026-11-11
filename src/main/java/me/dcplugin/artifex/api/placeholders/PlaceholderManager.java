package me.dcplugin.artifex.api.placeholders;

import me.dcplugin.artifex.Main;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Placeholder manager
 */
public class PlaceholderManager {

    private final Main plugin;

    // ConcurrentHashMap: the map is read on every placeholder request
    // (including from the PlaceholderAPI thread) and modified on reload.
    private final Map<String, Function<Player, String>> placeholders = new ConcurrentHashMap<>();
    private final Map<String, String> staticPlaceholders = new ConcurrentHashMap<>();

    // Placeholders from the Java API (items/*.java).
    // They are kept apart from the built-in ones: unregistering a Java placeholder
    // must not remove a built-in placeholder with the same name.
    private final Map<String, Function<Player, String>> customPlaceholders = new ConcurrentHashMap<>();

    private boolean placeholderApiEnabled = false;
    private final java.util.List<DCIPlaceholderExpansion> expansions = new java.util.concurrent.CopyOnWriteArrayList<>();

    public PlaceholderManager(Main plugin) {
        this.plugin = plugin;
        registerDefaults();
        checkPlaceholderApi();
    }

    // ===== REGISTRATION =====

    public void register(String identifier, Function<Player, String> resolver) {
        if (identifier == null || resolver == null) return;
        placeholders.put(identifier.toLowerCase(java.util.Locale.ROOT), resolver);
    }

    public void registerStatic(String identifier, String value) {
        if (identifier == null) return;
        staticPlaceholders.put(identifier.toLowerCase(java.util.Locale.ROOT), value);
    }

    public void registerAll(Map<String, Function<Player, String>> map) {
        if (map == null) return;
        for (Map.Entry<String, Function<Player, String>> entry : map.entrySet()) {
            register(entry.getKey(), entry.getValue());
        }
    }

    public void unregister(String identifier) {
        if (identifier == null) return;
        String key = identifier.toLowerCase(java.util.Locale.ROOT);
        placeholders.remove(key);
        staticPlaceholders.remove(key);
        customPlaceholders.remove(key);
    }

    /**
     * Registers a placeholder from the Java API (items/*.java).
     */
    public void registerCustom(String identifier, Function<Player, String> resolver) {
        if (identifier == null || resolver == null) return;
        customPlaceholders.put(identifier.toLowerCase(java.util.Locale.ROOT), resolver);
    }

    /**
     * Unregisters only the Java API placeholder (built-in ones are not affected).
     */
    public void unregisterCustom(String identifier) {
        if (identifier == null) return;
        customPlaceholders.remove(identifier.toLowerCase(java.util.Locale.ROOT));
    }

    public void clear() {
        placeholders.clear();
        staticPlaceholders.clear();
        customPlaceholders.clear();
    }

    // ===== REPLACEMENT =====

    public String replace(String text, Player player) {
        if (text == null || player == null) return text;

        // Static placeholders first
        for (Map.Entry<String, String> entry : staticPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        // Then the dynamic ones
        for (Map.Entry<String, Function<Player, String>> entry : placeholders.entrySet()) {
            try {
                String value = entry.getValue().apply(player);
                if (value != null) {
                    text = text.replace("{" + entry.getKey() + "}", value);
                    text = text.replace("%" + entry.getKey() + "%", value);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error in placeholder " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }

        return text;
    }

    public String replaceStatic(String text) {
        if (text == null) return text;
        for (Map.Entry<String, String> entry : staticPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    // ===== BUILT-IN PLACEHOLDERS =====

    private void registerDefaults() {
        register("player", (p) -> p.getName());
        register("player_uuid", (p) -> p.getUniqueId().toString());
        register("display_name", (p) -> p.getDisplayName());
        register("health", (p) -> String.valueOf((int) p.getHealth()));
        register("max_health", (p) -> String.valueOf((int) maxHealth(p)));
        register("health_percent", (p) -> {
            double max = maxHealth(p);
            // max can be 0 (for example during loading) — dividing by zero
            // produced Infinity and broke the formatting.
            if (max <= 0) return "0";
            return String.valueOf((int) ((p.getHealth() / max) * 100));
        });
        register("food", (p) -> String.valueOf(p.getFoodLevel()));
        register("saturation", (p) -> String.valueOf((int) p.getSaturation()));
        register("level", (p) -> String.valueOf(p.getLevel()));
        register("exp", (p) -> String.valueOf((int) (p.getExp() * 100)));
        register("x", (p) -> String.valueOf((int) p.getLocation().getX()));
        register("y", (p) -> String.valueOf((int) p.getLocation().getY()));
        register("z", (p) -> String.valueOf((int) p.getLocation().getZ()));
        register("world", (p) -> p.getWorld().getName());
        register("gamemode", (p) -> p.getGameMode().name());
        register("fly_status", (p) -> p.getAllowFlight() ? "on" : "off");
        register("item_in_hand", (p) -> {
            org.bukkit.inventory.ItemStack item = p.getInventory().getItemInMainHand();
            return item == null ? "AIR" : item.getType().name();
        });
        register("online", (p) -> String.valueOf(p.getServer().getOnlinePlayers().size()));
        register("max_players", (p) -> String.valueOf(p.getServer().getMaxPlayers()));
    }

    /**
     * Maximum health of the player (0 if the attribute is unavailable).
     * Previously this was a direct getAttribute(...).getValue() call — the attribute
     * could be null, and the placeholder threw an exception.
     */
    private static double maxHealth(Player player) {
        if (player == null) return 0;
        org.bukkit.attribute.AttributeInstance instance = player.getAttribute(Attribute.MAX_HEALTH);
        return instance == null ? 0 : instance.getValue();
    }

    // ===== RESOLVE (for the PlaceholderAPI expansion) =====

    /**
     * Resolve a placeholder value for a player.
     * Used by DCIPlaceholderExpansion.
     * @return resolved value or null if not found
     */
    public String resolve(String identifier, Player player) {
        if (identifier == null) return null;
        String key = identifier.toLowerCase(java.util.Locale.ROOT);

        // Java API placeholders take priority over the built-in ones
        Function<Player, String> custom = customPlaceholders.get(key);
        if (custom != null) {
            try {
                return custom.apply(player);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error in placeholder " + key + ": " + e.getMessage(), e);
            }
        }

        // Static placeholders
        String staticVal = staticPlaceholders.get(key);
        if (staticVal != null) return staticVal;

        // Dynamic placeholders
        Function<Player, String> resolver = placeholders.get(key);
        if (resolver != null) {
            try {
                return resolver.apply(player);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error in placeholder " + key + ": " + e.getMessage(), e);
            }
        }
        return null;
    }

    // ===== PlaceholderAPI DETECTION =====

    private void checkPlaceholderApi() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            placeholderApiEnabled = true;
            plugin.getLogger().info("PlaceholderAPI found!");
        } catch (ClassNotFoundException e) {
            placeholderApiEnabled = false;
        }
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    /**
     * Register the PlaceholderAPI expansion.
     * Called from Main after PlaceholderAPI is detected.
     */
    public void registerExpansion(me.dcplugin.artifex.Main plugin) {
        if (!placeholderApiEnabled) return;
        try {
            unregisterExpansion();
            for (String id : new String[]{"artifex", "dcustomitems", "dci"}) {
                DCIPlaceholderExpansion exp = new DCIPlaceholderExpansion(plugin, id);
                exp.register();
                expansions.add(exp);
            }
            plugin.getLogger().info("[PlaceholderAPI] Exported placeholders registered (%artifex_...%, %dcustomitems_...%, %dci_...%)!");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "[PlaceholderAPI] Failed to register expansion: " + e.getMessage(), e);
        }
    }

    /**
     * Unregisters the PlaceholderAPI expansion (on plugin shutdown).
     *
     * The expansion declared persist() = true, so it stayed registered
     * in PlaceholderAPI after the plugin was shut down and kept a reference to the old
     * classloader.
     */
    public void unregisterExpansion() {
        if (expansions.isEmpty()) return;
        for (DCIPlaceholderExpansion exp : expansions) {
            try {
                exp.unregister();
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[PlaceholderAPI] Failed to unregister expansion: " + e.getMessage(), e);
            }
        }
        expansions.clear();
    }

    // ===== GETTERS =====

    /**
     * Number of registered placeholders (no set copy).
     */
    public int getRegisteredPlaceholderCount() {
        return placeholders.size() + staticPlaceholders.size() + customPlaceholders.size();
    }

    public Set<String> getRegisteredPlaceholders() {
        Set<String> all = new HashSet<>(placeholders.keySet());
        all.addAll(staticPlaceholders.keySet());
        all.addAll(customPlaceholders.keySet());
        return Collections.unmodifiableSet(all);
    }

    public boolean hasPlaceholder(String identifier) {
        if (identifier == null) return false;
        String key = identifier.toLowerCase(java.util.Locale.ROOT);
        return placeholders.containsKey(key)
                || staticPlaceholders.containsKey(key)
                || customPlaceholders.containsKey(key);
    }
}
