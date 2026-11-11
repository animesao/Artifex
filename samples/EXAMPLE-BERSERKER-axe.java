import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.scheduler.*;

import java.util.*;

/**
 * 🔥 BERSERKER AXE - Rage mechanic
 * 
 * The less HP you have, the STRONGER you are!
 * 
 * Mechanic:
 * - 100-75% HP: x1.5 damage, Speed I
 * - 75-50% HP: x2 damage, Speed II
 * - 50-25% HP: x3 damage, Speed III, Regeneration I
 * - 25-0% HP: x5 damage, Speed IV, Regeneration II, Invisibility for 3 sec on hit
 * 
 * On kill: +2 hearts for 10 sec
 * On death: Rage explosion (damage to everything nearby)
 * Left Click: Rage Scream (AoE damage + knockback, 5 sec cooldown)
 */
public class BerserkerAxe extends AbstractCustomItem {

    // Data storage for the players
    private final Map<UUID, Double> damageMultipliers = new HashMap<>();
    private final Map<UUID, Integer> rageLevels = new HashMap<>();
    private final Map<UUID, Long> leftClickCooldowns = new HashMap<>(); // Left click cooldown
    private final Map<UUID, Integer> lastAppliedLevel = new HashMap<>(); // Last applied level
    
    private static final long LEFT_CLICK_COOLDOWN_MS = 5000; // 5 second cooldown on left click

    @Override
    public String getId() { return "berserker_axe"; }

