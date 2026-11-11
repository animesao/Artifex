/**
 * ============================================================
 * EXAMPLE: DARK BLADE - Java item with a custom model
 * ============================================================
 *
 * 📁 File structure:
 *
 * plugins/Artifex/
 * └── items/
 *     └── dark-sword.java          ← this file
 *
 * 🎮 Commands:
 *   /ci reload                     - reload the items
 *   /api-item give dark_sword      - give the item
 *   /api-item info dark_sword      - information about the item
 *
 * ============================================================
 */

import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

public class DarkSword extends AbstractCustomItem {

    // ===== MAIN PROPERTIES =====

    @Override
    public String getId() { return "dark_sword"; }

    @Override
    public String getDisplayName() { return "&5Dark Blade"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_SWORD; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Dark energy pulses in the blade",
            "",
            " &5Left Click: &7Dark wave (AoE damage)",
            " &5Right Click: &7Invisibility for 5 sec",
            "",
            " &5Damage: &c+12",
            " &5Vampirism chance: &c30%"
        );
    }

    // ====================================================================
    // 🎨 CUSTOM MODEL - PICK ONE OF THE VARIANTS:
    // ====================================================================
    //
    // VARIANT 1: item-model (recommended for 1.21.11+)
    // --------------------------------------------------------------------
    // Uses the custom_model_data component with strings
    // Command format: /give @s minecraft:netherite_sword[minecraft:custom_model_data={strings:["dark_sword"]}]
    //
    // In the resource pack the file: assets/minecraft/models/item/dark_sword.json
    // Example JSON:
    // {
    //   "parent": "minecraft:item/handheld",
    //   "textures": {
    //     "0": "minecraft:item/dark_sword"
    //   }
    // }
    //
    @Override
    public String getItemModel() { return "minecraft:item/smoke"; }

    //
    // VARIANT 2: item-model with a full namespace
    // --------------------------------------------------------------------
    // If the model in your resource pack has a unique namespace
    // Format: "namespace:path/to/model"
    //
    // Examples:
    //   "myplugin:item/dark_sword"     → assets/myplugin/models/item/dark_sword.json
    //   "minecraft:item/smoke"         → assets/minecraft/models/item/smoke.json
    //   "custom:weapons/sword"         → assets/custom/models/weapons/sword.json
    //
    // Uncomment the variant you need:
    //
    // @Override
    // public String getItemModel() { return "minecraft:item/smoke"; }
    //
    // @Override
    // public String getItemModel() { return "myplugin:item/dark_sword"; }
    //
    //
    // VARIANT 3: custom-model-data (legacy, for older versions)
    // --------------------------------------------------------------------
    // Uses the numeric custom_model_data value
    // Command format: /give @s minecraft:netherite_sword{CustomModelData:10018}
    //
    // Not recommended for 1.21.11+, use getItemModel()
    //
    // @Override
    // public int getCustomModelData() { return 10018; }
    //
    // ====================================================================

    // ===== ADDITIONAL PROPERTIES =====

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public long getClickCooldown() { return 1000; }

    @Override
    public String getPermission() { return "items.dark_sword"; }

    // ===== TRIGGERS =====

    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        // Right Click - Invisibility for 5 sec
        ItemAPI.vanish(player, 5);
        ItemAPI.particles(player, Particle.SMOKE, 50);
        ItemAPI.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        ItemAPI.title(player, "&5DARK SHADOW", "&7You have become invisible!");
    }

    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        // Left Click - Dark wave (AoE damage)
        ItemAPI.damageNearby(player, 8, 5);
        ItemAPI.particles(player, Particle.SMOKE, 100);
        ItemAPI.sound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2f);
        ItemAPI.title(player, "&5DARK WAVE", "");
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        // +12 damage
        event.setDamage(event.getDamage() + 12);

        // 30% vampirism chance
        if (Math.random() < 0.30) {
            ItemAPI.heal(player, 3);
            ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.HEART, 10);
            ItemAPI.sound(player, Sound.ENTITY_PLAYER_BURP, 0.5f, 2f);
            ItemAPI.message(player, "&5You absorbed life!");
        }

        ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.SMOKE, 20);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        // On kill - healing + strength
        ItemAPI.heal(killer, 5);
        ItemAPI.effect(killer, PotionEffectType.STRENGTH, 15, 2);
        ItemAPI.particles(killer, Particle.SMOKE, 80);
        ItemAPI.sound(killer, Sound.ENTITY_WITHER_DEATH, 0.3f, 2f);
        ItemAPI.title(killer, "&5KILL!", "&7+5 ❤ &5+Strength II");
    }

    @Override
    public void onEquip(Player player) {
        // When equipped
        ItemAPI.effect(player, PotionEffectType.DARKNESS, 999, 1);
        ItemAPI.particles(player, Particle.SMOKE, 30);
    }

    @Override
    public void onUnequip(Player player) {
        // When removed
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }
}
