package me.dcplugin.artifex.listeners;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ActionParser;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for the triggers of custom items.
 *
 * All actions inside the triggers are executed by the single {@link ActionParser}.
 * Line format: "trigger_name:action" (for example "on_kill:heal:20").
 */
public class TriggerListener implements Listener {

    private final Main plugin;

    // Jump cooldown: playerId -> time of the last jump
    private final Map<UUID, Long> jumpCooldowns = new HashMap<>();

    // Trigger cooldowns.
    // Internal structure: playerId -> (itemId:trigger -> expiry time).
    // Splitting by player gives O(1) cleanup on quit — previously all keys of
    // the HashMap had to be scanned (O(n) for every player who left).
    //
    // Keys inside:
    //   itemId:trigger — the cooldown of a specific trigger;
    //   itemId:cooldown — the cooldown set by the "cooldown:MS" action
    //                     (used by the public isOnCooldown/getCooldownRemaining).
    private final Map<UUID, Map<String, Long>> triggerCooldowns = new HashMap<>();

    private static final long JUMP_COOLDOWN_MS = 500;

    public TriggerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        checkTriggersForPlayer(damaged, "on_damage_taken", (player, item) -> {
            me.dcplugin.artifex.events.CustomItemDamageTakenEvent ev =
                    new me.dcplugin.artifex.events.CustomItemDamageTakenEvent(player, item, event);
            plugin.getServer().getPluginManager().callEvent(ev);
            return !ev.isCancelled();
        });

        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }

        if (damager != null) {
            checkTriggersForPlayer(damager, "on_damage_dealt", (player, item) -> {
                me.dcplugin.artifex.events.CustomItemDamageDealtEvent ev =
                        new me.dcplugin.artifex.events.CustomItemDamageDealtEvent(player, item, event);
                plugin.getServer().getPluginManager().callEvent(ev);
                return !ev.isCancelled();
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        checkTriggersForPlayer(player, "on_death", (victim, item) -> {
            me.dcplugin.artifex.events.CustomItemDeathEvent ev =
                    new me.dcplugin.artifex.events.CustomItemDeathEvent(victim, item, event);
            plugin.getServer().getPluginManager().callEvent(ev);
            return !ev.isCancelled();
        });

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            checkTriggersForPlayer(killer, "on_kill", (k, item) -> {
                me.dcplugin.artifex.events.CustomItemKillEvent ev =
                        new me.dcplugin.artifex.events.CustomItemKillEvent(k, item, player);
                plugin.getServer().getPluginManager().callEvent(ev);
                return !ev.isCancelled();
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Ignore micro-movements (camera rotation, swaying)
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        double dy = event.getTo().getY() - event.getFrom().getY();
        double horizontalDistSq = dx * dx + dz * dz;

        // Jump: upward movement of more than 0.25 blocks
        // with a horizontal movement of more than 0.5 blocks
        if (dy > 0.25 && horizontalDistSq > 0.25) {
            Long lastJump = jumpCooldowns.get(playerId);
            long now = System.currentTimeMillis();
            if (lastJump != null && (now - lastJump) < JUMP_COOLDOWN_MS) {
                return;
            }
            jumpCooldowns.put(playerId, now);
            checkTriggersForPlayer(player, "on_jump");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        // Only the start of the sprint (isSprinting() = true at the start)
        if (!event.isSprinting()) return;
        Player player = event.getPlayer();
        checkTriggersForPlayer(player, "on_sprint");
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        jumpCooldowns.remove(playerId);
        triggerCooldowns.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(droppedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null && customItem.hasTriggerActions()) {
                executeTriggerActions(player, "on_drop", customItem);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack pickedItem = event.getItem().getItemStack();

        String itemId = plugin.getItemHandler().getCustomItemId(pickedItem);
        if (itemId != null) {
            CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
            if (customItem != null && customItem.hasTriggerActions()) {
                executeTriggerActions(player, "on_pickup", customItem);
            }
        }
    }

    // === PUBLIC METHODS (called from PlayerListener and EquippedItemsChecker) ===

    public void executeEquipTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_equip", item);
        }
    }

    public void executeUnequipTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_unequip", item);
        }
    }

    public void executeRightClickTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_click_right", item);
        }
    }

    public void executeLeftClickTriggers(Player player, CustomItem item) {
        if (item != null && item.hasTriggerActions()) {
            executeTriggerActions(player, "on_click_left", item);
        }
    }

    // === INTERNAL LOGIC ===

    private void checkTriggersForPlayer(Player player, String trigger) {
        checkTriggersForPlayer(player, trigger, null);
    }

    private void checkTriggersForPlayer(Player player, String trigger, TriggerEventGate gate) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] armor = player.getInventory().getArmorContents();

        checkItemTriggers(player, mainHand, trigger, gate);
        checkItemTriggers(player, offHand, trigger, gate);
        for (ItemStack armorPiece : armor) {
            checkItemTriggers(player, armorPiece, trigger, gate);
        }
    }

    private void checkItemTriggers(Player player, ItemStack item, String trigger, TriggerEventGate gate) {
        if (item == null || item.getType() == Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(item);
        if (itemId == null) return;

        CustomItem customItem = plugin.getItemHandler().getCustomItem(itemId);
        if (customItem == null || !customItem.hasTriggerActions()) return;

        // The item's own permission gates its triggers too — otherwise a
        // player without the right would still trigger them (on_kill, on_pickup…).
        if (!customItem.isUsableBy(player)) return;

        // Bukkit event for third-party plugins: cancelling skips the triggers
        if (gate != null && !gate.check(player, customItem)) return;

        executeTriggerActions(player, trigger, customItem);
    }

    /**
     * Skips the item triggers if the Bukkit event is cancelled.
     */
    private interface TriggerEventGate {
        /** true = execute the triggers. */
        boolean check(Player player, CustomItem item);
    }

    /**
     * Executes the trigger actions. The line format in the config:
     * "on_kill:effect:REGENERATION:10:2" — split on the first ":".
     *
     * Cooldowns:
     *  - the item's {@code click-cooldown} — blocks the repeated triggering;
     *  - the pseudo-action {@code cooldown:MS} — sets its own trigger cooldown.
     *    Previously it only recorded the value but blocked nothing — now it
     *    really postpones the next triggering.
     */
    private void executeTriggerActions(Player player, String trigger, CustomItem item) {
        List<String> triggerActions = item.getTriggerActions();
        if (triggerActions == null || triggerActions.isEmpty()) return;

        UUID playerId = player.getUniqueId();
        String itemId = item.getId();
        long now = System.currentTimeMillis();

        String triggerKey = itemId + ":" + trigger;

        // The trigger is on cooldown — nothing is executed.
        if (isTriggerOnCooldown(playerId, triggerKey, now)) {
            return;
        }

        // Pass 1: find the custom cooldown ("cooldown:MS") in the list of actions.
        // It is handled first, otherwise it would block the very actions
        // that follow it in the same list.
        long customCooldown = -1L;
        for (String action : triggerActions) {
            String[] parsed = splitTriggerAction(action);
            if (parsed == null || !trigger.equals(parsed[0])) continue;
            String value = parsed[1];
            if (value.startsWith(COOLDOWN_PREFIX)) {
                long millis = parseLong(value.substring(COOLDOWN_PREFIX.length()).trim(), 3000);
                if (millis > customCooldown) customCooldown = millis;
            }
        }
        // Pass 2: execute the actions.
        boolean executed = false;
        for (String action : triggerActions) {
            String[] parsed = splitTriggerAction(action);
            if (parsed == null || !trigger.equals(parsed[0])) continue;

            String actionValue = parsed[1];

            // Pseudo-action cooldown:MS — sets the cooldown for this trigger
            if (actionValue.startsWith(COOLDOWN_PREFIX)) {
                continue;
            }

            ActionParser.execute(player, actionValue);
            executed = true;
        }

        // Update the cooldown after the execution (otherwise the events catch it again)
        long cooldown = customCooldown >= 0 ? customCooldown : item.getClickCooldown();
        if (cooldown > 0 && (executed || customCooldown >= 0)) {
            setTriggerExpiry(playerId, triggerKey, now + cooldown);
            if (customCooldown >= 0) {
                // Duplicated into the common item key — it is read by the public
                // isOnCooldown()/getCooldownRemaining().
                setTriggerExpiry(playerId, itemId + ":cooldown", now + customCooldown);
            }
        }
    }

    private static final String COOLDOWN_PREFIX = "cooldown:";

    /**
     * Splits the trigger action line into (trigger, value).
     */
    private static String[] splitTriggerAction(String action) {
        if (action == null) return null;
        int firstColon = action.indexOf(':');
        if (firstColon == -1) return null;
        return new String[]{action.substring(0, firstColon), action.substring(firstColon + 1)};
    }

    private boolean isTriggerOnCooldown(UUID playerId, String key, long now) {
        if (plugin.getConfig().getBoolean("cooldowns.persistent", true) && plugin.getCooldownStore() != null) {
            if (plugin.getCooldownStore().isOnCooldown(playerId.toString(), key, now)) {
                return true;
            }
        }
        Map<String, Long> playerCooldowns = triggerCooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(key);
        if (expiry == null) return false;
        if (now >= expiry) {
            playerCooldowns.remove(key);
            return false;
        }
        return true;
    }

    private void setTriggerExpiry(UUID playerId, String key, long expiry) {
        triggerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, expiry);
        if (plugin.getConfig().getBoolean("cooldowns.persistent", true) && plugin.getCooldownStore() != null) {
            plugin.getCooldownStore().put(playerId.toString(), key, expiry);
        }
    }

    // === PUBLIC COOLDOWN API ===

    /**
     * The cooldown set by the "cooldown:MS" action in the item triggers.
     */
    public boolean isOnCooldown(Player player, CustomItem item) {
        if (player == null || item == null) return false;
        return isTriggerOnCooldown(player.getUniqueId(), item.getId() + ":cooldown", System.currentTimeMillis());
    }

    /**
     * The remaining cooldown of the item (ms) within the click-cooldown.
     */
    public long getCooldownRemaining(Player player, CustomItem item) {
        if (player == null || item == null) return 0;
        if (plugin.getConfig().getBoolean("cooldowns.persistent", true) && plugin.getCooldownStore() != null) {
            long rem = plugin.getCooldownStore().remaining(player.getUniqueId().toString(), item.getId() + ":cooldown", System.currentTimeMillis());
            if (rem > 0) return rem;
        }
        Map<String, Long> playerCooldowns = triggerCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;
        Long expiry = playerCooldowns.get(item.getId() + ":cooldown");
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    private long parseLong(String raw, long def) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