    @Override
    public String getDisplayName() { return "&4&l🔥 Berserker Axe"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_AXE; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7&o\"The less blood, the sharper the blade\"",
            "",
            " &4⚔ &fRage mechanic:",
            " &7• 100-75% HP: &cx1.5 damage",
            " &7• 75-50% HP: &cx2 damage",
            " &7• 50-25% HP: &cx3 damage + regeneration",
            " &7• 25-0% HP: &cx5 damage + invisibility!",
            "",
            " &c🔥 &fOn kill: +2 hearts",
            " &4💀 &fOn death: RAGE EXPLOSION!",
            " &c⚡ &fLeft Click: Rage Scream (5 sec cooldown)",
            ""
        );
    }

    @Override
    public String getItemModel() { return "berserker_axe"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getPeriodicInterval() { return 40; } // Every 2 seconds (less load)

    @Override
    public long getClickCooldown() { return 1000; } // 1 second cooldown between hits

    /**
     * Called every 2 seconds - we update the rage level
     */
    @Override
    public void onPeriodic(Player player) {
        updateRageLevel(player);
    }

    /**
     * Updates the rage level based on HP
     */
    private void updateRageLevel(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = player.getHealth();
        double healthPercent = (currentHealth / maxHealth) * 100;

        int newLevel;
        double newMultiplier;

        if (healthPercent > 75) {
            newLevel = 1;
            newMultiplier = 1.5;
        } else if (healthPercent > 50) {
            newLevel = 2;
            newMultiplier = 2.0;
        } else if (healthPercent > 25) {
            newLevel = 3;
            newMultiplier = 3.0;
        } else {
            newLevel = 4;
            newMultiplier = 5.0;
        }

        UUID uuid = player.getUniqueId();
        Integer oldLevel = rageLevels.getOrDefault(uuid, 0);

        // Store the level and the multiplier
        rageLevels.put(uuid, newLevel);
        damageMultipliers.put(uuid, newMultiplier);

        // Show the notification ONLY when the level changes
        if (oldLevel != newLevel) {
            showRageChange(player, newLevel, newMultiplier);
            // Apply the effects ONLY when the level changes
            applyRageEffects(player, newLevel);
            lastAppliedLevel.put(uuid, newLevel);
        }
    }

    /**
     * Shows the notification about the rage change
     */
    private void showRageChange(Player player, int level, double multiplier) {
        switch (level) {
            case 1:
                player.sendTitle("", "", 0, 0, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 2f);
                break;
            case 2:
                player.sendTitle(
                    ChatColor.DARK_RED + "⚡ RAGE I",
                    ChatColor.GRAY + "x1.5 damage + Speed",
                    5, 30, 10
                );
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 20);
                break;
            case 3:
                player.sendTitle(
                    ChatColor.RED + "🔥 RAGE II",
                    ChatColor.GRAY + "x3 damage + Regeneration!",
                    5, 40, 10
                );
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 50);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 30);
                break;
            case 4:
                player.sendTitle(
                    ChatColor.DARK_RED + "" + ChatColor.BOLD + "💀 RAGE III",
                    ChatColor.RED + "x5 damage + INVISIBILITY!",
                    5, 60, 15
                );
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 100);
                player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 2, 0), 50);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30);
                break;
        }
    }

    /**
     * Applies the effects depending on the rage level
     */
    private void applyRageEffects(Player player, int level) {
        // Remove the old effects
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        switch (level) {
            case 2:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
                break;
            case 3:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));
                break;
            case 4:
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 3, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false));
                break;
        }
    }

    /**
     * On hit - we deal BONUS damage based on the rage
     */
    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        double multiplier = damageMultipliers.getOrDefault(uuid, 1.0);

        // Bonus damage
        double baseDamage = event.getDamage();
        double bonusDamage = baseDamage * (multiplier - 1.0);
        event.setDamage(baseDamage + bonusDamage);

        LivingEntity target = null;
        if (event.getEntity() instanceof LivingEntity) {
            target = (LivingEntity) event.getEntity();
        }

        if (target != null) {
            int level = rageLevels.getOrDefault(uuid, 1);

            // Particles on hit
            switch (level) {
                case 2:
                    target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0), 15);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.5f);
                    break;
                case 3:
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 25);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);
                    break;
                case 4:
                    target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 40);
                    target.getWorld().spawnParticle(Particle.LAVA, target.getLocation().add(0, 1, 0), 15);
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 1);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
                    
                    // Invisibility on hit (maximum rage)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30);
                    break;
            }
        }
    }

    /**
     * On kill - healing
     */
    @Override
    public void onKill(Player killer, Player victim) {
        double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(killer.getHealth() + 4, maxHealth);
        killer.setHealth(newHealth);

        killer.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 2, 0), 15);
        killer.getWorld().spawnParticle(Particle.FLAME, killer.getLocation().add(0, 1, 0), 20);
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        killer.sendTitle(
            ChatColor.GREEN + "+2 ❤",
            ChatColor.GRAY + "Health theft!",
            5, 30, 10
        );
    }

    /**
     * On death - RAGE EXPLOSION!
     */
    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        double explosionRadius = 10.0;
        double explosionDamage = 20.0;

        Location loc = player.getLocation();
        player.getWorld().createExplosion(
            loc.getX(), loc.getY(), loc.getZ(),
            0f, false, false
        );

        // Visual effects
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 150);
        player.getWorld().spawnParticle(Particle.LAVA, loc, 80);
        player.getWorld().spawnParticle(Particle.SMOKE, loc, 40);
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);

        // Damage to everything in the radius
        for (Entity entity : player.getNearbyEntities(explosionRadius, explosionRadius, explosionRadius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(explosionDamage, player);
                living.setFireTicks(80);
                living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 15);
            }
        }

        // Message
        for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).sendMessage(ChatColor.RED + "💀 " + ChatColor.DARK_RED + player.getName() + " exploded in rage!");
            }
        }
    }

    /**
     * On Left Click - Rage Scream (AoE damage + knockback)
     * Cooldown: 5 seconds
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check the cooldown
        Long lastClick = leftClickCooldowns.get(uuid);
        if (lastClick != null && (currentTime - lastClick) < LEFT_CLICK_COOLDOWN_MS) {
            long remaining = LEFT_CLICK_COOLDOWN_MS - (currentTime - lastClick);
            double secondsLeft = remaining / 1000.0;
            player.sendTitle(
                "",
                ChatColor.RED + "Cooldown: " + String.format("%.1f", secondsLeft) + " sec",
                0, 20, 0
            );
            return;
        }
        
        // Set the cooldown
        leftClickCooldowns.put(uuid, currentTime);
        
        // AoE damage to everything within 5 blocks
        double radius = 5.0;
        double damage = 8.0;
        int hitCount = 0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity living = (LivingEntity) entity;
                living.damage(damage, player);
                living.setFireTicks(40);
                hitCount++;

                // Knockback
                org.bukkit.util.Vector knockback = living.getLocation().toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.5);
                living.setVelocity(knockback);
            }
        }

        // Effects
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 80);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 40);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
        
        player.sendTitle(
            ChatColor.RED + "💀 RAGE SCREAM",
            ChatColor.GRAY + "Area damage! Hit: " + hitCount,
            5, 30, 10
        );
    }
}
