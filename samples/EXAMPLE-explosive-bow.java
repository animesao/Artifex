import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class ExplosiveBow extends AbstractCustomItem {

    @Override
    public String getId() { return "explosive_bow"; }

    @Override
    public String getDisplayName() { return "&c🏹 &fExplosive Bow"; }

    @Override
    public Material getMaterial() { return Material.BOW; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Shoots explosive arrows",
            " &7Every arrow causes an explosion",
            "",
            " &c✦ &fDamage: x2",
            " &e✦ &fExplosion: 3 blocks",
            "",
            " &7Right Click - charge up the power"
        );
    }

    @Override
    public String getItemModel() { return "explosive_bow"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public void onBowShoot(EntityShootBowEvent event, Player player, Arrow arrow) {
        // Mark the arrow as explosive
        arrow.setMetadata("explosive", new FixedMetadataValue(getPlugin(), true));
        arrow.setMetadata("power", new FixedMetadataValue(getPlugin(), 2));

        // Effects
        ItemAPI.particles(player, Particle.FLAME, 30);
        ItemAPI.sound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        ItemAPI.message(player, "&c🏹 &7Explosive arrow released!");
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Charge up the power
        ItemAPI.effect(player, org.bukkit.potion.PotionEffectType.STRENGTH, 10, 1);
        ItemAPI.particles(player, Particle.CRIT, 50);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(player, "&c🔥 &fPOWER!", "&7Arrows will become stronger!");
    }

    private me.dcplugin.artifex.Main getPlugin() {
        return me.dcplugin.artifex.Main.getInstance();
    }
}
