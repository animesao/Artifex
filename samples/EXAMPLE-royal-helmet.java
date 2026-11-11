import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Example of a royal helmet (armor) on the Java API.
 * 
 * Grants defensive effects when equipped.
 * Reflects damage in combat.
 */
public class RoyalHelmet extends AbstractCustomItem {

    @Override
    public String getId() { return "royal_helmet"; }

    @Override
    public String getDisplayName() { return "&6👑 Royal Helmet"; }

    @Override
    public Material getMaterial() { return Material.NETHERITE_HELMET; }

    @Override
    public java.util.List<String> getLore() {
        return java.util.List.of(
            "",
            " &7Helmet of ancient kings",
            "",
            " &6Passive: &fResistance I",
            " &6On hit: &fDamage reflection",
            ""
        );
    }

    @Override
    public String getItemModel() { return "royal_helmet"; }

    @Override
    public boolean isUnbreakable() { return true; }

    @Override
    public boolean isGlowing() { return true; }

    @Override
    public String getActivationSlot() { return "HEAD"; }

    @Override
    public String getType() { return "ARMOR"; }

    @Override
    public java.util.List<me.dcplugin.artifex.api.RecipeDef> getRecipes() {
        // The recipe works both in the regular crafting table and in the GUI craft (/craft).
        // Ingredients: a material (NETHERITE_INGOT) or the ID of another custom
        // item — YAML or Java (for example "vampire-blade").
        java.util.Map<Character, String> keys = new java.util.HashMap<>();
        keys.put('N', "NETHERITE_INGOT");
        return java.util.List.of(
            me.dcplugin.artifex.api.RecipeDef.shaped(
                java.util.List.of("NNN", "N N"), keys)
        );
    }

    @Override
    public long getPeriodicInterval() { return 60; } // Every 3 seconds

    @Override
    public void onPeriodic(Player player) {
        // Passive resistance effect
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 5, 1);
        ItemAPI.particles(player, Particle.ENCHANT, 5);
    }

    @Override
    public void onEquip(Player player) {
        ItemAPI.effect(player, PotionEffectType.RESISTANCE, 999, 1);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1f);
        ItemAPI.particles(player, Particle.TOTEM_OF_UNDYING, 30);
        ItemAPI.title(player, "&6👑 ROYAL HELMET", "&7The power of kings is with you!");
    }

    @Override
    public void onUnequip(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        ItemAPI.sound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.8f);
    }

    @Override
    public void onDamageTaken(org.bukkit.event.entity.EntityDamageEvent event, Player player) {
        // Reflect 30% of the damage
        double reflected = event.getDamage() * 0.3;
        event.setDamage(event.getDamage() * 0.7);
        
        if (event.getDamageSource().getCausingEntity() instanceof Player) {
            Player attacker = (Player) event.getDamageSource().getCausingEntity();
            attacker.damage(reflected);
            ItemAPI.message(attacker, "&6👑 Damage reflected!");
        }
        
        ItemAPI.particlesAt(player.getLocation(), Particle.TOTEM_OF_UNDYING, 20);
        ItemAPI.sound(player, Sound.ITEM_SHIELD_BLOCK, 1f, 1.5f);
    }
}
