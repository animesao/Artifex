package me.dcplugin.artifex.handlers;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemExporterTest {

    @Test
    void testInferType() {
        assertEquals("ARMOR", ItemExporter.inferType(Material.DIAMOND_HELMET));
        assertEquals("ARMOR", ItemExporter.inferType(Material.NETHERITE_CHESTPLATE));
        assertEquals("ARMOR", ItemExporter.inferType(Material.IRON_LEGGINGS));
        assertEquals("ARMOR", ItemExporter.inferType(Material.GOLDEN_BOOTS));
        assertEquals("ARMOR", ItemExporter.inferType(Material.ELYTRA));

        assertEquals("CONSUMABLE", ItemExporter.inferType(Material.GOLDEN_APPLE));
        assertEquals("CONSUMABLE", ItemExporter.inferType(Material.ENCHANTED_GOLDEN_APPLE));
        assertEquals("CONSUMABLE", ItemExporter.inferType(Material.POTION));
        assertEquals("CONSUMABLE", ItemExporter.inferType(Material.SPLASH_POTION));
        assertEquals("CONSUMABLE", ItemExporter.inferType(Material.COOKED_BEEF));

        assertEquals("TOOL", ItemExporter.inferType(Material.DIAMOND_SWORD));
        assertEquals("TOOL", ItemExporter.inferType(Material.BOW));
        assertEquals("TOOL", ItemExporter.inferType(Material.STICK));
    }

    @Test
    void testInferActivationSlot() {
        assertEquals("HEAD", ItemExporter.inferActivationSlot(Material.DIAMOND_HELMET));
        assertEquals("CHEST", ItemExporter.inferActivationSlot(Material.NETHERITE_CHESTPLATE));
        assertEquals("CHEST", ItemExporter.inferActivationSlot(Material.ELYTRA));
        assertEquals("LEGS", ItemExporter.inferActivationSlot(Material.IRON_LEGGINGS));
        assertEquals("FEET", ItemExporter.inferActivationSlot(Material.GOLDEN_BOOTS));
        assertEquals("OFFHAND", ItemExporter.inferActivationSlot(Material.SHIELD));
        assertEquals("HAND", ItemExporter.inferActivationSlot(Material.DIAMOND_SWORD));
    }

    @Test
    void testIsValidId() {
        assertTrue(ItemExporter.isValidId("vampire_blade"));
        assertTrue(ItemExporter.isValidId("fire-sword"));
        assertTrue(ItemExporter.isValidId("item123"));
        assertTrue(ItemExporter.isValidId("Sword"));

        assertFalse(ItemExporter.isValidId("invalid id"));
        assertFalse(ItemExporter.isValidId("item.name"));
        assertFalse(ItemExporter.isValidId("sword@magic"));
        assertFalse(ItemExporter.isValidId(""));
        assertFalse(ItemExporter.isValidId(null));
    }

    @Test
    void testYamlQuote() {
        assertEquals("\"simple\"", ItemExporter.yamlQuote("simple"));
        assertEquals("\"hello \\\"world\\\"\"", ItemExporter.yamlQuote("hello \"world\""));
        assertEquals("\"path\\\\to\\\\file\"", ItemExporter.yamlQuote("path\\to\\file"));
        assertEquals("\"\"", ItemExporter.yamlQuote(null));
    }

    @Test
    void testGenerateYamlBasic() {
        String yaml = ItemExporter.generateYaml("diamond_blade", "DIAMOND_SWORD", 1, "&bDiamond Blade",
                java.util.List.of("&7A legendary sword", "&6Forged in starlight"),
                java.util.Map.of("SHARPNESS", 5, "UNBREAKING", 3),
                java.util.List.of("HIDE_ENCHANTS"),
                true,
                1001,
                "minecraft:diamond_sword_custom",
                null,
                java.util.Map.of("GENERIC_ATTACK_DAMAGE", 9.5),
                "TOOL",
                "HAND",
                null);

        assertNotNull(yaml);
        assertTrue(yaml.contains("diamond_blade:"));
        assertTrue(yaml.contains("type: TOOL"));
        assertTrue(yaml.contains("activation-slot: HAND"));
        assertTrue(yaml.contains("type: DIAMOND_SWORD"));
        assertTrue(yaml.contains("title: \"&bDiamond Blade\""));
        assertTrue(yaml.contains("lore:"));
        assertTrue(yaml.contains("- \"&7A legendary sword\""));
        assertTrue(yaml.contains("SHARPNESS: 5"));
        assertTrue(yaml.contains("HIDE_ENCHANTS"));
        assertTrue(yaml.contains("unbreakable: true"));
        assertTrue(yaml.contains("custom-model-data: 1001"));
        assertTrue(yaml.contains("item-model: \"minecraft:diamond_sword_custom\""));
        assertTrue(yaml.contains("GENERIC_ATTACK_DAMAGE: 9.5"));
        assertTrue(yaml.contains("click-cooldown: 0"));
        assertTrue(yaml.contains("yaml-language-server:"));
    }

    @Test
    void testGenerateYamlArmor() {
        String yaml = ItemExporter.generateYaml("dragon_armor", "NETHERITE_CHESTPLATE", 1, "&#ffaa00Dragon Armor",
                null,
                java.util.Map.of("PROTECTION", 4),
                null,
                false,
                -1,
                null,
                null,
                java.util.Map.of("GENERIC_ARMOR", 8.0, "GENERIC_ARMOR_TOUGHNESS", 3.0),
                "ARMOR",
                "CHEST",
                null);

        assertNotNull(yaml);
        assertTrue(yaml.contains("dragon_armor:"));
        assertTrue(yaml.contains("type: ARMOR"));
        assertTrue(yaml.contains("activation-slot: CHEST"));
        assertTrue(yaml.contains("type: NETHERITE_CHESTPLATE"));
        assertTrue(yaml.contains("PROTECTION: 4"));
        assertTrue(yaml.contains("GENERIC_ARMOR: 8.0"));
    }
}
