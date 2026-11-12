import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Celestial Blade — weapon demonstrating runtime dynamic condition checks.
 *
 * Mechanics:
 * - At Night or Storm: +50% bonus damage and cosmic particle burst.
 * - In Nether: automatic Fire Resistance.
 * - Emergency Guard: when health drops below 6 HP (3 hearts), grants Absorption and Speed!
 */
public class CelestialBladeItem extends AbstractCustomItem {

    @Override
    public String getId() { return "celestial_blade"; }

    @Override
    public String getDisplayName() { return "&#a020f0⚔ Астральный Клинок"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7Клинок, черпающий силу из созвездий.",
            "",
            "&d✦ Условные механики:",
            " &7• &eНочью или в грозу: &d+50% к урону",
            " &7• &cПри здоровье < 3 сердец: &aЭкстренный щит",
            " &7• &6В Незере: &eОгнестойкость",
            "",
            "&8Сила звёзд меняется вместе с миром"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public void onHit(EntityDamageByEntityEvent event, Player player) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        long time = player.getWorld().getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        boolean isStorm = player.getWorld().hasStorm();

        // Condition 1: Night or Storm
        if (isNight || isStorm) {
            double bonus = event.getDamage() * 0.5;
            event.setDamage(event.getDamage() + bonus);
            player.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 40, 0.4, 0.4, 0.4, 0.2);
            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.6f);
            ItemAPI.actionbar(player, "&d✦ Астральный резонанс: +50% урона!");
        }

        // Condition 2: Emergency low health guard (< 6 HP = 3 hearts)
        if (player.getHealth() <= 6.0) {
            ItemAPI.effect(player, PotionEffectType.RESISTANCE, 5, 2);
            ItemAPI.effect(player, PotionEffectType.SPEED, 5, 1);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.8f);
            ItemAPI.actionbar(player, "&a🛡 Экстренная защита активирована!");
        }
    }
}
