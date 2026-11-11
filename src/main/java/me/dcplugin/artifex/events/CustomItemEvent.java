package me.dcplugin.artifex.events;

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Base event for custom items.
 *
 * Events work both for YAML items (the {@link CustomItem} model)
 * and for Java items from the API ({@link AbstractCustomItem}).
 *
 * Third-party plugins can listen for specific events:
 * <pre>
 * public class MyListener implements Listener {
 *     {@literal @}EventHandler
 *     public void onEquip(CustomItemEquipEvent event) {
 *         event.setCancelled(true); // forbid equipping/effects
 *     }
 *
 *     {@literal @}EventHandler
 *     public void onUse(CustomItemUseEvent event) {
 *         Player player = event.getPlayer();
 *         String itemId = event.getItemId();          // common ID
 *         if (event.getJavaItem() != null) {           // Java API item
 *             AbstractCustomItem javaItem = event.getJavaItem();
 *         } else {                                     // YAML item
 *             CustomItem item = event.getCustomItem();
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class CustomItemEvent extends Event {

    private final Player player;
    private final CustomItem customItem;
    private final AbstractCustomItem javaItem;

    /** For YAML items. */
    protected CustomItemEvent(Player player, CustomItem customItem) {
        this(player, customItem, null);
    }

    /** For Java API items (AbstractCustomItem). */
    protected CustomItemEvent(Player player, AbstractCustomItem javaItem) {
        this(player, null, javaItem);
    }

    private CustomItemEvent(Player player, CustomItem customItem, AbstractCustomItem javaItem) {
        this.player = player;
        this.customItem = customItem;
        this.javaItem = javaItem;
    }

    /**
     * The player who equipped / used the item.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * The custom item model (from YAML). {@code null} if the item was
     * created through the Java API — see {@link #getJavaItem()} in that case.
     */
    public CustomItem getCustomItem() {
        return customItem;
    }

    /**
     * The Java API item (AbstractCustomItem). {@code null} if the item is
     * defined in YAML — see {@link #getCustomItem()} in that case.
     */
    public AbstractCustomItem getJavaItem() {
        return javaItem;
    }

    /**
     * true if the event refers to a Java API item.
     */
    public boolean isJavaItem() {
        return javaItem != null;
    }

    /**
     * The custom item ID (for example "vampire-blade").
     */
    public String getItemId() {
        if (customItem != null) return customItem.getId();
        return javaItem != null ? javaItem.getId() : null;
    }
}
