package me.dcplugin.dcustomitems.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized caching of the Bukkit enum lookups.
 *
 * Material.valueOf(), Particle.valueOf(), Sound.valueOf() and so on use
 * reflection/registries — caching removes this overhead on repeated calls.
 *
 * All maps are ConcurrentHashMap: the cache is read from the server main
 * thread, but the lookups may also arrive from the async pool (DB, update
 * checker). ConcurrentHashMap does not allow null values, so "not found"
 * is cached via a sentinel.
 */
public final class EnumCache {

    private EnumCache() {} // utility class

    /** The "value not found" marker — ConcurrentHashMap does not store null. */
    private static final Object NULL = new Object();

    // ===== Material =====

    private static final Map<String, Object> MATERIAL_CACHE = new ConcurrentHashMap<>();

    /**
     * Get the Material by name (safe, cached)
     *
     * @return the Material, or null if not found
     */
    public static Material getMaterial(String name) {
        if (name == null || name.isEmpty()) return null;
        // Remove the namespace prefix: minecraft:diamond_sword -> DIAMOND_SWORD
        String clean = name;
        if (clean.contains(":")) {
            clean = clean.substring(clean.indexOf(':') + 1);
        }
        // Replace the hyphens with underscores: diamond-sword -> DIAMOND_SWORD
        clean = clean.replace('-', '_');
        String key = clean.toUpperCase(Locale.ROOT);
        Object cached = MATERIAL_CACHE.computeIfAbsent(key, k -> {
            try {
                return Material.valueOf(k);
            } catch (IllegalArgumentException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Material) cached;
    }

    /**
     * Check whether the material is AIR
     */
    public static boolean isAir(Material material) {
        return material == null || material.isAir();
    }

    // ===== Particle =====

    private static final Map<String, Object> PARTICLE_CACHE = new ConcurrentHashMap<>();

    public static Particle getParticle(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = PARTICLE_CACHE.computeIfAbsent(key, k -> {
            try {
                return Particle.valueOf(k);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Particle) cached;
    }

    // ===== Sound =====

    private static final Map<String, Object> SOUND_CACHE = new ConcurrentHashMap<>();

    /**
     * The sound constants are marked as deprecated in Paper 1.21+, but the registry
     * keys of the sounds are different ("entity.item_break" instead of
     * "ENTITY_ITEM_BREAK"), so valueOf with a fallback to the constant field
     * remains the most reliable approach.
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static Sound getSound(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = SOUND_CACHE.computeIfAbsent(key, k -> {
            try {
                return Sound.valueOf(k);
            } catch (Exception | LinkageError e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (Sound) cached;
    }

    // ===== PotionEffectType =====

    private static final Map<String, Object> EFFECT_CACHE = new ConcurrentHashMap<>();

    /**
     * Lazy initialization of the aliases: static references to PotionEffectType
     * require an initialized Bukkit and would break the class loading
     * in a clean environment (unit tests without a server).
     */
    private static void ensureEffectAliases() {
        if (!EFFECT_CACHE.isEmpty()) return;
        EFFECT_CACHE.put("INCREASE_DAMAGE", PotionEffectType.STRENGTH);
        EFFECT_CACHE.put("DAMAGE_RESISTANCE", PotionEffectType.RESISTANCE);
    }

    public static PotionEffectType getPotionEffect(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);

        ensureEffectAliases();
        Object cached = EFFECT_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (PotionEffectType) cached;

        // Attempt via the Bukkit API
        PotionEffectType effectType = PotionEffectType.getByName(key);
        if (effectType != null) {
            EFFECT_CACHE.put(key, effectType);
            return effectType;
        }
        EFFECT_CACHE.put(key, NULL);
        return null;
    }

    // ===== Enchantment =====

    private static final Map<String, Object> ENCHANTMENT_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public static Enchantment getEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.toLowerCase(Locale.ROOT).replace('-', '_');

        Object cached = ENCHANTMENT_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (Enchantment) cached;

        Enchantment ench = null;
        try {
            ench = Enchantment.getByKey(NamespacedKey.minecraft(key));
        } catch (Exception ignored) {
            // Fall through to getByName
        }
        if (ench == null) {
            try {
                ench = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                // Unknown enchantment — return null
            }
        }
        ENCHANTMENT_CACHE.put(key, ench == null ? NULL : ench);
        return ench;
    }

    // ===== ItemFlag =====

    private static final Map<String, Object> FLAG_CACHE = new ConcurrentHashMap<>();

    public static ItemFlag getItemFlag(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = normalize(name);
        Object cached = FLAG_CACHE.computeIfAbsent(key, k -> {
            try {
                return ItemFlag.valueOf(k);
            } catch (IllegalArgumentException e) {
                return NULL;
            }
        });
        return cached == NULL ? null : (ItemFlag) cached;
    }

    // ===== Attribute =====

    private static final Map<String, Object> ATTRIBUTE_CACHE = new ConcurrentHashMap<>();

    /**
     * Resolves the attribute by name.
     *
     * Paper 1.21.3+ no longer knows the legacy names like {@code GENERIC_ATTACK_DAMAGE}:
     * {@code Attribute.valueOf} looks the key up in the registry and throws an
     * IllegalArgumentException. Both the legacy names
     * (the GENERIC_/PLAYER_/ZOMBIE_ prefixes) and the modern ones (attack_damage)
     * are supported here, as well as the registry keys.
     *
     * @return the Attribute, or null if the attribute is unknown
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static Attribute getAttribute(String name) {
        if (name == null || name.isEmpty()) return null;
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        Object cached = ATTRIBUTE_CACHE.get(key);
        if (cached == NULL) return null;
        if (cached != null) return (Attribute) cached;

        Attribute attribute = resolveAttribute(key);
        ATTRIBUTE_CACHE.put(key, attribute == null ? NULL : attribute);
        return attribute;
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute resolveAttribute(String upperKey) {
        // 1) The direct name (the modern form: ATTACK_DAMAGE / attack_damage)
        Attribute direct = valueOfAttribute(upperKey);
        if (direct != null) return direct;

        // 2) Legacy prefixes: GENERIC_MAX_HEALTH -> MAX_HEALTH
        for (String prefix : LEGACY_ATTRIBUTE_PREFIXES) {
            if (upperKey.startsWith(prefix) && upperKey.length() > prefix.length()) {
                Attribute stripped = valueOfAttribute(upperKey.substring(prefix.length()));
                if (stripped != null) return stripped;
            }
        }

        // 3) Match against the registry key (minecraft:attack_damage)
        String registryKey = upperKey.toLowerCase(Locale.ROOT);
        for (Attribute attribute : allAttributes()) {
            NamespacedKey key = attribute.getKey();
            if (key != null && registryKey.equals(key.getKey())) {
                return attribute;
            }
        }
        return null;
    }

    private static final String[] LEGACY_ATTRIBUTE_PREFIXES = {
            "GENERIC_", "PLAYER_", "ZOMBIE_", "HORSE_", "LLAMA_", "MINECART_", "BOAT_"
    };

    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute valueOfAttribute(String upperKey) {
        try {
            return Attribute.valueOf(upperKey);
        } catch (Exception | LinkageError e) {
            // Unknown attribute name. LinkageError — because without a server
            // the static initializer of Attribute fails with
            // ExceptionInInitializerError.
            return null;
        }
    }

    /**
     * All attributes of the registry. In an environment without a server (unit
     * tests) the registry is unavailable — an empty array is returned instead
     * of crashing.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private static Attribute[] allAttributes() {
        try {
            return Attribute.values();
        } catch (Exception | LinkageError e) {
            return new Attribute[0];
        }
    }

    // ===== Helpers =====

    /** Normalization of the enum constant name: uppercase + hyphens -> underscores. */
    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * The cache sizes (for debugging/logging)
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("materials", MATERIAL_CACHE.size());
        stats.put("particles", PARTICLE_CACHE.size());
        stats.put("sounds", SOUND_CACHE.size());
        stats.put("effects", EFFECT_CACHE.size());
        stats.put("enchantments", ENCHANTMENT_CACHE.size());
        stats.put("flags", FLAG_CACHE.size());
        stats.put("attributes", ATTRIBUTE_CACHE.size());
        return stats;
    }
}
