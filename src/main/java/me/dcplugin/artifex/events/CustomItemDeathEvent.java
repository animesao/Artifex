package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Event fired when a player with a custom item dies (on_death / onDeath).
 *
 * Works for YAML items (getCustomItem()) and Java API items
 * (getJavaItem()). Fired BEFORE the triggers/hook are executed.
 *
 * If the event is cancelled the standard triggers/hook are not executed
 * (the Bukkit death itself is not cancelled).
 */
public class CustomItemDeathEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerDeathEvent deathEvent;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemDeathEvent(Player player, CustomItem customItem, PlayerDeathEvent deathEvent) {
        super(player, customItem);
        this.deathEvent = deathEvent;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemDeathEvent(Player player, AbstractCustomItem javaItem, PlayerDeathEvent deathEvent) {
        super(player, javaItem);
        this.deathEvent = deathEvent;
    }

    /** The dead player (same as getPlayer()). */
    public Player getVictim() {
        return getPlayer();
    }

    /** The original Bukkit death event. */
    public PlayerDeathEvent getDeathEvent() {
        return deathEvent;
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
