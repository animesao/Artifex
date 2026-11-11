import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.Main;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 🎨 DeluxeMenuX - Advanced Menu System
 *
 * A full menu system like DeluxeMenus
 *
 * Features:
 * - YAML menu configuration
 * - Items with commands
 * - Conditions and checks
 * - Animations and sounds
 * - Placeholders
 * - Navigation between menus
 */
public class deluxemenuxModule extends Module implements Listener {

    private final Map<String, MenuData> menus = new LinkedHashMap<>();
    private final Map<UUID, String> openMenus = new HashMap<>();

    public deluxemenuxModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    @Override
    protected void onEnable() {
        // Register the listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Load all the menus
        loadAllMenus();

        plugin.getLogger().info("[DeluxeMenuX] Loaded " + menus.size() + " menus");
    }

    @Override
    protected void onDisable() {
        HandlerList.unregisterAll(this);
        menus.clear();
        openMenus.clear();
    }

    /**
     * Load all the menus from the menus/ folder
     */
    private void loadAllMenus() {
        menus.clear();

        File menusDir = new File(folder, "menus");
        if (!menusDir.exists()) {
            menusDir.mkdirs();
        }

        // We only create the missing examples. Custom YAML files are not overwritten.
        createDefaultMenus(menusDir);

        File[] menuFiles = menusDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (menuFiles == null) return;

        for (File menuFile : menuFiles) {
            try {
                loadMenu(menuFile);
            } catch (Exception e) {
                plugin.getLogger().warning("[DeluxeMenuX] Error loading menu: " + menuFile.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Load a menu from a YAML file
     */
    private void loadMenu(File menuFile) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        String menuId = menuFile.getName().replace(".yml", "");
        String title = colorize(config.getString("title", "Menu"));
        int size = config.getInt("size", 27);
        String openSound = config.getString("open-sound", "");
        String closeSound = config.getString("close-sound", "");

        // Load the items
        Map<Integer, MenuItem> items = new HashMap<>();
        if (config.contains("items")) {
            for (String slotKey : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    MenuItem item = loadMenuItem(config, "items." + slotKey);
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (NumberFormatException e) {
                    // Invalid slot
                }
            }
        }

        // Fill the background
        MenuItem fillItem = null;
        if (config.contains("fill")) {
            fillItem = loadMenuItem(config, "fill");
        }

        MenuData menu = new MenuData(menuId, title, size, items, fillItem, openSound, closeSound);
        menus.put(menuId, menu);
    }

    /**
     * Load a menu item
     */
    private MenuItem loadMenuItem(YamlConfiguration config, String path) {
        String materialName = config.getString(path + ".material", "STONE");
        String name = config.getString(path + ".name", "");
        List<String> lore = config.getStringList(path + ".lore");
        String command = config.getString(path + ".command", "");
        String message = config.getString(path + ".message", "");
        String permission = config.getString(path + ".permission", "");
        String sound = config.getString(path + ".sound", "");
        String close = config.getString(path + ".close", "false");
        int amount = config.getInt(path + ".amount", 1);
        int data = config.getInt(path + ".data", 0);

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) material = Material.STONE;

        return new MenuItem(material, name, lore, command, message, permission, sound,
                           close.equalsIgnoreCase("true"), amount, (byte) data);
    }

    /**
     * Create the default menus
     */
    private void createDefaultMenus(File menusDir) {
        // We only create the missing files so that the owner's settings are not wiped.
        if (!new File(menusDir, "shop.yml").exists()) createShopMenu(menusDir);
        if (!new File(menusDir, "main.yml").exists()) createMainMenu(menusDir);
        if (!new File(menusDir, "kits.yml").exists()) createKitsMenu(menusDir);
    }

    private void createShopMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("title", "&6🛒 Shop");
        config.set("size", 45);
        config.set("open-sound", "UI_BUTTON_CLICK");
        config.set("close-sound", "UI_BUTTON_CLICK");

        // Background
        config.set("fill.material", "GRAY_STAINED_GLASS_PANE");
        config.set("fill.name", " ");

        // Header
        config.set("items.4.material", "PLAYER_HEAD");
        config.set("items.4.name", "&6&lShop");
        config.set("items.4.lore", Arrays.asList("", "&7Choose a category", ""));

        // Diamonds
        config.set("items.19.material", "DIAMOND");
        config.set("items.19.name", "&bDiamonds");
        config.set("items.19.lore", Arrays.asList("", "&7Click to buy", ""));
        config.set("items.19.command", "give %player% diamond 1");
        config.set("items.19.message", "&aYou bought a diamond!");
        config.set("items.19.sound", "ENTITY_PLAYER_LEVELUP");

        // Emeralds
        config.set("items.22.material", "EMERALD");
        config.set("items.22.name", "&aEmeralds");
        config.set("items.22.lore", Arrays.asList("", "&7Click to buy", ""));
        config.set("items.22.command", "give %player% emerald 1");
        config.set("items.22.message", "&aYou bought an emerald!");
        config.set("items.22.sound", "ENTITY_PLAYER_LEVELUP");

        // Gold
        config.set("items.25.material", "GOLD_INGOT");
        config.set("items.25.name", "&6Gold");
        config.set("items.25.lore", Arrays.asList("", "&7Click to buy", ""));
        config.set("items.25.command", "give %player% gold_ingot 1");
        config.set("items.25.message", "&aYou bought gold!");
        config.set("items.25.sound", "ENTITY_PLAYER_LEVELUP");

        // Close
        config.set("items.40.material", "BARRIER");
        config.set("items.40.name", "&cClose");
        config.set("items.40.lore", Arrays.asList("", "&7Close the menu", ""));
        config.set("items.40.close", "true");
        config.set("items.40.sound", "UI_BUTTON_CLICK");

        try {
            config.save(new File(menusDir, "shop.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("[DeluxeMenuX] Error creating shop menu");
        }
    }

    private void createKitsMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("title", "&6&lKits");
        config.set("size", 27);
        config.set("open-sound", "UI_BUTTON_CLICK");
        config.set("close-sound", "UI_BUTTON_CLICK");
        config.set("fill.material", "BLACK_STAINED_GLASS_PANE");
        config.set("fill.name", " ");
        config.set("items.11.material", "STONE_SWORD");
        config.set("items.11.name", "&a&lStarter Kit");
        config.set("items.11.lore", Arrays.asList("", "&7Get the starter set", "", "&eClick to receive it"));
        config.set("items.11.permission", "deluxemenux.kit.start");
        config.set("items.11.command", "give %player% stone_sword 1");
        config.set("items.11.message", "&aStarter kit given!");
        config.set("items.11.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.13.material", "IRON_CHESTPLATE");
        config.set("items.13.name", "&b&lIron Kit");
        config.set("items.13.lore", Arrays.asList("", "&7Iron armor and tools", "", "&eClick to receive it"));
        config.set("items.13.permission", "deluxemenux.kit.iron");
        config.set("items.13.command", "give %player% iron_chestplate 1");
        config.set("items.13.message", "&bIron kit given!");
        config.set("items.13.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.15.material", "DIAMOND");
        config.set("items.15.name", "&b&lDiamond Kit");
        config.set("items.15.lore", Arrays.asList("", "&7Premium kit", "", "&eClick to receive it"));
        config.set("items.15.permission", "deluxemenux.kit.diamond");
        config.set("items.15.command", "give %player% diamond 3");
        config.set("items.15.message", "&bDiamond kit given!");
        config.set("items.15.sound", "ENTITY_PLAYER_LEVELUP");
        config.set("items.22.material", "BARRIER");
        config.set("items.22.name", "&cClose");
        config.set("items.22.close", true);
        try { config.save(new File(menusDir, "kits.yml")); }
        catch (Exception e) { plugin.getLogger().warning("[DeluxeMenuX] Error creating kits menu"); }
    }

    private void createMainMenu(File menusDir) {
        YamlConfiguration config = new YamlConfiguration();

        config.set("title", "&6&lMain Menu");
        config.set("size", 27);
        config.set("open-sound", "UI_BUTTON_CLICK");

        // Background
        config.set("fill.material", "BLACK_STAINED_GLASS_PANE");
        config.set("fill.name", " ");

        // Shop
        config.set("items.11.material", "EMERALD_BLOCK");
        config.set("items.11.name", "&a&lShop");
        config.set("items.11.lore", Arrays.asList("", "&7Open the shop", ""));
        config.set("items.11.command", "menu shop");
        config.set("items.11.sound", "UI_BUTTON_CLICK");

        // Inventory
        config.set("items.13.material", "CHEST");
        config.set("items.13.name", "&b&lInventory");
        config.set("items.13.lore", Arrays.asList("", "&7Manage the items", ""));
        config.set("items.13.command", "ci list");
        config.set("items.13.sound", "UI_BUTTON_CLICK");

        // Commands
        config.set("items.15.material", "COMMAND_BLOCK");
        config.set("items.15.name", "&e&lCommands");
        config.set("items.15.lore", Arrays.asList("", "&7A list of the commands", ""));
        config.set("items.15.command", "help");
        config.set("items.15.sound", "UI_BUTTON_CLICK");

        // Close
        config.set("items.22.material", "BARRIER");
        config.set("items.22.name", "&cClose");
        config.set("items.22.lore", Arrays.asList("", "&7Close the menu", ""));
        config.set("items.22.close", "true");
        config.set("items.22.sound", "UI_BUTTON_CLICK");

        try {
            config.save(new File(menusDir, "main.yml"));
        } catch (Exception e) {
            plugin.getLogger().warning("[DeluxeMenuX] Error creating main menu");
        }
    }

    /**
     * Open a menu for a player
     */
    @Override
    public boolean openMenu(Player player, String menuId) {
        MenuData menu = menus.get(menuId.toLowerCase());
        if (menu == null) {
            player.sendMessage(colorize("&cMenu not found: " + menuId));
            return false;
        }

        // Check the permissions
        // TODO: permission check

        // Create the inventory
        Inventory inventory = plugin.getServer().createInventory(
            new MenuHolder(menuId),
            menu.size,
            menu.title
        );

        // Set the background
        if (menu.fillItem != null) {
            ItemStack fillStack = createItemStack(menu.fillItem);
            for (int i = 0; i < menu.size; i++) {
                inventory.setItem(i, fillStack);
            }
        }

        // Set the items
        for (Map.Entry<Integer, MenuItem> entry : menu.items.entrySet()) {
            ItemStack item = createItemStack(entry.getValue());
            inventory.setItem(entry.getKey(), item);
        }

        // Play the open sound
        if (!menu.openSound.isEmpty()) {
            playSound(player, menu.openSound);
        }

        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), menuId);

        return true;
    }

