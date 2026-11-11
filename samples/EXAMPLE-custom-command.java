import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

import java.util.*;

/**
 * 🔥 BERSERK COMMAND - Example of a custom command in Java
 * 
 * The /berserk command:
 * - Enables/disables the berserk mode
 * - Low HP = more damage
 * - Visual effects
 * - Automatically disables on death
 * 
 * Usage:
 *   /berserk - Enable/disable
 *   /berserk on - Enable
 *   /berserk off - Disable
 *   /berserk status - Status
 */
public class BerserkCommand extends CustomCommand {

    // Storage of the active berserkers
    private static final Set<UUID> activeBerserkers = new HashSet<>();

    public BerserkCommand() {
        super(
            "berserk",                    // Command name
            "Enable/disable berserk",     // Description
            "/berserk [on|off|status]",   // Usage
            "dci.command.berserk",        // Permission
            List.of("brk", "rage"),       // Aliases
            "&cYou do not have permission for berserk!" // Error message
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "&cPlayers only!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // Determine the action
        String action = args.length > 0 ? args[0].toLowerCase() : "toggle";

        switch (action) {
            case "on":
            case "enable":
                enableBerserk(player);
                break;

            case "off":
            case "disable":
                disableBerserk(player);
                break;

            case "status":
                showStatus(player);
                break;

            default: // toggle
                if (activeBerserkers.contains(uuid)) {
                    disableBerserk(player);
                } else {
                    enableBerserk(player);
                }
                break;
        }

        return true;
    }

    /**
     * Enable berserk
     */
    private void enableBerserk(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeBerserkers.contains(uuid)) {
            sendMessage(player, "&cYou are already berserk!");
            return;
        }

        activeBerserkers.add(uuid);

        // Effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 0, false, false));

        // Visual effects
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 100);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);

        // Title
        player.sendTitle(
            ChatColor.DARK_RED + "" + ChatColor.BOLD + "🔥 BERSERK!",
            ChatColor.RED + "Strength x2 | Speed x2",
            10, 60, 20
        );

        sendMessage(player, "&4You entered BERSERK mode! (+2 damage, +speed)");
    }

    /**
     * Disable berserk
     */
    private void disableBerserk(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activeBerserkers.contains(uuid)) {
            sendMessage(player, "&cYou are not berserk!");
            return;
        }

        activeBerserkers.remove(uuid);

        // Remove the effects
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

        // Visual effects
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1f, 1f);

        // Title
        player.sendTitle(
            ChatColor.GRAY + "Berserk disabled",
            "",
            5, 20, 5
        );

        sendMessage(player, "&7Berserk mode has been disabled.");
    }

    /**
     * Show the status
     */
    private void showStatus(Player player) {
        UUID uuid = player.getUniqueId();
        boolean isActive = activeBerserkers.contains(uuid);
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        int healthPercent = (int) ((health / maxHealth) * 100);

        sendMessage(player, "&6=== BERSERK ===");
        sendMessage(player, "&7Status: " + (isActive ? "&aActive" : "&cDisabled"));
        sendMessage(player, "&7HP: &c" + (int) health + "/" + (int) maxHealth + " (" + healthPercent + "%)");

        if (isActive) {
            int bonusDamage = calculateBonusDamage(healthPercent);
            sendMessage(player, "&7Bonus damage: &c+" + bonusDamage);
        }

        sendMessage(player, "&6================");
    }

    /**
     * Calculate the bonus damage based on HP
     */
    private int calculateBonusDamage(int healthPercent) {
        if (healthPercent > 75) return 1;
        if (healthPercent > 50) return 2;
        if (healthPercent > 25) return 4;
        return 8; // Maximum bonus at critical HP
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (cursor == 0) {
            return Arrays.asList("on", "off", "status");
        }
        return Collections.emptyList();
    }

    /**
     * Check whether berserk is active for a player
     */
    public static boolean isActive(Player player) {
        return activeBerserkers.contains(player.getUniqueId());
    }

    /**
     * Get the bonus damage for a player
     */
    public static int getBonusDamage(Player player) {
        if (!isActive(player)) return 0;
        double health = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        int healthPercent = (int) ((health / maxHealth) * 100);

        if (healthPercent > 75) return 2;
        if (healthPercent > 50) return 4;
        if (healthPercent > 25) return 6;
        return 10;
    }

    /**
     * Clear on plugin shutdown
     */
    @Override
    public void onUnregister() {
        // Disable berserk for everyone
        for (UUID uuid : new HashSet<>(activeBerserkers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableBerserk(player);
            }
        }
        activeBerserkers.clear();
    }
}
