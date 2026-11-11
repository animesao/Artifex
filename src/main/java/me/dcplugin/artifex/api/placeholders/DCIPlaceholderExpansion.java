package me.dcplugin.artifex.api.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * PlaceholderAPI expansion for DC-CustomItems.
 *
 * All placeholders are available as %dcustomitems_<identifier>% and
 * as the shorter %dci_<identifier>% alias.
 *
 * Built-in placeholders:
 *   %dci_player%            - Player name
 *   %dci_player_uuid%       - Player UUID
 *   %dci_display_name%      - Display name
 *   %dci_health%            - Current health
 *   %dci_max_health%        - Max health
 *   %dci_health_percent%    - Health percentage
 *   %dci_food%              - Food level
 *   %dci_saturation%        - Saturation
 *   %dci_level%             - XP level
 *   %dci_exp%               - XP percentage
 *   %dci_x%, %dci_y%, %dci_z% - Coordinates
 *   %dci_world%             - World name
 *   %dci_gamemode%          - Game mode
 *   %dci_fly_status%        - Fly status (on/off)
 *   %dci_item_in_hand%      - Item in main hand
 *   %dci_online%            - Online players count
 *   %dci_max_players%       - Max players
 *   %dci_version%           - Plugin version
 *   %dci_item_count%        - Total custom items loaded
 *   %dci_active_effects%    - Active effects from custom items
 *   %dci_active_effects_count% - Number of active effects
 *
 * Item-specific placeholders (dynamic):
 *   %dci_has_<item_id>%        - "true" if player has item in inventory
 *   %dci_holding_<item_id>%    - "true" if player is holding item
 *   %dci_equipped_<item_id>%   - "true" if item is in correct activation slot
 *   %dci_item_amount_<item_id>%- Amount of item in inventory
 */
