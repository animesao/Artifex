package me.dcplugin.dcustomitems.handlers;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.AbstractCustomItem;
import me.dcplugin.dcustomitems.api.RecipeDef;
import me.dcplugin.dcustomitems.models.CustomItem;
import me.dcplugin.dcustomitems.utils.EnumCache;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.FurnaceRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Crafting recipe manager for custom items.
 *
 * Recipes are defined in the YAML section of the item:
 * <pre>
 * my-item:
 *   item: ...
 *   recipes:
 *     shaped:
 *       - pattern:
 *           - " A "
 *           - "ABA"
 *           - " A "
 *         keys:
 *           A: DIAMOND            # material
 *           B: my-other-item      # or the ID of another custom item
 *         amount: 1
 *     shapeless:
 *       - ingredients: [ DIAMOND, DIAMOND, STICK ]
 *     furnace:
 *       - ingredient: IRON_INGOT
 *         experience: 0.7
 *         cooking-time: 200       # ticks
 * </pre>
 *
 * Recipes are automatically removed and recreated on /ci reload.
 */
public class RecipeManager {

    private final Main plugin;
    private final Set<NamespacedKey> registered = new HashSet<>();
    // Recipe keys of Java API items (separate: registered by ItemRegistry)
    private final Set<NamespacedKey> javaKeys = new HashSet<>();

    public RecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Remove all recipes added by this manager (called before a reload).
     */
    public void clear() {
        for (NamespacedKey key : registered) {
            try {
                plugin.getServer().removeRecipe(key);
            } catch (Exception ignored) {
                // Server without removeRecipe (old Paper) — the recipes will disappear after a restart
            }
        }
        registered.clear();
    }

    /**
     * Register the recipes from the "recipes" section of the item (if any).
     */
    public void register(CustomItem item) {
        if (item == null || !item.hasRecipes()) return;

        Map<String, Object> recipes = item.getRecipes();
        ItemStack result = item.getItemStack().clone();

        registerShaped(item, recipes, result);
        registerShapeless(item, recipes, result);
        registerFurnace(item, recipes, result);
        registerSmithing(item, recipes, result);
        registerStonecutting(item, recipes, result);
    }

    // ===== Java API items (AbstractCustomItem.getRecipes()) =====

    /**
     * Remove the recipes of Java items (called before they are re-registered).
     */
    public void clearJava() {
        for (NamespacedKey key : javaKeys) {
            try {
                plugin.getServer().removeRecipe(key);
            } catch (Exception ignored) {}
        }
        javaKeys.clear();
    }

    /**
     * Register the recipes declared by Java API items through
     * {@code getRecipes()} (RecipeDef). Called by ItemRegistry after loading.
     */
    public void registerJavaItems(Collection<AbstractCustomItem> items) {
        clearJava();
        if (items == null || items.isEmpty()) return;

        for (AbstractCustomItem item : items) {
            if (item == null) continue;
            List<RecipeDef> defs;
            try {
                defs = item.getRecipes();
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Error in getRecipes() for '" + item.getId() + "': " + e.getMessage(), e);
                continue;
            }
            if (defs == null || defs.isEmpty()) continue;

            ItemStack result = item.createItemStack();
            int index = 0;
            for (RecipeDef def : defs) {
                registerJavaRecipe(item, def, result, index++);
            }
        }
    }

