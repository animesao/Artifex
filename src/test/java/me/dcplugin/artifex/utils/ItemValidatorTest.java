package me.dcplugin.artifex.utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ItemValidatorTest {

    private static final Set<String> MATS = Set.of("DIAMOND", "STICK", "IRON_INGOT", "NETHERITE_UPGRADE_SMITHING_TEMPLATE");
    private static final Set<String> ITEMS = Set.of("vampire-blade");

    @Test
    void shapedOk() {
        var issues = ItemValidator.validateShaped("x",
            List.of(" A ", "ABA", " A "),
            Map.of('A', "DIAMOND", 'B', "STICK"), MATS, ITEMS);
        assertTrue(issues.isEmpty(), issues::toString);
    }

    @Test
    void shapedMissingKey() {
        var issues = ItemValidator.validateShaped("x",
            List.of(" A "), Map.of('B', "STICK"), MATS, ITEMS);
        assertFalse(issues.isEmpty());
    }

    @Test
    void shapelessUnknownIngredient() {
        var issues = ItemValidator.validateShapeless("x", List.of("DIAMOND", "NOPE"), MATS, ITEMS);
        assertEquals(1, issues.size());
    }

    @Test
    void shapelessCustomItemOk() {
        var issues = ItemValidator.validateShapeless("x", List.of("vampire-blade"), MATS, ITEMS);
        assertTrue(issues.isEmpty());
    }

    @Test
    void smithingValidation() {
        assertTrue(ItemValidator.validateSmithing("x",
            "NETHERITE_UPGRADE_SMITHING_TEMPLATE", "DIAMOND", "IRON_INGOT", MATS, ITEMS).isEmpty());
        assertFalse(ItemValidator.validateSmithing("x", "", "DIAMOND", "IRON_INGOT", MATS, ITEMS).isEmpty());
    }

    @Test
    void stonecuttingValidation() {
        assertTrue(ItemValidator.validateStonecutting("x", "DIAMOND", MATS, ITEMS).isEmpty());
        assertFalse(ItemValidator.validateStonecutting("x", "NOPE", MATS, ITEMS).isEmpty());
    }
}
