package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a custom item is crafted in the GUI crafting table
 * (/craft, customcraft module).
 *
 * Fired at the moment the result slot is clicked — BEFORE the ingredients are
 * consumed. Works for YAML items (getCustomItem()) and Java API items
 * (getJavaItem()).
 *
 * If the event is cancelled the craft is not performed: the ingredients stay in
 * the grid and the result preview does not disappear. Otherwise the granted item
 * can be replaced via {@link #setResult(ItemStack)} (for example to add
 * enchantments) — the player receives the modified stack.
 *
 * <pre>
 * {@literal @}EventHandler
 * public void onCraft(CustomItemCraftEvent event) {
 *     if (event.getItemId() != null &amp;&amp; event.getItemId().startsWith("admin-")) {
 *         event.setCancelled(true); // forbid crafting admin items
 *     }
 * }
 * </pre>
 */
public class CustomItemCraftEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private ItemStack result;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemCraftEvent(Player player, CustomItem customItem, ItemStack result) {
        super(player, customItem);
        this.result = result;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemCraftEvent(Player player, AbstractCustomItem javaItem, ItemStack result) {
        super(player, javaItem);
        this.result = result;
    }

    /**
     * The item that will be given to the player. The stack can be modified —
     * the player receives exactly that stack (for example with extra enchantments).
     */
    public ItemStack getResult() {
        return result;
    }

    /**
     * Replace the granted item. A null value is ignored — the previous stack stays.
     */
    public void setResult(ItemStack result) {
        if (result != null) {
            this.result = result;
        }
    }

    /**
     * The amount of granted items (shortcut for result.getAmount()).
     */
    public int getAmount() {
        return result == null ? 0 : result.getAmount();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
