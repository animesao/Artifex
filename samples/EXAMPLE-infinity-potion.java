import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Example of an infinite potion on the Java API.
 * 
 * A consumable item that grants powerful effects.
 * Usage: Right Click while holding the item.
 */
public class InfinityPotion extends AbstractCustomItem {

    @Override
    public String getId() { return "infinity_potion"; }

    @Override
    public String getDisplayName() { return "&b✨ Potion of Infinity"; }

    @Override
    public Material getMaterial() { return Material.EXPERIENCE_BOTTLE; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Potion brewed from stardust",
            "",
            " &bEffect:",
            " &f• Speed III for 30 sec",
            " &f• Strength II for 30 sec",
            " &f• Resistance II for 30 sec",
            " &f• Full healing",
            "",
            " &c⚠ Single-use!"
        );
    }

    @Override
    public String getItemModel() { return "infinity_potion"; }

    @Override
    public void onRightClick(org.bukkit.event.player.PlayerInteractEvent event, Player player) {
        // Apply the powerful effects
        ItemAPI.effect(player, PotionEffectType.SPEED, 30, 3);        // Speed III
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 30, 2);     // Strength II
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 30, 2);   // Resistance II
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 10, 2); // Regeneration II
        
        // Full healing
        ItemAPI.heal(player, 20);
        
        // Effects
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 100);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 2f, 0.5f);
        ItemAPI.title(player, "&b✨ INFINITY!", "&7All powers awakened!");
        
        // Take the item
        event.getItem().setAmount(event.getItem().getAmount() - 1);
    }
}
