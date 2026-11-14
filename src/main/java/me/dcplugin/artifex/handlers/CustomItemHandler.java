package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom item manager.
 * Delegates loading to ItemLoader, lore to LoreManager, uses to UsesManager.
 */
public class CustomItemHandler {

    private final Main plugin;

    /**
     * The loaded items.
     *
     * volatile + atomic map replacement: a reload swaps the whole map,
     * and readers (including the PlaceholderAPI thread) see either the old
     * or the new one — never a partially filled one. The ConcurrentHashMap
     * inside gives a "weakly consistent" iteration without ConcurrentModificationException.
     */
    private volatile Map<String, CustomItem> customItems = new ConcurrentHashMap<>();

    private final NamespacedKey customItemKey;
    private final NamespacedKey itemUsesKey;
    private final NamespacedKey itemModelKey;
    private final NamespacedKey acquiredTimeKey;
    private final NamespacedKey[] legacyCustomItemKeys;
    private final NamespacedKey[] legacyAcquiredTimeKeys;

    // Delegates
    private final ItemLoader itemLoader;
    private final LoreManager loreManager;
    private final UsesManager usesManager;
    private final RecipeManager recipeManager;

    public CustomItemHandler(Main plugin) {
        this.plugin = plugin;
        this.customItemKey = new NamespacedKey(plugin, "custom_item_id");
        this.itemUsesKey = new NamespacedKey(plugin, "item_uses");
        this.itemModelKey = new NamespacedKey(plugin, "item_model");

        this.legacyCustomItemKeys = new NamespacedKey[] {
            new NamespacedKey("dcustomitems", "custom_item_id"),
            new NamespacedKey("dc-customitems", "custom_item_id")
        };
        this.legacyAcquiredTimeKeys = new NamespacedKey[] {
            new NamespacedKey("dcustomitems", "acquired_time"),
            new NamespacedKey("dc-customitems", "acquired_time")
        };

        this.itemLoader = new ItemLoader(plugin, customItemKey, itemModelKey);
        this.loreManager = new LoreManager(plugin);
        this.usesManager = new UsesManager(itemUsesKey);
        this.recipeManager = new RecipeManager(plugin);

        this.acquiredTimeKey = new NamespacedKey(plugin, "acquired_time");
    }

    // ===== Loading =====

    public void loadCustomItems() {
        // Load into a new map and swap it atomically: readers never see
        // a half-empty set of items.
        Map<String, CustomItem> loaded = new ConcurrentHashMap<>(itemLoader.loadAll());
        customItems = loaded;

        recipeManager.clear();

        // Register the crafting recipes after all items are loaded:
        // the ingredients may reference other custom items.
        for (CustomItem item : loaded.values()) {
            if (item.hasRecipes()) {
                recipeManager.register(item);
            }
        }

        plugin.getLogger().info("Loaded " + customItems.size() + " custom items");
    }

    public void reloadItems() {
        loadCustomItems();
    }

    /**
     * The crafting recipe manager (the recipes section of a YAML item).
     */
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    // ===== Item lookup =====

    public CustomItem getCustomItem(String itemId) {
        return customItems.get(itemId);
    }

