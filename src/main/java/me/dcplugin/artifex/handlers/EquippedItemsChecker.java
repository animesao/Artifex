package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.events.CustomItemEquipEvent;
import me.dcplugin.artifex.events.CustomItemPeriodicEvent;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import me.dcplugin.artifex.utils.EnumCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Global equipped-items check task.
 * Replaces the per-player runTaskLater with a single global BukkitRunnable.
 * Each player is checked no more than once every RECALC_COOLDOWN_MS.
 */
public class EquippedItemsChecker extends BukkitRunnable {

    private final Main plugin;
    private static final long RECALC_COOLDOWN_MS = 150; // 150ms between recalculations for a single player

    // Slot indices in the id array, one row per slot.
    private static final int SLOT_HAND = 0;
    private static final int SLOT_OFFHAND = 1;
    private static final int SLOT_FEET = 2;
    private static final int SLOT_LEGS = 3;
    private static final int SLOT_CHEST = 4;
    private static final int SLOT_HEAD = 5;
    private static final int SLOT_COUNT = 6;

    private static final String[] ARMOR_SLOTS = {"FEET", "LEGS", "CHEST", "HEAD"};

    // Per-player state
    private final Map<UUID, Long> lastRecalcTime = new HashMap<>();
    private final Map<UUID, String> lastEquipmentHash = new HashMap<>();
    private final Map<UUID, Set<String>> previousActiveItems = new HashMap<>();
    private final Set<UUID> processingPlayers = new HashSet<>();

    // Snapshot of the item IDs in the player's slots (updated no more than once
    // every RECALC_COOLDOWN_MS). Needed to avoid reading the PDC every tick.
    private final Map<UUID, SlotSnapshot> slotCache = new HashMap<>();

    // Trail particles: tick counter per player
    private final Map<UUID, Integer> trailTickCounter = new HashMap<>();
    // World tracking for per-item world restrictions
    private final Map<UUID, String> lastWorldName = new HashMap<>();

    // Global tick counter: expensive checks are not performed every tick
    private int globalTick = 0;
    private static final int DURATION_CHECK_INTERVAL = 20;  // once per second
    private static final int WORLD_CHECK_INTERVAL = 20;     // once per second

    // Java API items with getPeriodicInterval() > 0: id -> item
    // (updated once per second; while empty the periodic cycle does not run)
    private final Map<String, AbstractCustomItem> periodicJavaItems = new HashMap<>();

    // Whether the loaded YAML items have trail particles.
    // Updated once per second together with periodicJavaItems — so that the
    // PDC is not read for an empty result.
    private boolean trailItemsPresent = false;

    /** Cached key: NamespacedKey validates and lowercases on every call. */
    private final org.bukkit.NamespacedKey acquiredTimeKey;

    public EquippedItemsChecker(Main plugin) {
        this.plugin = plugin;
        this.acquiredTimeKey = new org.bukkit.NamespacedKey(plugin, "acquired_time");
    }

    /**
     * Start the global task (call once at startup)
     */
    public void start() {
        this.runTaskTimer(plugin, 1L, 1L); // Every tick
    }

    /**
     * Drops the cached equipment state.
     *
     * <p>Must be called after a reload: the equipment hash is built from the
     * item IDs, so after the YAML definitions change the hash stays the same
     * and the new effects/attributes would never be applied.
     */
    public void invalidateAll() {
        lastEquipmentHash.clear();
        slotCache.clear();
        previousActiveItems.clear();
    }

