import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;
import org.bukkit.util.*;

import java.util.*;

/**
 * ⚡ SHADOW BLADE - Dash mechanic
 * 
 * Right Click - Dash forward 15 blocks
 * Left Click - Dash to the target (if you are looking at a mob/player)
 * 
 * Mechanic:
 * - Dash through the air
 * - Leaves shadow remnants behind
 * - Damage to enemies you dash through
 * - 5 sec cooldown
 * - 3 charges (regenerates 1 charge every 10 sec)
 * 
 * Additionally:
 * - Invisibility for 2 sec after the dash
 * - Speed I for 3 sec after the dash
 */
public class ShadowDashblade extends AbstractCustomItem {

    private final Map<UUID, Integer> charges = new HashMap<>();
    private final Map<UUID, Long> lastRegen = new HashMap<>();

    @Override
    public String getId() { return "shadow_dashblade"; }

    @Override
    public String getDisplayName() { return "&5&l⚡ Shadow Blade"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7&o\"The shadow knows no bounds...\"",
            "",
            " &5⚡ &fDash mechanic:",
            " &7• Right Click - Dash forward (15 blocks)",
            " &7• Left Click - Dash to the target",
            "",
            " &d✦ &fCharges: &e3/3",
            " &7• Regeneration: 1 charge / 10 sec",
            "",
            " &5🔮 &fBonuses after the dash:",
            " &7• Invisibility 2 sec",
            " &7• Speed I 3 sec",
            " &7• Damage to enemies on the way!",
            ""
        );
    }

    @Override
    public String getItemModel() { return "shadow_dashblade"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getPeriodicInterval() { return 200; } // Every 10 sec

    /**
     * Charge regeneration
     */
    @Override
    public void onPeriodic(Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);
        Long last = lastRegen.getOrDefault(uuid, 0L);

        if (currentCharges < 3 && System.currentTimeMillis() - last > 10000) {
            charges.put(uuid, Math.min(currentCharges + 1, 3));
            lastRegen.put(uuid, System.currentTimeMillis());

            if (charges.get(uuid) < 3) {
                sendActionbar(player, ChatColor.LIGHT_PURPLE + "⚡ Charge regenerated: " + charges.get(uuid) + "/3");
            }
        }
    }

    /**
     * Right Click - Dash forward
     */
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);

        if (currentCharges <= 0) {
            player.sendMessage(ChatColor.RED + "❌ No charges left!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Spend a charge
        charges.put(uuid, currentCharges - 1);

        // Dash forward
        performDash(player, player.getLocation().getDirection().multiply(15), true);

        // Notification
        sendActionbar(player, ChatColor.LIGHT_PURPLE + "⚡ Charges: " + charges.get(uuid) + "/3");
    }

    /**
     * Left Click - Dash to the target (if you are looking at a mob/player)
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        int currentCharges = charges.getOrDefault(uuid, 3);

        if (currentCharges <= 0) {
            player.sendMessage(ChatColor.RED + "❌ No charges left!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Look for a target within 20 blocks
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
            player.getLocation().add(0, player.getEyeHeight(), 0),
            player.getLocation().getDirection(),
            20,
            1.0,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace != null && rayTrace.getHitEntity() != null) {
            // Dash to the target
            Location targetLoc = rayTrace.getHitEntity().getLocation();
            charges.put(uuid, currentCharges - 1);

            performDashToTarget(player, targetLoc);

            // Damage on landing
            if (rayTrace.getHitEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) rayTrace.getHitEntity();
                target.damage(12, player);
                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30);
            }

            sendActionbar(player, ChatColor.LIGHT_PURPLE + "⚡ Charges: " + charges.get(uuid) + "/3");
        } else {
            // Normal forward dash if there is no target
            charges.put(uuid, currentCharges - 1);
            performDash(player, player.getLocation().getDirection().multiply(10), true);
            sendActionbar(player, ChatColor.LIGHT_PURPLE + "⚡ Charges: " + charges.get(uuid) + "/3");
        }
    }

    /**
     * Dash forward with damage
     */
    private void sendActionbar(Player player, String message) {
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }

    private void performDash(Player player, org.bukkit.util.Vector direction, boolean damageEntities) {
        Location startLoc = player.getLocation().clone();

        // Effects before the dash
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 2f);

        // Teleportation
        Location targetLoc = player.getLocation().add(direction);
        targetLoc.setY(player.getWorld().getHighestBlockYAt(targetLoc) + 1);
        player.teleport(targetLoc);

        // Effects after the dash
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        // Damage to everything on the way
        if (damageEntities) {
            for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(10, player);
                    living.getWorld().spawnParticle(Particle.CRIT, living.getLocation().add(0, 1, 0), 20);
                }
            }
        }

        // Bonuses after the dash
        applyPostDashEffects(player);

        // Shadow remnants
        createShadowTrail(startLoc, player.getLocation());
    }

    /**
     * Dash to the target (fast teleportation)
     */
    private void performDashToTarget(Player player, Location target) {
        Location startLoc = player.getLocation().clone();

        // Effects before the dash
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);

        // Fast teleportation (via scheduler for the animation)
        Bukkit.getScheduler().runTaskLater(
            Bukkit.getPluginManager().getPlugin("Artifex"),
            () -> {
                player.teleport(target.add(0, 0, 0));
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 50);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                applyPostDashEffects(player);
                createShadowTrail(startLoc, player.getLocation());
            },
            2L // 2 tick delay
        );
    }

    /**
     * Bonuses after the dash
     */
    private void applyPostDashEffects(Player player) {
        // Invisibility 2 sec
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));

        // Speed I for 3 sec
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));

        // Particles
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 30);
    }

    /**
     * Creates shadow remnants between the start and end points
     */
    private void createShadowTrail(Location start, Location end) {
        org.bukkit.util.Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        org.bukkit.util.Vector step = direction.normalize().multiply(0.5);

        Location current = start.clone();
        for (double i = 0; i < distance; i += 0.5) {
            current.add(step);
            start.getWorld().spawnParticle(
                Particle.LARGE_SMOKE, 
                current, 
                2, 
                0.1, 0.1, 0.1, 
                0.01
            );
        }
    }
}
