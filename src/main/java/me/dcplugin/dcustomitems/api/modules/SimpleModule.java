package me.dcplugin.dcustomitems.api.modules;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple YAML-based module
 * 
 * Example items.yml:
 * items:
 *   diamond:
 *     material: DIAMOND
 *     name: "&bDiamond"
 *     lore:
 *       - "&7Just a diamond"
 *     price: 100
 *     command: "give %player% diamond 1"
 * 
 * menu:
 *   title: "&6Shop"
 *   size: 27
 *   fill:
 *     material: GRAY_STAINED_GLASS_PANE
 *     name: " "
 *   items:
 *     13:
 *       item: diamond
 */
public class SimpleModule extends Module implements InventoryHolder, Listener {

    private Inventory menu;

    public SimpleModule(Main plugin, String id, File folder, YamlConfiguration config) {
        super(plugin, id, folder);
        this.config = config;
    }

    @Override
    public Inventory getInventory() {
        return menu;
    }

    @Override
    protected void onEnable() {
        // Create the menu if the setting is present
        if (config.contains("menu")) {
            createMenu();
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    protected void onDisable() {
        HandlerList.unregisterAll(this);
        menu = null;
    }

    /**
     * Creates the menu from the config
     */
    private void createMenu() {
        String title = colorize(config.getString("menu.title", "Menu"));
        int size = config.getInt("menu.size", 27);
        
        menu = plugin.getServer().createInventory(this, size, ColorUtils.colorize(title));
        
        // Fill the background
        if (config.contains("menu.fill")) {
            String materialName = config.getString("menu.fill.material", "GRAY_STAINED_GLASS_PANE");
            String name = config.getString("menu.fill.name", " ");
            
            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material != null) {
                ItemStack fillItem = createItem(material, name);
                for (int i = 0; i < size; i++) {
                    menu.setItem(i, fillItem);
                }
            }
        }
        
        // Place the items
        if (config.contains("menu.items")) {
            for (String slotKey : config.getConfigurationSection("menu.items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    String itemId = config.getString("menu.items." + slotKey + ".item");
                    
                    if (itemId != null && itemsConfig.contains("items." + itemId)) {
                        Map<String, Object> itemData = getItem(itemId);
                        ItemStack item = createItemFromData(itemData);
                        if (item != null) {
                            menu.setItem(slot, item);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Invalid slot
                }
            }
        }
    }

    /**
     * Opens the menu for a player
     */
    public void openMenu(Player player) {
        if (menu != null) {
            player.openInventory(menu);
        }
    }

    /**
     * Handles a click in the menu
     */
    public void handleMenuClick(Player player, int slot) {
        if (!config.contains("menu.items." + slot)) return;
        
        String itemId = config.getString("menu.items." + slot + ".item");
        if (itemId == null) return;
        
        Map<String, Object> itemData = getItem(itemId);
        if (itemData.isEmpty()) return;
        
        // Execute the command
        String command = (String) itemData.get("command");
        if (command != null) {
            command = command.replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
        
        // Send the message
        String message = (String) itemData.get("message");
        if (message != null) {
            message = message.replace("%player%", player.getName());
            player.sendMessage(ColorUtils.colorize(message));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != this) return;

        handleMenuClick(player, event.getSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
    }

    /**
     * Creates an ItemStack from the data
     */
    private ItemStack createItemFromData(Map<String, Object> data) {
        String materialName = (String) data.get("material");
        String name = (String) data.get("name");
        List<String> lore = (List<String>) data.get("lore");
        
        Material material = Material.matchMaterial(materialName != null ? materialName.toUpperCase() : "STONE");
        if (material == null) material = Material.STONE;
        
        return createItem(material, name, lore != null ? lore.toArray(new String[0]) : new String[0]);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(colorize(name));
            }
            if (lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
