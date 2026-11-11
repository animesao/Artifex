import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Example of a Phoenix talisman on the Java API.
 * 
 * On death it resurrects the player with effects.
 * Usage: automatically on death.
 */
public class PhoenixTotem extends AbstractCustomItem {

    @Override
    public String getId() { return "phoenix_totem"; }

    @Override
    public String getDisplayName() { return "&c🔥 Phoenix Talisman"; }

    @Override
    public Material getMaterial() { return Material.TOTEM_OF_UNDYING; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Ancient talisman crafted from",
            " &7the ashes of a fire bird",
            "",
            " &cEffect:",
            " &f• Resurrect on death",
            " &f• Full healing",
            " &f• Strength + Speed for 30 sec",
            "",
            " &7Cooldown: 5 minutes"
        );
    }

    @Override
    public String getItemModel() { return "phoenix_totem"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getActivationSlot() { return "OFFHAND"; }

    @Override
    public long getClickCooldown() { return 300000; } // 5 minutes

    @Override
    public void onDeath(Player player, org.bukkit.event.entity.PlayerDeathEvent event) {
        // Cancel the death and resurrect
        event.setCancelled(true);
        
        // Resurrect with full health
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        
        // Grant powerful effects
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 30, 2);
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 2);
        ItemAPI.effect(player, PotionEffectType.FIRE_RESISTANCE, 60, 1);
        
        // Resurrection effects
        ItemAPI.particles(player, Particle.FLAME, 200);
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 100);
        ItemAPI.sound(player, Sound.ITEM_TOTEM_USE, 2f, 0.5f);
        ItemAPI.title(player, "&c🔥 PHOENIX!", "&7You rose from the ashes!");
        
        // Take the talisman
        player.getInventory().removeItem(org.bukkit.inventory.ItemStack.from(
            new org.bukkit.inventory.ItemStack(Material.TOTEM_OF_UNDYING)
        ));
    }

    @Override
    public void onRightClick(org.bukkit.event.player.PlayerInteractEvent event, Player player) {
        ItemAPI.message(player, "&c🔥 Phoenix Talisman is ready to protect you!");
        ItemAPI.particles(player, Particle.FLAME, 20);
        ItemAPI.sound(player, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
    }
}
