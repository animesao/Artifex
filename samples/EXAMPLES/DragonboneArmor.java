import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.api.RecipeDef;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/**
 * 🐉 Dragonbone Armor — a complete reference example of a Java item.
 *
 * The file is in items/EXAMPLES/ — this folder is NOT loaded and NOT copied
 * to the server (see DefaultContentExtractor and ItemRegistry). To enable
 * the item: copy the file into plugins/Artifex/items/ (or into any
 * other subfolder) and run /ci reload.
 *
 * What the example shows:
 *   1. getRecipes()            — crafting recipes (shaped + shapeless). They work
 *                                in the regular crafting table AND in the GUI /craft. An ingredient
 *                                can be a material or the ID of a custom
 *                                item — YAML or Java.
 *   2. getActivationSlot/type  — armor: it only works in the CHEST slot.
 *   3. onEquip/onUnequip       — equip hooks (the global checker calls them;
 *                                CustomItemEquipEvent fires before them).
 *   4. getPeriodicInterval+onPeriodic — a passive effect while the item is worn.
 *   5. onDamageTaken           — a combat hook (defense + particles).
 *
 * The item identity is the PDC key dcustomitems:custom_item_id (ItemAPI),
 * so recipes/events see it exactly like the YAML items.
 */
public class DragonboneArmor extends AbstractCustomItem {

    // ===== IDENTIFICATION =====

    @Override
    public String getId() {
        return "dragonbone_armor";
    }

    @Override
    public String getDisplayName() {
        return "&6🐉 Dragonbone Chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7Armor forged from the bones",
            " &7of an ancient dragon.",
            "",
            " &6Passive: &fStrength II + Regeneration I",
            " &6On hit: &f-25% damage",
            "",
            " &8Craft: 4x NETHERITE_INGOT, 4x vampire-blade, 1x dragon_breath",
            ""
        );
    }

    @Override
    public String getItemModel() {
        return "dragonbone_armor";
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    // ===== SLOT AND TYPE =====

    /** The item is only active when worn in the chestplate slot. */
    @Override
    public String getActivationSlot() {
        return "CHEST";
    }

    @Override
    public String getType() {
        return "ARMOR";
    }

    // ===== CRAFTING RECIPES =====

    /**
     * Shaped: a 3x3 grid, a space = an empty cell.
     * 'B' refers to ANOTHER custom item by its ID (vampire-blade —
     * a regular YAML item; you can also refer to a Java item or to itself).
     */
    @Override
    public List<RecipeDef> getRecipes() {
        Map<Character, String> keys = Map.of(
            'N', "NETHERITE_INGOT", // a material
            'B', "vampire-blade",   // a YAML item as an ingredient
            'D', "dragon_breath"    // a vanilla material (the dragon drop)
        );
        return List.of(
            // 3 rows of the same length (maximum 3x3)
            RecipeDef.shaped(List.of(
                "NBN",
                "NDN",
                "N N"
            ), keys),
            // A shapeless variant: 6 diamonds + the item itself (repair/upgrade)
            RecipeDef.shapeless(List.of(
                "DIAMOND", "DIAMOND", "DIAMOND",
                "DIAMOND", "DIAMOND", "DIAMOND",
                "dragonbone_armor"
            ))
        );
    }

    // ===== EQUIPPING =====

    /** Called when it is equipped (after CustomItemEquipEvent). */
    @Override
    public void onEquip(Player player) {
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 999, 2);   // Strength II
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 999, 1);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.9f);
        ItemAPI.particles(player, Particle.DRAGON_BREATH, 30);
        ItemAPI.message(player, "&6🐉 The power of the ancient dragon flows in your veins!");
    }

    /** Called when it is removed (after CustomItemEquipEvent). */
    @Override
    public void onUnequip(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.5f);
        ItemAPI.message(player, "&7The power of the dragon has left you.");
    }

    /**
     * A passive effect while the chestplate is worn: called by the global checker
     * every getPeriodicInterval() ticks. Here the effects are refreshed so that
     * another source cannot "eat" them.
     */
    @Override
    public long getPeriodicInterval() {
        return 60; // every 3 seconds
    }

    @Override
    public void onPeriodic(Player player) {
        ItemAPI.effect(player, PotionEffectType.STRENGTH, 6, 2);
        ItemAPI.effect(player, PotionEffectType.REGENERATION, 6, 1);
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : 20.0;
        if (player.getHealth() < maxHealth) {
            ItemAPI.particles(player, Particle.DRAGON_BREATH, 3);
        }
    }

    // ===== COMBAT =====

    /** Defense: -25% incoming damage while the chestplate is worn. */
    @Override
    public void onDamageTaken(EntityDamageEvent event, Player player) {
        event.setDamage(event.getDamage() * 0.75);
        ItemAPI.particlesAt(player.getLocation(), Particle.DRAGON_BREATH, 10);
        ItemAPI.sound(player, Sound.BLOCK_ANVIL_PLACE, 0.4f, 1.6f);
    }

    // ===== MISC =====

    /** The click cooldown (if you add onRightClick/onLeftClick). */
    @Override
    public long getClickCooldown() {
        return 1500;
    }

    /** The permission to use it (null = no restrictions). */
    @Override
    public String getPermission() {
        return null;
    }
}
