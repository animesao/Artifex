package me.dcplugin.artifex.api;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecipeDefTest {

    @Test
    void shapedKeepsPattern() {
        RecipeDef def = RecipeDef.shaped(List.of(" A ", "ABA"), Map.of('A', "DIAMOND", 'B', "STICK"));
        assertEquals(RecipeDef.Type.SHAPED, def.getType());
        assertEquals(2, def.getPattern().size());
        assertTrue(def.describe().contains("shaped"));
    }

    @Test
    void smithingStoresTriple() {
        RecipeDef def = RecipeDef.smithing("T", "DIAMOND", "IRON_INGOT");
        assertEquals(RecipeDef.Type.SMITHING, def.getType());
        assertEquals(3, def.getIngredients().size());
        assertTrue(def.describe().contains("smithing"));
    }

    @Test
    void stonecuttingDefaults() {
        RecipeDef def = RecipeDef.stonecutting("DIAMOND");
        assertEquals(RecipeDef.Type.STONECUTTING, def.getType());
        assertEquals(1, def.getAmount());
    }

    @Test
    void builderFurnace() {
        RecipeDef def = RecipeDef.builder(RecipeDef.Type.FURNACE).ingredient("IRON_INGOT").experience(0.7f).build();
        assertEquals(RecipeDef.Type.FURNACE, def.getType());
        assertEquals(0.7f, def.getExperience());
    }
}
