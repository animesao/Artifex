import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.api.placeholders.PlaceholderManager;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 📦 Template Module — a template of a Artifex module
 *
 * ==========================================
 * HOW TO CREATE YOUR OWN MODULE:
 * ==========================================
 *
 * 1. Copy the _template/ folder → items/my-module/
 * 2. Rename template.java → my-module.java
 * 3. In the file replace:
 *    - "templateModule" → "myModule"
 *    - "template" → "my-module" (everywhere it appears)
 * 4. Implement the logic you need in onEnable/onDisable
 * 5. Run /ci reload
 *
 * ==========================================
 * AVAILABLE HOOKS:
 * ==========================================
 *
 * - onEnable()     — called when the module is enabled
 * - onDisable()    — called when the module is disabled
 * - config         — YamlConfiguration from config.yml
 * - itemsConfig    — YamlConfiguration from items.yml
 * - plugin         — the plugin's main class (Main)
 * - id             — the module ID (the folder name)
 * - folder         — the module folder
 *
 * ==========================================
 * COMMAND REGISTRATION:
 * ==========================================
 *
 * Option 1: Through plugin.yml (recommended)
 *   buy:
 *     description: Buy item
 *     usage: /buy <id>
 *     permission: mymodule.buy
 *
 * Option 2: Dynamic registration
 *   registerDynamicCommand("buy", this);
 *
 * ==========================================
 * PLACEHOLDER REGISTRATION:
 * ==========================================
 *
 * plugin.getPlaceholderManager().register("mymodule_stat", (player) -> {
 *     return "value";
 * });
 *
 * Available as: %dci_mymodule_stat%
 *
 * ==========================================
 * LISTENER REGISTRATION:
 * ==========================================
 *
 * plugin.getServer().getPluginManager().registerEvents(this, plugin);
 *
 * ==========================================
 */
public class templateModule extends Module implements Listener, CommandExecutor, TabCompleter {

    // ===== CONSTRUCTOR (do not touch) =====

    public templateModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    // ===== MAIN MODULE HOOKS =====

    /**
     * Called when the module is ENABLED.
     *
     * Register here:
     * - Commands
     * - Event listeners
     * - Placeholders
     * - Timers/tasks
     * - Data loading
     */
    @Override
    protected void onEnable() {
        // Register the commands (the base class removes them on disable)
        registerDynamicCommand("mycommand", this, "template.mycommand");

        // Register the event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Register the placeholders
        registerPlaceholders();

        // Register the module's own texts in the central message system.
        // An administrator overrides them in items/messages.java:
        //   MessagesConfig.set("template.some-text", "...");
        registerMessage("template.some-text", "&aHello from the template module!");
        registerMessage("template.no-permission", "&cYou are not allowed to use this!");

        // Log the successful load
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");
        plugin.getLogger().info("[Template] Module enabled!");
    }

    /**
     * Called when the module is DISABLED.
     *
     * Clean up here:
     * - Cancel the timers
     * - Save the data
     * - Close the connections
     */
    @Override
    protected void onDisable() {
        // Unregister all the listeners of this module
        org.bukkit.event.HandlerList.unregisterAll(this);

        plugin.getLogger().info("[Template] Module disabled!");
    }

    // ===== COMMAND REGISTRATION =====

    /**
     * Register a command.
     *
     * Way 1: Add it to plugin.yml (better):
     *   mycommand:
     *     description: My command
     *     usage: /mycommand <args>
     *     permission: template.mycommand
     *
     * Way 2: Dynamic registration (the base class):
     *   registerDynamicCommand("mycommand", this, "template.mycommand");
     * The command is removed automatically when the module is disabled.
     */

    // ===== COMMAND HANDLING =====

