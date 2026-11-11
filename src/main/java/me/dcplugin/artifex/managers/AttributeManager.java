package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.EnumCache;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AttributeManager {

    private final Main plugin;
    private final Map<UUID, Map<Attribute, AttributeModifier>> playerAppliedAttributes;

    public AttributeManager(Main plugin) {
        this.plugin = plugin;
        this.playerAppliedAttributes = new HashMap<>();
    }

    /**
     * Applies the attributes of a custom item to the player
     */
    public void applyAttributes(Player player, CustomItem item, String slotType) {
        if (item == null || item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            Attribute attribute = EnumCache.getAttribute(entry.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Unknown attribute: " + entry.getKey() + " for item: " + item.getId());
                continue;
            }
            double value = entry.getValue();

            // Create a unique modifier for this item and slot
            String modifierName = "custom_" + item.getId() + "_" + slotType + "_" + attributeKey(attribute);
            AttributeModifier modifier = new AttributeModifier(
                    modifierKey(modifierName),
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
            );

            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                // Remove the old modifier if there is one
                AttributeModifier oldModifier = appliedModifiers.get(attribute);
                if (oldModifier != null) {
                    attributeInstance.removeModifier(oldModifier);
                }

                // Apply the new modifier
                attributeInstance.addModifier(modifier);
                appliedModifiers.put(attribute, modifier);
            }
        }
    }

    /**
     * Removes all attributes of custom items from the player
     */
    public void removeAllAttributes(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.remove(playerId);

        if (appliedModifiers != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : appliedModifiers.entrySet()) {
                AttributeInstance attributeInstance = player.getAttribute(entry.getKey());
                if (attributeInstance != null) {
                    try {
                        attributeInstance.removeModifier(entry.getValue());
                    } catch (Exception ignored) {
                        // The modifier was already removed or does not exist
                    }
                }
            }
        }
    }

    /**
     * Applies the attributes from the map (to sum up the attributes of several items)
     */
    public void applyAttributesFromMap(Player player, Map<Attribute, Double> attributesMap) {
        UUID playerId = player.getUniqueId();

        // First remove all old attributes
        removeAllAttributes(player);

        if (attributesMap == null || attributesMap.isEmpty()) {
            return;
        }

        Map<Attribute, AttributeModifier> appliedModifiers = playerAppliedAttributes.computeIfAbsent(playerId, k -> new HashMap<>());

        for (Map.Entry<Attribute, Double> entry : attributesMap.entrySet()) {
            Attribute attribute = entry.getKey();
            double value = entry.getValue();

            try {
                // Stable modifier (without a timestamp, for correct removal)
                String modifierName = "custom_total_" + attributeKey(attribute);
                AttributeModifier modifier = new AttributeModifier(
                        modifierKey(modifierName),
                        value,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.ANY
                );

                AttributeInstance attributeInstance = player.getAttribute(attribute);
                if (attributeInstance != null) {
                    // Remove the old modifier if there is one
                    AttributeModifier oldModifier = appliedModifiers.get(attribute);
                    if (oldModifier != null) {
                        attributeInstance.removeModifier(oldModifier);
                    }
                    attributeInstance.addModifier(modifier);
                    appliedModifiers.put(attribute, modifier);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Error while applying attribute " + attributeKey(attribute) + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Adds the item attributes to the map for the subsequent summation
     */
    public void addAttributesToMap(Map<Attribute, Double> attributesMap, CustomItem item) {
        if (item == null || item.getAttributes() == null || item.getAttributes().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Double> entry : item.getAttributes().entrySet()) {
            Attribute attribute = EnumCache.getAttribute(entry.getKey());
            if (attribute == null) {
                plugin.getLogger().warning("Unknown attribute: " + entry.getKey() + " for item: " + item.getId());
                continue;
            }
            // Sum up the attributes of different items
            attributesMap.merge(attribute, entry.getValue(), Double::sum);
        }
    }

    /**
     * Clears all data on plugin disable, removing modifiers from online players first.
     */
    public void cleanup() {
        if (plugin != null && plugin.getServer() != null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                removeAllAttributes(player);
            }
        }
        playerAppliedAttributes.clear();
    }

    // ===== Helpers =====

    /**
     * The registry key of the attribute (for example {@code attack_damage}).
     * It is used in the modifier name — without it the key would be
     * {@code minecraft:attack_damage}, i.e. in the vanilla Minecraft namespace.
     */
    private static String attributeKey(Attribute attribute) {
        NamespacedKey key = attribute.getKey();
        return key != null ? key.getKey() : "attribute";
    }

    /**
     * Builds a NamespacedKey in the plugin namespace.
     *
     * {@code NamespacedKey.fromString} without a namespace prefix returns a
     * key from the {@code minecraft} namespace (or null for invalid
     * characters) — while {@code new NamespacedKey(plugin, ...)} throws an
     * IllegalArgumentException. Therefore the key is normalized manually:
     * only {@code [a-z0-9._-]}.
     */
    private NamespacedKey modifierKey(String rawKey) {
        String sanitized = rawKey == null ? "attribute" : rawKey.toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            sb.append(valid ? c : '_');
        }
        if (sb.length() == 0) sb.append("attribute");
        return new NamespacedKey(plugin, sb.toString());
    }
}
