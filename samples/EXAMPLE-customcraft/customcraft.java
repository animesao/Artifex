import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.RecipeDef;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import me.dcplugin.artifex.utils.EnumCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 🛠 CustomCraft — custom crafting through a GUI.
 *
 * Takes the recipes from the `recipes` YAML section of all custom items
 * (shaped / shapeless) and gives the player a virtual crafting table.
 *
 * Item identity is based on PDC (PersistentDataContainer / NBT):
 *  - custom items are matched by their ID (dcustomitems:<id>), not by name;
 *  - ingredients from other plugins are matched by material, their NBT is not changed;
 *  - the finished item goes through ItemHandler initialization
 *    (uses, duration, lore) — a stable result.
 *
 * Modularity: delete the items/customcraft/ folder — /craft disappears.
 */
public class customcraftModule extends Module implements Listener, CommandExecutor, TabCompleter {

    // Settings
    private String commandName = "craft";
    private String commandPermission = "dci.craft";
    private String guiTitle = "§8⚒ CustomCraft";
    private Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private int resultSlot = 15;
    private boolean returnOnClose = true;

    private List<CraftRecipe> recipes = new ArrayList<>();
    private final Map<UUID, CraftView> openViews = new HashMap<>();

    public customcraftModule(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    // ===== LIFECYCLE =====

    @Override
    protected void onEnable() {
        loadSettings();
        reloadRecipes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!enabled) return;
            registerDynamicCommand(commandName, this, commandPermission);
        });
        plugin.getLogger().info("[CustomCraft] GUI crafting enabled. Recipes: " + recipes.size());
    }

    @Override
    protected void onDisable() {
        HandlerList.unregisterAll(this);
        // Return the items from the open windows
        for (CraftView view : new ArrayList<>(openViews.values())) {
            returnItems(view);
        }
        openViews.clear();
        recipes.clear();
        plugin.getLogger().info("[CustomCraft] Module disabled");
    }

    private void loadSettings() {
        commandName = config.getString("command", "craft");
        commandPermission = config.getString("permission", "dci.craft");
        guiTitle = colorize(config.getString("gui.title", "&8⚒ CustomCraft"));
        String fillerName = config.getString("gui.filler", "GRAY_STAINED_GLASS_PANE");
        Material filler = EnumCache.getMaterial(fillerName);
        if (filler != null) fillerMaterial = filler;
        resultSlot = config.getInt("gui.result-slot", 15);
        returnOnClose = config.getBoolean("gui.return-items-on-close", true);
    }

    /**
     * Rebuild the recipes from all the custom items (YAML + Java API,
     * including itself as an ingredient). Called on /ci reload
     * through a module restart.
     */
    private void reloadRecipes() {
        recipes.clear();
        for (CustomItem item : plugin.getItemHandler().getAllCustomItems().values()) {
            if (item == null || !item.hasRecipes()) continue;
            collectRecipes(item);
        }
        for (AbstractCustomItem javaItem : plugin.getApiItemRegistry().getAllItems().values()) {
            if (javaItem == null) continue;
            try {
                List<RecipeDef> defs = javaItem.getRecipes();
                if (defs == null || defs.isEmpty()) continue;
                for (RecipeDef def : defs) {
                    CraftRecipe recipe = CraftRecipe.parseJava(plugin, javaItem, def);
                    if (recipe != null) recipes.add(recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[CustomCraft] Error in the recipes of the Java item '"
                        + javaItem.getId() + "': " + e.getMessage());
            }
        }
    }

    private void collectRecipes(CustomItem item) {
        Map<String, Object> root = item.getRecipes();
        if (root == null) return;

        Object shaped = root.get("shaped");
        if (shaped instanceof List) {
            for (Object recipeObj : (List<?>) shaped) {
                CraftRecipe recipe = CraftRecipe.parseShaped(plugin, item, asMap(recipeObj));
                if (recipe != null) recipes.add(recipe);
            }
        }

        Object shapeless = root.get("shapeless");
        if (shapeless instanceof List) {
            for (Object recipeObj : (List<?>) shapeless) {
                CraftRecipe recipe = CraftRecipe.parseShapeless(plugin, item, asMap(recipeObj));
                if (recipe != null) recipes.add(recipe);
            }
        }
        // furnace recipes work in a normal furnace (RecipeManager), they are not needed in the GUI grid
    }

    // ===== COMMAND =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cPlayers only!"));
            return true;
        }
        Player player = (Player) sender;
        if (recipes.isEmpty()) {
            player.sendMessage(colorize(config.getString("messages.no-recipes", "&cYou have no available recipes.")));
            return true;
        }
        openCraftGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }

    private void openCraftGui(Player player) {
        Inventory inv = plugin.getServer().createInventory(new CraftHolder(player.getUniqueId()), 27, guiTitle);
        CraftView view = new CraftView(player.getUniqueId(), inv);
        openViews.put(player.getUniqueId(), view);
        fillFiller(inv);
        player.openInventory(inv);
    }

    private void fillFiller(Inventory inv) {
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (isGridSlot(slot) || slot == resultSlot) continue;
            inv.setItem(slot, filler);
        }
    }

    /** The slots of the 3x3 grid in the 27-slot chest: (row*3) + column horizontally */
    private boolean isGridSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 0 && row <= 2 && col >= 0 && col <= 2;
    }

    private int gridRow(int slot) {
        return slot / 9;
    }

    private int gridCol(int slot) {
        return slot % 9;
    }

    private int gridSlot(int row, int col) {
        return row * 9 + col;
    }

    // ===== VIEW HELPERS =====

    private CraftView viewOf(Inventory inventory) {
        if (inventory == null || !(inventory.getHolder() instanceof CraftHolder)) return null;
        CraftHolder holder = (CraftHolder) inventory.getHolder();
        return openViews.get(holder.playerId);
    }

    /** Build a 3x3 array from the grid items of the window. */
    private ItemStack[][] readGrid(Inventory inv) {
        ItemStack[][] grid = new ItemStack[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                grid[row][col] = inv.getItem(gridSlot(row, col));
            }
        }
        return grid;
    }

    private void writeGrid(Inventory inv, ItemStack[][] grid) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                inv.setItem(gridSlot(row, col), grid[row][col]);
            }
        }
    }

    /** Update the result slot based on the current grid. */
    private void updateResult(CraftView view) {
        ItemStack[][] grid = readGrid(view.inventory);
        view.current = null;

        // Remove the result of the previous preview (it is virtual)
        view.inventory.setItem(resultSlot, null);

        if (isEmptyGrid(grid)) return;

        for (CraftRecipe recipe : recipes) {
            if (recipe.matches(grid)) {
                ItemStack result = recipe.buildResult();
                if (result == null) continue; // the source item was removed from the registry
                view.inventory.setItem(resultSlot, result);
                view.current = recipe;
                return;
            }
        }
    }

    private boolean isEmptyGrid(ItemStack[][] grid) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (grid[r][c] != null) return false;
            }
        }
        return true;
    }

    /** Take the ingredients according to the recipe and give the result to the cursor. */
    private void doCraft(CraftView view, Player player) {
        CraftRecipe recipe = view.current;
        if (recipe == null) return;

        ItemStack resultPreview = view.inventory.getItem(resultSlot);
        if (resultPreview == null) return;

        // Bukkit event: third-party plugins can cancel the craft
        // or replace the item being given (getResult/setResult)
        me.dcplugin.artifex.events.CustomItemCraftEvent craftEvent =
                recipe.javaItem != null
                        ? new me.dcplugin.artifex.events.CustomItemCraftEvent(player, recipe.javaItem, resultPreview.clone())
                        : new me.dcplugin.artifex.events.CustomItemCraftEvent(player, recipe.item, resultPreview.clone());
        plugin.getServer().getPluginManager().callEvent(craftEvent);
        if (craftEvent.isCancelled()) return; // the ingredients stay in the grid
        ItemStack result = craftEvent.getResult();

        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize()) {
                player.sendMessage(colorize("&cFree up your cursor!"));
                return;
            }
        }

        ItemStack[][] grid = readGrid(view.inventory);
        // We try to consume the ingredients (it must match, since the recipe was found)
        if (!recipe.consume(grid)) {
            view.inventory.setItem(resultSlot, null);
            view.current = null;
            return;
        }
        writeGrid(view.inventory, grid);

        if (cursor == null || cursor.getType() == Material.AIR) {
            player.setItemOnCursor(result.clone());
        } else {
            cursor.setAmount(cursor.getAmount() + result.getAmount());
            player.setItemOnCursor(cursor);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage(colorize(config.getString("messages.crafted", "&aItem crafted!")));
        updateResult(view);
    }

    private void returnItems(CraftView view) {
        Player player = Bukkit.getPlayer(view.playerId);
        if (player == null) return;
        ItemStack[][] grid = readGrid(view.inventory);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (grid[r][c] != null) {
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(grid[r][c]);
                    for (ItemStack drop : left.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    grid[r][c] = null;
                }
            }
        }
        writeGrid(view.inventory, grid);
        view.inventory.setItem(resultSlot, null);
    }

    // ===== EVENTS =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        CraftView view = viewOf(event.getView().getTopInventory());
        if (view == null) return;

        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true); // full manual control (by cursor)

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 0) return;

        if (rawSlot < topSize) {
            // Click on the top window: the grid or the result
            if (isGridSlot(rawSlot)) {
                handleSlotSwap(view.inventory, rawSlot);
                updateResult(view);
            } else if (rawSlot == resultSlot) {
                doCraft(view, player);
            }
            return;
        }

        // Click on the bottom (the player's inventory): we take/place items with the cursor
        int rel = rawSlot - topSize;
        if (rel >= 0 && rel < event.getView().getBottomInventory().getSize()) {
            handleSlotSwap(event.getView().getBottomInventory(), rel);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            CraftView view = viewOf(event.getView().getTopInventory());
            if (view != null) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        CraftView view = viewOf(event.getInventory());
        if (view == null) return;
        openViews.remove(view.playerId);
        if (returnOnClose) {
            returnItems(view);
        } else {
            // We still clear the virtual result
            view.inventory.setItem(resultSlot, null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CraftView view = openViews.remove(event.getPlayer().getUniqueId());
        if (view != null) returnItems(view);
    }

    /**
     * Universal cursor swap: take / place / stack / exchange.
     * Works for the grid (top window) and for the player's inventory (bottom).
     */
    private void handleSlotSwap(Inventory inv, int slot) {
        Player player = playerOf(inv);
        if (player == null) return;
        ItemStack cursor = player.getItemOnCursor();
        ItemStack slotItem = inv.getItem(slot);

        if (slotItem == null) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                inv.setItem(slot, cursor.clone());
                player.setItemOnCursor(null);
            }
        } else if (cursor == null || cursor.getType() == Material.AIR) {
            // Take the item onto the cursor
            inv.setItem(slot, null);
            player.setItemOnCursor(slotItem);
        } else if (cursor.isSimilar(slotItem)
                && cursor.getAmount() + slotItem.getAmount() <= cursor.getMaxStackSize()) {
            // Stack the stacks
            inv.setItem(slot, null);
            cursor.setAmount(cursor.getAmount() + slotItem.getAmount());
            player.setItemOnCursor(cursor);
        } else {
            // Swap the places
            inv.setItem(slot, cursor.clone());
            player.setItemOnCursor(slotItem);
        }
    }

    private Player playerOf(Inventory inv) {
        if (inv.getHolder() instanceof CraftHolder) {
            return Bukkit.getPlayer(((CraftHolder) inv.getHolder()).playerId);
        }
        // The player's bottom inventory: the holder is the Player itself
        org.bukkit.inventory.InventoryHolder holder = inv.getHolder();
        if (holder instanceof Player) return (Player) holder;
        return null;
    }

    // ===== UTILS =====

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return null;
    }

    private String colorize(String msg) {
        return msg == null ? "" : msg.replace("&", "§");
    }

    // ===== RECIPE MODEL =====

    static final class CraftRecipe {
        final Main plugin;
        final CustomItem item;              // null for Java API items
        final AbstractCustomItem javaItem;  // null for YAML items
        final int amount;
        final boolean shaped;
        // shaped: 3x3 character matrix (' ' = empty)
        final char[][] matrix;
        final Map<Character, String> keyMap;
        // shapeless: a multiset of ingredients (identity -> count)
        final Map<String, Integer> ingredients;

        private CraftRecipe(Main plugin, CustomItem item, AbstractCustomItem javaItem, int amount,
                            boolean shaped, char[][] matrix, Map<Character, String> keyMap,
                            Map<String, Integer> ingredients) {
            this.plugin = plugin;
            this.item = item;
            this.javaItem = javaItem;
            this.amount = amount;
            this.shaped = shaped;
            this.matrix = matrix;
            this.keyMap = keyMap;
            this.ingredients = ingredients;
        }

        /**
         * The crafting result: a stable ItemStack of the source item
         * (YAML: uses + acquired-time; Java API: createItemStack).
         * null, if the item disappeared from the registry after /ci reload.
         */
        ItemStack buildResult() {
            if (javaItem != null) {
                if (plugin.getApiItemRegistry().getItem(javaItem.getId()) == null) return null;
                ItemStack result = javaItem.createItemStack();
                result.setAmount(Math.max(1, amount));
                return result;
            }
            if (item == null || plugin.getItemHandler().getCustomItem(item.getId()) == null) return null;
            ItemStack result = item.getItemStack().clone();
            result.setAmount(Math.max(1, amount));
            // Stable initialization: uses + acquisition time (duration)
            plugin.getItemHandler().updateItemWithUses(result);
            plugin.getItemHandler().ensureAcquiredTime(result);
            return result;
        }

        static CraftRecipe parseShaped(Main plugin, CustomItem item, Map<String, Object> recipe) {
            if (recipe == null) return null;
            Object patternObj = recipe.get("pattern");
            Object keysObj = recipe.get("keys");
            if (!(patternObj instanceof List) || !(keysObj instanceof Map)) return null;

            List<String> pattern = new ArrayList<>();
            for (Object o : (List<?>) patternObj) pattern.add(String.valueOf(o));
            if (pattern.isEmpty() || pattern.size() > 3) return null;

            int width = pattern.get(0).length();
            if (width < 1 || width > 3) return null;
            for (String row : pattern) {
                if (row.length() != width) return null; // all rows of the same length
            }

            Map<Character, String> keyMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) keysObj).entrySet()) {
                String k = String.valueOf(entry.getKey());
                if (k.isEmpty()) continue;
                keyMap.put(k.charAt(0), String.valueOf(entry.getValue()));
            }

            // Normalize to 3x3 (aligned to the top-left corner)
            char[][] matrix = new char[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    matrix[r][c] = ' ';
                }
            }
            for (int r = 0; r < pattern.size(); r++) {
                for (int c = 0; c < width; c++) {
                    char ch = pattern.get(r).charAt(c);
                    matrix[r][c] = ch;
                    if (ch != ' ' && !keyMap.containsKey(ch)) {
                        return null; // a key without an ingredient
                    }
                }
            }
            return new CraftRecipe(plugin, item, null, intValue(recipe.get("amount"), 1), true, matrix, keyMap, null);
        }

        static CraftRecipe parseShapeless(Main plugin, CustomItem item, Map<String, Object> recipe) {
            if (recipe == null) return null;
            Object ingredientsObj = recipe.get("ingredients");
            if (!(ingredientsObj instanceof List)) return null;

            Map<String, Integer> ingredients = new LinkedHashMap<>();
            for (Object o : (List<?>) ingredientsObj) {
                String raw = String.valueOf(o);
                ingredients.put(raw, ingredients.getOrDefault(raw, 0) + 1);
            }
            if (ingredients.isEmpty()) return null;

            return new CraftRecipe(plugin, item, null, intValue(recipe.get("amount"), 1), false,
                    null, null, ingredients);
        }

        /**
         * Java API recipe (RecipeDef from getRecipes()). A furnace is not
         * needed in the GUI grid — it works in a normal furnace via RecipeManager.
         */
        static CraftRecipe parseJava(Main plugin, AbstractCustomItem javaItem, RecipeDef def) {
            if (def == null) return null;

            if (def.getType() == RecipeDef.Type.SHAPED) {
                List<String> pattern = def.getPattern();
                if (pattern.isEmpty() || pattern.size() > 3) return null;
                int width = pattern.get(0).length();
                if (width < 1 || width > 3) return null;
                for (String row : pattern) {
                    if (row.length() != width) return null; // all rows of the same length
                }

                Map<Character, String> keyMap = new HashMap<>();
                for (Map.Entry<Character, String> entry : def.getKeys().entrySet()) {
                    if (entry.getKey() == null || entry.getKey() == ' ') continue;
                    keyMap.put(entry.getKey(), entry.getValue());
                }

                // Normalize to 3x3 (aligned to the top-left corner)
                char[][] matrix = new char[3][3];
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        matrix[r][c] = ' ';
                    }
                }
                for (int r = 0; r < pattern.size(); r++) {
                    for (int c = 0; c < width; c++) {
                        char ch = pattern.get(r).charAt(c);
                        matrix[r][c] = ch;
                        if (ch != ' ' && !keyMap.containsKey(ch)) {
                            return null; // a key without an ingredient
                        }
                    }
                }
                return new CraftRecipe(plugin, null, javaItem, def.getAmount(), true, matrix, keyMap, null);
            }

            if (def.getType() == RecipeDef.Type.SHAPELESS) {
                Map<String, Integer> ingredients = new LinkedHashMap<>();
                for (String ing : def.getIngredients()) {
                    ingredients.put(ing, ingredients.getOrDefault(ing, 0) + 1);
                }
                if (ingredients.isEmpty()) return null;
                return new CraftRecipe(plugin, null, javaItem, def.getAmount(), false,
                        null, null, ingredients);
            }

            return null; // furnace
        }

        static int intValue(Object value, int def) {
            if (value instanceof Number) return ((Number) value).intValue();
            return def;
        }

        boolean matches(ItemStack[][] grid) {
            if (shaped) return matchesShaped(grid);
            return matchesShapeless(grid);
        }

        private boolean matchesShaped(ItemStack[][] grid) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    char ch = matrix[r][c];
                    ItemStack stack = grid[r][c];
                    if (ch == ' ') {
                        if (stack != null) return false;
                    } else {
                        if (!matchesIngredient(keyMap.get(ch), stack)) return false;
                    }
                }
            }
            return true;
        }

        private boolean matchesShapeless(ItemStack[][] grid) {
            Map<String, Integer> found = new HashMap<>();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack stack = grid[r][c];
                    if (stack == null) continue;
                    String identity = ingredientIdentityOf(stack);
                    found.put(identity, found.getOrDefault(identity, 0) + 1);
                }
            }
            if (found.size() != ingredients.size()) return false;
            for (Map.Entry<String, Integer> need : ingredients.entrySet()) {
                if (!need.getValue().equals(found.get(identityOfRaw(need.getKey())))) return false;
            }
            return true;
        }

        /** Consume the ingredients. Returns false if the grid no longer matches. */
        boolean consume(ItemStack[][] grid) {
            if (shaped) {
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        char ch = matrix[r][c];
                        if (ch == ' ') continue;
                        if (!matchesIngredient(keyMap.get(ch), grid[r][c])) return false;
                        decrement(grid, r, c);
                    }
                }
                return true;
            }

            // shapeless: we write off one by one until all the requirements are covered
            Map<String, Integer> remaining = new HashMap<>(ingredients);
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack stack = grid[r][c];
                    if (stack == null) continue;
                    String identity = ingredientIdentityOf(stack);
                    Integer need = remaining.get(identity);
                    if (need != null && need > 0) {
                        remaining.put(identity, need - 1);
                        decrement(grid, r, c);
                    }
                }
            }
            for (Integer value : remaining.values()) {
                if (value > 0) return false;
            }
            return true;
        }

        private void decrement(ItemStack[][] grid, int r, int c) {
            ItemStack stack = grid[r][c];
            if (stack.getAmount() > 1) {
                stack.setAmount(stack.getAmount() - 1);
            } else {
                grid[r][c] = null;
            }
        }

        /** Whether the stack matches the ingredient. */
        private boolean matchesIngredient(String raw, ItemStack stack) {
            if (raw == null || stack == null) return false;
            String identity = identityOfRaw(raw);
            if (identity.startsWith("material:")) {
                Material material = EnumCache.getMaterial(raw);
                return material != null && stack.getType() == material;
            }
            // custom item: we compare the PDC identity (dcustomitems:<id>)
            // case-insensitive — both YAML and Java API items are matched by ID
            String customId = plugin.getItemHandler().getCustomItemId(stack);
            return customId != null && identity.substring("custom:".length()).equalsIgnoreCase(customId);
        }

        /** Identity of a raw ingredient: material:X or custom:<id>. */
        private String identityOfRaw(String raw) {
            if (raw == null) return "";
            Material material = EnumCache.getMaterial(raw);
            if (material != null) return "material:" + material.name();
            return "custom:" + raw.toLowerCase();
        }

        /** Identity of a stack based on its real content. */
        private String ingredientIdentityOf(ItemStack stack) {
            String customId = plugin.getItemHandler().getCustomItemId(stack);
            if (customId != null) return "custom:" + customId;
            return "material:" + stack.getType().name();
        }
    }

    // ===== HOLDER / VIEW =====

    static class CraftHolder implements InventoryHolder {
        final UUID playerId;
        CraftHolder(UUID playerId) {
            this.playerId = playerId;
        }
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    static final class CraftView {
        final UUID playerId;
        final Inventory inventory;
        CraftRecipe current;

        CraftView(UUID playerId, Inventory inventory) {
            this.playerId = playerId;
            this.inventory = inventory;
        }
    }
}
