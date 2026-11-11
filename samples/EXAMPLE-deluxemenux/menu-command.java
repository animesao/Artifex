import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 🎨 /menu command - Open a menu
 * 
 * Usage:
 *   /menu          - the main menu
 *   /menu <id>     - open a menu by ID
 *   /menu list     - a list of the menus
 *   /menu reload   - reload the menus
 */
public class menu_commandItem extends CustomCommand {

    public menu_commandItem() {
        super(
            "menu",
            "Open a menu",
            "/menu [id]",
            "deluxemenux.use",
            "deluxemenu"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Main plugin = Main.getInstance();
        if (plugin == null) {
            msg(sender, "&cPlugin not found!");
            return true;
        }

        // Get the DeluxeMenuX module
        Module module = plugin.getModuleManager().getModule("deluxemenux");
        if (module == null) {
            msg(sender, "&cThe DeluxeMenuX module is not loaded!");
            return true;
        }

        if (args.length == 0) {
            // Open the main menu
            if (sender instanceof Player) {
                openMenu((Player) sender, "main");
            } else {
                msg(sender, "&cPlayers only!");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender, module);
            default:
                // Open a menu by ID
                if (sender instanceof Player) {
                    openMenu((Player) sender, args[0]);
                } else {
                    msg(sender, "&cPlayers only!");
                }
                return true;
        }
    }

    private boolean handleList(CommandSender sender) {
        msg(sender, "&6=== Menus ===");
        // TODO: Get the list of menus from the module
        msg(sender, "&e/shop &7- Shop");
        msg(sender, "&e/main &7- Main menu");
        return true;
    }

    private boolean handleReload(CommandSender sender, Module module) {
        if (!sender.hasPermission("deluxemenux.admin")) {
            msg(sender, "&cYou do not have permission!");
            return true;
        }
        
        module.reload();
        msg(sender, "&aMenus have been reloaded!");
        return true;
    }

    private void openMenu(Player player, String menuId) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;
        
        Module module = plugin.getModuleManager().getModule("deluxemenux");
        module.openMenu(player, menuId);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("reload");
            options.add("shop");
            options.add("main");
            return options;
        }
        return Collections.emptyList();
    }
}
