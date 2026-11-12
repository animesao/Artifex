import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Echo Shard — artifact that demonstrates custom drop tables.
 *
 * Configured to drop from Warden (100%), Enderman (5%), and Sculk Catalysts (10%).
 */
public class EchoShardItem extends AbstractCustomItem {

    @Override
    public String getId() { return "echo_shard"; }

    @Override
    public String getDisplayName() { return "&#00aaff✦ Осколок Эха"; }

    @Override
    public Material getMaterial() { return Material.ECHO_SHARD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7Пульсирующий фрагмент тёмных глубин.",
            "&7Издаёт низкочастотные звуковые волны.",
            "",
            "&6✦ Источники дропа:",
            " &7• &cХранитель (Warden): &a100% шанс",
            " &7• &dЭндермен: &a5% шанс",
            " &7• &3Блоки скалка: &a10% шанс",
            "",
            "&8Используется для создания мистических артефактов"
        );
    }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public void onPlayerKill(Player killer, org.bukkit.entity.Entity victim) {
        killer.playSound(killer.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.0f);
        killer.spawnParticle(Particle.SCULK_SOUL, killer.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }
}
