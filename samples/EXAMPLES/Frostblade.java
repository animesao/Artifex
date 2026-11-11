import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.api.RecipeDef;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/**
 * ❄ Frostblade — a complete reference example of a Java WEAPON (clicks + cooldown).
 *
 * The file is in items/EXAMPLES/ — the folder is NOT copied to the server and is NOT
 * compiled at runtime (see DefaultContentExtractor / ItemRegistry).
 * To enable it: copy it into plugins/Artifex/items/ and /ci reload,
 * then /api-item give frostblade.
 *
 * What the example shows:
 *   1. onRightClick / onLeftClick — the click hooks (called by ApiEventListener,
 *      only while the item is in the hand; Right/Left Click on a block/air).
 *   2. getClickCooldown() — the cooldown between clicks in milliseconds.
 *      ⚠️ In the current implementation this is ONE timer per player: both clicks (LMB and
 *      RMB) share it. Do not set different cooldowns for the left and right button.
 *   3. onDamageDealt — the hook when you hit an entity (LMB on a mob goes here,
 *      and NOT into onLeftClick).
 *   4. onKill — a player kill (the hook only receives Player victims).
 *   5. getRecipes() — crafting in the crafting table and in the GUI /craft.
 *
 * getPermission() is checked on clicks (LMB/RMB), just like for YAML items:
 * without the permission the click is cancelled and the item does not trigger.
 */
public class Frostblade extends AbstractCustomItem {

    // ===== IDENTIFICATION =====

    @Override
    public String getId() {
        return "frostblade";
    }

    @Override
    public String getDisplayName() {
        return "&b❄ Frost Blade";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_SWORD;
    }

    @Override
    public List<String> getLore() {
        return List.of(
            "",
            " &7A blade from the eternal frost.",
            "",
            " &bLeft Click: &fFrost dash forward",
            " &bRight Click: &fFrost explosion (freeze + damage)",
            " &bHits: &fslow down the target and add damage",
            "",
            " &8Cooldown: 1.5 sec"
        );
    }

    @Override
    public String getItemModel() {
        // Resource pack: assets/minecraft/models/item/frostblade.json
        return "frostblade";
    }

    @Override
    public boolean isUnbreakable() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    /**
     * The shared click cooldown (LMB and RMB) in milliseconds.
     * While the cooldown is active the click is ignored completely (and the
     * CustomItemUseEvent does not fire).
     */
    @Override
    public long getClickCooldown() {
        return 1500;
    }

    // ===== CRAFTING =====

    @Override
    public List<RecipeDef> getRecipes() {
        Map<Character, String> keys = Map.of(
            'N', "NETHERITE_INGOT",
            'B', "BLUE_ICE",
            'S', "STICK"
        );
        return List.of(RecipeDef.shaped(List.of(
            " N ",
            "NSN",
            "B B"
        ), keys));
    }

    // ===== CLICKS =====

    /**
     * RMB — Frost explosion: AoE around the player.
     * Slows down and knocks back the living entities, deals damage (4 HP).
     */
    @Override
    public void onRightClick(PlayerInteractEvent event, Player player) {
        double radius = 5.0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) continue;
            LivingEntity target = (LivingEntity) entity;

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 1, false, true));
            target.damage(4.0, player);

            // A slight push away from the player
            org.bukkit.util.Vector away = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            target.setVelocity(away.multiply(0.9).setY(0.4));
        }

        ItemAPI.particlesCustom(player, Particle.SNOWFLAKE, 120, 2, 2, 2);
        ItemAPI.sound(player, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
        ItemAPI.sound(player, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
        ItemAPI.title(player, "&b❄ FROST EXPLOSION", "&fEverything around freezes");
    }

    /**
     * LMB — Frost dash: a dash forward + a frost trail.
     * (Attacking mobs goes into onDamageDealt, not here.)
     */
    @Override
    public void onLeftClick(PlayerInteractEvent event, Player player) {
        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(1.6).setY(0.15));

        ItemAPI.particles(player, Particle.SNOWFLAKE, 60);
        ItemAPI.sound(player, Sound.ENTITY_SNOWBALL_THROW, 1f, 1.4f);
    }

    // ===== COMBAT =====

    /**
     * A hit on the target: freezes it (Slowness III for 3 sec) and adds damage.
     */
    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event, Player player) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 2, false, true));
            event.setDamage(event.getDamage() + 3.0);
        }

        ItemAPI.particlesAt(event.getEntity().getLocation(), Particle.ITEM_SNOWBALL, 15);
        ItemAPI.sound(player, Sound.BLOCK_GLASS_BREAK, 0.5f, 2f);
    }

    /**
     * A player kill: "shards" + a bit of absorption for the winner.
     * Note: onKill is only called when a PLAYER is killed (PlayerDeathEvent).
     */
    @Override
    public void onKill(Player killer, Player victim) {
        ItemAPI.particlesAt(victim.getLocation(), Particle.SNOWFLAKE, 200);
        ItemAPI.sound(killer, Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
        ItemAPI.effect(killer, PotionEffectType.ABSORPTION, 30, 2);
        ItemAPI.message(killer, "&b❄ The victim shattered into ice!");
    }
}
