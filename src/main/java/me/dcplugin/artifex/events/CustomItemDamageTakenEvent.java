package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Event fired when the player takes damage while a custom item is equipped/held
 * (on_damage_taken / onDamageTaken). Fired BEFORE the triggers/hook are executed.
 *
 * If the event is cancelled the standard triggers/hook are not executed.
 * Use getBukkitEvent()/setDamage() to modify the damage.
 */
public class CustomItemDamageTakenEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EntityDamageEvent bukkitEvent;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemDamageTakenEvent(Player player, CustomItem customItem, EntityDamageEvent bukkitEvent) {
        super(player, customItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemDamageTakenEvent(Player player, AbstractCustomItem javaItem, EntityDamageEvent bukkitEvent) {
        super(player, javaItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** The original Bukkit event (damage and similar can be modified). */
    public EntityDamageEvent getBukkitEvent() {
        return bukkitEvent;
    }

    /** The resulting damage (shortcut). */
    public double getDamage() {
        return bukkitEvent.getDamage();
    }

    /** Set the damage (shortcut). */
    public void setDamage(double damage) {
        bukkitEvent.setDamage(damage);
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
