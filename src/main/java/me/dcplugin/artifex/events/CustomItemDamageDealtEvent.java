package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Event fired when a custom item deals damage (on_damage_dealt / onDamageDealt).
 *
 * Works for YAML items (getCustomItem()) and Java API items
 * (getJavaItem()). Fired BEFORE the triggers/hook are executed on damage.
 *
 * If the event is cancelled the standard triggers/hook are not executed
 * (the Bukkit damage itself is not cancelled; use event.getBukkitEvent() for that).
 */
public class CustomItemDamageDealtEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final EntityDamageByEntityEvent bukkitEvent;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemDamageDealtEvent(Player player, CustomItem customItem, EntityDamageByEntityEvent bukkitEvent) {
        super(player, customItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemDamageDealtEvent(Player player, AbstractCustomItem javaItem, EntityDamageByEntityEvent bukkitEvent) {
        super(player, javaItem);
        this.bukkitEvent = bukkitEvent;
    }

    /** The original Bukkit event (damage and similar can be modified). */
    public EntityDamageByEntityEvent getBukkitEvent() {
        return bukkitEvent;
    }

    /** The resulting damage (shortcut for getBukkitEvent().getDamage()). */
    public double getDamage() {
        return bukkitEvent.getDamage();
    }

    /** Set the damage (shortcut for bukkitEvent.setDamage()). */
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
