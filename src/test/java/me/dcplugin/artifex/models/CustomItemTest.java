package me.dcplugin.artifex.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the model of a YAML item.
 *
 * <p>The class must stay loadable without a running server: the tests only
 * touch the parts that do not need the Bukkit registry.
 */
class CustomItemTest {

    private static CustomItem item(String id, String type, String slot, String permission) {
        return CustomItem.builder(id, null)
                .type(type)
                .activationSlot(slot)
                .permission(permission)
                .build();
    }

    @Test
    void typeIsNormalisedToUpperCase() {
        assertEquals("CONSUMABLE", item("a", "consumable", "HAND", null).getType());
        assertEquals("TOOL", item("a", "Tool", "HAND", null).getType());
    }

    @Test
    void activationSlotIsNormalisedToUpperCase() {
        assertEquals("HAND", item("a", "TOOL", "hand", null).getActivationSlot());
        assertEquals("OFFHAND", item("a", "TOOL", "OffHand", null).getActivationSlot());
    }

    @Test
    void nullTypeFallsBackToRune() {
        assertEquals("RUNE", item("a", null, "HAND", null).getType());
    }

    @Test
    void collectionsAreImmutable() {
        CustomItem custom = CustomItem.builder("a", null)
                .effects(List.of("SPEED:1"))
                .attributes(java.util.Map.of("ATTACK_DAMAGE", 1.0))
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> custom.getEffects().add("JUMP:1"));
        assertThrows(UnsupportedOperationException.class,
                () -> custom.getAttributes().put("ARMOR", 1.0));
    }

    @Test
    void permissionHelpers() {
        assertTrue(item("a", "TOOL", "HAND", null).hasPermission() == false);
        assertTrue(item("a", "TOOL", "HAND", "some.node").hasPermission());
    }

    @Test
    void worldRestrictions() {
        CustomItem custom = CustomItem.builder("a", null)
                .allowedWorlds(List.of("world"))
                .disabledWorlds(List.of("nether"))
                .build();
        assertTrue(custom.isAllowedInWorld("world"));
        assertFalse(custom.isAllowedInWorld("nether"));
        assertFalse(custom.isAllowedInWorld("the_end"));
    }
}
