import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class AncientShield extends AbstractCustomItem {

    @Override
    public String getId() { return "ancient_shield"; }

    @Override
    public String getDisplayName() { return "&6🛡 &fAncient Shield"; }

    @Override
    public Material getMaterial() { return Material.SHIELD; }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Powerful shield of ancient warriors",
            " &7Increases maximum health",
            "",
            " &6✦ &fHealth: +4 hearts",
            " &e✦ &fResistance I",
            "",
            " &7Left Click - activate the barrier",
            " &7Right Click - regeneration"
        );
    }

    @Override
    public String getItemModel() { return "ancient_shield"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public void onEquip(Player player) {
        // Increase maximum health ( NamespacedKey method)
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            NamespacedKey key = new NamespacedKey("artifex", "ancient_shield_health");
            AttributeModifier modifier = new AttributeModifier(
                key,
                8, // +4 hearts = +8 HP
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.OFFHAND
            );
            player.getAttribute(Attribute.MAX_HEALTH).addTransientModifier(modifier);
        }

        // Grant resistance
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 999999, 0);

        ItemAPI.message(player, "&6🛡 &fAncient Shield activated!");
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1f);
    }

    @Override
    public void onUnequip(Player player) {
        // Remove the health increase
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            player.getAttribute(Attribute.MAX_HEALTH).getModifiers().stream()
                .filter(m -> m.getKey() != null && m.getKey().getKey().equals("ancient_shield_health"))
                .forEach(player.getAttribute(Attribute.MAX_HEALTH)::removeModifier);
        }

        // Remove resistance
        player.removePotionEffect(PotionEffectType.RESISTANCE);

        ItemAPI.message(player, "&6🛡 &fAncient Shield deactivated.");
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.5f);
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // Barrier activation
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 5, 1);
        ItemAPI.particles(player, Particle.ENCHANTED_HIT, 50);
        ItemAPI.sound(player, Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
        ItemAPI.title(player, "&6🛡 &fBARRIER!", "&7Resistance II for 5 sec!");
    }

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Regeneration
        ItemAPI.heal(player, 8); // +4 hearts
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 5, 1);
        ItemAPI.particles(player, Particle.HEART, 30);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        ItemAPI.title(player, "&a❤ &fREGENERATION!", "+8 HP");
    }
}
