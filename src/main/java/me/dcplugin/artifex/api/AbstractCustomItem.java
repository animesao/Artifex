package me.dcplugin.artifex.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
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

import java.util.List;


/**
 * Abstract base class for creating custom items in Java.
 *
 * Developers should extend this class and override the methods they need.
 * Every item must carry the @ItemInfo annotation or override getId().
 *
 * Example:
 * <pre>
 * public class FireSword extends AbstractCustomItem {
 *
 *     @Override
 *     public String getId() { return "fire_sword"; }
 *
 *     @Override
 *     public String getDisplayName() { return "&cFlaming Sword"; }
 *
 *     @Override
 *     public Material getMaterial() { return Material.NETHERITE_SWORD; }
 *
 *     @Override
 *     public void onLeftClick(PlayerInteractEvent event, Player player) {
 *         player.setFireTicks(100);
 *         ItemAPI.particles(player, Particle.FLAME, 50);
 *         ItemAPI.sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
 *     }
 * }
 * </pre>
 */
public abstract class AbstractCustomItem {

    // ===== MAIN PROPERTIES =====

    /**
     * Unique item ID (used in commands and configs).
     */
    public abstract String getId();

    /**
     * Display name of the item (supports colour codes &).
     */
    public abstract String getDisplayName();

    /**
     * Material of the item (for example, Material.DIAMOND_SWORD).
     */
    public abstract Material getMaterial();

    /**
     * Item description (lore). An empty list means no description.
     */
    public List<String> getLore() {
        return List.of();
    }

    /**
     * Stack size of the item.
     */
    public int getAmount() {
        return 1;
    }

    /**
     * Custom model (item-model for 1.21.11).
     */
    public String getItemModel() {
        return null;
    }

    /**
     * CustomModelData (legacy, for older versions).
     */
    public int getCustomModelData() {
        return -1;
    }

    /**
     * Whether the item is unbreakable.
     */
    public boolean isUnbreakable() {
        return false;
    }

    /**
     * Enchantment glint.
     */
    public boolean isGlowing() {
        return false;
    }

    /**
     * Click cooldown in milliseconds.
     */
    public long getClickCooldown() {
        return 0;
    }

    /**
     * Permission required to use the item (null = no restriction).
     * Checked on clicks (left/right click) — same as for YAML items.
     */
    public String getPermission() {
        return null;
    }

    /**
     * Whether the item can be placed in the world.
     */
    public boolean isPlaceable() {
        return false;
    }

    /**
     * Item type (RUNE, TOOL, ARMOR, CONSUMABLE).
     */
    public String getType() {
        return "TOOL";
    }

    /**
     * Activation slot (HAND, OFFHAND, HEAD, CHEST, LEGS, FEET).
     */
    public String getActivationSlot() {
        return "HAND";
    }

    // ===== MECHANICS, SAME AS FOR YAML ITEMS =====

    /**
     * Maximum number of uses of the item. 0 = unlimited.
     * The counter is stored in the PDC (as for YAML items) and is consumed on click.
     * Once the uses run out, the remaining stack is removed.
     */
    public int getMaxUses() {
        return 0;
    }

    /**
     * Item lifetime in seconds, counted from the moment it is given. 0 = permanent.
     * Checked by the global checker: once it expires, the item disappears.
     */
    public long getDuration() {
        return 0;
    }

    /**
     * Whether the item is allowed in a world. The global checker removes items
     * in forbidden worlds (same as for YAML items).
     */
    public boolean isAllowedInWorld(String worldName) {
        return true;
    }

    /**
     * Message shown when the uses are depleted (null = default).
     */
    public String getUsesDepletedMessage() {
        return null;
    }

    /**
     * Message shown when the lifetime expires (null = default).
     */
    public String getDurationExpiredMessage() {
        return null;
    }

    /**
     * Message shown when the item is blocked in a world (null = default).
     */
    public String getWorldBlockedMessage() {
        return null;
    }

    /**
     * Crafting recipes of this item (shaped / shapeless / furnace).
     *
     * Recipes are registered in the regular crafting table and in the GUI crafting menu (/craft).
     * Ingredients: a material name ("DIAMOND") or the ID of another custom
     * item ("vampire-blade") — YAML or Java, both are recognised.
     *
     * <pre>
     * {@literal @}Override
     * public List&lt;RecipeDef&gt; getRecipes() {
     *     return List.of(RecipeDef.shaped(List.of(" A ", "ABA", " A "),
     *         Map.of('A', "DIAMOND", 'B', "STICK")));
     * }
     * </pre>
     */
    public List<RecipeDef> getRecipes() {
        return List.of();
    }

    // ===== ACTIONS =====

    /**
     * Called when the item is used with a right click (right mouse button).
     */
    public void onRightClick(PlayerInteractEvent event, Player player) {}

    /**
     * Called when the item is used with a left click (left mouse button).
     */
    public void onLeftClick(PlayerInteractEvent event, Player player) {}

    /**
     * Called when the item is equipped.
     */
    public void onEquip(Player player) {}

    /**
     * Called when the item is unequipped.
     */
    public void onUnequip(Player player) {}

    /**
     * Called when a mob/player is killed with the item.
     */
    public void onKill(Player killer, Player victim) {}

    /**
     * Called when the player dies while holding this item.
     */
    public void onDeath(Player player, PlayerDeathEvent event) {}

    /**
     * Called when the player takes damage (with the item in hand or on armour).
     */
    public void onDamageTaken(EntityDamageEvent event, Player player) {}

    /**
     * Called when the player deals damage (with the item in hand).
     */
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {}

    /**
     * Called when the player jumps (with the item in hand).
     */
    public void onJump(Player player) {}

    /**
     * Called when the player moves (with the item in hand).
     */
    public void onMove(PlayerMoveEvent event, Player player) {}

    /**
     * Called when the item is dropped.
     */
    public void onDrop(Player player, PlayerDropItemEvent event) {}

    /**
     * Called when the item is picked up.
     */
    public void onPickup(Player player) {}

    /**
     * Called when a block is broken with the item.
     */
    public void onBlockBreak(Player player, BlockBreakEvent event) {}

    /**
     * Called when the hotbar slot changes (with the item in hand).
     */
    public void onSlotChange(PlayerItemHeldEvent event, Player player) {}

    /**
     * Called when the hands are swapped (F key).
     */
    public void onSwapHand(PlayerSwapHandItemsEvent event, Player player) {}

    // ===== PERIODIC EFFECTS =====

    /**
     * Interval of the periodic effects in ticks (20 ticks = 1 second).
     * Return 0 if no periodic effects are needed.
     */
    public long getPeriodicInterval() {
        return 0;
    }

    /**
     * Called periodically (every getPeriodicInterval() ticks).
     * Use it for equipment effects (armour, etc.).
     */
    public void onPeriodic(Player player) {}

    // ===== UTILITIES =====

    /**
     * Creates an ItemStack from the properties of this item.
     */
    public ItemStack createItemStack() {
        return ItemAPI.createItem(this);
    }

    /**
     * Checks whether the player is holding this item.
     */
    public boolean isHeldBy(Player player) {
        return ItemAPI.isHolding(player, getId());
    }

    /**
     * Checks whether this item is equipped by the player.
     */
    public boolean isEquippedBy(Player player) {
        return ItemAPI.isEquipped(player, getId());
    }
}