    private void registerJavaRecipe(AbstractCustomItem item, RecipeDef def, ItemStack result, int index) {
        if (def == null) return;
        try {
            NamespacedKey key = javaKey(item.getId(), def.getType().name().toLowerCase(), index);
            ItemStack out = result.clone();
            out.setAmount(def.getAmount() <= 0 ? 1 : def.getAmount());

            switch (def.getType()) {
                case SHAPED: {
                    List<String> pattern = def.getPattern();
                    if (pattern.isEmpty() || pattern.size() > 3) {
                        plugin.getLogger().warning("[Recipes] '" + item.getId() + "': the shaped pattern must be 1-3 rows");
                        return;
                    }
                    ShapedRecipe shaped = new ShapedRecipe(key, out);
                    shaped.shape(pattern.toArray(new String[0]));
                    for (String row : pattern) {
                        for (char c : row.toCharArray()) {
                            if (c == ' ') continue;
                            String choiceRaw = def.getKeys().get(c);
                            RecipeChoice choice = choiceRaw == null ? null : resolveChoice(choiceRaw);
                            if (choice == null) {
                                plugin.getLogger().warning("[Recipes] '" + item.getId()
                                        + "': unknown ingredient '" + choiceRaw + "' for key '" + c + "'");
                                return;
                            }
                            shaped.setIngredient(c, choice);
                        }
                    }
                    addJava(key, shaped, item.getId());
                    break;
                }
                case SHAPELESS: {
                    if (def.getIngredients().isEmpty()) return;
                    ShapelessRecipe shapeless = new ShapelessRecipe(key, out);
                    for (String ingredient : def.getIngredients()) {
                        RecipeChoice choice = resolveChoice(ingredient);
                        if (choice == null) {
                            plugin.getLogger().warning("[Recipes] '" + item.getId()
                                    + "': unknown ingredient '" + ingredient + "'");
                            return;
                        }
                        shapeless.addIngredient(choice);
                    }
                    addJava(key, shapeless, item.getId());
                    break;
                }
                case FURNACE: {
                    if (def.getIngredients().isEmpty()) return;
                    RecipeChoice choice = resolveChoice(def.getIngredients().get(0));
                    if (choice == null) return;
                    FurnaceRecipe furnace = new FurnaceRecipe(key, out, choice,
                            def.getExperience(), def.getCookingTime() <= 0 ? 200 : def.getCookingTime());
                    addJava(key, furnace, item.getId());
                    break;
                }
                case SMITHING: {
                    if (def.getIngredients().size() < 3) {
                        plugin.getLogger().warning("[Recipes] '" + item.getId() + "': smithing requires template+base+addition");
                        return;
                    }
                    RecipeChoice template = resolveChoice(def.getIngredients().get(0));
                    RecipeChoice base = resolveChoice(def.getIngredients().get(1));
                    RecipeChoice addition = resolveChoice(def.getIngredients().get(2));
                    if (template == null || base == null || addition == null) return;
                    try {
                        org.bukkit.inventory.SmithingTransformRecipe smithing =
                            new org.bukkit.inventory.SmithingTransformRecipe(key, out, template, base, addition);
                        addJava(key, smithing, item.getId());
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Recipes] smithing is not available on this server core: " + e.getMessage());
                    }
                    break;
                }
                case STONECUTTING: {
                    if (def.getIngredients().isEmpty()) return;
                    RecipeChoice choice = resolveChoice(def.getIngredients().get(0));
                    if (choice == null) return;
                    try {
                        org.bukkit.inventory.StonecuttingRecipe stone =
                            new org.bukkit.inventory.StonecuttingRecipe(key, out, choice);
                        addJava(key, stone, item.getId());
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Recipes] stonecutting is not available on this server core: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Error in java recipe '" + item.getId() + "' #" + index + ": " + e.getMessage(), e);
        }
    }

    private boolean addJava(NamespacedKey key, Recipe recipe, String itemId) {
        try {
            if (plugin.getServer().addRecipe(recipe)) {
                javaKeys.add(key);
                return true;
            }
            plugin.getLogger().warning("[Recipes] Could not add recipe '" + key + "' (duplicate?)");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Error while registering java recipe '" + itemId + "': " + e.getMessage(), e);
        }
        return false;
    }

    private NamespacedKey javaKey(String itemId, String type, int index) {
        return new NamespacedKey(plugin, "java_" + itemId.toLowerCase() + "_" + type + "_" + index);
    }

    // ===== Shaped =====

    private void registerShaped(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("shaped");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            List<String> pattern = asStringList(recipe.get("pattern"));
            Map<String, Object> keys = asMap(recipe.get("keys"));
            if (pattern.isEmpty() || keys == null) {
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "shaped", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));

                ShapedRecipe shaped = new ShapedRecipe(key, out);
                shaped.shape(pattern.toArray(new String[0]));

                boolean valid = true;
                for (String row : pattern) {
                    for (char c : row.toCharArray()) {
                        if (c == ' ') continue;
                        String choiceRaw = asString(keys.get(String.valueOf(c)));
                        if (choiceRaw == null) {
                            valid = false;
                            break;
                        }
                        RecipeChoice choice = resolveChoice(choiceRaw);
                        if (choice == null) {
                            plugin.getLogger().warning("[Recipes] '" + item.getId()
                                    + "': unknown ingredient '" + choiceRaw + "' for key '" + c + "'");
                            valid = false;
                            break;
                        }
                        shaped.setIngredient(c, choice);
                    }
                    if (!valid) break;
                }

                if (valid && add(key, shaped, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': shaped recipe #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Error in shaped recipe '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Shapeless =====

    private void registerShapeless(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("shapeless");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            List<String> ingredients = asStringList(recipe.get("ingredients"));
            if (ingredients.isEmpty()) {
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "shapeless", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));

                ShapelessRecipe shapeless = new ShapelessRecipe(key, out);
                boolean valid = true;
                for (String ingredient : ingredients) {
                    RecipeChoice choice = resolveChoice(ingredient);
                    if (choice == null) {
                        plugin.getLogger().warning("[Recipes] '" + item.getId()
                                + "': unknown ingredient '" + ingredient + "'");
                        valid = false;
                        break;
                    }
                    shapeless.addIngredient(choice);
                }

                if (valid && add(key, shapeless, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': shapeless recipe #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Error in shapeless recipe '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Furnace =====

    private void registerFurnace(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("furnace");
        if (!(raw instanceof List)) return;

        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) {
                index++;
                continue;
            }

            String ingredient = asString(recipe.get("ingredient"));
            if (ingredient == null) {
                index++;
                continue;
            }

            RecipeChoice choice = resolveChoice(ingredient);
            if (choice == null) {
                plugin.getLogger().warning("[Recipes] '" + item.getId()
                        + "': unknown furnace ingredient '" + ingredient + "'");
                index++;
                continue;
            }

            try {
                NamespacedKey key = recipeKey(item.getId(), "furnace", index);
                ItemStack out = result.clone();
                float experience = (float) asDouble(recipe.get("experience"), 0.0);
                int cookingTime = asInt(recipe.get("cooking-time"), 200);

                FurnaceRecipe furnace = new FurnaceRecipe(key, out, choice, experience, cookingTime);
                if (add(key, furnace, item)) {
                    plugin.getLogger().info("[Recipes] '" + item.getId() + "': furnace recipe #" + index);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[Recipes] Error in furnace recipe '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    // ===== Helpers =====

    private void registerSmithing(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("smithing");
        if (!(raw instanceof List)) return;
        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) { index++; continue; }
            RecipeChoice template = resolveChoice(asString(recipe.get("template")));
            RecipeChoice base = resolveChoice(asString(recipe.get("base")));
            RecipeChoice addition = resolveChoice(asString(recipe.get("addition")));
            if (template == null || base == null || addition == null) {
                plugin.getLogger().warning("[Recipes] '" + item.getId() + "': smithing #%d - check template/base/addition".formatted(index));
                index++; continue;
            }
            try {
                NamespacedKey key = recipeKey(item.getId(), "smithing", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));
                org.bukkit.inventory.SmithingTransformRecipe r =
                    new org.bukkit.inventory.SmithingTransformRecipe(key, out, template, base, addition);
                if (add(key, r, item)) plugin.getLogger().info("[Recipes] '" + item.getId() + "': smithing recipe #" + index);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "[Recipes] Error in smithing recipe '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    private void registerStonecutting(CustomItem item, Map<String, Object> recipes, ItemStack result) {
        Object raw = recipes.get("stonecutting");
        if (!(raw instanceof List)) return;
        int index = 0;
        for (Object recipeObj : (List<?>) raw) {
            Map<String, Object> recipe = asMap(recipeObj);
            if (recipe == null) { index++; continue; }
            RecipeChoice choice = resolveChoice(asString(recipe.get("ingredient")));
            if (choice == null) {
                plugin.getLogger().warning("[Recipes] '" + item.getId() + "': stonecutting #%d - unknown ingredient".formatted(index));
                index++; continue;
            }
            try {
                NamespacedKey key = recipeKey(item.getId(), "stonecutting", index);
                ItemStack out = result.clone();
                out.setAmount(asInt(recipe.get("amount"), 1));
                org.bukkit.inventory.StonecuttingRecipe r = new org.bukkit.inventory.StonecuttingRecipe(key, out, choice);
                if (add(key, r, item)) plugin.getLogger().info("[Recipes] '" + item.getId() + "': stonecutting recipe #" + index);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "[Recipes] Error in stonecutting recipe '" + item.getId() + "': " + e.getMessage(), e);
            }
            index++;
        }
    }

    private boolean add(NamespacedKey key, Recipe recipe, CustomItem item) {
        try {
            if (plugin.getServer().addRecipe(recipe)) {
                registered.add(key);
                return true;
            }
            plugin.getLogger().warning("[Recipes] Could not add recipe '" + key + "' (duplicate?)");
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Recipes] Error while registering recipe '" + item.getId() + "': " + e.getMessage(), e);
        }
        return false;
    }

    private NamespacedKey recipeKey(String itemId, String type, int index) {
        return new NamespacedKey(plugin, (itemId + "_" + type + "_" + index).toLowerCase(Locale.ROOT));
    }

    /**
     * Ingredient: a material (DIAMOND) or the ID of a custom item
     * (vampire-blade) — YAML or Java API, both are recognized.
     */
    private RecipeChoice resolveChoice(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        Material material = EnumCache.getMaterial(raw);
        if (material != null) {
            return new RecipeChoice.MaterialChoice(material);
        }

        // YAML item
        CustomItem custom = plugin.getItemHandler().getCustomItem(raw.toLowerCase(Locale.ROOT));
        if (custom != null) {
            return exactChoice(custom.getItemStack());
        }

        // Java API item (AbstractCustomItem).
        // The registry may not exist yet (e.g. the YAML items are loaded
        // before the registry) — then it is looked up later, at the recipe registration stage.
        if (plugin.getApiItemRegistry() == null) {
            return null;
        }
        AbstractCustomItem javaItem = plugin.getApiItemRegistry().getItem(raw.toLowerCase(Locale.ROOT));
        if (javaItem == null) javaItem = plugin.getApiItemRegistry().getItem(raw);
        if (javaItem != null) {
            return exactChoice(javaItem.createItemStack());
        }
        return null;
    }

    private RecipeChoice exactChoice(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(1);
        return new RecipeChoice.ExactChoice(copy);
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                out.add(String.valueOf(o));
            }
        } else if (value instanceof String) {
            out.add((String) value);
        }
        return out;
    }

    private int asInt(Object value, int def) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private double asDouble(Object value, double def) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
