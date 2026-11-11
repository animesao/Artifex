import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 🚀 OPTIMIZED ITEM
 * 
 * This file shows how to create performant items.
 * 
 * Optimization rules:
 * 1. Use cooldowns (getClickCooldown)
 * 2. Limit particles (less than 100)
 * 3. Do not do heavy operations in onRightClick
 * 4. Cache the data
 */
public class optimization extends AbstractCustomItem {

    @Override
    public String getId() { return "optimization_sword"; }

    @Override
    public String getDisplayName() { return "&a&l⚡ Optimized Sword"; }

    @Override
    public Material getMaterial() { return Material.IRON_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7This item is optimized",
            " &7for maximum performance!",
            "",
            " &7Right Click: Healing (3 sec cooldown)",
            " &7Left Click: Damage (1 sec cooldown)",
            ""
        );
    }

    @Override
    public String getItemModel() { return "optimization_sword"; }

    @Override
    public long getClickCooldown() { return 3000; } // 3 seconds of cooldown!

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // ✅ OPTIMIZED:
        // - Few particles (20 instead of 1000)
        // - One sound
        // - Fast healing
        ItemAPI.heal(player, 8);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 2, 0), 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    @Override
    public void onDamageDealt(org.bukkit.event.entity.EntityDamageByEntityEvent event, Player player) {
        // ✅ OPTIMIZED:
        // - Few particles
        // - Simple logic
        event.setDamage(event.getDamage() * 1.5);
        event.getEntity().getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 15);
    }
}
