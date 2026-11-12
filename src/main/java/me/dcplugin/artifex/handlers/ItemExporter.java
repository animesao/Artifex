package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Service for exporting a held ItemStack into a clean, editable YAML configuration file
 * in the plugins/Artifex/items/ directory.
 */
public class ItemExporter {

    private final Main plugin;

    public static record ExportResult(boolean success, String message, File file, String itemId) {}

    public ItemExporter(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Exports the given ItemStack into items/<itemId>.yml.
     *
     * @param itemStack the item held in hand
     * @param rawId     the target item ID
     * @param overwrite whether to overwrite an existing configuration file or item ID
     * @return ExportResult containing status and details
     */
    public ExportResult exportItem(ItemStack itemStack, String rawId, boolean overwrite) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new ExportResult(false, "You must hold an item in your hand to export it.", null, null);
        }

        if (rawId == null || rawId.isBlank()) {
            return new ExportResult(false, "Item ID cannot be empty.", null, null);
        }

        String itemId = rawId.trim().toLowerCase(Locale.ROOT);
        if (!isValidId(itemId)) {
            return new ExportResult(false, "Item ID '" + itemId + "' is invalid. Use letters, numbers, underscores and hyphens.", null, itemId);
        }

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
        }

        File targetFile = new File(itemsDir, itemId + ".yml");
        if (targetFile.exists() && !overwrite) {
            return new ExportResult(false, "File 'items/" + itemId + ".yml' already exists. Use --force to overwrite.", targetFile, itemId);
        }

        if (plugin.getItemHandler() != null && plugin.getItemHandler().getCustomItem(itemId) != null && !overwrite) {
            return new ExportResult(false, "Custom item '" + itemId + "' is already registered in Artifex. Use --force to overwrite.", targetFile, itemId);
        }

        CustomItem existing = null;
        if (plugin.getItemHandler() != null) {
            String currentCustomId = plugin.getItemHandler().getCustomItemId(itemStack);
            if (currentCustomId != null) {
                existing = plugin.getItemHandler().getCustomItem(currentCustomId);
            }
        }

        String yaml = generateYaml(itemStack, itemId, existing);

        try {
            Files.writeString(targetFile.toPath(), yaml, StandardCharsets.UTF_8);
            if (plugin.getItemHandler() != null) {
                plugin.getItemHandler().reloadItems();
            }
            return new ExportResult(true, "Item '" + itemId + "' successfully exported to items/" + itemId + ".yml!", targetFile, itemId);
        } catch (Exception e) {
            return new ExportResult(false, "Failed to write items/" + itemId + ".yml: " + e.getMessage(), targetFile, itemId);
        }
    }

    /**
     * Generates a fully formatted YAML configuration string from an ItemStack.
     */
    public static String generateYaml(ItemStack itemStack, String itemId, CustomItem existing) {
        Material material = itemStack.getType();
        ItemMeta meta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : null;

        String type = existing != null ? existing.getType() : inferType(material);
        String slot = existing != null ? existing.getActivationSlot() : inferActivationSlot(material);

        String displayName = (meta != null && meta.hasDisplayName()) ? ColorUtils.decolorize(meta.getDisplayName()) : null;
        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                lore.add(ColorUtils.decolorize(line));
            }
        }

        Map<String, Integer> enchants = new LinkedHashMap<>();
        Map<Enchantment, Integer> metaEnchants = (meta != null && meta.hasEnchants()) ? meta.getEnchants() : itemStack.getEnchantments();
        if (metaEnchants != null && !metaEnchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : metaEnchants.entrySet()) {
                String enchName;
                try {
                    enchName = entry.getKey().getKey().getKey().toUpperCase(Locale.ROOT);
                } catch (Throwable t) {
                    enchName = entry.getKey().toString().toUpperCase(Locale.ROOT);
                }
                enchants.put(enchName, entry.getValue());
            }
        }

        List<String> flags = new ArrayList<>();
        if (meta != null && meta.getItemFlags() != null) {
            for (ItemFlag flag : meta.getItemFlags()) {
                flags.add(flag.name());
            }
        }

        boolean unbreakable = meta != null && meta.isUnbreakable();

        int customModelData = -1;
        if (meta != null && meta.hasCustomModelData()) {
            try {
                customModelData = meta.getCustomModelData();
            } catch (Throwable ignored) {}
        }

        String itemModel = null;
        if (meta != null) {
            try {
                if (meta.hasItemModel() && meta.getItemModel() != null) {
                    itemModel = meta.getItemModel().toString();
                }
            } catch (Throwable ignored) {}
        }

        String texture = null;
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile profile = skullMeta.getPlayerProfile();
                if (profile != null && profile.getTextures() != null && profile.getTextures().getSkin() != null) {
                    texture = profile.getTextures().getSkin().toString();
                }
            } catch (Throwable ignored) {}
        }

        Map<String, Double> attributes = new LinkedHashMap<>();
        if (meta != null && meta.hasAttributeModifiers()) {
            try {
                var modifiers = meta.getAttributeModifiers();
                if (modifiers != null) {
                    for (var entry : modifiers.entries()) {
                        String attrName = null;
                        if (entry.getKey() != null) {
                            try {
                                attrName = entry.getKey().getKey().getKey().toUpperCase(Locale.ROOT);
                            } catch (Throwable t) {
                                attrName = entry.getKey().toString().toUpperCase(Locale.ROOT);
                            }
                        }
                        if (attrName != null && entry.getValue() != null) {
                            attributes.put(attrName, entry.getValue().getAmount());
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } else if (existing != null && existing.getAttributes() != null && !existing.getAttributes().isEmpty()) {
            attributes.putAll(existing.getAttributes());
        }

        Boolean glintOverride = null;
        if (meta != null) {
            try {
                if (meta.hasEnchantmentGlintOverride()) {
                    glintOverride = meta.getEnchantmentGlintOverride();
                }
            } catch (Throwable ignored) {}
        }

        String colorHex = null;
        if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta lm) {
            try {
                colorHex = String.format("#%06X", (lm.getColor().asRGB() & 0xFFFFFF));
            } catch (Throwable ignored) {}
        } else if (meta instanceof org.bukkit.inventory.meta.PotionMeta pm) {
            try {
                if (pm.hasColor() && pm.getColor() != null) {
                    colorHex = String.format("#%06X", (pm.getColor().asRGB() & 0xFFFFFF));
                }
            } catch (Throwable ignored) {}
        }

        String trimPattern = null;
        String trimMaterial = null;
        if (meta instanceof org.bukkit.inventory.meta.ArmorMeta am) {
            try {
                if (am.hasTrim() && am.getTrim() != null) {
                    trimPattern = am.getTrim().getPattern().getKey().getKey();
                    trimMaterial = am.getTrim().getMaterial().getKey().getKey();
                }
            } catch (Throwable ignored) {}
        }

        return generateYaml(itemId, material.name(), itemStack.getAmount(), displayName, lore,
                enchants, flags, unbreakable, customModelData, itemModel, texture, attributes,
                glintOverride, colorHex, trimPattern, trimMaterial, type, slot, existing);
    }

    /**
     * Backward-compatible overload from pure data values.
     */
    public static String generateYaml(String itemId, String materialName, int amount, String displayName,
                                      List<String> lore, Map<String, Integer> enchants, List<String> flags,
                                      boolean unbreakable, int customModelData, String itemModel, String texture,
                                      Map<String, Double> attributes, String type, String slot, CustomItem existing) {
        return generateYaml(itemId, materialName, amount, displayName, lore, enchants, flags, unbreakable,
                customModelData, itemModel, texture, attributes, null, null, null, null, type, slot, existing);
    }

    /**
     * Core YAML generator method from pure data values including modern components.
     */
    public static String generateYaml(String itemId, String materialName, int amount, String displayName,
                                      List<String> lore, Map<String, Integer> enchants, List<String> flags,
                                      boolean unbreakable, int customModelData, String itemModel, String texture,
                                      Map<String, Double> attributes, Boolean glintOverride, String colorHex,
                                      String trimPattern, String trimMaterial, String type, String slot, CustomItem existing) {
        StringBuilder sb = new StringBuilder();
        sb.append("# yaml-language-server: $schema=https://raw.githubusercontent.com/animesao/Artifex/main/schema/items.schema.json\n");
        sb.append(itemId).append(":\n");
        sb.append("  type: ").append(type != null ? type : "TOOL").append("\n");
        sb.append("  activation-slot: ").append(slot != null ? slot : "HAND").append("\n");
        sb.append("  placeable: ").append(existing != null ? existing.isPlaceable() : false).append("\n");

        if (existing != null && existing.hasPermission()) {
            sb.append("  permission: ").append(yamlQuote(existing.getPermission())).append("\n");
        }

        sb.append("\n  item:\n");
        sb.append("    type: ").append(materialName != null ? materialName : "STONE").append("\n");

        if (displayName != null && !displayName.isEmpty()) {
            sb.append("    title: ").append(yamlQuote(displayName)).append("\n");
        }

        if (amount > 1) {
            sb.append("    amount: ").append(amount).append("\n");
        }

        if (lore != null && !lore.isEmpty()) {
            sb.append("    lore:\n");
            for (String line : lore) {
                sb.append("      - ").append(yamlQuote(line)).append("\n");
            }
        }

        // Enchantments
        if (enchants != null && !enchants.isEmpty()) {
            sb.append("    enchantments:\n");
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                sb.append("      ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // ItemFlags
        if (flags != null && !flags.isEmpty()) {
            sb.append("    item-flags:\n");
            for (String flag : flags) {
                sb.append("      - ").append(flag).append("\n");
            }
        }

        if (unbreakable) {
            sb.append("    unbreakable: true\n");
        }

        // Custom Model Data
        if (customModelData > 0) {
            sb.append("    custom-model-data: ").append(customModelData).append("\n");
        }

        // Modern 1.21.4+ Item Model
        if (itemModel != null && !itemModel.isEmpty()) {
            sb.append("    item-model: ").append(yamlQuote(itemModel)).append("\n");
        }

        // Skull texture
        if (texture != null && !texture.isEmpty()) {
            sb.append("    texture: ").append(yamlQuote(texture)).append("\n");
        }

        // Glint override (1.20.5+)
        if (glintOverride != null) {
            sb.append("    glint: ").append(glintOverride).append("\n");
        }

        // Color (Leather / Potion)
        if (colorHex != null && !colorHex.isEmpty()) {
            sb.append("    color: ").append(yamlQuote(colorHex)).append("\n");
        }

        // Armor trim (1.20+)
        if (trimPattern != null && trimMaterial != null) {
            sb.append("    trim:\n");
            sb.append("      pattern: ").append(yamlQuote(trimPattern)).append("\n");
            sb.append("      material: ").append(yamlQuote(trimMaterial)).append("\n");
        }

        // Attributes
        if (attributes != null && !attributes.isEmpty()) {
            sb.append("\n  # Attribute modifiers\n");
            sb.append("  attributes:\n");
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Existing or template mechanics
        long clickCooldown = existing != null ? existing.getClickCooldown() : 0;
        sb.append("\n  # Cooldown & Usage Limits\n");
        sb.append("  click-cooldown: ").append(clickCooldown).append("\n");

        if (existing != null && existing.hasMaxUses()) {
            sb.append("  max-uses: ").append(existing.getMaxUses()).append("\n");
        } else {
            sb.append("  # max-uses: 100\n");
        }

        if (existing != null && existing.getDuration() > 0) {
            sb.append("  duration: ").append(existing.getDuration()).append("\n");
        } else {
            sb.append("  # duration: 3600 # lifetime in seconds\n");
        }

        // Effects
        if (existing != null && existing.getEffects() != null && !existing.getEffects().isEmpty()) {
            sb.append("\n  effects:\n");
            for (String eff : existing.getEffects()) {
                sb.append("    - ").append(yamlQuote(eff)).append("\n");
            }
        } else {
            sb.append("\n  # Constant or equipped potion effects\n");
            sb.append("  # effects:\n");
            sb.append("  #   - \"SPEED:1\"\n");
        }

        // Actions
        if (existing != null && existing.getRightClickActions() != null && !existing.getRightClickActions().isEmpty()) {
            sb.append("\n  right-click-actions:\n");
            for (String act : existing.getRightClickActions()) {
                sb.append("    - ").append(yamlQuote(act)).append("\n");
            }
        } else {
            sb.append("\n  # Actions performed on right click\n");
            sb.append("  # right-click-actions:\n");
            sb.append("  #   - \"heal:5\"\n");
            sb.append("  #   - \"message:&aItem used!\"\n");
        }

        if (existing != null && existing.getLeftClickActions() != null && !existing.getLeftClickActions().isEmpty()) {
            sb.append("\n  left-click-actions:\n");
            for (String act : existing.getLeftClickActions()) {
                sb.append("    - ").append(yamlQuote(act)).append("\n");
            }
        } else {
            sb.append("\n  # Actions performed on left click\n");
            sb.append("  # left-click-actions:\n");
            sb.append("  #   - \"sound:ENTITY_PLAYER_ATTACK_SWEEP:1:1\"\n");
        }

        // Triggers
        if (existing != null && existing.getTriggerActions() != null && !existing.getTriggerActions().isEmpty()) {
            sb.append("\n  triggers:\n");
            for (String trig : existing.getTriggerActions()) {
                int colon = trig.indexOf(':');
                if (colon > 0) {
                    String trigName = trig.substring(0, colon);
                    String action = trig.substring(colon + 1);
                    sb.append("    ").append(trigName).append(":\n");
                    sb.append("      - ").append(yamlQuote(action)).append("\n");
                }
            }
        } else {
            sb.append("\n  # Event triggers (on_kill, on_damage_taken, on_jump, etc.)\n");
            sb.append("  # triggers:\n");
            sb.append("  #   on_kill:\n");
            sb.append("  #     - \"heal:10\"\n");
        }

        return sb.toString();
    }

    /**
     * Checks if the ID contains only valid characters.
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) return false;
        return id.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Checks if a material is consumable or food safely without triggering registry initialization.
     */
    public static boolean isConsumableMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        if (name.contains("POTION") || name.contains("STEW") || name.contains("SOUP") || name.contains("BOTTLE")) {
            return true;
        }
        try {
            return material.isEdible();
        } catch (Throwable t) {
            return name.startsWith("COOKED_") || name.startsWith("RAW_") || name.endsWith("_APPLE")
                    || name.equals("APPLE") || name.equals("BREAD") || name.equals("CARROT")
                    || name.equals("GOLDEN_CARROT") || name.equals("POTATO") || name.equals("BAKED_POTATO")
                    || name.equals("BEEF") || name.equals("PORKCHOP") || name.equals("MUTTON")
                    || name.equals("CHICKEN") || name.equals("RABBIT") || name.equals("COD")
                    || name.equals("SALMON") || name.equals("COOKIE") || name.equals("MELON_SLICE")
                    || name.equals("SWEET_BERRIES") || name.equals("GLOW_BERRIES") || name.equals("HONEY_BOTTLE");
        }
    }

    /**
     * Infers the item type category from the material.
     */
    public static String inferType(Material material) {
        if (material == null) return "TOOL";
        String name = material.name();
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || material == Material.ELYTRA) {
            return "ARMOR";
        }
        if (isConsumableMaterial(material) || material == Material.GOLDEN_APPLE || material == Material.ENCHANTED_GOLDEN_APPLE) {
            return "CONSUMABLE";
        }
        return "TOOL";
    }

    /**
     * Infers the activation slot from the material.
     */
    public static String inferActivationSlot(Material material) {
        if (material == null) return "HAND";
        String name = material.name();
        if (name.endsWith("_HELMET")) return "HEAD";
        if (name.endsWith("_CHESTPLATE") || material == Material.ELYTRA) return "CHEST";
        if (name.endsWith("_LEGGINGS")) return "LEGS";
        if (name.endsWith("_BOOTS")) return "FEET";
        if (material == Material.SHIELD) return "OFFHAND";
        return "HAND";
    }

    /**
     * Wraps a string in double quotes and escapes special characters for YAML.
     */
    public static String yamlQuote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