    /**
     * Create an ItemStack from a MenuItem
     */
    private ItemStack createItemStack(MenuItem menuItem) {
        ItemStack item = new ItemStack(menuItem.material, menuItem.amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (!menuItem.name.isEmpty()) {
                meta.setDisplayName(colorize(menuItem.name));
            }
            if (!menuItem.lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : menuItem.lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Handle a click in a menu
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();

        if (topInventory == null || !(topInventory.getHolder() instanceof MenuHolder)) return;

        event.setCancelled(true);

        MenuHolder holder = (MenuHolder) topInventory.getHolder();
        String menuId = holder.getMenuId();
        MenuData menu = menus.get(menuId);

        if (menu == null) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.size) return;

        MenuItem menuItem = menu.items.get(slot);
        if (menuItem == null) return;

        // Check the permissions
        if (!menuItem.permission.isEmpty() && !player.hasPermission(menuItem.permission)) {
            player.sendMessage(colorize("&cYou do not have permission!"));
            return;
        }

        // Internal DeluxeMenuX transitions must be executed on behalf of the player.
        // Previously `menu shop` was sent through the console sender, so the
        // menu-command.java replied "Players only!" instead of opening the menu.
        if (!menuItem.command.isEmpty()) {
            String cmd = menuItem.command.replace("%player%", player.getName());
            if (!executeInternalMenuCommand(player, cmd)) {
                // Regular commands (give, help, etc.) are still executed
                // from the console so that they do not require the player's permissions.
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        }

        // Send the message
        if (!menuItem.message.isEmpty()) {
            String msg = menuItem.message.replace("%player%", player.getName());
            player.sendMessage(colorize(msg));
        }

        // Play the sound
        if (!menuItem.sound.isEmpty()) {
            playSound(player, menuItem.sound);
        }

        // Close the menu
        if (menuItem.close) {
            player.closeInventory();
        }
    }

    /**
     * Execute a menu navigation command without going through the
     * Bukkit CommandMap again. This preserves the Player context for /menu, /shop and /kits.
     */
    private boolean executeInternalMenuCommand(Player player, String command) {
        String normalized = command == null ? "" : command.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isEmpty()) return false;

        String[] parts = normalized.split("\\s+");
        String root = parts[0].toLowerCase(Locale.ROOT);
        String menuId;

        if (root.equals("menu") || root.equals("deluxemenu") || root.equals("deluxemenux")) {
            menuId = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "main";
        } else if (root.equals("shop") || root.equals("kits") || root.equals("kit")) {
            menuId = root.equals("kit") ? "kits" : root;
        } else {
            return false;
        }

        Module menuModule = plugin.getModuleManager().getModule("deluxemenux");
        if (menuModule == null || !menuModule.openMenu(player, menuId)) {
            player.sendMessage(colorize("&cMenu not found: " + menuId));
        }
        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        openMenus.remove(player.getUniqueId());
    }

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {}
    }

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }

    // ===== INNER CLASSES =====

    public static class MenuData {
        final String id;
        final String title;
        final int size;
        final Map<Integer, MenuItem> items;
        final MenuItem fillItem;
        final String openSound;
        final String closeSound;

        public MenuData(String id, String title, int size, Map<Integer, MenuItem> items,
                       MenuItem fillItem, String openSound, String closeSound) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.items = items;
            this.fillItem = fillItem;
            this.openSound = openSound;
            this.closeSound = closeSound;
        }
    }

    public static class MenuItem {
        final Material material;
        final String name;
        final List<String> lore;
        final String command;
        final String message;
        final String permission;
        final String sound;
        final boolean close;
        final int amount;
        final byte data;

        public MenuItem(Material material, String name, List<String> lore, String command,
                       String message, String permission, String sound, boolean close,
                       int amount, byte data) {
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.command = command;
            this.message = message;
            this.permission = permission;
            this.sound = sound;
            this.close = close;
            this.amount = amount;
            this.data = data;
        }
    }

    public static class MenuHolder implements InventoryHolder {
        private final String menuId;

        public MenuHolder(String menuId) {
            this.menuId = menuId;
        }

        public String getMenuId() {
            return menuId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
