import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.handlers.ItemCatalogGui;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /itembrowser command — opens an interactive in-game chest GUI catalog of all custom items.
 *
 * Usage:
 *   /itembrowser [category]
 *
 * Categories:
 *   ALL, TOOL, ARMOR, CONSUMABLE, RUNE
 */
public class GuiBrowserCommand extends CustomCommand {

    public GuiBrowserCommand() {
        super("itembrowser", "Open interactive catalog of custom items", "/itembrowser [category]", "artifex.menu");
        setAliases("ib", "cmenu", "artifex-menu");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be executed by players!");
            return true;
        }

        String category = args.length > 0 ? args[0].toUpperCase() : "ALL";
        ItemCatalogGui.open(player, category, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (args.length == 1) {
            return List.of("ALL", "TOOL", "ARMOR", "CONSUMABLE", "RUNE");
        }
        return Collections.emptyList();
    }
}