    /**
     * Mark a player for recalculation (called from events)
     */
    public void markForRecalculation(Player player) {
        if (player == null) return;
        // The 150 ms gate is deliberately NOT reset here: the equipment hash
        // detects any change by itself, and resetting the timer made every
        // inventory event re-read the PDC of all six slots on the next tick
        // (3x the work). Changes are still noticed within 150 ms.
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        globalTick++;

        Collection<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers();

        // === Equipment recalculation (only when the slot contents change) ===
        for (Player player : onlinePlayers) {
            UUID playerId = player.getUniqueId();

            // Check the processing flag
            if (processingPlayers.contains(playerId)) {
                continue;
            }

            // The slots are read no more than once every RECALC_COOLDOWN_MS.
            // lastRecalcTime is updated here and not only when the slot
            // contents change: otherwise the hash check (and 6 PDC reads)
            // would run every tick for a player with unchanged equipment —
            // that is 20 times per second instead of 6.7.
            SlotSnapshot snapshot = refreshSlots(player, playerId, now);
            if (snapshot == null) continue;

            // The hash is built together with the snapshot: the slots only
            // change when the snapshot is refreshed, so rebuilding the hash
            // every tick was pure waste (20 StringBuilders per second and
            // player).
            String lastHash = lastEquipmentHash.get(playerId);

            // Nothing changed — skip
            if (Objects.equals(snapshot.hash, lastHash)) {
                continue;
            }

            // Recalculate
            processingPlayers.add(playerId);
            try {
                lastEquipmentHash.put(playerId, snapshot.hash);
                recalculateEquipment(player, playerId, snapshot.ids);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Error while updating equipment for " + player.getName() + ": " + e.getMessage(), e);
            } finally {
                processingPlayers.remove(playerId);
            }
        }

        // === Trail particles + Java API periodic hooks (every tick) ===
        if (!periodicJavaItems.isEmpty() || hasTrailItems()) {
            for (Player player : onlinePlayers) {
                UUID playerId = player.getUniqueId();
                int tick = trailTickCounter.getOrDefault(playerId, 0) + 1;
                trailTickCounter.put(playerId, tick);
                // We use the snapshot from the first pass — it is not older than
                // RECALC_COOLDOWN_MS, so there is no extra PDC read.
                SlotSnapshot snapshot = slotCache.get(playerId);
                if (snapshot == null) continue;
                spawnTrailParticles(player, snapshot.ids, tick);
                runJavaPeriodic(player, snapshot.ids, tick);
            }
        } else {
            // Neither trails nor periodic effects — the tick counter is still
            // needed (it is used as the base for the intervals), but the PDC
            // does not have to be read.
            for (Player player : onlinePlayers) {
                UUID playerId = player.getUniqueId();
                trailTickCounter.put(playerId, trailTickCounter.getOrDefault(playerId, 0) + 1);
            }
        }

        // Cache of Java items with periodic effects — once per second
        if (globalTick % DURATION_CHECK_INTERVAL == 0) {
            refreshPeriodicJavaItems();
        }

        // === Duration check (once per second, not every tick) ===
        if (globalTick % DURATION_CHECK_INTERVAL == 0) {
            for (Player player : onlinePlayers) {
                checkItemDurations(player);
            }
        }

        // === World restriction check (once per second and only on a world change) ===
        if (globalTick % WORLD_CHECK_INTERVAL == 0) {
            for (Player player : onlinePlayers) {
                UUID playerId = player.getUniqueId();
                String currentWorld = player.getWorld().getName();
                String prevWorld = lastWorldName.get(playerId);
                // null (the first check) also counts as a change —
                // otherwise items banned in the current world would stay
                // with the player until the world changes.
                if (!currentWorld.equals(prevWorld)) {
                    checkWorldRestrictions(player);
                }
                lastWorldName.put(playerId, currentWorld);
            }
        }
    }

    /**
     * Whether the loaded items have trail particles.
     * It is checked every tick, so a flag is kept and refreshed once per second.
     */
    private boolean hasTrailItems() {
        return trailItemsPresent;
    }

    // ===== Reading the slots =====

    /**
     * Snapshot of the item IDs in the player's slots.
     */
    private static final class SlotSnapshot {
        private final String[] ids;
        /** Hash of the IDs — built once, together with the snapshot. */
        private final String hash;

        private SlotSnapshot(String[] ids) {
            this.ids = ids;
            this.hash = buildHash(ids);
        }
    }

    /**
     * Returns the current snapshot of the player's slots, refreshing it no more
     * often than once every RECALC_COOLDOWN_MS.
     *
     * Any change is detected by the hash inside the snapshot, so
     * markForRecalculation() does not have to break the 150 ms gate.
     */
    private SlotSnapshot refreshSlots(Player player, UUID playerId, long now) {
        SlotSnapshot cached = slotCache.get(playerId);
        Long lastRecalc = lastRecalcTime.get(playerId);
        boolean stale = cached == null
                || lastRecalc == null
                || (now - lastRecalc) >= RECALC_COOLDOWN_MS;

        if (!stale) return cached;

        SlotSnapshot snapshot = new SlotSnapshot(readSlotIds(player));
        slotCache.put(playerId, snapshot);
        lastRecalcTime.put(playerId, now);
        return snapshot;
    }

