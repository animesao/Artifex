import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Runebound Greatsword — socketable custom weapon.
 *
 * Starts with 3 open sockets that can accept gems and runes
 * via inventory drag & drop!
 */
public class RuneboundBladeItem extends AbstractCustomItem {

    @Override
    public String getId() { return "runebound_blade"; }

    @Override
    public String getDisplayName() { return "&#00ffc8⚔ Рунный Клинок"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7Великий клинок с магическими гнёздами,",
            "&7в которые можно вставлять самоцветы.",
            "",
            "&6✦ Базовые свойства:",
            " &7• &fУрон: &c+9.0",
            " &7• &fПрочность: &aНеразрушим"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public ItemStack createItem() {
        ItemStack item = super.createItem();
        // Initialize with 3 empty gem sockets
        return ItemAPI.setSockets(item, 3);
    }

    @Override
    public void onHit(EntityDamageByEntityEvent event, Player player) {
        if (event.getEntity() instanceof LivingEntity target) {
            int usedSockets = ItemAPI.getUsedSockets(player.getInventory().getItemInMainHand());
            if (usedSockets > 0) {
                // Bonus damage based on socket count
                event.setDamage(event.getDamage() + (usedSockets * 2.5));
                target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.4f);
            }
        }
    }
}
