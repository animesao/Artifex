package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener that forwards Bukkit events to AbstractCustomItem.
 */
public class ApiEventListener implements Listener {

    private final Main plugin;
    private final ItemRegistry registry;
    // Cooldown per player AND item: one item's cooldown must not block another
    private final Map<UUID, Map<String, Long>> lastClicks = new HashMap<>();

    public ApiEventListener(Main plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem == null) return;

        boolean isRightClick = event.getAction().name().contains("RIGHT");
        boolean isLeftClick = event.getAction().name().contains("LEFT");
        if (!isRightClick && !isLeftClick) return; // PHYSICAL and similar — not an item click

        // Usage permission (same as for YAML items)
        String permission = customItem.getPermission();
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            event.setCancelled(true);
            player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                    plugin.getMessageManager().getMessage("items.no-permission",
                            "&cYou don't have permission to use this item.")));
            return;
        }

        // Cooldown
        long cooldown = customItem.getClickCooldown();
        if (cooldown > 0) {
            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastClick = lastClicks
                    .computeIfAbsent(playerId, k -> new HashMap<>())
                    .get(customItem.getId());
            if (lastClick != null && (now - lastClick) < cooldown) {
                return;
            }
            lastClicks.computeIfAbsent(playerId, k -> new HashMap<>()).put(customItem.getId(), now);
        }

        // Use limit (getMaxUses()), same as for YAML items
        int maxUses = customItem.getMaxUses();
        if (maxUses > 0) {
            int uses = plugin.getItemHandler().getStoredUses(item);
            if (uses == -1) {
                uses = maxUses;
                plugin.getItemHandler().setStoredUses(item, uses);
                updateHandItem(event, player, item);
            }
            if (uses <= 0) {
                event.setCancelled(true);
                player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                        customItem.getUsesDepletedMessage() != null
                                ? customItem.getUsesDepletedMessage()
                                : plugin.getMessageManager().getMessage("items.uses-depleted",
                                        "&cThe item was used up and is gone!")));
                return;
            }
            int remaining = plugin.getItemHandler().decrementStoredUses(item);
            if (remaining <= 0) {
                event.setCancelled(true);
                player.sendMessage(me.dcplugin.dcustomitems.utils.ColorUtils.processMessage(player,
                        customItem.getUsesDepletedMessage() != null
                                ? customItem.getUsesDepletedMessage()
                                : plugin.getMessageManager().getMessage("items.uses-depleted",
                                        "&cThe item was used up and is gone!")));
                // Remove the depleted item from the hand
                if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(null);
                }
                return;
            }
            updateHandItem(event, player, item);
        }

        // Event for third-party plugins — cancelling it ignores the click entirely
        me.dcplugin.dcustomitems.events.CustomItemUseEvent useEvent =
                new me.dcplugin.dcustomitems.events.CustomItemUseEvent(player, customItem,
                        isRightClick
                                ? me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.RIGHT_CLICK
                                : me.dcplugin.dcustomitems.events.CustomItemUseEvent.UseType.LEFT_CLICK);
        org.bukkit.Bukkit.getPluginManager().callEvent(useEvent);
        if (useEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (isRightClick) {
            customItem.onRightClick(event, player);
        } else if (isLeftClick) {
            customItem.onLeftClick(event, player);
        }
    }

    /**
     * Refresh the item in the hand after the PDC (uses) has changed.
     */
    private void updateHandItem(PlayerInteractEvent event, Player player, ItemStack item) {
        org.bukkit.inventory.EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        if (hand == org.bukkit.inventory.EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(item);
        } else if (hand == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        }
    }

    /**
     * Clear the click cooldown of a player.
     *
     * Without this, lastClicks grew forever: an entry was added for every
     * player who ever clicked an item and was never removed — that was a
     * memory leak for every player who joined the server.
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastClicks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();

        // onDamageTaken for the victim
        for (AbstractCustomItem item : getEquippedCustomItems(damaged)) {
            if (fireDamageTakenEvent(event, damaged, item)) {
                item.onDamageTaken(event, damaged);
            }
        }

        // onDamageDealt for the attacker
        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }

        if (damager != null) {
            ItemStack damagerItem = damager.getInventory().getItemInMainHand();
            String id = ItemAPI.getCustomItemId(damagerItem);
            if (id != null) {
                AbstractCustomItem item = registry.getItem(id);
                if (item != null && fireDamageDealtEvent(event, damager, item)) {
                    item.onDamageDealt(event, damager);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // onDeath for the dead player
        for (AbstractCustomItem item : getEquippedCustomItems(player)) {
            if (fireDeathEvent(event, player, item)) {
                item.onDeath(player, event);
            }
        }

        // onKill for the killer
        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            ItemStack killerItem = killer.getInventory().getItemInMainHand();
            String id = ItemAPI.getCustomItemId(killerItem);
            if (id != null) {
                AbstractCustomItem item = registry.getItem(id);
                if (item != null && fireKillEvent(killer, player, item)) {
                    item.onKill(killer, player);
                }
            }
        }
    }

    private boolean fireDamageTakenEvent(EntityDamageEvent event, Player player, AbstractCustomItem item) {
        me.dcplugin.dcustomitems.events.CustomItemDamageTakenEvent ev =
                new me.dcplugin.dcustomitems.events.CustomItemDamageTakenEvent(player, item, event);
        org.bukkit.Bukkit.getPluginManager().callEvent(ev);
        return !ev.isCancelled();
    }

    private boolean fireDamageDealtEvent(EntityDamageByEntityEvent event, Player player, AbstractCustomItem item) {
        me.dcplugin.dcustomitems.events.CustomItemDamageDealtEvent ev =
                new me.dcplugin.dcustomitems.events.CustomItemDamageDealtEvent(player, item, event);
        org.bukkit.Bukkit.getPluginManager().callEvent(ev);
        return !ev.isCancelled();
    }

    private boolean fireDeathEvent(PlayerDeathEvent event, Player player, AbstractCustomItem item) {
        me.dcplugin.dcustomitems.events.CustomItemDeathEvent ev =
                new me.dcplugin.dcustomitems.events.CustomItemDeathEvent(player, item, event);
        org.bukkit.Bukkit.getPluginManager().callEvent(ev);
        return !ev.isCancelled();
    }

    private boolean fireKillEvent(Player killer, Player victim, AbstractCustomItem item) {
        me.dcplugin.dcustomitems.events.CustomItemKillEvent ev =
                new me.dcplugin.dcustomitems.events.CustomItemKillEvent(killer, item, victim);
        org.bukkit.Bukkit.getPluginManager().callEvent(ev);
        return !ev.isCancelled();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        // No Java items at all — the whole handler would be pure overhead.
        // This is the hot path of the plugin: without this check a server
        // without the Java API paid ~8 ItemMeta clones per move event.
        if (registry.getItemCount() == 0) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        // PlayerMoveEvent fires ~20 times per second for every player,
        // including on camera rotation. If the position did not change —
        // there is nothing to call onMove/onJump for.
        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        Player player = event.getPlayer();
        List<AbstractCustomItem> equipped = getEquippedCustomItems(player);
        if (equipped.isEmpty()) return;

        boolean isJump = (event.getTo().getY() - event.getFrom().getY()) > 0.25;

        for (AbstractCustomItem customItem : equipped) {
            customItem.onMove(event, player);
            if (isJump) {
                customItem.onJump(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onBlockBreak(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        String id = ItemAPI.getCustomItemId(dropped);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onDrop(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null) return;

        String id = ItemAPI.getCustomItemId(item);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onSlotChange(event, player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        String id = ItemAPI.getCustomItemId(main);
        if (id == null) return;

        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null) {
            customItem.onSwapHand(event, player);
        }
    }

    /**
     * Gets all unique custom items equipped by the player (hands and armor).
     */
    private List<AbstractCustomItem> getEquippedCustomItems(Player player) {
        List<AbstractCustomItem> items = new ArrayList<>(6);
        addEquippedItem(items, player.getInventory().getItemInMainHand());
        addEquippedItem(items, player.getInventory().getItemInOffHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            addEquippedItem(items, armor);
        }
        return items;
    }

    private void addEquippedItem(List<AbstractCustomItem> list, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;
        String id = plugin.getItemHandler().getCustomItemId(item);
        if (id == null) return;
        AbstractCustomItem customItem = registry.getItem(id);
        if (customItem != null && !list.contains(customItem)) {
            list.add(customItem);
        }
    }

    /**
     * Finds a custom item in the player's hands or armour.
     *
     * <p>Uses the handler's lookup (one metadata read per slot) instead of
     * {@code ItemAPI.isCustomItem}, which reads the metadata twice.
     */
    private ItemStack findCustomItem(Player player) {
        // Main hand
        ItemStack main = player.getInventory().getItemInMainHand();
        if (plugin.getItemHandler().getCustomItemId(main) != null) return main;

        // Off hand
        ItemStack off = player.getInventory().getItemInOffHand();
        if (plugin.getItemHandler().getCustomItemId(off) != null) return off;

        // Armour
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (plugin.getItemHandler().getCustomItemId(armor) != null) return armor;
        }

        return null;
    }
}
