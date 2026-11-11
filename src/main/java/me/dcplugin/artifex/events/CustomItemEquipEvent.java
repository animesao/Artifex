package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a custom item is equipped/unequipped.
 *
 * Fired when the player puts the item into an activation slot (or takes it out),
 * before the equip effects/messages/triggers are applied (YAML items)
 * or before the {@code onEquip()/onUnequip()} hook is invoked (Java API items).
 *
 * If the event is cancelled, the standard equip effects and messages
 * are not applied (for equip) and the unequip particles are not spawned (for unequip),
 * and the Java item hook is not invoked.
 */
public class CustomItemEquipEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean equip;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemEquipEvent(Player player, CustomItem customItem, boolean equip) {
        super(player, customItem);
        this.equip = equip;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemEquipEvent(Player player, AbstractCustomItem javaItem, boolean equip) {
        super(player, javaItem);
        this.equip = equip;
    }

    /**
     * true = the item was equipped, false = it was removed.
     */
    public boolean isEquip() {
        return equip;
    }

    /**
     * true = the item was removed.
     */
    public boolean isUnequip() {
        return !equip;
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
