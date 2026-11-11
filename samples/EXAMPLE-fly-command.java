import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ✈ /fly command - Enable/disable flight
 * 
 * Usage:
 *   /fly        - enable/disable flight for yourself
 *   /fly <nick> - enable/disable flight for a player
 * 
 * Permission: dci.command.fly
 */
public class fly_commandItem extends CustomCommand {

    public fly_commandItem() {
        super(
            "fly",           // Command ID
            "Enable/disable flight",  // Description
            "/fly [nick]",   // Usage
            "dci.command.fly" // Permission
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Get the target player
        Player target = getTarget(sender, args, 0);
        if (target == null) {
            return false;
        }

        // Toggle flight
        boolean wasFlying = target.getAllowFlight();
        target.setAllowFlight(!wasFlying);
        target.setFlying(!wasFlying);

        // Send the message
        if (!wasFlying) {
            msg(sender, "&a✈ Flight enabled for &e" + target.getName());
            if (!sender.equals(target)) {
                msg(target, "&a✈ Flight enabled!");
            }
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        } else {
            msg(sender, "&c✈ Flight disabled for &e" + target.getName());
            if (!sender.equals(target)) {
                msg(target, "&c✈ Flight disabled!");
            }
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            // Autocomplete the names of online players
            List<String> players = new ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return Collections.emptyList();
    }
}
