import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.*;

/**
 * ✅ EXAMPLE OF A COMMAND (SUCCESSFUL)
 * 
 * This command works without errors!
 * 
 * It will print a green message in the console:
 * [API] ✅ Command: /heal
 * 
 * Usage:
 * /heal [player]
 */
public class EXAMPLEcommandSuccess extends CustomCommand {

    public EXAMPLEcommandSuccess() {
        super(
            "heal",                          // Command name
            "Heal yourself or a player",     // Description
            "/heal [player]",                // Usage
            "dci.command.heal",              // Permission
            "healme", "health"               // Aliases
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Permission check
        if (!hasPermission(sender)) {
            msg(sender, getPermissionMessage());
            return true;
        }

        // Get the target
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // Healing
        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);

        // Effects
        target.getWorld().spawnParticle(org.bukkit.Particle.HEART, target.getLocation().add(0, 2, 0), 20);
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Messages
        if (sender.equals(target)) {
            msg(sender, "&aYou have been fully healed!");
        } else {
            msg(sender, "&aYou healed " + target.getName() + "!");
            msg(target, "&aYou have been healed!");
        }

        return true;
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            java.util.List<String> players = new java.util.ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return java.util.Collections.emptyList();
    }
}