    /**
     * Reads the IDs of the custom items from all slots of the player.
     *
     * A single pass over the PDC instead of several: getItemMeta() clones the
     * meta item, and there were more than ten calls per player per tick
     * (hash + checkSlot + trails + periodic).
     */
    private String[] readSlotIds(Player player) {
        String[] ids = new String[SLOT_COUNT];
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();

        ids[SLOT_HAND] = getItemId(inventory.getItemInMainHand());
        ids[SLOT_OFFHAND] = getItemId(inventory.getItemInOffHand());

        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length && i < ARMOR_SLOTS.length; i++) {
            ids[SLOT_FEET + i] = getItemId(armor[i]);
        }
        return ids;
    }

    private String getItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        return plugin.getItemHandler().getCustomItemId(item);
    }

    /**
     * Full equipment recalculation for the player
     */
    private void recalculateEquipment(Player player, UUID playerId, String[] slotIds) {
        // Remove the old effects and attributes
        plugin.getEffectManager().removeAllEffectsFromPlayer(player);
        plugin.getAttributeManager().removeAllAttributes(player);

        Map<PotionEffectType, Integer> totalEffects = new HashMap<>();
        Map<org.bukkit.attribute.Attribute, Double> totalAttributes = new HashMap<>();
        Set<String> activeSets = new HashSet<>();
        Set<String> currentActiveItemIds = new HashSet<>();

        // Check the hands and the armor (the IDs are already read — no repeated PDC reads)
        checkSlot(player, slotIds[SLOT_HAND], "HAND", totalEffects, totalAttributes, currentActiveItemIds, activeSets);
        checkSlot(player, slotIds[SLOT_OFFHAND], "OFFHAND", totalEffects, totalAttributes, currentActiveItemIds, activeSets);
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            checkSlot(player, slotIds[SLOT_FEET + i], ARMOR_SLOTS[i], totalEffects, totalAttributes, currentActiveItemIds, activeSets);
        }

        // Set bonuses
        for (String setId : activeSets) {
            if (plugin.getArmorSetManager().hasFullSet(player, setId)) {
                plugin.getEffectManager().addSetBonusEffectsToMap(totalEffects, setId);
            }
        }

        // Apply the effects and attributes
        plugin.getEffectManager().applyEffectsFromMap(player, totalEffects);
        plugin.getAttributeManager().applyAttributesFromMap(player, totalAttributes);

        // Determine the equipped/unequipped items
        Set<String> previousIds = previousActiveItems.getOrDefault(playerId, new HashSet<>());
        Set<String> newlyEquipped = new HashSet<>(currentActiveItemIds);
        newlyEquipped.removeAll(previousIds);
        Set<String> newlyUnequipped = new HashSet<>(previousIds);
        newlyUnequipped.removeAll(currentActiveItemIds);

        // Effects for the equipped items (YAML + Java API)
        for (String itemId : newlyEquipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Event for third-party plugins (can cancel the standard effects)
                CustomItemEquipEvent equipEvent = new CustomItemEquipEvent(player, customItem, true);
                plugin.getServer().getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) continue;

                me.dcplugin.artifex.utils.ItemFX.spawnEquipEffects(plugin, player, customItem, true);
                if (customItem.hasEquipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getEquipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeEquipTriggers(player, customItem);
                continue;
            }

            // Java API item: event (can be cancelled) + the onEquip hook
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null) {
                CustomItemEquipEvent equipEvent = new CustomItemEquipEvent(player, javaItem, true);
                plugin.getServer().getPluginManager().callEvent(equipEvent);
                if (equipEvent.isCancelled()) continue;
                try {
                    javaItem.onEquip(player);
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Error in onEquip for '" + itemId + "' for " + player.getName() + ": " + e.getMessage(), e);
                }
            }
        }

        // Effects for the unequipped items (YAML + Java API)
        for (String itemId : newlyUnequipped) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Event for third-party plugins
                CustomItemEquipEvent unequipEvent = new CustomItemEquipEvent(player, customItem, false);
                plugin.getServer().getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) continue;

                me.dcplugin.artifex.utils.ItemFX.spawnEquipEffects(plugin, player, customItem, false);
                if (customItem.hasUnequipMessage()) {
                    String msg = ColorUtils.processMessage(player, customItem.getUnequipMessage());
                    if (!msg.trim().isEmpty()) player.sendMessage(msg);
                }
                plugin.getTriggerListener().executeUnequipTriggers(player, customItem);
                continue;
            }

            // Java API item: event (can be cancelled) + the onUnequip hook
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null) {
                CustomItemEquipEvent unequipEvent = new CustomItemEquipEvent(player, javaItem, false);
                plugin.getServer().getPluginManager().callEvent(unequipEvent);
                if (unequipEvent.isCancelled()) continue;
                try {
                    javaItem.onUnequip(player);
                } catch (Exception e) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                            "Error in onUnequip for '" + itemId + "' for " + player.getName() + ": " + e.getMessage(), e);
                }
            }
        }

        previousActiveItems.put(playerId, currentActiveItemIds);
    }

    private void checkSlot(Player player, String itemId, String slotType,
                           Map<PotionEffectType, Integer> totalEffects,
                           Map<org.bukkit.attribute.Attribute, Double> totalAttributes,
                           Set<String> currentActiveItemIds,
                           Set<String> activeSets) {
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null) {
            // Java API item (no YAML model): only counted in its own slot
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null && slotType.equalsIgnoreCase(javaItem.getActivationSlot())) {
                currentActiveItemIds.add(itemId);
            }
            return;
        }

        // The item's own permission gates its passive bonuses too — otherwise
        // a player without the right would still get the effects, attributes
        // and triggers of an item he picked up.
        if (!customItem.isUsableBy(player)) {
            return;
        }

        if (slotType.equalsIgnoreCase(customItem.getActivationSlot())) {
            plugin.getEffectManager().addEffectsToMap(totalEffects, customItem);
            plugin.getAttributeManager().addAttributesToMap(totalAttributes, customItem);
            currentActiveItemIds.add(itemId);
            if (customItem.getArmorSetId() != null) {
                activeSets.add(customItem.getArmorSetId());
            }
        }
    }

    private static String buildHash(String[] slotIds) {
        StringBuilder hash = new StringBuilder(128);
        hash.append("MAIN:").append(slotIds[SLOT_HAND]).append(';');
        hash.append("OFF:").append(slotIds[SLOT_OFFHAND]).append(';');
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            hash.append(ARMOR_SLOTS[i]).append(':').append(slotIds[SLOT_FEET + i]).append(';');
        }
        return hash.toString();
    }

    // === Trail particles ===

    private void spawnTrailParticles(Player player, String[] slotIds, int tick) {
        spawnTrailForItem(player, slotIds[SLOT_HAND], tick);
        spawnTrailForItem(player, slotIds[SLOT_OFFHAND], tick);
    }

    private void spawnTrailForItem(Player player, String itemId, int tick) {
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTrailParticles()) return;

        int interval = customItem.getTrailParticleInterval();
        if (interval <= 0 || tick % interval != 0) return;

        for (String particleStr : customItem.getTrailParticles()) {
            try {
                String[] parts = particleStr.split(":");
                org.bukkit.Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 0.5, 0), count, 0.3, 0.3, 0.3);
            } catch (Exception e) {
                plugin.getLogger().warning("Error while spawning trail particles: " + particleStr);
            }
        }
    }

    // === Java API periodic hooks (onPeriodic) ===

    /**
     * Cache of Java items with getPeriodicInterval() > 0 (once per second).
     */
    private void refreshPeriodicJavaItems() {
        periodicJavaItems.clear();
        for (AbstractCustomItem item : plugin.getApiItemRegistry().getAllItems().values()) {
            if (item != null && item.getPeriodicInterval() > 0) {
                periodicJavaItems.put(item.getId(), item);
            }
        }

        // Whether there are items with trail particles — so that the PDC is not read for nothing
        trailItemsPresent = false;
        for (CustomItem item : plugin.getItemHandler().getAllCustomItems().values()) {
            if (item != null && item.hasTrailParticles()) {
                trailItemsPresent = true;
                break;
            }
        }
    }

    /**
     * Invocation of onPeriodic for the equipped Java items according to their interval.
     * An empty cache = return without a single PDC read.
     */
    private void runJavaPeriodic(Player player, String[] slotIds, int tick) {
        if (periodicJavaItems.isEmpty()) return;

        for (Map.Entry<String, AbstractCustomItem> entry : periodicJavaItems.entrySet()) {
            AbstractCustomItem item = entry.getValue();
            long interval = item.getPeriodicInterval();
            if (interval <= 0 || tick % interval != 0) continue;

            // The item must be in its activation slot
            int slotIndex = slotIndex(item.getActivationSlot());
            if (slotIndex < 0) continue;
            String slotId = slotIds[slotIndex];
            if (slotId == null || !slotId.equals(entry.getKey())) continue;

            // Bukkit event: cancelling skips this onPeriodic call
            CustomItemPeriodicEvent periodicEvent = new CustomItemPeriodicEvent(player, item, interval);
            plugin.getServer().getPluginManager().callEvent(periodicEvent);
            if (periodicEvent.isCancelled()) continue;

            try {
                item.onPeriodic(player);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Error in onPeriodic for '" + entry.getKey() + "' for " + player.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    private static int slotIndex(String activationSlot) {
        if (activationSlot == null) return -1;
        switch (activationSlot.toUpperCase(java.util.Locale.ROOT)) {
            case "HAND": return SLOT_HAND;
            case "OFFHAND": return SLOT_OFFHAND;
            case "FEET": return SLOT_FEET;
            case "LEGS": return SLOT_LEGS;
            case "CHEST": return SLOT_CHEST;
            case "HEAD": return SLOT_HEAD;
            default: return -1;
        }
    }

    // === Duration check ===

    private void checkItemDurations(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                if (!customItem.hasDuration()) continue;

                // Убеждаемся, что время получения записано.
                // getContents() отдаёт КОПИИ стеков, поэтому запись
                // acquired_time в копию без setContents() терялась — и
                // предмет с duration: никогда не истекал.
                if (plugin.getItemHandler().ensureAcquiredTime(item)) {
                    changed = true;
                }

                if (plugin.getItemHandler().isExpired(item)) {
                    contents[i] = null;
                    changed = true;

                    // Send message
                    if (customItem.hasMaxDurationMessage()) {
                        String msg = ColorUtils.processMessage(player, customItem.getMaxDurationMessage());
                        if (!msg.trim().isEmpty()) player.sendMessage(msg);
                    } else {
                        player.sendMessage(ColorUtils.processMessage(player,
                            plugin.getMessageManager().getMessage("items.expired", "&cItem {item} has expired!")
                                .replace("{item}", customItem.getId())));
                    }

                    // Play sound and particles
                    player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                        player.getLocation().add(0, 1, 0), 20);
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
                continue;
            }

            // Java API item: getDuration() in seconds (0 = permanent)
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem == null || javaItem.getDuration() <= 0) continue;

            if (plugin.getItemHandler().ensureAcquiredTime(item)) {
                changed = true;
            }
            Long acquired = getAcquiredTime(item);
            // The acquisition time is unknown — the item is not considered expired.
            // Previously getDuration() was substituted here, which made the item
            // get removed on the very first tick.
            if (acquired == null) continue;

            long elapsed = (System.currentTimeMillis() - acquired) / 1000L;
            if (elapsed >= javaItem.getDuration()) {
                contents[i] = null;
                changed = true;
                player.sendMessage(ColorUtils.processMessage(player,
                    javaItem.getDurationExpiredMessage() != null
                            ? javaItem.getDurationExpiredMessage()
                            : plugin.getMessageManager().getMessage("items.expired", "&cItem {item} has expired!")
                        .replace("{item}", itemId)));
                player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, player.getLocation().add(0, 1, 0), 20);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
            markForRecalculation(player);
        }
    }

    // === World restriction check ===

    /**
     * Check whether the player's items are allowed in the current world
     */
    public void checkWorldRestrictions(Player player) {
        String worldName = player.getWorld().getName();
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String itemId = plugin.getItemHandler().getCustomItemId(item);
            if (itemId == null) continue;

            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                if (!customItem.isAllowedInWorld(worldName)) {
                    contents[i] = null;
                    changed = true;
                    player.sendMessage(ColorUtils.processMessage(player,
                        plugin.getMessageManager().getMessage("items.world-blocked", "&cItem {item} is banned in this world!")
                            .replace("{item}", customItem.getId())));
                }
                continue;
            }

            // Java API item: isAllowedInWorld(String)
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem != null && !javaItem.isAllowedInWorld(worldName)) {
                contents[i] = null;
                changed = true;
                player.sendMessage(ColorUtils.processMessage(player,
                    javaItem.getWorldBlockedMessage() != null
                            ? javaItem.getWorldBlockedMessage()
                            : plugin.getMessageManager().getMessage("items.world-blocked", "&cItem {item} is banned in this world!")
                        .replace("{item}", itemId)));
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
            markForRecalculation(player);
        }
    }

    /**
     * The item acquisition time from the PDC (null if not set).
     */
    private Long getAcquiredTime(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(acquiredTimeKey,
                org.bukkit.persistence.PersistentDataType.LONG);
    }

    /**
     * Cleanup when a player leaves
     */
    public void onPlayerQuit(UUID playerId) {
        lastRecalcTime.remove(playerId);
        lastEquipmentHash.remove(playerId);
        previousActiveItems.remove(playerId);
        processingPlayers.remove(playerId);
        trailTickCounter.remove(playerId);
        lastWorldName.remove(playerId);
        slotCache.remove(playerId);
    }

    /**
     * Full stop of the task
     */
    @Override
    public void cancel() {
        super.cancel();
        lastRecalcTime.clear();
        lastEquipmentHash.clear();
        previousActiveItems.clear();
        processingPlayers.clear();
        trailTickCounter.clear();
        lastWorldName.clear();
        slotCache.clear();
        periodicJavaItems.clear();
    }
}
