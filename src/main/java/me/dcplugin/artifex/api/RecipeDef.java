package me.dcplugin.artifex.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declaration of a crafting recipe for Java items (AbstractCustomItem).
 *
 * <p>Developers return a list of such recipes from {@code getRecipes()}:
 * <pre>
 * {@literal @}Override
 * public List&lt;RecipeDef&gt; getRecipes() {
 *     Map&lt;Character, String&gt; keys = new HashMap&lt;&gt;();
 *     keys.put('A', "DIAMOND");
 *     keys.put('B', "STICK");
 *     return List.of(
 *         RecipeDef.shaped(List.of(" A ", "ABA", " A "), keys),
 *         RecipeDef.shapeless(List.of("IRON_INGOT", "vampire-blade"))
 *     );
 * }
 * </pre>
 *
 * <p>Ingredients are specified just like in YAML: a material name
 * ({@code DIAMOND}) or the ID of another custom item
 * ({@code vampire-blade}) — YAML or Java, both are recognised.
 * Recipes end up both in the regular crafting table (RecipeManager) and in the GUI
 * crafting module {@code customcraft} (/craft).
 */
public final class RecipeDef {

    public enum Type { SHAPED, SHAPELESS, FURNACE, SMITHING, STONECUTTING }

    private final Type type;
    private final List<String> pattern;              // shaped
    private final Map<Character, String> keys;       // shaped
    private final List<String> ingredients;          // shapeless / furnace
    private final int amount;
    private final float experience;                  // furnace
    private final int cookingTime;                   // furnace (ticks)

    private RecipeDef(Type type, List<String> pattern, Map<Character, String> keys,
                      List<String> ingredients, int amount, float experience, int cookingTime) {
        this.type = type;
        this.pattern = pattern == null ? List.of() : List.copyOf(pattern);
        this.keys = keys == null ? Map.of() : Map.copyOf(keys);
        this.ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        this.amount = amount;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    // ===== SHAPED =====

    public static RecipeDef shaped(List<String> pattern, Map<Character, String> keys) {
        return shaped(pattern, keys, 1);
    }

    public static RecipeDef shaped(List<String> pattern, Map<Character, String> keys, int amount) {
        return new RecipeDef(Type.SHAPED, pattern, keys, null, amount, 0, 0);
    }

    // ===== SHAPELESS =====

    public static RecipeDef shapeless(String... ingredients) {
        return shapeless(List.of(ingredients), 1);
    }

    public static RecipeDef shapeless(List<String> ingredients) {
        return shapeless(ingredients, 1);
    }

    public static RecipeDef shapeless(List<String> ingredients, int amount) {
        return new RecipeDef(Type.SHAPELESS, null, null, ingredients, amount, 0, 0);
    }

    // ===== FURNACE =====

    public static RecipeDef furnace(String ingredient) {
        return furnace(ingredient, 0.0f, 200);
    }

    public static RecipeDef furnace(String ingredient, float experience, int cookingTime) {
        return new RecipeDef(Type.FURNACE, null, null, List.of(ingredient), 1, experience, cookingTime);
    }

    public static RecipeDef smithing(String template, String base, String addition) {
        return smithing(template, base, addition, 1);
    }

    public static RecipeDef smithing(String template, String base, String addition, int amount) {
        return new RecipeDef(Type.SMITHING, null, null,
            List.of(template == null ? "" : template, base == null ? "" : base, addition == null ? "" : addition),
            amount <= 0 ? 1 : amount, 0, 0);
    }

    public static RecipeDef stonecutting(String ingredient) {
        return stonecutting(ingredient, 1);
    }

    public static RecipeDef stonecutting(String ingredient, int amount) {
        return new RecipeDef(Type.STONECUTTING, null, null, List.of(ingredient), amount <= 0 ? 1 : amount, 0, 0);
    }

    // ===== Getters =====

    public Type getType() { return type; }
    public List<String> getPattern() { return pattern; }
    public Map<Character, String> getKeys() { return keys; }
    public List<String> getIngredients() { return ingredients; }

    /** Number of items produced (1 by default). */
    public int getAmount() { return amount; }

    /** Smelting experience (furnace). */
    public float getExperience() { return experience; }

    /** Smelting time in ticks (furnace, 200 by default). */
    public int getCookingTime() { return cookingTime; }

    /** String representation for logs/debugging. */
    public String describe() {
        switch (type) {
            case SHAPED:
                return "shaped " + pattern;
            case SHAPELESS:
                return "shapeless " + ingredients;
            case SMITHING:
                return "smithing " + ingredients;
            case STONECUTTING:
                return "stonecutting " + ingredients;
            default:
                return "furnace " + ingredients;
        }
    }

    // Optional builder-style helper for clearer examples
    public static Builder builder(Type type) { return new Builder(type); }

    /** Step-by-step builder (optional, for those who like chaining). */
    public static final class Builder {
        private final Type type;
        private final List<String> pattern = new ArrayList<>();
        private final Map<Character, String> keys = new LinkedHashMap<>();
        private final List<String> ingredients = new ArrayList<>();
        private int amount = 1;
        private float experience;
        private int cookingTime = 200;

        private Builder(Type type) { this.type = type; }

        public Builder row(String row) { pattern.add(row); return this; }
        public Builder key(char c, String ingredient) { keys.put(c, ingredient); return this; }
        public Builder ingredient(String ingredient) { ingredients.add(ingredient); return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder experience(float experience) { this.experience = experience; return this; }
        public Builder cookingTime(int cookingTime) { this.cookingTime = cookingTime; return this; }

        public RecipeDef build() {
            if (type == Type.FURNACE) {
                return new RecipeDef(type, null, null, ingredients, amount, experience, cookingTime);
            }
            List<String> patternCopy = type == Type.SHAPED ? new ArrayList<>(pattern) : null;
            Map<Character, String> keysCopy = type == Type.SHAPED ? new LinkedHashMap<>(keys) : null;
            List<String> ingredientsCopy = type == Type.SHAPELESS ? new ArrayList<>(ingredients) : null;
            return new RecipeDef(type, patternCopy, keysCopy, ingredientsCopy, amount, experience, cookingTime);
        }
    }
}
