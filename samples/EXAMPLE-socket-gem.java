import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Ruby Gem of Power — socketable gem for custom weapons and armor.
 *
 * Players can drag and drop this item onto any weapon with open sockets
 * to permanently inlay it and gain bonus effects.
 */
public class RubyGemItem extends AbstractCustomItem {

    @Override
    public String getId() { return "ruby_gem"; }

    @Override
    public String getDisplayName() { return "&#ff2244✦ Рубин Ярости"; }

    @Override
    public Material getMaterial() { return Material.RUBY_TEXTURE_PLACEHOLDER(); }

    private static Material RUBY_TEXTURE_PLACEHOLDER() {
        return Material.REDSTONE;
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "&7Древний драгоценный камень,",
            "&7наполненный первородным огнём.",
            "",
            "&6✦ Характеристики сокета:",
            " &c• &fУрон от огня: &c+3.0",
            " &e• &fОгненные искры при ударе",
            "",
            "&8Перетащите на предмет с открытым сокетом"
        );
    }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public ItemStack createItem() {
        ItemStack item = super.createItem();
        // Convert to recognized socketable gem
        return ItemAPI.makeGem(item, "&#ff2244Рубин Ярости", "+3.0 Урон от огня");
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.5f);
        player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 15, 0.2, 0.2, 0.2, 0.05);
        player.sendMessage("§c✦ Рубин пульсирует огненным теплом. Перетащите его в инвентаре на оружие с сокетом.");
    }
}
