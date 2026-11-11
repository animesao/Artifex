package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.handlers.EquippedItemsChecker;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.ActionParser;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import me.dcplugin.dcustomitems.utils.ItemFX;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final Main plugin;

    // Time of the last processed click (to filter out duplicates of
    // "hand + off hand" within a single tick).
    // A WeakHashMap was a mistake here: the UUID could be garbage collected,
    // and the protection against double triggering would disappear. The entry
    // is removed explicitly on quit.
    private final Map<UUID, Long> lastProcessedClick = new HashMap<>();

    // Click cooldown per item: playerId -> itemId -> time of the last click.
    // Previously the key was only playerId, so the cooldown of one item
    // blocked another item with a different cooldown.
    private final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    private static final long MIN_CLICK_DELAY = 50L;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the global checker (lazy initialization)
     */
    private EquippedItemsChecker getChecker() {
        return plugin.getEquippedItemsChecker();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        // The global task will handle it on the next tick
        getChecker().markForRecalculation(player);

        if (plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().notifyPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        plugin.getEffectManager().stopPeriodicEffects(player);
        plugin.getAttributeManager().removeAllAttributes(player);

        lastProcessedClick.remove(playerId);
        playerCooldowns.remove(playerId);
        getChecker().onPlayerQuit(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        String customItemId = plugin.getItemHandler().getCustomItemId(item);
        if (customItemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(customItemId);
        if (customItem == null) return;

        // Permission to use the item
        if (customItem.hasPermission() && !player.hasPermission(customItem.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("items.no-permission",
                "&cYou don't have permission to use this item."));
            return;
        }

        if (event.getAction().name().contains("PHYSICAL")) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastProcessedClick.get(playerId);

        if (lastClick != null && (currentTime - lastClick) < MIN_CLICK_DELAY) {
            return;
        }

        lastProcessedClick.put(playerId, currentTime);

        boolean isRightClick = event.getAction().name().contains("RIGHT");
        boolean isLeftClick = event.getAction().name().contains("LEFT");

        ItemStack actualItem = item;
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            actualItem = player.getInventory().getItemInMainHand();
        } else if (hand == EquipmentSlot.OFF_HAND) {
            actualItem = player.getInventory().getItemInOffHand();
        }

        List<String> actions = null;
        if (isRightClick) {
            actions = customItem.getRightClickActions();
        } else if (isLeftClick) {
            actions = customItem.getLeftClickActions();
        }

        long cooldown = customItem.getClickCooldown();
        if (cooldown > 0) {
            Long lastClickTime = getCooldownTime(playerId, customItemId);
            if (lastClickTime != null && (currentTime - lastClickTime) < cooldown) {
                long remaining = cooldown - (currentTime - lastClickTime);
                double secondsLeft = remaining / 1000.0;
                if (customItem.hasCooldownMessage()) {
                    String cdMsg = ColorUtils.processMessage(player, customItem.getCooldownMessage().replace("{seconds}", String.format("%.1f", secondsLeft)));
                    if (!cdMsg.trim().isEmpty()) {
                        player.sendMessage(cdMsg);
                    }
                }
                ItemStack updated = plugin.getItemHandler().updateItemWithCooldown(actualItem, lastClickTime, cooldown);
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(updated);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(updated);
                }
                return;
            }
        }

        if (customItem.hasMaxUses()) {
            int currentUses = plugin.getItemHandler().getItemUses(actualItem);
            if (currentUses == -1) {
                currentUses = customItem.getMaxUses();
                plugin.getItemHandler().setItemUses(actualItem, currentUses);
            }

            if (currentUses <= 0) {
                event.setCancelled(true);
                String msg = customItem.hasUsesDepletedMessage()
                    ? ColorUtils.processMessage(player, customItem.getUsesDepletedMessage())
                    : plugin.getMessageManager().getMessage("items.uses-depleted", "&cThe item was used up and is gone!");
                player.sendMessage(msg);
                return;
            }
        }

        // Use event — third-party plugins can cancel the click before cooldown & uses are deducted
        me.dcplugin.dcustomitems.events.CustomItemUseEvent useEvent = new me.dcplugin.dcustomitems.events.CustomItemUseEvent(
                player, customItem,
                isRightClick
                        ? me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.RIGHT_CLICK
                        : me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.LEFT_CLICK);
        plugin.getServer().getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (cooldown > 0) {
            setCooldownTime(playerId, customItemId, currentTime);
            if (plugin.getConfig().getBoolean("items.visual-cooldown-animation", true)) {
                int cdTicks = (int) Math.max(1, cooldown / 50L);
                player.setCooldown(actualItem.getType(), cdTicks);
            }
        }

        boolean consumable = "CONSUMABLE".equals(customItem.getType());

        if (customItem.hasMaxUses()) {
            plugin.getItemHandler().decrementItemUses(actualItem);

            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(actualItem);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(actualItem);
            }

            int newUses = plugin.getItemHandler().getItemUses(actualItem);
            if (newUses <= 0) {
                if (consumable) {
                    plugin.getEffectManager().applyEffects(player, customItem);
                }
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else if (hand == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(null);
                }

                String msg = customItem.hasUsesDepletedMessage()
                    ? ColorUtils.processMessage(player, customItem.getUsesDepletedMessage())
                    : plugin.getMessageManager().getMessage("items.uses-depleted", "&cThe item was used up and is gone!");
                player.sendMessage(msg);
                return;
            }
        }

        if (consumable) {
            plugin.getEffectManager().applyEffects(player, customItem);
            consumeOne(actualItem, hand, player);
            event.setCancelled(true);
            return;
        }

        // All actions are executed by the single ActionParser
        if (actions != null && !actions.isEmpty()) {
            for (String actionStr : actions) {
                ActionParser.execute(player, actionStr);
            }
            event.setCancelled(true);
        }

        if (isRightClick) {
            plugin.getTriggerListener().executeRightClickTriggers(player, customItem);
        } else if (isLeftClick) {
            plugin.getTriggerListener().executeLeftClickTriggers(player, customItem);
        }

        if (!customItem.getEffects().isEmpty()) {
            plugin.getEffectManager().applyEffects(player, customItem);
        }

        if (cooldown > 0) {
            ItemStack updated = plugin.getItemHandler().updateItemWithCooldown(actualItem, currentTime, cooldown);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(updated);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(updated);
            }
        }
    }

    // ===== Cooldown helpers =====

    private Long getCooldownTime(UUID playerId, String itemId) {
        if (plugin.getConfig().getBoolean("cooldowns.persistent", true) && plugin.getCooldownStore() != null) {
            long remaining = plugin.getCooldownStore().remaining(playerId.toString(), itemId, System.currentTimeMillis());
            if (remaining > 0) {
                CustomItem item = plugin.getItemHandler().getCustomItem(itemId);
                long totalCd = item != null ? item.getClickCooldown() : remaining;
                return System.currentTimeMillis() - (totalCd - remaining);
            }
        }
        Map<String, Long> items = playerCooldowns.get(playerId);
        return items == null ? null : items.get(itemId);
    }

    private void setCooldownTime(UUID playerId, String itemId, long time) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(itemId, time);
        if (plugin.getConfig().getBoolean("cooldowns.persistent", true) && plugin.getCooldownStore() != null) {
            CustomItem item = plugin.getItemHandler().getCustomItem(itemId);
            long cooldown = item != null ? item.getClickCooldown() : 0;
            if (cooldown > 0) {
                plugin.getCooldownStore().put(playerId.toString(), itemId, time + cooldown);
            }
        }
    }

    /**
     * Decreases the item amount by 1 in the correct hand.
     *
     * Previously this was {@code actualItem.setAmount(amount - 1)}: with amount = 1
     * a stack with amount = 0 ended up in the slot, which occupied the slot and
     * was not picked up by the auto-pickup. Now the slot is cleared explicitly.
     */
    private void consumeOne(ItemStack actualItem, EquipmentSlot hand, Player player) {
        if (actualItem == null) return;
        int left = actualItem.getAmount() - 1;
        if (left <= 0) {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            }
            return;
        }
        actualItem.setAmount(left);
        if (hand == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(actualItem);
        } else if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(actualItem);
        }
    }

    // ===== Equipment change events → delegate to global checker =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player) {
            getChecker().markForRecalculation((org.bukkit.entity.Player) event.getWhoClicked());
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        getChecker().markForRecalculation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        getChecker().markForRecalculation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(droppedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                ItemFX.spawnEquipEffects(plugin, player, customItem, false);
            }
        }
        getChecker().markForRecalculation(player);
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(pickedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null) {
                // Set the acquisition time for duration items
                if (plugin.getItemHandler().ensureAcquiredTime(pickedItem)) {
                    event.getItem().setItemStack(pickedItem);
                }

                // Check the world restrictions
                if (!customItem.isAllowedInWorld(player.getWorld().getName())) {
                    event.setCancelled(true);
                    player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                        plugin.getMessageManager().getMessage("items.world-blocked", "&cItem {item} is banned in this world!")
                            .replace("{item}", customItem.getId())));
                    return;
                }

                ItemFX.spawnEquipEffects(plugin, player, customItem, true);
            } else if (plugin.getApiItemRegistry() != null) {
                // Java API item: the world is checked as well and the acquisition time is set
                me.dcplugin.dcustomitems.api.AbstractCustomItem javaItem =
                        plugin.getApiItemRegistry().getItem(itemId);
                if (javaItem != null) {
                    if (plugin.getItemHandler().ensureAcquiredTime(pickedItem)) {
                        event.getItem().setItemStack(pickedItem);
                    }
                    if (!javaItem.isAllowedInWorld(player.getWorld().getName())) {
                        event.setCancelled(true);
                        player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                            javaItem.getWorldBlockedMessage() != null
                                    ? javaItem.getWorldBlockedMessage()
                                    : plugin.getMessageManager().getMessage("items.world-blocked",
                                            "&cItem {item} is banned in this world!")
                                            .replace("{item}", itemId)));
                        return;
                    }
                }
            }
        }
        getChecker().markForRecalculation(player);
    }
}