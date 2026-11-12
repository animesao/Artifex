package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Management of lore templates for items:
 * substitution of %cooldown% and %uses% in the item lore.
 */
public class LoreManager {

    private final Main plugin;

    /** Cached key: NamespacedKey validates and lowercases on every call. */
    private final org.bukkit.NamespacedKey itemUsesKey;

    public LoreManager(Main plugin) {
        this.plugin = plugin;
        this.itemUsesKey = new org.bukkit.NamespacedKey(plugin, "item_uses");
    }

    /**
     * Update the item lore with the %cooldown% and %uses% placeholders and player PAPI context.
     *
     * @param player       the player (for PAPI placeholders, may be null)
     * @param itemStack    the item
     * @param customItem   the custom item
     * @param cooldownStr  the cooldown string (e.g. "3.5")
     * @param uses         the current number of uses
     */
    public ItemStack applyLoreTemplate(Player player, ItemStack itemStack, CustomItem customItem, String cooldownStr, int uses) {
        List<String> loreTemplate = customItem.getLoreTemplate();
        if (loreTemplate == null || loreTemplate.isEmpty()) return itemStack;

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        List<String> newLore = new ArrayList<>();
        for (String line : loreTemplate) {
            String processed = line.replace("%cooldown%", cooldownStr);
            if (customItem.hasMaxUses()) {
                processed = processed.replace("%uses%", String.valueOf(uses));
            } else {
                processed = processed.replace("%uses%", "∞");
            }
            if (player != null) {
                newLore.add(ColorUtils.processMessage(player, processed));
            } else {
                newLore.add(ColorUtils.colorize(processed));
            }
        }
        meta.setLore(newLore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Update the item lore with the %cooldown% and %uses% placeholders
     *
     * @param itemStack    the item
     * @param customItem   the custom item
     * @param cooldownStr  the cooldown string (e.g. "3.5")
     * @param uses         the current number of uses
     */
    public ItemStack applyLoreTemplate(ItemStack itemStack, CustomItem customItem, String cooldownStr, int uses) {
        return applyLoreTemplate(null, itemStack, customItem, cooldownStr, uses);
    }

    /**
     * Update the lore with the cooldown (uses stays unchanged)
     */
    public ItemStack applyCooldownLore(Player player, ItemStack itemStack, CustomItem customItem, long remainingMs) {
        double seconds = remainingMs / 1000.0;
        String cooldownStr = String.format(Locale.US, "%.1f", seconds);

        // Get the current uses from the item meta (if present).
        // The key must match the one used by UsesManager
        // (new NamespacedKey(plugin, "item_uses")): a manually specified
        // namespace did not match the plugin namespace, and %uses% always
        // showed the maximum instead of the remaining count.
        int uses = -1;
        if (customItem.hasMaxUses()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                Integer storedUses = meta.getPersistentDataContainer().get(
                        itemUsesKey,
                        org.bukkit.persistence.PersistentDataType.INTEGER);
                uses = storedUses != null ? storedUses : customItem.getMaxUses();
            }
        }

        return applyLoreTemplate(player, itemStack, customItem, cooldownStr, uses);
    }

    /**
     * Update the lore with the cooldown (uses stays unchanged)
     */
    public ItemStack applyCooldownLore(ItemStack itemStack, CustomItem customItem, long remainingMs) {
        return applyCooldownLore(null, itemStack, customItem, remainingMs);
    }

    /**
     * Update the lore with the number of uses (cooldown = 0.0)
     */
    public ItemStack applyUsesLore(Player player, ItemStack itemStack, CustomItem customItem, int uses) {
        return applyLoreTemplate(player, itemStack, customItem, "0.0", uses);
    }

    /**
     * Update the lore with the number of uses (cooldown = 0.0)
     */
    public ItemStack applyUsesLore(ItemStack itemStack, CustomItem customItem, int uses) {
        return applyUsesLore(null, itemStack, customItem, uses);
    }
}
