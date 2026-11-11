import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * /heal command - Healing
 * 
 * Put this file in plugins/Artifex/items/
 * Run /ci reload
 * The command will be available!
 */
public class HealCommand extends CustomCommand {

    public HealCommand() {
        super("heal", "Heal yourself or a player", "/heal [player]", "dci.command.heal", "healme");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // Healing
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);

        // Clear the negative effects
        for (var effect : target.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.POISON) ||
                effect.getType().equals(PotionEffectType.WITHER) ||
                effect.getType().equals(PotionEffectType.NAUSEA)) {
                target.removePotionEffect(effect.getType());
            }
        }

        // Effects
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 30);
        target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 20);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Messages
        if (sender.equals(target)) {
            msg(sender, "&aYou have been fully healed!");
        } else {
            msg(sender, "&aYou healed " + target.getName() + "!");
            msg(target, "&aYou have been healed!");
        }

        // Title
        title(target, "&a❤ HEALING!", "&7Full health!");

        return true;
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            // Names of online players
            java.util.List<String> players = new java.util.ArrayList<>();
            for (Player p : sender.getServer().getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        return java.util.Collections.emptyList();
    }
}
