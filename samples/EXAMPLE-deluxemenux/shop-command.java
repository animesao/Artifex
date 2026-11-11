import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Opens the DeluxeMenuX shop.
 *
 * Command:
 * /shop
 */
public class shop_commandItem extends CustomCommand {

    public shop_commandItem() {
        super(
            "shop",
            "Open the shop",
            "/shop",
            "deluxemenux.use"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            msg(sender, "&cPlayers only!");
            return true;
        }

        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getModuleManager() == null) {
            msg(sender, "&cThe DeluxeMenuX module is not loaded!");
            return true;
        }

        Module module = plugin.getModuleManager().getModule("deluxemenux");
        if (!module.openMenu((Player) sender, "shop")) {
            msg(sender, "&cThe shop menu was not found!");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }
}
