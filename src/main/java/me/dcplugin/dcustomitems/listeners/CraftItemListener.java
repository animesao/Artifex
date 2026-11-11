package me.dcplugin.dcustomitems.listeners;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.events.CustomItemCraftEvent;
import me.dcplugin.dcustomitems.models.CustomItem;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Locale;

/**
 * Handles crafting events for custom items:
 * - Cancels vanilla crafts if matrix contains custom items (protects against accidental consumption).
 * - Duplicates CustomItemCraftEvent for the VANILLA crafting table.
 *
 * The GUI crafting (/craft, customcraft module) fires the event itself; here
 * crafting in the vanilla table/inventory grid is caught. Cancelling
 * CustomItemCraftEvent also cancels the CraftItemEvent — the result is not
 * taken and the ingredients stay.
 */
public class CraftItemListener implements Listener {

    private final Main plugin;

    public CraftItemListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!plugin.getConfig().getBoolean("crafting.protect-custom-items-in-vanilla-recipes", true)) {
            return;
        }

        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        // Custom plugin recipes handle their own exact ingredient matching
        if (isPluginRecipe(recipe)) {
            return;
        }

        // Vanilla (or foreign) recipe: block if any matrix item is a custom item
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            if (plugin.getItemHandler().getCustomItemId(item) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Extra safeguard against client craft packets if prepare was skipped
        if (plugin.getConfig().getBoolean("crafting.protect-custom-items-in-vanilla-recipes", true)
                && event.getRecipe() != null && !isPluginRecipe(event.getRecipe())) {
            for (ItemStack matrixItem : event.getInventory().getMatrix()) {
                if (matrixItem != null && matrixItem.getType() != org.bukkit.Material.AIR
                        && plugin.getItemHandler().getCustomItemId(matrixItem) != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == org.bukkit.Material.AIR) return;

        String itemId = plugin.getItemHandler().getCustomItemId(result);
        if (itemId == null) return;

        // The ingredients may yield a result with an amount > 1 — initialize the copy as is
        CustomItemCraftEvent craftEvent;
        CustomItem yamlItem = plugin.getItemHandler().getCustomItem(itemId);
        if (yamlItem != null) {
            craftEvent = new CustomItemCraftEvent(player, yamlItem, result.clone());
        } else {
            AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(itemId);
            if (javaItem == null) return;
            craftEvent = new CustomItemCraftEvent(player, javaItem, result.clone());
        }

        plugin.getServer().getPluginManager().callEvent(craftEvent);
        if (craftEvent.isCancelled()) {
            event.setCancelled(true);
        }
    }

    private boolean isPluginRecipe(Recipe recipe) {
        if (recipe instanceof Keyed keyed) {
            String ns = keyed.getKey().getNamespace().toLowerCase(Locale.ROOT);
            String pluginNs = plugin.getName().toLowerCase(Locale.ROOT);
            return ns.equals(pluginNs) || ns.equals("dcustomitems") || ns.equals("dc-customitems");
        }
        return false;
    }
}
