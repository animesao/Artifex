import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Opens the DeluxeMenuX kits menu.
 *
 * Commands:
 * /kits
 * /kits list
 */
public class kits_commandItem extends CustomCommand {

    public kits_commandItem() {
        super(
            "kits",
            "Open the kits menu",
            "/kits",
            "deluxemenux.use",
            "kit"
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
        if (!module.openMenu((Player) sender, "kits")) {
            msg(sender, "&cThe kits menu was not found!");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        return Collections.emptyList();
    }
}
