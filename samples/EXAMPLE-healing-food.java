import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class HealingFood extends AbstractCustomItem {

    @Override
    public String getId() { return "healing_food"; }

    @Override
    public String getDisplayName() { return "&a🍎 &fMagic Apple"; }

    @Override
    public Material getMaterial() { return Material.GOLDEN_APPLE; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Restores health and",
            " &7grants powerful effects",
            "",
            " &7Right Click - eat",
            "",
            " &a✦ &fHealing: +8 hearts",
            " &b✦ &fSpeed II: 30 sec",
            " &e✦ &fResistance I: 30 sec"
        );
    }

    @Override
    public String getItemModel() { return "healing_food"; }

    @Override
    public boolean hasCooldown() { return true; }

    @Override
    public int getCooldownSeconds() { return 30; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Healing
        ItemAPI.heal(player, 16); // +8 hearts

        // Effects
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 1);
        ItemAPI.effect(player, PotionEffectType.DAMAGE_RESISTANCE, 30, 0);

        // Effects
        ItemAPI.particles(player, Particle.HEART, 30);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        // Title
        ItemAPI.title(player, "&a🍎 &fMagic Apple", "&7Health restored!");

        // Remove the item from the hand
        player.getInventory().getItemInMainHand().setAmount(0);
    }
}
