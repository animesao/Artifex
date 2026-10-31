package me.dcplugin.dcustomitems.managers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manager of the effects of custom items.
 *
 * Uses a map-based approach: the effects are applied via applyEffectsFromMap(),
 * which is called from the global EquippedItemsChecker (1 task per server).
 *
 * The dead code (per-player BukkitRunnable) was removed — startPeriodicEffects()
 * was never called from the outside.
 */
public class EffectManager {

    private final Main plugin;

    // Track which effects were applied to each player (for removal)
    // ConcurrentHashMap: главный поток пишет сюда, а поток PlaceholderAPI
    // читает (getAppliedEffects) — обычный HashMap давал гонку.
    private final Map<UUID, Map<PotionEffectType, Integer>> playerAppliedEffects;

    // Effect duration: 60 seconds (in ticks)
    // EquippedItemsChecker refreshes every 150ms, so 60s is enough
    private static final int EFFECT_DURATION_TICKS = 60 * 20;

    // Cache of the parsed set bonuses: setId -> (effectType -> amplifier).
    // It is recalculated only on a config reload, not on every equipment
    // recalculation (that was a hot path — recalculateEquipment is called
    // on every change of the player's inventory).
    private final Map<String, Map<PotionEffectType, Integer>> setBonusCache = new HashMap<>();

    public EffectManager(Main plugin) {
        this.plugin = plugin;
        this.playerAppliedEffects = new java.util.concurrent.ConcurrentHashMap<>();
    }

    // ===== Parsing =====

    public Map<PotionEffectType, Integer> parseEffects(List<String> effectStrings) {
        Map<PotionEffectType, Integer> parsedEffects = new HashMap<>();

        if (effectStrings == null) return parsedEffects;

        for (String effectString : effectStrings) {
            if (effectString == null) continue;
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 2) {
                    String effectName = parts[0].toUpperCase(java.util.Locale.ROOT);

                    PotionEffectType effectType = EnumCache.getPotionEffect(effectName);

                    if (effectType != null) {
                        int amplifier = Integer.parseInt(parts[1].trim());
                        parsedEffects.put(effectType, amplifier);
                    } else {
                        plugin.getLogger().warning("Unknown effect: " + effectName);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error while parsing effect: " + effectString);
            }
        }

        return parsedEffects;
    }

    // ===== Apply / Remove (one-shot) =====

