package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a custom item is used (left/right click).
 *
 * Fired before the click actions are executed (right/left-click-actions,
 * triggers and effects — for YAML; the {@code onRightClick/onLeftClick} hooks —
 * for the Java API). If the event is cancelled the click is fully ignored:
 * actions, triggers, hooks and item consumption are not performed.
 */
public class CustomItemUseEvent extends CustomItemEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum UseType {
        RIGHT_CLICK, LEFT_CLICK
    }

    private final UseType useType;
    private boolean cancelled;

    /** For YAML items. */
    public CustomItemUseEvent(Player player, CustomItem customItem, UseType useType) {
        super(player, customItem);
        this.useType = useType;
    }

    /** For Java API items (AbstractCustomItem). */
    public CustomItemUseEvent(Player player, AbstractCustomItem javaItem, UseType useType) {
        super(player, javaItem);
        this.useType = useType;
    }

    /**
     * The click type: RIGHT_CLICK or LEFT_CLICK.
     */
    public UseType getUseType() {
        return useType;
    }

    /**
     * true if this is a right click.
     */
    public boolean isRightClick() {
        return useType == UseType.RIGHT_CLICK;
    }

    /**
     * true if this is a left click.
     */
    public boolean isLeftClick() {
        return useType == UseType.LEFT_CLICK;
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