    public String getCustomItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return null;
        var pdc = meta.getPersistentDataContainer();
        String id = pdc.get(customItemKey, PersistentDataType.STRING);
        if (id != null) return id;
        for (NamespacedKey legacyKey : legacyCustomItemKeys) {
            id = pdc.get(legacyKey, PersistentDataType.STRING);
            if (id != null) return id;
        }
        return null;
    }

    /**
     * All loaded YAML items (read-only).
     *
     * Previously a copy of the map was returned here — it was created on every
     * call, and the method was invoked from the PlaceholderAPI placeholders
     * and from /ci validate. Now this is a live read-only view.
     *
     * The map is replaced as a whole on reload (atomically), and inside it is
     * a ConcurrentHashMap, so iteration from another thread (PlaceholderAPI)
     * is safe and does not throw ConcurrentModificationException.
     */
    public Map<String, CustomItem> getAllCustomItems() {
        return Collections.unmodifiableMap(customItems);
    }

    /**
     * The number of loaded YAML items (without copying the map).
     */
    public int getCustomItemCount() {
        return customItems.size();
    }

    public boolean isCustomItem(ItemStack itemStack) {
        return getCustomItemId(itemStack) != null;
    }

    // ===== Lore =====

    /**
     * Update the item lore with the %cooldown% and %uses% placeholders and player PAPI context
     */
    public ItemStack updateItemWithCooldown(Player player, ItemStack itemStack, long lastClickTime, long cooldownMs) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return itemStack;

        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null) return itemStack;

        long now = System.currentTimeMillis();
        long remaining = 0;
        if (lastClickTime > 0 && cooldownMs > 0) {
            long elapsed = now - lastClickTime;
            remaining = Math.max(0, cooldownMs - elapsed);
        }

        return loreManager.applyCooldownLore(player, itemStack, customItem, remaining);
    }

    /**
     * Update the item lore with the %cooldown% and %uses% placeholders
     */
    public ItemStack updateItemWithCooldown(ItemStack itemStack, long lastClickTime, long cooldownMs) {
        return updateItemWithCooldown(null, itemStack, lastClickTime, cooldownMs);
    }

    /**
     * Update the item lore with the %uses% placeholder and player PAPI context
     */
    public ItemStack updateItemWithUses(Player player, ItemStack itemStack) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return itemStack;

        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) return itemStack;

        int uses = getItemUses(itemStack);
        if (uses == -1) {
            uses = customItem.getMaxUses();
            setItemUses(itemStack, uses);
        }

        return loreManager.applyUsesLore(player, itemStack, customItem, uses);
    }

    /**
     * Update the item lore with the %uses% placeholder
     */
    public ItemStack updateItemWithUses(ItemStack itemStack) {
        return updateItemWithUses(null, itemStack);
    }

    // ===== Uses =====

    public int getItemUses(ItemStack itemStack) {
        int uses = usesManager.getUses(itemStack);
        if (uses != -1) return uses;

        // Fallback: return maxUses from CustomItem
        String customItemId = getCustomItemId(itemStack);
        if (customItemId != null) {
            CustomItem customItem = getCustomItem(customItemId);
            if (customItem != null && customItem.hasMaxUses()) {
                return customItem.getMaxUses();
            }
        }
        return -1;
    }

    public void setItemUses(ItemStack itemStack, int uses) {
        usesManager.setUses(itemStack, uses);
    }

    // ===== Uses without a YAML model (for Java API items) =====

    /**
     * The raw use counter from the PDC (-1 if not set).
     * Does not depend on the YAML model — it also works for AbstractCustomItem.
     */
    public int getStoredUses(ItemStack itemStack) {
        return usesManager.getUses(itemStack);
    }

    /**
     * Write the use counter into the PDC (for Java API items).
     */
    public void setStoredUses(ItemStack itemStack, int uses) {
        usesManager.setUses(itemStack, uses);
    }

    /**
     * Decrease the use counter by 1 without relying on the YAML model.
     * Returns the new value (or -1 if the counter was not set).
     */
    public int decrementStoredUses(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return -1;
        int uses = usesManager.getUses(itemStack);
        if (uses == -1) return -1;
        uses = Math.max(0, uses - 1);
        usesManager.setUses(itemStack, uses);
        return uses;
    }

    public void decrementItemUses(Player player, ItemStack itemStack) {
        String customItemId = getCustomItemId(itemStack);
        if (customItemId == null) return;

        CustomItem customItem = getCustomItem(customItemId);
        if (customItem == null || !customItem.hasMaxUses()) return;

        int uses = usesManager.decrementUses(itemStack, customItem);
        if (uses >= 0) {
            loreManager.applyUsesLore(player, itemStack, customItem, uses);
        }
    }

    public void decrementItemUses(ItemStack itemStack) {
        decrementItemUses(null, itemStack);
    }

    // ===== Duration =====

    /**
     * Set the acquisition time of the item (if not set yet).
     *
     * @return true if the item was changed
     */
    public boolean ensureAcquiredTime(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        if (pdc.has(acquiredTimeKey, PersistentDataType.LONG)) {
            return false;
        }
        for (NamespacedKey legacyKey : legacyAcquiredTimeKeys) {
            if (pdc.has(legacyKey, PersistentDataType.LONG)) return false;
        }
        pdc.set(acquiredTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        itemStack.setItemMeta(meta);
        return true;
    }

    /**
     * Get the remaining lifetime of the item (in seconds). -1 = permanent.
     */
    public long getRemainingDuration(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return -1;
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return -1;
        var pdc = meta.getPersistentDataContainer();
        Long acquiredTime = pdc.get(acquiredTimeKey, PersistentDataType.LONG);
        if (acquiredTime == null) {
            for (NamespacedKey legacyKey : legacyAcquiredTimeKeys) {
                acquiredTime = pdc.get(legacyKey, PersistentDataType.LONG);
                if (acquiredTime != null) break;
            }
        }
        if (acquiredTime == null) return -1;

        String itemId = getCustomItemId(itemStack);
        if (itemId == null) return -1;
        CustomItem customItem = getCustomItem(itemId);
        if (customItem == null || !customItem.hasDuration()) return -1;

        long elapsed = (System.currentTimeMillis() - acquiredTime) / 1000;
        return Math.max(0, customItem.getDuration() - elapsed);
    }

    /**
     * Check whether the item lifetime has expired
     */
    public boolean isExpired(ItemStack itemStack) {
        return getRemainingDuration(itemStack) == 0;
    }
}
