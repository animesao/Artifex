package me.dcplugin.artifex.utils;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Shared visual effects (particles and sounds) for custom items.
 * Previously the logic was duplicated in PlayerListener and EquippedItemsChecker.
 */
public final class ItemFX {

    private ItemFX() {}

    /**
     * Spawns the particles and sounds on equipping/unequipping an item.
     *
     * @param plugin   the plugin instance (for the logs)
     * @param player   the player
     * @param item     the item
     * @param isEquip  true = equip (equip-*), false = unequip (unequip-*)
     */
    public static void spawnEquipEffects(Main plugin, Player player, CustomItem item, boolean isEquip) {
        List<String> particles = isEquip ? item.getEquipParticles() : item.getUnequipParticles();
        List<String> sounds = isEquip ? item.getEquipSounds() : item.getUnequipSounds();

        for (String particleStr : particles) {
            try {
                String[] parts = particleStr.split(":");
                Particle particle = EnumCache.getParticle(parts[0]);
                if (particle == null) continue;
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
            } catch (Exception e) {
                plugin.getLogger().warning("Error while spawning particles: " + particleStr);
            }
        }

        for (String soundStr : sounds) {
            try {
                String[] parts = soundStr.split(":");
                Sound sound = EnumCache.getSound(parts[0]);
                if (sound == null) continue;
                float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Error while playing sound: " + soundStr);
            }
        }
    }
}