public class DCIPlaceholderExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final String identifier;

    public DCIPlaceholderExpansion(Main plugin) {
        this(plugin, "artifex");
    }

    public DCIPlaceholderExpansion(Main plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getAuthor() {
        return "animesao";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep registered even when PlaceholderAPI reloads
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return handleGlobalPlaceholder(identifier);
        }

        // Try built-in placeholders from PlaceholderManager
        String value = plugin.getPlaceholderManager().resolve(identifier, player);
        if (value != null) {
            return value;
        }

        String id = identifier.toLowerCase(java.util.Locale.ROOT);

        // Plugin-specific static placeholders
        switch (id) {
            case "version":
                return plugin.getDescription().getVersion();
            case "item_count":
                return String.valueOf(plugin.getItemHandler().getCustomItemCount());
            case "java_item_count":
                return plugin.getApiItemRegistry() != null
                    ? String.valueOf(plugin.getApiItemRegistry().getItemCount())
                    : "0";
            case "command_count":
                return plugin.getApiItemRegistry() != null
                    ? String.valueOf(plugin.getApiItemRegistry().getCommandCount())
                    : "0";
            case "placeholder_count":
                return plugin.getPlaceholderManager() != null
                    ? String.valueOf(plugin.getPlaceholderManager().getRegisteredPlaceholderCount())
                    : "0";
            case "module_count":
                return plugin.getModuleManager() != null
                    ? String.valueOf(plugin.getModuleManager().getModuleCount())
                    : "0";
            case "database_type":
                return plugin.getDatabaseManager() != null
                    ? plugin.getDatabaseManager().getType()
                    : "none";
            case "active_effects":
                return getActiveEffects(player);
            case "active_effects_count":
                return String.valueOf(plugin.getEffectManager().getAppliedEffectCount(player));
        }

        // Dynamic item-specific placeholders
        return resolveItemPlaceholder(player, id);
    }

    /**
     * Get active effects from custom items as a formatted string.
     * Format: "SPEED:I, STRENGTH:II, FIRE_RESISTANCE:I"
     */
    private String getActiveEffects(Player player) {
        java.util.Map<org.bukkit.potion.PotionEffectType, Integer> effects =
            plugin.getEffectManager().getAppliedEffects(player);
        if (effects.isEmpty()) return "none";

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<org.bukkit.potion.PotionEffectType, Integer> entry : effects.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey().getName())
              .append(":")
              .append(toRoman(entry.getValue()));
        }
        return sb.toString();
    }

    /**
     * Convert effect level to Roman numeral.
     */
    private String toRoman(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(level);
        }
    }

    /**
     * Resolve dynamic item-specific placeholders.
     * Supports: has_, holding_, equipped_, item_amount_
     */
    private String resolveItemPlaceholder(Player player, String identifier) {
        String itemId = null;
        String prefix = null;

        if (identifier.startsWith("has_")) {
            prefix = "has_";
        } else if (identifier.startsWith("holding_")) {
            prefix = "holding_";
        } else if (identifier.startsWith("equipped_")) {
            prefix = "equipped_";
        } else if (identifier.startsWith("item_amount_")) {
            prefix = "item_amount_";
        }

        if (prefix == null) return null;

        // Extract item ID (everything after the prefix)
        // item_id may contain underscores, so we need to try matching against known items
        String remainder = identifier.substring(prefix.length());
        itemId = findMatchingItemId(remainder);

        if (itemId == null) return null;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        String activationSlot = null;
        if (customItem != null) {
            activationSlot = customItem.getActivationSlot();
        } else {
            // A Java API item — it has an activation slot as well
            me.dcplugin.artifex.api.AbstractCustomItem javaItem =
                    plugin.getApiItemRegistry() != null
                            ? plugin.getApiItemRegistry().getItem(itemId)
                            : null;
            if (javaItem == null) return "false";
            activationSlot = javaItem.getActivationSlot();
        }

        switch (prefix) {
            case "has_":
                return String.valueOf(hasItemAnywhere(player, itemId));
            case "holding_":
                return String.valueOf(isHoldingItem(player, itemId));
            case "equipped_":
                return String.valueOf(isItemEquipped(player, itemId, activationSlot));
            case "item_amount_":
                return String.valueOf(countItem(player, itemId));
            default:
                return null;
        }
    }

    /**
     * Cache of the known item IDs.
     *
     * The list of IDs was copied on every placeholder request — on a server with
     * active scoreboards that is thousands of copies per second. The cache is rebuilt
     * only when the set of items has changed (a checksum of the contents is
     * compared, not just the count: a reload may replace one item with
     * another).
     */
    private java.util.Set<String> knownItemIds = java.util.Set.of();
    private int knownItemIdsChecksum;

    /**
     * Find a matching item ID from the remainder string.
     * Tries exact match first, then tries progressively shorter segments
     * (from right to left) to handle multi-word IDs like "vampire_blade".
     *
     * Both YAML items and Java API items are taken into account — previously only
     * YAML was searched, so %dci_holding_<java-item>% never worked.
     */
    private String findMatchingItemId(String remainder) {
        // Exact match
        if (plugin.getItemHandler().getCustomItem(remainder) != null) {
            return remainder;
        }
        if (plugin.getApiItemRegistry() != null
                && plugin.getApiItemRegistry().getItem(remainder) != null) {
            return remainder;
        }

        java.util.Set<String> knownIds = getKnownItemIds();

        // Try exact match first (most common case)
        if (knownIds.contains(remainder)) {
            return remainder;
        }

        // Try progressively shorter matches from the left
        // e.g., for "fire_resistance_potion", try:
        //   "fire_resistance_potion" -> exact match
        //   "fire_resistance" -> no match
        //   "fire" -> no match
        int lastUnderscore = remainder.lastIndexOf('_');
        while (lastUnderscore > 0) {
            String candidate = remainder.substring(0, lastUnderscore);
            if (knownIds.contains(candidate)) {
                return candidate;
            }
            lastUnderscore = candidate.lastIndexOf('_');
        }

        return null;
    }

    /**
     * Current set of item IDs (cached by a checksum of the contents).
     */
    private java.util.Set<String> getKnownItemIds() {
        java.util.Set<String> yamlIds = plugin.getItemHandler().getAllCustomItems().keySet();
        java.util.Set<String> javaIds = plugin.getApiItemRegistry() != null
                ? plugin.getApiItemRegistry().getItemIdsView()
                : java.util.Set.of();

        int checksum = 17;
        for (String id : yamlIds) checksum = checksum * 31 + id.hashCode();
        for (String id : javaIds) checksum = checksum * 31 + id.hashCode();

        if (checksum != knownItemIdsChecksum || knownItemIds.isEmpty()) {
            java.util.Set<String> ids = new java.util.HashSet<>(yamlIds);
            ids.addAll(javaIds);
            knownItemIds = java.util.Collections.unmodifiableSet(ids);
            knownItemIdsChecksum = checksum;
        }
        return knownItemIds;
    }

    private boolean hasItemAnywhere(Player player, String itemId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isCustomItemWithId(item, itemId)) return true;
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && isCustomItemWithId(item, itemId)) return true;
        }
        // Check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && isCustomItemWithId(offhand, itemId)) return true;
        return false;
    }

    private boolean isHoldingItem(Player player, String itemId) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (main != null && isCustomItemWithId(main, itemId)) ||
               (off != null && isCustomItemWithId(off, itemId));
    }

    private boolean isItemEquipped(Player player, String itemId, String activationSlot) {
        String slot = activationSlot == null ? "" : activationSlot.toUpperCase(java.util.Locale.ROOT);

        switch (slot) {
            case "HAND":
                return isCustomItemWithId(player.getInventory().getItemInMainHand(), itemId);
            case "OFFHAND":
                return isCustomItemWithId(player.getInventory().getItemInOffHand(), itemId);
            case "HEAD":
                return isCustomItemWithId(player.getInventory().getHelmet(), itemId);
            case "CHEST":
                return isCustomItemWithId(player.getInventory().getChestplate(), itemId);
            case "LEGS":
                return isCustomItemWithId(player.getInventory().getLeggings(), itemId);
            case "FEET":
                return isCustomItemWithId(player.getInventory().getBoots(), itemId);
            default:
                return hasItemAnywhere(player, itemId);
        }
    }

    private int countItem(Player player, String itemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isCustomItemWithId(item, itemId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean isCustomItemWithId(ItemStack item, String itemId) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = plugin.getItemHandler().getCustomItemId(item);
        return itemId.equals(id);
    }

    private String handleGlobalPlaceholder(String identifier) {
        switch (identifier.toLowerCase()) {
            case "version":
                return plugin.getDescription().getVersion();
            case "item_count":
                return String.valueOf(plugin.getItemHandler().getCustomItemCount());
            case "online":
                return String.valueOf(plugin.getServer().getOnlinePlayers().size());
            case "max_players":
                return String.valueOf(plugin.getServer().getMaxPlayers());
            default:
                return null;
        }
    }
}
