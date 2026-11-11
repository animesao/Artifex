package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player is killed (on_kill / onKill) by the item in the
 * killer's hand.
 *
 * Works for YAML items (getCustomItem()) and Java API items
 * (getJavaItem()). Fired BEFORE the triggers/hook are executed.
 *
 * If the event is cancelled the standard triggers/hook are not executed.
 * Note: just like in the current handler, only PLAYER kills are caught
 * (the killer is a PlayerDeathEvent); mob kills do not reach this event.
 */
public class CustomItemKillEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player victim;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemKillEvent(Player killer, CustomItem customItem, Player victim) {
        super(killer, customItem);
        this.victim = victim;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemKillEvent(Player killer, AbstractCustomItem javaItem, Player victim) {
        super(killer, javaItem);
        this.victim = victim;
    }

    /** The killed player. */
    public Player getVictim() {
        return victim;
    }

    /** The killer (same as getPlayer()). */
    public Player getKiller() {
        return getPlayer();
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