    /**
     * Apply the effects of the item (one-shot, for example CONSUMABLE)
     */
    public void applyEffects(Player player, CustomItem item) {
        if (item == null || item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

        for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
            int amplifier = Math.max(0, entry.getValue() - 1);
            PotionEffect existing = player.getPotionEffect(entry.getKey());
            if (existing != null && existing.getAmplifier() > amplifier) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(), EFFECT_DURATION_TICKS, amplifier, false, false
            ), true);
        }
    }

    /**
     * Remove the effects of a specific item
     */
    public void removeEffects(Player player, CustomItem item) {
        if (item == null || item.getParsedEffects() == null || item.getParsedEffects().isEmpty()) return;

        for (PotionEffectType effectType : item.getParsedEffects().keySet()) {
            player.removePotionEffect(effectType);
        }
    }

    // ===== Map-based (for EquippedItemsChecker) =====

    /**
     * Add the item effects to the map for the summation.
     *
     * The effect level is NOT summed up: two items with effect level 1 and 2
     * give level 2 (the maximum), not 3. Previously this was Integer::sum,
     * which made equipping two items unexpectedly strengthen the effect.
     */
    public void addEffectsToMap(Map<PotionEffectType, Integer> effectsMap, CustomItem item) {
        if (item == null || item.getParsedEffects() == null) return;
        for (Map.Entry<PotionEffectType, Integer> entry : item.getParsedEffects().entrySet()) {
            effectsMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    /**
     * Add the set-bonus effects to the map
     */
    public void addSetBonusEffectsToMap(Map<PotionEffectType, Integer> effectsMap, String setId) {
        if (setId == null) return;

        Map<PotionEffectType, Integer> setBonusEffects = getSetBonusEffects(setId);
        if (setBonusEffects.isEmpty()) {
            // The warning is issued once per set: the equipment recalculation happens
            // on every inventory change, otherwise the log would be flooded.
            if (!warnedMissingSets.add(setId)) return;
            plugin.getLogger().warning("Set bonuses for '" + setId + "' were not found in the config!");
            return;
        }

        for (Map.Entry<PotionEffectType, Integer> entry : setBonusEffects.entrySet()) {
            effectsMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    /** The sets for which a warning was already issued. */
    private final Set<String> warnedMissingSets = new HashSet<>();

    /**
     * The parsed set-bonus effects (from the cache).
     */
    public Map<PotionEffectType, Integer> getSetBonusEffects(String setId) {
        if (setId == null) return Map.of();
        Map<PotionEffectType, Integer> cached = setBonusCache.get(setId);
        if (cached != null) return cached;

        List<String> effectStrings = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");
        Map<PotionEffectType, Integer> parsed = effectStrings == null || effectStrings.isEmpty()
                ? Map.of()
                : Map.copyOf(parseEffects(effectStrings));
        setBonusCache.put(setId, parsed);
        return parsed;
    }

    /**
     * Reset the set-bonus cache (after a config reload).
     */
    public void invalidateSetBonusCache() {
        setBonusCache.clear();
        warnedMissingSets.clear();
    }

    /**
     * Apply the summed-up effects from the map to the player
     */
    public void applyEffectsFromMap(Player player, Map<PotionEffectType, Integer> effectsMap) {
        UUID playerId = player.getUniqueId();

        // Save for removal (immutable — external calls must not change the state)
        playerAppliedEffects.put(playerId, java.util.Collections.unmodifiableMap(new HashMap<>(effectsMap)));

        // Apply
        for (Map.Entry<PotionEffectType, Integer> entry : effectsMap.entrySet()) {
            int amplifier = Math.max(0, entry.getValue() - 1);
            PotionEffect existing = player.getPotionEffect(entry.getKey());
            if (existing != null && existing.getAmplifier() > amplifier) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(),
                    EFFECT_DURATION_TICKS,
                    amplifier,
                    false,
                    false
            ), true);
        }
    }

    /**
     * Remove all previously applied effects of custom items
     */
    public void removeAllEffectsFromPlayer(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();

        Map<PotionEffectType, Integer> appliedEffects = playerAppliedEffects.remove(playerId);
        if (appliedEffects != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : appliedEffects.entrySet()) {
                PotionEffectType effectType = entry.getKey();
                PotionEffect current = player.getPotionEffect(effectType);
                int appliedAmp = Math.max(0, entry.getValue() - 1);
                if (current != null && current.getAmplifier() > appliedAmp) {
                    continue;
                }
                player.removePotionEffect(effectType);
            }
        }
    }

    // ===== Compatibility (called from PlayerListener.onPlayerQuit and PluginBootstrap.disable) =====

    /**
     * Remove the effects of custom items from the player and forget its state.
     *
     * The method used to be a stub: the effects stayed on the player until the
     * end of their duration (60 s), and the entry in playerAppliedEffects stayed
     * in memory — that was a leak for every player who left.
     */
    public void stopPeriodicEffects(Player player) {
        removeAllEffectsFromPlayer(player);
    }

    /**
     * Stop all effects (on plugin disable)
     */
    public void stopAllEffects() {
        playerAppliedEffects.clear();
    }

    /**
     * Get the map of applied effects of custom items for the player.
     *
     * A copy is returned: the external code must not change the internal state
     * (otherwise it gets out of sync with removeAllEffectsFromPlayer).
     */
    /**
     * Number of effects applied to the player (no map copy).
     */
    public int getAppliedEffectCount(Player player) {
        if (player == null) return 0;
        Map<PotionEffectType, Integer> effects = playerAppliedEffects.get(player.getUniqueId());
        return effects == null ? 0 : effects.size();
    }

    public Map<PotionEffectType, Integer> getAppliedEffects(Player player) {
        if (player == null) return new HashMap<>();
        Map<PotionEffectType, Integer> effects = playerAppliedEffects.get(player.getUniqueId());
        return effects == null ? new HashMap<>() : new HashMap<>(effects);
    }
}