    /**
     * Handling of the /mycommand command
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "mycommand":
                return handleMyCommand(sender, args);
            default:
                return false;
        }
    }

    private boolean handleMyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;

        // Get the prefix from the config
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");

        if (args.length == 0) {
            player.sendMessage(colorize(prefix + "&7Usage: &e/mycommand <arg>"));
            return true;
        }

        // Your logic goes here
        player.sendMessage(colorize(prefix + "&aYou ran the command with the argument: &e" + args[0]));
        return true;
    }

    // ===== TAB COMPLETE =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Options for the first argument
            completions.addAll(Arrays.asList("option1", "option2", "option3"));
        }

        return completions;
    }

    // ===== EVENT LISTENERS =====

    /**
     * Example: when a player joins the server
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String prefix = config.getString("settings.message-prefix", "&a[Module] &r");

        // Your logic goes here
        // player.sendMessage(colorize(prefix + "Welcome!"));
    }

    /**
     * Example: when a player leaves
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Save the data, clear the cache, etc.
    }

    // ===== PLACEHOLDERS =====

    /**
     * Registration of the module placeholders.
     * Available as: %dci_<identifier>%
     */
    private void registerPlaceholders() {
        PlaceholderManager pm = plugin.getPlaceholderManager();
        if (pm == null) return;

        // Example: %dci_mymodule_stat%
        pm.register("mymodule_stat", (player) -> {
            // Return the placeholder value
            return "42";
        });

        // Example: %dci_mymodule_balance%
        pm.register("mymodule_balance", (player) -> {
            // A query to the database, the cache, etc.
            return "1000";
        });
    }

    // ===== WORKING WITH ITEMS =====

    /**
     * Example: get a custom item by ID
     */
    private CustomItem getCustomItem(String itemId) {
        return plugin.getItemHandler().getCustomItem(itemId);
    }

    /**
     * Example: give an item to a player
     */
    private void giveItem(Player player, String itemId, int amount) {
        CustomItem item = getCustomItem(itemId);
        if (item == null) return;

        ItemStack stack = item.getItemStack().clone();
        stack.setAmount(amount);
        plugin.getItemHandler().updateItemWithUses(stack);
        player.getInventory().addItem(stack);
    }

    /**
     * Example: check whether the player has the item
     */
    private boolean hasItem(Player player, String itemId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Example: take an item away from a player
     */
    private boolean removeItem(Player player, String itemId, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (item != null && itemId.equals(plugin.getItemHandler().getCustomItemId(item))) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }

    // ===== WORKING WITH THE DATABASE =====

    /**
     * Example: a database query
     */
    private void databaseExample() {
        if (plugin.getDatabaseManager() == null) return;

        // Create a table
        plugin.getDatabaseManager().execute(
            "CREATE TABLE IF NOT EXISTS my_data (uuid TEXT, key TEXT, value TEXT)"
        );

        // Insert data
        plugin.getDatabaseManager().execute(
            "INSERT INTO my_data (uuid, key, value) VALUES (?, ?, ?)",
            "player-uuid", "mykey", "myvalue"
        );

        // Get the data
        String value = plugin.getDatabaseManager().queryString(
            "SELECT value FROM my_data WHERE uuid=? AND key=?",
            "player-uuid", "mykey"
        );
    }

    // ===== GUI / MENU =====

    /**
     * Example: create a GUI menu
     */
    private void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, colorize("&6My Menu"));

        // Fill the background
        ItemStack glass = new ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; i++) {
            menu.setItem(i, glass);
        }

        // Add the items
        ItemStack diamond = new ItemStack(org.bukkit.Material.DIAMOND);
        ItemMeta diamondMeta = diamond.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName(colorize("&bDiamond"));
            diamond.setItemMeta(diamondMeta);
        }
        menu.setItem(13, diamond);

        player.openInventory(menu);
    }

    // ===== TIMERS / TASKS =====

    /**
     * Example: start a repeating task
     */
    private void startRepeatingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Runs every 20 ticks (1 second)
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Your logic goes here
            }
        }, 0L, 20L); // delay=0, period=20
    }

    /**
     * Example: a delayed task
     */
    private void scheduleDelayedTask(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Runs after 100 ticks (5 seconds)
            player.sendMessage(colorize("&a5 seconds have passed!"));
        }, 100L);
    }

    // ===== UTILS =====

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }
}
