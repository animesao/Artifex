import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mjolnir — legendary hammer featuring interactive Action Bar Cooldown HUD.
 *
 * When clicked during cooldown, it displays a graphical progress meter:
 *   ⏳ Мьёльнир (3.2с) [■■■■□□□□□□]
 */
public class MjolnirItem extends AbstractCustomItem {

    private static final long COOLDOWN_MS = 5000;
    private final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();

    @Override
    public String getId() { return "mjolnir_hammer"; }

    @Override
    public String getDisplayName() { return "&#ffd700⚡ Молот Мьёльнир"; }

    @Override
    public Material getMaterial() { return Material.MACE; }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7Оружие громовержца.",
            "&7Поражает врагов небесной молнией.",
            "",
            "&6✦ Способность: &fГрозовой удар",
            "&7ПКМ: призывает молнию в точку прицела",
            "&7Перезарядка: &e5 секунд"
        );
    }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastUsed.get(uuid);

        if (last != null && (now - last) < COOLDOWN_MS) {
            long remaining = COOLDOWN_MS - (now - last);
            // Live Action Bar Cooldown HUD
            ItemAPI.sendCooldownBar(player, getDisplayName(), remaining, COOLDOWN_MS);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 0.5f);
            return;
        }

        lastUsed.put(uuid, now);

        // Strike lightning forward
        org.bukkit.block.Block target = player.getTargetBlockExact(30);
        org.bukkit.Location strikeLoc = target != null ? target.getLocation() : player.getLocation().add(player.getLocation().getDirection().multiply(10));
        strikeLoc.getWorld().strikeLightning(strikeLoc);

        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        ItemAPI.actionbar(player, "&a⚡ Грозовой удар поразил цель!");
    }
}
