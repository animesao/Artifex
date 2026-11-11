package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event fired for the periodic effect of a Java API item (onPeriodic).
 *
 * Fired before every {@code onPeriodic} invocation of an equipped
 * Java item (the global equipped-items checker). YAML items have
 * no periodic effects.
 *
 * If the event is cancelled the specific {@code onPeriodic} call is skipped.
 */
public class CustomItemPeriodicEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final long interval;
    private boolean cancelled;

    public CustomItemPeriodicEvent(Player player, AbstractCustomItem javaItem, long interval) {
        super(player, javaItem);
        this.interval = interval;
    }

    /** The item interval (in ticks), see getPeriodicInterval(). */
    public long getInterval() {
        return interval;
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
