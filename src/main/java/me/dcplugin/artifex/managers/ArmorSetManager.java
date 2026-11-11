package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorSetManager {

    private final Main plugin;
    private final Map<String, List<String>> armorSetEffects;
    private final Map<String, Integer> requiredPieces;

    public ArmorSetManager(Main plugin) {
        this.plugin = plugin;
        this.armorSetEffects = new HashMap<>();
        this.requiredPieces = new HashMap<>();
        loadSetBonuses();
    }

    private void loadSetBonuses() {
        armorSetEffects.clear();
        requiredPieces.clear();

        org.bukkit.configuration.ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("set-bonuses");
        if (section == null) return;

        for (String setId : section.getKeys(false)) {
            List<String> effects = plugin.getConfig().getStringList("set-bonuses." + setId + ".effects");
            armorSetEffects.put(setId, effects == null ? Collections.emptyList() : effects);
            int pieces = plugin.getConfig().getInt("set-bonuses." + setId + ".required-pieces",
                    plugin.getConfig().getInt("set-bonuses." + setId + ".pieces", 4));
            requiredPieces.put(setId, Math.max(1, pieces));
        }
    }

    public int countSetPieces(Player player, String setId) {
        if (player == null || setId == null || setId.isEmpty()) {
            return 0;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        int setPieces = 0;

        for (ItemStack armorPiece : armor) {
            if (armorPiece != null && armorPiece.getType() != org.bukkit.Material.AIR) {
                String itemId = plugin.getItemHandler().getCustomItemId(armorPiece);
                if (itemId != null) {
                    var customItem = plugin.getItemHandler().getCustomItem(itemId);
                    if (customItem != null && setId.equals(customItem.getArmorSetId())) {
                        setPieces++;
                    }
                }
            }
        }

        return setPieces;
    }

    public int getRequiredPieces(String setId) {
        if (setId == null) return 4;
        return requiredPieces.getOrDefault(setId, 4);
    }

    public boolean hasFullSet(Player player, String setId) {
        return countSetPieces(player, setId) >= getRequiredPieces(setId);
    }

    public List<String> getSetEffects(String setId) {
        if (setId == null) return Collections.emptyList();
        return armorSetEffects.getOrDefault(setId, Collections.emptyList());
    }

    public void reload() {
        loadSetBonuses();
        // The cache of the parsed set-bonus effects is reset as well,
        // otherwise the old values would be used after a reload.
        plugin.getEffectManager().invalidateSetBonusCache();
    }
}
