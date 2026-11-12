package me.dcplugin.artifex.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Called when a player sockets a gem or rune into a socketable custom item.
 */
public class CustomItemSocketEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final ItemStack targetItem;
    private final ItemStack gemItem;
    private final String gemName;
    private final String gemBonus;

    public CustomItemSocketEvent(Player player, ItemStack targetItem, ItemStack gemItem, String gemName, String gemBonus) {
        this.player = player;
        this.targetItem = targetItem;
        this.gemItem = gemItem;
        this.gemName = gemName;
        this.gemBonus = gemBonus;
    }

    public Player getPlayer() { return player; }
    public ItemStack getTargetItem() { return targetItem; }
    public ItemStack getGemItem() { return gemItem; }
    public String getGemName() { return gemName; }
    public String getGemBonus() { return gemBonus; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
