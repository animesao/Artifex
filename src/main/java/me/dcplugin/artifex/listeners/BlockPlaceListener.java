package me.dcplugin.artifex.listeners;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {

    private final Main plugin;

    public BlockPlaceListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String customItemId = plugin.getItemHandler().getCustomItemId(item);

        if (customItemId == null) {
            return;
        }

        // YAML item
        CustomItem customItem = plugin.getItemHandler().getCustomItem(customItemId);
        if (customItem != null) {
            if (!customItem.isPlaceable()) {
                event.setCancelled(true);
                // No message is shown - the placement is simply blocked
            }
            return;
        }

        // Java API item — previously the placeable check was only done for
        // YAML items, so Java items could be placed as blocks.
        if (plugin.getApiItemRegistry() == null) return;
        AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(customItemId);
        if (javaItem != null && !javaItem.isPlaceable()) {
            event.setCancelled(true);
        }
    }
}
