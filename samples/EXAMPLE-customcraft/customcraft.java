import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.RecipeDef;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import me.dcplugin.artifex.utils.EnumCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 🛠 CustomCraft 2.1 — Мастерская кастомных крафтов и внутриигровой конструктор.
 *
 * Новые возможности:
 *  - Поддержка произвольного количества предметов в каждом слоте крафта
 *    (например: 3 алмаза в слоте 1, 1 палка в слоте 2).
 *  - Интерактивное удаление крафтов прямо через GUI (кнопка в деталях или Shift+ПКМ в каталоге).
 *  - Окно детального просмотра любого рецепта с визуализацией сетки ингредиентов.
 *  - Полная поддержка кастомных предметов Artifex, NBT и ванильных материалов.
 */
public class customcraft extends Module implements Listener, CommandExecutor, TabCompleter {

    // ===== НАСТРОЙКИ =====
    private String commandName = "craft";
    private String commandPermission = "dci.craft";
    private String adminPermission = "dci.craft.admin";

    private String mainTitle = "&8⚒ &lМастерская Крафта";
    private String stationChooserTitle = "&8⚙ &lВыберите станцию крафта";
    private String builderTitleTemplate = "&8✨ &lКонструктор: &e{station}";
    private String craftTitle = "&8⚒ &lВиртуальный верстак";
    private String catalogTitle = "&8📖 &lКаталог рецептов";
    private Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private int craftResultSlot = 15;

    // Рецепты для виртуального верстака и каталога
    private final List<CraftRecipe> virtualRecipes = new ArrayList<>();
    // Ключи зарегистрированных в Bukkit рецептов
    private final Set<NamespacedKey> registeredBukkitKeys = new HashSet<>();

    // Сессии игроков
    private final Map<UUID, CraftView> openCraftViews = new HashMap<>();
    private final Map<UUID, BuilderSession> activeBuilderSessions = new HashMap<>();

    public customcraft(Main plugin, String id, File folder) {
        super(plugin, id, folder);
    }

    // ===== ЖИЗНЕННЫЙ ЦИКЛ =====

    @Override
    protected void onEnable() {
        loadSettings();
        reloadRecipes();
        loadSavedRecipes();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Безопасная отложенная регистрация команды (для Paper Async Command Builder)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!enabled) return;
            registerDynamicCommand(commandName, this, commandPermission);
        });

        plugin.getLogger().info("[CustomCraft] Модуль успешно запущен. Доступно рецептов: " + virtualRecipes.size());
    }

    @Override
    protected void onDisable() {
        HandlerList.unregisterAll(this);

        for (CraftView view : new ArrayList<>(openCraftViews.values())) {
            returnCraftItems(view);
        }
        openCraftViews.clear();

        for (BuilderSession session : new ArrayList<>(activeBuilderSessions.values())) {
            returnBuilderItems(session);
        }
        activeBuilderSessions.clear();

        for (NamespacedKey key : registeredBukkitKeys) {
            try {
                plugin.getServer().removeRecipe(key);
            } catch (Throwable ignored) {}
        }
        registeredBukkitKeys.clear();
        virtualRecipes.clear();

        plugin.getLogger().info("[CustomCraft] Модуль отключен");
    }

    private void loadSettings() {
        commandName = config.getString("command", "craft");
        commandPermission = config.getString("permission", "dci.craft");
        adminPermission = config.getString("admin-permission", "dci.craft.admin");

        mainTitle = colorize(config.getString("gui.main-title", "&8⚒ &lМастерская Крафта"));
        stationChooserTitle = colorize(config.getString("gui.station-chooser-title", "&8⚙ &lВыберите станцию крафта"));
        builderTitleTemplate = config.getString("gui.builder-title", "&8✨ &lКонструктор: &e{station}");
        craftTitle = colorize(config.getString("gui.craft-title", "&8⚒ &lВиртуальный верстак"));
        catalogTitle = colorize(config.getString("gui.catalog-title", "&8📖 &lКаталог рецептов"));

        String fillerName = config.getString("gui.filler", "GRAY_STAINED_GLASS_PANE");
        Material filler = EnumCache.getMaterial(fillerName);
        if (filler != null) fillerMaterial = filler;

        craftResultSlot = config.getInt("gui.result-slot", 15);
    }

    // ===== ПЕРЕЗАГРУЗКА И СБОР РЕЦЕПТОВ =====

    public void reloadRecipes() {
        virtualRecipes.clear();

        if (plugin.getItemHandler() != null) {
            for (CustomItem item : plugin.getItemHandler().getAllCustomItems().values()) {
                if (item == null || !item.hasRecipes()) continue;
                collectItemRecipes(item);
            }
        }

        if (plugin.getApiItemRegistry() != null) {
            for (AbstractCustomItem javaItem : plugin.getApiItemRegistry().getAllItems().values()) {
                if (javaItem == null) continue;
                try {
                    List<RecipeDef> defs = javaItem.getRecipes();
                    if (defs == null || defs.isEmpty()) continue;
                    for (RecipeDef def : defs) {
                        CraftRecipe recipe = CraftRecipe.parseJava(plugin, javaItem, def);
                        if (recipe != null) virtualRecipes.add(recipe);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[CustomCraft] Ошибка в рецепте Java-предмета '"
                            + javaItem.getId() + "': " + e.getMessage());
                }
            }
        }
    }

    private void collectItemRecipes(CustomItem item) {
        Map<String, Object> root = item.getRecipes();
        if (root == null) return;

        Object shaped = root.get("shaped");
        if (shaped instanceof List) {
            for (Object recipeObj : (List<?>) shaped) {
                CraftRecipe recipe = CraftRecipe.parseShaped(plugin, item, asMap(recipeObj));
                if (recipe != null) virtualRecipes.add(recipe);
            }
        }

        Object shapeless = root.get("shapeless");
        if (shapeless instanceof List) {
            for (Object recipeObj : (List<?>) shapeless) {
                CraftRecipe recipe = CraftRecipe.parseShapeless(plugin, item, asMap(recipeObj));
                if (recipe != null) virtualRecipes.add(recipe);
            }
        }
    }

    // ===== КОМАНДЫ =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cКоманда доступна только игрокам!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(commandPermission) && !player.isOp()) {
            player.sendMessage(colorize(config.getString("messages.no-permission", "&cУ вас нет прав для /craft!")));
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("create") || sub.equals("new")) {
                if (!hasAdminPermission(player)) {
                    player.sendMessage(colorize(config.getString("messages.no-admin-permission", "&cУ вас нет прав администратора!")));
                    return true;
                }
                openStationChooser(player);
                return true;
            } else if (sub.equals("table") || sub.equals("bench") || sub.equals("grid")) {
                openCraftGui(player);
                return true;
            } else if (sub.equals("list") || sub.equals("catalog")) {
                openRecipeCatalog(player, 0);
                return true;
            } else if (sub.equals("delete") || sub.equals("remove")) {
                if (!hasAdminPermission(player)) {
                    player.sendMessage(colorize(config.getString("messages.no-admin-permission", "&cУ вас нет прав администратора!")));
                    return true;
                }
                if (args.length >= 2) {
                    String targetId = args[1].toLowerCase(Locale.ROOT);
                    CraftRecipe found = null;
                    for (CraftRecipe cr : virtualRecipes) {
                        if (cr.id != null) {
                            String lower = cr.id.toLowerCase(Locale.ROOT);
                            if (lower.equals(targetId) || lower.startsWith(targetId)) {
                                found = cr;
                                break;
                            }
                        }
                    }
                    if (found != null) {
                        deleteRecipe(player, found);
                    } else {
                        player.sendMessage(colorize("&cРецепт с UUID/ID '&e" + targetId + "&c' не найден!"));
                    }
                    return true;
                }
                openRecipeCatalog(player, 0);
                player.sendMessage(colorize("&e[CustomCraft] Выберите рецепт для удаления (или используйте &cShift + ПКМ &eв каталоге)."));
                return true;
            } else if (sub.equals("reload") && hasAdminPermission(player)) {
                reloadRecipes();
                loadSavedRecipes();
                player.sendMessage(colorize("&a[CustomCraft] Рецепты успешно перезагружены!"));
                return true;
            }
        }

        openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("table");
            list.add("list");
            if (sender.isOp() || sender.hasPermission(adminPermission)) {
                list.add("create");
                list.add("delete");
                list.add("reload");
            }
            return filterPrefix(list, args[0]);
        } else if (args.length == 2 && (sender.isOp() || sender.hasPermission(adminPermission))) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("delete") || sub.equals("remove")) {
                for (CraftRecipe cr : virtualRecipes) {
                    if (cr.id != null && !cr.id.startsWith("yaml_") && !cr.id.startsWith("java_")) {
                        list.add(cr.id);
                    }
                }
                return filterPrefix(list, args[1]);
            }
        }
        return Collections.emptyList();
    }

    private boolean hasAdminPermission(Player player) {
        return player.isOp() || player.hasPermission(adminPermission);
    }

    // ===== GUI 1: ГЛАВНОЕ МЕНЮ ВЫБОРА СТАНЦИИ =====

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.MAIN_MENU, null, 0, null), 36, mainTitle);
        fillBorder(inv);

        inv.setItem(10, createGuiItem(Material.CRAFTING_TABLE, "&e&l⚒ Верстак (3x3)",
                "&7Открыть интерактивный верстак 3x3",
                "&7для крафта кастомных предметов.",
                "",
                "&e▶ Нажмите, чтобы открыть"));

        inv.setItem(11, createGuiItem(Material.FURNACE, "&6&l🔥 Обычная печь",
                "&7Рецепты плавки и обжига предметов.",
                "&7Работает как в игре, так и в блоках печи.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(12, createGuiItem(Material.BLAST_FURNACE, "&c&l⛏ Плавильня",
                "&7Быстрая переплавка руд и металлов.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(13, createGuiItem(Material.SMOKER, "&6&l🍖 Коптильня",
                "&7Быстрое приготовление и копчение пищи.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(14, createGuiItem(Material.CAMPFIRE, "&e&l🏕 Костёр",
                "&7Рецепты жарки на костре.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(15, createGuiItem(Material.SMITHING_TABLE, "&b&l🔨 Кузнечный стол",
                "&7Улучшение и ковка кастомного снаряжения",
                "&7по кузнечным шаблонам.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(16, createGuiItem(Material.STONECUTTER, "&f&l🪨 Камнерез",
                "&7Точная обработка камня и материалов.",
                "",
                "&e▶ Нажмите для просмотра рецептов"));

        inv.setItem(30, createGuiItem(Material.BOOK, "&a&l📖 Каталог всех рецептов",
                "&7Посмотреть полный список всех",
                "&7зарегистрированных кастомных рецептов.",
                "&7Управление и удаление рецептов.",
                "",
                "&e▶ Нажмите для перехода"));

        if (hasAdminPermission(player)) {
            inv.setItem(32, createGuiItem(Material.NETHER_STAR, "&d&l➕ Создать кастомный крафт",
                    "&7Визуальный конструктор рецептов прямо в игре!",
                    "&7Поддерживает количества в слотах (например: 3 алмаза),",
                    "&7кастомные предметы Artifex и NBT.",
                    "",
                    "&a▶ Нажмите для создания"));
        }

        player.openInventory(inv);
    }

    // ===== GUI 2: ВЫБОР СТАНЦИИ ДЛЯ СОЗДАНИЯ РЕЦЕПТА =====

    public void openStationChooser(Player player) {
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.STATION_CHOOSER, null, 0, null), 36, stationChooserTitle);
        fillBorder(inv);

        inv.setItem(10, createGuiItem(Material.CRAFTING_TABLE, "&e&l⚒ Верстак: Форменный (3x3)",
                "&7Рецепт с точным расположением и",
                "&7количеством предметов в сетке 3х3.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(11, createGuiItem(Material.FIREWORK_STAR, "&d&l🌀 Верстак: Бесформенный",
                "&7Ингредиенты с точным количеством",
                "&7в любом порядке в верстаке.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(12, createGuiItem(Material.FURNACE, "&6&l🔥 Печь (Smelting)",
                "&7Плавление предметов в обычной печи",
                "&7с настройкой времени и опыта.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(13, createGuiItem(Material.BLAST_FURNACE, "&c&l⛏ Плавильня (Blasting)",
                "&7Быстрое плавление в плавильне.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(14, createGuiItem(Material.SMOKER, "&6&l🍖 Коптильня (Smoking)",
                "&7Приготовление пищи в коптильне.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(15, createGuiItem(Material.CAMPFIRE, "&e&l🏕 Костёр (Campfire)",
                "&7Жарка на обычном костре.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(16, createGuiItem(Material.SMITHING_TABLE, "&b&l🔨 Кузнечный стол (Smithing)",
                "&7Кузнечное улучшение: Шаблон +",
                "&7Основа + Материал = Результат.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(22, createGuiItem(Material.STONECUTTER, "&f&l🪨 Камнерез (Stonecutting)",
                "&7Один блок -> Кастомный результат.",
                "",
                "&a▶ Выбрать эту станцию"));

        inv.setItem(31, createGuiItem(Material.ARROW, "&c&l⬅ Назад в главное меню",
                "&7Вернуться к списку станций"));

        player.openInventory(inv);
    }

    // ===== GUI 3: ВИЗУАЛЬНЫЙ КОНСТРУКТОР РЕЦЕПТОВ =====

    public void openRecipeBuilder(Player player, StationType station) {
        String title = colorize(builderTitleTemplate.replace("{station}", station.displayName));
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.BUILDER, station, 0, null), 54, title);
        BuilderSession session = new BuilderSession(player.getUniqueId(), station, inv);
        activeBuilderSessions.put(player.getUniqueId(), session);

        fillBorder54(inv);

        if (station == StationType.WORKBENCH_SHAPED || station == StationType.WORKBENCH_SHAPELESS) {
            for (int slot : GRID_3X3_SLOTS) {
                inv.setItem(slot, null);
            }
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&e&l⟹ Результат ⟹", "&7Поместите результат крафта правее!"));
            inv.setItem(25, null);
        } else if (station == StationType.FURNACE || station == StationType.BLAST_FURNACE
                || station == StationType.SMOKER || station == StationType.CAMPFIRE) {
            inv.setItem(20, null);
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&e&l⟹ Обжиг ⟹", "&7Поместите готовый результат правее!"));
            inv.setItem(25, null);
            updateCookingButtons(session);
        } else if (station == StationType.SMITHING) {
            inv.setItem(19, null);
            inv.setItem(20, null);
            inv.setItem(21, null);
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&b&l⟹ Ковка ⟹", "&7Результат улучшения"));
            inv.setItem(25, null);
        } else if (station == StationType.STONECUTTER) {
            inv.setItem(20, null);
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&f&l⟹ Резка ⟹", "&7Результат камнереза"));
            inv.setItem(25, null);
        }

        inv.setItem(48, createGuiItem(Material.RED_STAINED_GLASS_PANE, "&c&l❌ Отмена", "&7Вернуть все предметы и выйти"));
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "&a&l💾 СОХРАНИТЬ РЕЦЕПТ",
                "&7Сохраняет рецепт в игру и конфиг.",
                "&7Поддерживает количество в слотах (например, 3 алмаза),",
                "&7NBT, текстуры и ванильные материалы!"));
        inv.setItem(50, createGuiItem(Material.BOOK, "&e&lℹ Подсказка по количеству",
                "&7• Вы можете класть любое количество предметов в слоты!",
                "&7• Например: 3 алмаза в один слот и 1 палка в другой.",
                "&7• При крафте будет списываться именно указанное число!"));

        player.openInventory(inv);
    }

    private void updateCookingButtons(BuilderSession session) {
        double seconds = session.cookingTime / 20.0;
        session.inventory.setItem(29, createGuiItem(Material.CLOCK, "&e&l⏱ Время плавки: &a" + seconds + " сек",
                "&7Текущее: &f" + session.cookingTime + " тиков",
                "",
                "&a[ЛКМ] &7+1 секунда (+20 тиков)",
                "&c[ПКМ] &7-1 секунда (-20 тиков)"));

        session.inventory.setItem(33, createGuiItem(Material.EXPERIENCE_BOTTLE, "&b&l✨ Опыт: &a" + String.format(Locale.ROOT, "%.2f", session.experience) + " XP",
                "&7Опыт, выдаваемый за плавку предмета.",
                "",
                "&a[ЛКМ] &7+0.2 XP",
                "&c[ПКМ] &7-0.2 XP"));
    }

    // ===== GUI 4: ВИРТУАЛЬНЫЙ ВЕРСТАК (3X3) =====

    public void openCraftGui(Player player) {
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.VIRTUAL_WORKBENCH, null, 0, null), 27, craftTitle);
        CraftView view = new CraftView(player.getUniqueId(), inv);
        openCraftViews.put(player.getUniqueId(), view);

        fillCraftFiller(inv);
        inv.setItem(craftResultSlot, null);

        inv.setItem(26, createGuiItem(Material.ARROW, "&c&l⬅ В меню", "&7Вернуться в главное меню"));

        player.openInventory(inv);
    }

    private void fillCraftFiller(Inventory inv) {
        ItemStack filler = createFiller();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (isGridSlot(slot) || slot == craftResultSlot || slot == 26) continue;
            inv.setItem(slot, filler);
        }
    }

    // ===== GUI 5: КАТАЛОГ РЕЦЕПТОВ (С УДАЛЕНИЕМ) =====

    public void openRecipeCatalog(Player player, int page) {
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.RECIPE_CATALOG, null, page, null), 54, catalogTitle);
        fillBorder54(inv);

        int pageSize = 28;
        int total = virtualRecipes.size();
        int maxPage = Math.max(0, (total - 1) / pageSize);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        int start = page * pageSize;
        int end = Math.min(start + pageSize, total);

        int[] catalogSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        boolean isAdmin = hasAdminPermission(player);

        for (int i = 0; i < (end - start); i++) {
            CraftRecipe recipe = virtualRecipes.get(start + i);
            ItemStack result = recipe.getResultPreview();
            if (result == null) continue;

            ItemStack icon = result.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8§m------------------------");
                lore.add("§7Тип: §e" + recipe.getStationDisplayName());
                lore.add("§7Количество на выходе: §a" + recipe.amount);
                if (recipe.id != null) {
                    lore.add("§8UUID: §7" + recipe.id);
                }
                lore.add("§8§m------------------------");
                lore.add("§e▶ [ЛКМ] Посмотреть рецепт");
                if (isAdmin) {
                    lore.add("§c▶ [Shift+ПКМ] УДАЛИТЬ РЕЦЕПТ");
                }
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(catalogSlots[i], icon);
        }

        if (page > 0) {
            inv.setItem(48, createGuiItem(Material.ARROW, "&a&l⬅ Страница " + page, "&7Перейти на предыдущую"));
        }
        inv.setItem(49, createGuiItem(Material.OAK_DOOR, "&c&l🏠 В главное меню", "&7Вернуться"));
        if (page < maxPage) {
            inv.setItem(50, createGuiItem(Material.ARROW, "&a&lСтраница " + (page + 2) + " ➡", "&7Перейти на следующую"));
        }

        player.openInventory(inv);
    }

    // ===== GUI 6: ДЕТАЛИ И УДАЛЕНИЕ РЕЦЕПТА =====

    public void openRecipeDetails(Player player, CraftRecipe recipe, int fromPage) {
        String shortId = recipe.id != null ? (recipe.id.length() > 13 ? recipe.id.substring(0, 8) + "..." : recipe.id) : "Рецепт";
        String title = colorize("&8🔍 &lДетали: &e" + shortId);
        Inventory inv = Bukkit.createInventory(new CraftHolder(player.getUniqueId(), GuiType.RECIPE_DETAILS, recipe.stationType, fromPage, recipe), 54, title);
        fillBorder54(inv);

        if (recipe.isShaped && recipe.shapedMatrix != null) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = recipe.shapedMatrix[r][c];
                    if (st != null && st.getType() != Material.AIR) {
                        inv.setItem(GRID_3X3_SLOTS[r * 3 + c], st.clone());
                    }
                }
            }
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&e&l⟹ Результат ⟹", "&7Требуемые предметы и их количество"));
            inv.setItem(25, recipe.createResult(plugin));

        } else if (!recipe.isShaped && recipe.rawIngredients != null && !recipe.rawIngredients.isEmpty()) {
            for (int i = 0; i < Math.min(GRID_3X3_SLOTS.length, recipe.rawIngredients.size()); i++) {
                ItemStack st = recipe.rawIngredients.get(i);
                if (st != null && st.getType() != Material.AIR) {
                    inv.setItem(GRID_3X3_SLOTS[i], st.clone());
                }
            }
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&d&l⟹ Результат ⟹", "&7Бесформенный крафт"));
            inv.setItem(25, recipe.createResult(plugin));

        } else if (recipe.rawIngredients != null && !recipe.rawIngredients.isEmpty()) {
            inv.setItem(20, recipe.rawIngredients.get(0).clone());
            inv.setItem(23, createGuiItem(Material.SPECTRAL_ARROW, "&6&l⟹ Обжиг ⟹",
                    "&7Время: &e" + (recipe.cookingTime / 20.0) + " сек",
                    "&7Опыт: &b" + recipe.experience + " XP"));
            inv.setItem(25, recipe.createResult(plugin));
        }

        inv.setItem(45, createGuiItem(Material.ARROW, "&c&l⬅ Назад в каталог", "&7Вернуться к списку рецептов"));

        if (hasAdminPermission(player)) {
            inv.setItem(49, createGuiItem(Material.BARRIER, "&4&l🗑 УДАЛИТЬ ЭТОТ РЕЦЕПТ",
                    "&7Нажмите, чтобы навсегда удалить этот рецепт",
                    "&7из игры и конфигурации recipes.yml.",
                    "&8UUID: &7" + (recipe.id != null ? recipe.id : "—"),
                    "",
                    "&c▶ Нажмите для удаления"));
        }

        player.openInventory(inv);
    }

    // ===== ОБРАБОТКА КЛИКОВ (EVENT HANDLERS) =====

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof CraftHolder)) return;

        CraftHolder holder = (CraftHolder) top.getHolder();
        int rawSlot = event.getRawSlot();
        boolean isTop = rawSlot >= 0 && rawSlot < top.getSize();

        // 1. ГЛАВНОЕ МЕНЮ
        if (holder.guiType == GuiType.MAIN_MENU) {
            event.setCancelled(true);
            if (!isTop) return;

            switch (rawSlot) {
                case 10: openCraftGui(player); break;
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 30: openRecipeCatalog(player, 0); break;
                case 32:
                    if (hasAdminPermission(player)) openStationChooser(player);
                    break;
            }
            return;
        }

        // 2. ВЫБОР СТАНЦИИ
        if (holder.guiType == GuiType.STATION_CHOOSER) {
            event.setCancelled(true);
            if (!isTop) return;

            StationType chosen = null;
            switch (rawSlot) {
                case 10: chosen = StationType.WORKBENCH_SHAPED; break;
                case 11: chosen = StationType.WORKBENCH_SHAPELESS; break;
                case 12: chosen = StationType.FURNACE; break;
                case 13: chosen = StationType.BLAST_FURNACE; break;
                case 14: chosen = StationType.SMOKER; break;
                case 15: chosen = StationType.CAMPFIRE; break;
                case 16: chosen = StationType.SMITHING; break;
                case 22: chosen = StationType.STONECUTTER; break;
                case 31: openMainMenu(player); return;
            }

            if (chosen != null) {
                openRecipeBuilder(player, chosen);
            }
            return;
        }

        // 3. КОНСТРУКТОР РЕЦЕПТОВ (BUILDER)
        if (holder.guiType == GuiType.BUILDER) {
            BuilderSession session = activeBuilderSessions.get(player.getUniqueId());
            if (session == null) return;

            if (isTop) {
                if (rawSlot == 48) {
                    event.setCancelled(true);
                    returnBuilderItems(session);
                    player.sendMessage(colorize(config.getString("messages.recipe-canceled", "&eСоздание рецепта отменено.")));
                    openMainMenu(player);
                    return;
                }
                if (rawSlot == 49) {
                    event.setCancelled(true);
                    saveCreatedRecipe(player, session);
                    return;
                }
                if (rawSlot == 50) {
                    event.setCancelled(true);
                    return;
                }

                if (session.station == StationType.FURNACE || session.station == StationType.BLAST_FURNACE
                        || session.station == StationType.SMOKER || session.station == StationType.CAMPFIRE) {
                    if (rawSlot == 29) {
                        event.setCancelled(true);
                        if (event.isLeftClick()) session.cookingTime = Math.min(6000, session.cookingTime + 20);
                        else if (event.isRightClick()) session.cookingTime = Math.max(20, session.cookingTime - 20);
                        updateCookingButtons(session);
                        return;
                    }
                    if (rawSlot == 33) {
                        event.setCancelled(true);
                        if (event.isLeftClick()) session.experience = Math.min(100.0f, session.experience + 0.2f);
                        else if (event.isRightClick()) session.experience = Math.max(0.0f, session.experience - 0.2f);
                        updateCookingButtons(session);
                        return;
                    }
                }

                if (!isAllowedBuilderSlot(session.station, rawSlot)) {
                    event.setCancelled(true);
                }
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        // 4. КАТАЛОГ (ПРОСМОТР + БЫСТРОЕ УДАЛЕНИЕ)
        if (holder.guiType == GuiType.RECIPE_CATALOG) {
            event.setCancelled(true);
            if (!isTop) return;

            if (rawSlot == 48) {
                openRecipeCatalog(player, holder.page - 1);
                return;
            } else if (rawSlot == 49) {
                openMainMenu(player);
                return;
            } else if (rawSlot == 50) {
                openRecipeCatalog(player, holder.page + 1);
                return;
            }

            // Клик по карточке рецепта
            int indexInPage = getCatalogIndex(rawSlot);
            if (indexInPage >= 0) {
                int globalIndex = holder.page * 28 + indexInPage;
                if (globalIndex >= 0 && globalIndex < virtualRecipes.size()) {
                    CraftRecipe clickedRecipe = virtualRecipes.get(globalIndex);

                    // Быстрое удаление через Shift+ПКМ
                    if (hasAdminPermission(player) && (event.isShiftClick() && event.isRightClick())) {
                        deleteRecipe(player, clickedRecipe);
                        openRecipeCatalog(player, holder.page);
                        return;
                    }

                    // Обычный клик открывает подробные детали рецепта
                    openRecipeDetails(player, clickedRecipe, holder.page);
                }
            }
            return;
        }

        // 5. ДЕТАЛИ И УДАЛЕНИЕ РЕЦЕПТА
        if (holder.guiType == GuiType.RECIPE_DETAILS) {
            event.setCancelled(true);
            if (!isTop) return;

            if (rawSlot == 45) { // Назад в каталог
                openRecipeCatalog(player, holder.page);
                return;
            }

            if (rawSlot == 49 && hasAdminPermission(player)) { // Удалить рецепт
                if (holder.targetRecipe != null) {
                    deleteRecipe(player, holder.targetRecipe);
                    openRecipeCatalog(player, holder.page);
                }
                return;
            }
            return;
        }

        // 6. ВИРТУАЛЬНЫЙ ВЕРСТАК (3X3)
        if (holder.guiType == GuiType.VIRTUAL_WORKBENCH) {
            CraftView view = openCraftViews.get(player.getUniqueId());
            if (view == null) return;

            if (isTop) {
                if (rawSlot == 26) {
                    event.setCancelled(true);
                    returnCraftItems(view);
                    openMainMenu(player);
                    return;
                }

                if (rawSlot == craftResultSlot) {
                    event.setCancelled(true);
                    handleCraftResultClick(player, view, event.getClick());
                    return;
                }

                if (!isGridSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (view.inventory.getViewers().contains(player)) {
                    updateCraftResult(view);
                }
            });
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof CraftHolder)) return;

        CraftHolder holder = (CraftHolder) top.getHolder();

        if (holder.guiType == GuiType.MAIN_MENU || holder.guiType == GuiType.STATION_CHOOSER
                || holder.guiType == GuiType.RECIPE_CATALOG || holder.guiType == GuiType.RECIPE_DETAILS) {
            event.setCancelled(true);
            return;
        }

        if (holder.guiType == GuiType.BUILDER) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize() && !isAllowedBuilderSlot(holder.stationType, rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (holder.guiType == GuiType.VIRTUAL_WORKBENCH) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize() && (!isGridSlot(rawSlot) || rawSlot == craftResultSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            CraftView view = openCraftViews.get(event.getWhoClicked().getUniqueId());
            if (view != null) {
                Bukkit.getScheduler().runTask(plugin, () -> updateCraftResult(view));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Inventory top = event.getInventory();
        if (top == null || !(top.getHolder() instanceof CraftHolder)) return;

        CraftHolder holder = (CraftHolder) top.getHolder();

        if (holder.guiType == GuiType.BUILDER) {
            BuilderSession session = activeBuilderSessions.remove(player.getUniqueId());
            if (session != null) {
                returnBuilderItems(session);
            }
        }

        if (holder.guiType == GuiType.VIRTUAL_WORKBENCH) {
            CraftView view = openCraftViews.remove(player.getUniqueId());
            if (view != null) {
                returnCraftItems(view);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        BuilderSession bs = activeBuilderSessions.remove(uid);
        if (bs != null) returnBuilderItems(bs);

        CraftView cv = openCraftViews.remove(uid);
        if (cv != null) returnCraftItems(cv);
    }

    // ===== УДАЛЕНИЕ РЕЦЕПТА =====

    public void deleteRecipe(Player player, CraftRecipe recipe) {
        if (recipe == null) return;

        // Если рецепт привязан к исходному YAML-предмету плагина
        if (recipe.id == null || recipe.id.startsWith("yaml_") || recipe.id.startsWith("java_")) {
            player.sendMessage(colorize("&eЭтот рецепт привязан к файлу предмета в items/. Для изменения удалите его в файле предмета."));
            playErrorSound(player);
            return;
        }

        // 1. Снятие с регистрации в Bukkit
        if (recipe.namespacedKey != null) {
            try {
                plugin.getServer().removeRecipe(recipe.namespacedKey);
            } catch (Throwable ignored) {}
            registeredBukkitKeys.remove(recipe.namespacedKey);
        }

        // 2. Удаление из списка виртуального верстака
        virtualRecipes.remove(recipe);

        // 3. Удаление из recipes.yml
        File recipesFile = new File(folder, "recipes.yml");
        if (recipesFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(recipesFile);
            if (yaml.isConfigurationSection("recipes")) {
                yaml.set("recipes." + recipe.id, null);
                ConfigurationSection root = yaml.getConfigurationSection("recipes");
                if (root != null) {
                    for (String k : root.getKeys(false)) {
                        if (recipe.id != null && recipe.id.equalsIgnoreCase(root.getString(k + ".uuid"))) {
                            yaml.set("recipes." + k, null);
                        }
                    }
                }
                try {
                    yaml.save(recipesFile);
                } catch (IOException e) {
                    plugin.getLogger().warning("[CustomCraft] Ошибка при сохранении recipes.yml: " + e.getMessage());
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        player.sendMessage(colorize("&a✔ Рецепт &e" + recipe.id + " &aуспешно удалён из игры и конфигурации!"));
    }

    // ===== СОХРАНЕНИЕ КАСТОМНОГО РЕЦЕПТА (BUILDER LOGIC) =====

    private void saveCreatedRecipe(Player player, BuilderSession session) {
        Inventory inv = session.inventory;
        StationType station = session.station;

        ItemStack result = inv.getItem(25);
        if (result == null || result.getType() == Material.AIR) {
            player.sendMessage(colorize(config.getString("messages.recipe-error-no-result", "&cОшибка: поместите результат в слот справа!")));
            playErrorSound(player);
            return;
        }

        boolean hasIngredients = false;
        if (station == StationType.WORKBENCH_SHAPED || station == StationType.WORKBENCH_SHAPELESS) {
            for (int slot : GRID_3X3_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    hasIngredients = true;
                    break;
                }
            }
        } else if (station == StationType.FURNACE || station == StationType.BLAST_FURNACE
                || station == StationType.SMOKER || station == StationType.CAMPFIRE || station == StationType.STONECUTTER) {
            ItemStack in = inv.getItem(20);
            if (in != null && in.getType() != Material.AIR) hasIngredients = true;
        } else if (station == StationType.SMITHING) {
            ItemStack t = inv.getItem(19);
            ItemStack b = inv.getItem(20);
            ItemStack a = inv.getItem(21);
            if (t != null && b != null && a != null
                    && t.getType() != Material.AIR && b.getType() != Material.AIR && a.getType() != Material.AIR) {
                hasIngredients = true;
            }
        }

        if (!hasIngredients) {
            player.sendMessage(colorize(config.getString("messages.recipe-error-no-ingredients", "&cОшибка: укажите ингредиенты!")));
            playErrorSound(player);
            return;
        }

        UUID recipeUuid = UUID.randomUUID();
        String uniqueId = recipeUuid.toString().toLowerCase(Locale.ROOT);

        try {
            boolean registered = registerAndPersistRecipe(uniqueId, session, result);
            if (registered) {
                player.sendMessage(colorize(config.getString("messages.recipe-saved", "&a✔ Рецепт успешно создан и добавлен на сервер!")
                        .replace("{station}", station.displayName)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

                returnBuilderItems(session);
                activeBuilderSessions.remove(player.getUniqueId());
                player.closeInventory();
            } else {
                player.sendMessage(colorize("&cНе удалось зарегистрировать рецепт в Bukkit!"));
                playErrorSound(player);
            }
        } catch (Exception e) {
            player.sendMessage(colorize("&cОшибка при сохранении: " + e.getMessage()));
            plugin.getLogger().warning("[CustomCraft] Ошибка создания рецепта: " + e.getMessage());
            playErrorSound(player);
        }
    }

    private boolean registerAndPersistRecipe(String id, BuilderSession session, ItemStack result) throws IOException {
        NamespacedKey key = new NamespacedKey(plugin, id);
        StationType station = session.station;
        Inventory inv = session.inventory;

        File recipesFile = new File(folder, "recipes.yml");
        YamlConfiguration yaml = recipesFile.exists() ? YamlConfiguration.loadConfiguration(recipesFile) : new YamlConfiguration();
        ConfigurationSection root = yaml.isConfigurationSection("recipes") ? yaml.getConfigurationSection("recipes") : yaml.createSection("recipes");
        ConfigurationSection sec = root.createSection(id);

        sec.set("uuid", id);
        sec.set("station", station.name());
        serializeItemToConfig(sec.createSection("result"), result);

        Recipe bukkitRecipe = null;

        if (station == StationType.WORKBENCH_SHAPED) {
            ItemStack[][] matrix = new ItemStack[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = inv.getItem(GRID_3X3_SLOTS[r * 3 + c]);
                    matrix[r][c] = (st != null && st.getType() != Material.AIR) ? st.clone() : null;
                }
            }

            Map<Character, RecipeChoice> keyMap = new HashMap<>();
            Map<Character, ItemStack> exampleKeyMap = new HashMap<>();
            char currentKey = 'A';
            List<String> patternLines = new ArrayList<>();

            for (int r = 0; r < 3; r++) {
                StringBuilder row = new StringBuilder();
                for (int c = 0; c < 3; c++) {
                    ItemStack st = matrix[r][c];
                    if (st == null || st.getType() == Material.AIR) {
                        row.append(' ');
                    } else {
                        char match = ' ';
                        for (Map.Entry<Character, ItemStack> entry : exampleKeyMap.entrySet()) {
                            if (isItemSimilarWithAmount(entry.getValue(), st)) {
                                match = entry.getKey();
                                break;
                            }
                        }
                        if (match == ' ') {
                            match = currentKey++;
                            exampleKeyMap.put(match, st.clone());
                            keyMap.put(match, toRecipeChoice(st));
                        }
                        row.append(match);
                    }
                }
                patternLines.add(row.toString());
            }

            ShapedRecipe shaped = new ShapedRecipe(key, result);
            shaped.shape(patternLines.toArray(new String[0]));
            for (Map.Entry<Character, RecipeChoice> entry : keyMap.entrySet()) {
                shaped.setIngredient(entry.getKey(), entry.getValue());
            }
            bukkitRecipe = shaped;

            sec.set("pattern", patternLines);
            ConfigurationSection keysSec = sec.createSection("keys");
            for (Map.Entry<Character, ItemStack> e : exampleKeyMap.entrySet()) {
                serializeItemToConfig(keysSec.createSection(String.valueOf(e.getKey())), e.getValue());
            }

            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), true, matrix, keyMap, null, null, result, station, 0, 0));

        } else if (station == StationType.WORKBENCH_SHAPELESS) {
            List<RecipeChoice> choices = new ArrayList<>();
            List<ItemStack> rawIngredients = new ArrayList<>();
            for (int slot : GRID_3X3_SLOTS) {
                ItemStack st = inv.getItem(slot);
                if (st != null && st.getType() != Material.AIR) {
                    choices.add(toRecipeChoice(st));
                    rawIngredients.add(st.clone());
                }
            }

            ShapelessRecipe shapeless = new ShapelessRecipe(key, result);
            for (RecipeChoice choice : choices) {
                shapeless.addIngredient(choice);
            }
            bukkitRecipe = shapeless;

            ConfigurationSection ingSec = sec.createSection("ingredients");
            for (int i = 0; i < rawIngredients.size(); i++) {
                serializeItemToConfig(ingSec.createSection("item_" + i), rawIngredients.get(i));
            }

            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, choices, rawIngredients, result, station, 0, 0));

        } else if (station == StationType.FURNACE) {
            ItemStack in = inv.getItem(20);
            FurnaceRecipe fr = new FurnaceRecipe(key, result, toRecipeChoice(in), session.experience, session.cookingTime);
            bukkitRecipe = fr;
            serializeItemToConfig(sec.createSection("ingredient"), in);
            sec.set("cooking-time", session.cookingTime);
            sec.set("experience", session.experience);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in.clone()), result, station, session.cookingTime, session.experience));

        } else if (station == StationType.BLAST_FURNACE) {
            ItemStack in = inv.getItem(20);
            BlastingRecipe br = new BlastingRecipe(key, result, toRecipeChoice(in), session.experience, session.cookingTime);
            bukkitRecipe = br;
            serializeItemToConfig(sec.createSection("ingredient"), in);
            sec.set("cooking-time", session.cookingTime);
            sec.set("experience", session.experience);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in.clone()), result, station, session.cookingTime, session.experience));

        } else if (station == StationType.SMOKER) {
            ItemStack in = inv.getItem(20);
            SmokingRecipe sr = new SmokingRecipe(key, result, toRecipeChoice(in), session.experience, session.cookingTime);
            bukkitRecipe = sr;
            serializeItemToConfig(sec.createSection("ingredient"), in);
            sec.set("cooking-time", session.cookingTime);
            sec.set("experience", session.experience);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in.clone()), result, station, session.cookingTime, session.experience));

        } else if (station == StationType.CAMPFIRE) {
            ItemStack in = inv.getItem(20);
            CampfireRecipe cr = new CampfireRecipe(key, result, toRecipeChoice(in), session.experience, session.cookingTime);
            bukkitRecipe = cr;
            serializeItemToConfig(sec.createSection("ingredient"), in);
            sec.set("cooking-time", session.cookingTime);
            sec.set("experience", session.experience);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in.clone()), result, station, session.cookingTime, session.experience));

        } else if (station == StationType.SMITHING) {
            ItemStack t = inv.getItem(19);
            ItemStack b = inv.getItem(20);
            ItemStack a = inv.getItem(21);
            SmithingTransformRecipe str = new SmithingTransformRecipe(key, result, toRecipeChoice(t), toRecipeChoice(b), toRecipeChoice(a));
            bukkitRecipe = str;
            serializeItemToConfig(sec.createSection("template"), t);
            serializeItemToConfig(sec.createSection("base"), b);
            serializeItemToConfig(sec.createSection("addition"), a);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(t.clone(), b.clone(), a.clone()), result, station, 0, 0));

        } else if (station == StationType.STONECUTTER) {
            ItemStack in = inv.getItem(20);
            StonecuttingRecipe scr = new StonecuttingRecipe(key, result, toRecipeChoice(in));
            bukkitRecipe = scr;
            serializeItemToConfig(sec.createSection("ingredient"), in);
            virtualRecipes.add(new CraftRecipe(id, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in.clone()), result, station, 0, 0));
        }

        if (bukkitRecipe != null) {
            boolean added = plugin.getServer().addRecipe(bukkitRecipe);
            if (added) {
                registeredBukkitKeys.add(key);
                yaml.save(recipesFile);
                return true;
            }
        }

        return false;
    }

    private void loadSavedRecipes() {
        File recipesFile = new File(folder, "recipes.yml");
        if (!recipesFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(recipesFile);
        ConfigurationSection root = yaml.getConfigurationSection("recipes");
        if (root == null) return;

        int loadedCount = 0;
        for (String keyStr : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(keyStr);
            if (sec == null) continue;

            try {
                String recipeId = sec.getString("uuid", keyStr);
                NamespacedKey key = new NamespacedKey(plugin, keyStr);
                StationType station = StationType.valueOf(sec.getString("station", "WORKBENCH_SHAPED"));
                ItemStack result = deserializeItemFromConfig(sec.getConfigurationSection("result"));
                if (result == null) continue;

                Recipe bukkitRecipe = null;

                if (station == StationType.WORKBENCH_SHAPED) {
                    List<String> pattern = sec.getStringList("pattern");
                    ConfigurationSection keysSec = sec.getConfigurationSection("keys");
                    if (pattern.isEmpty() || keysSec == null) continue;

                    ShapedRecipe shaped = new ShapedRecipe(key, result);
                    shaped.shape(pattern.toArray(new String[0]));

                    Map<Character, RecipeChoice> keyChoices = new HashMap<>();
                    ItemStack[][] matrix = new ItemStack[3][3];

                    for (String k : keysSec.getKeys(false)) {
                        char c = k.charAt(0);
                        ItemStack ing = deserializeItemFromConfig(keysSec.getConfigurationSection(k));
                        if (ing != null) {
                            RecipeChoice choice = toRecipeChoice(ing);
                            shaped.setIngredient(c, choice);
                            keyChoices.put(c, choice);
                        }
                    }

                    for (int r = 0; r < 3; r++) {
                        String row = pattern.get(r);
                        for (int c = 0; c < 3; c++) {
                            char ch = row.charAt(c);
                            if (ch != ' ' && keysSec.isConfigurationSection(String.valueOf(ch))) {
                                matrix[r][c] = deserializeItemFromConfig(keysSec.getConfigurationSection(String.valueOf(ch)));
                            }
                        }
                    }

                    bukkitRecipe = shaped;
                    virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), true, matrix, keyChoices, null, null, result, station, 0, 0));

                } else if (station == StationType.WORKBENCH_SHAPELESS) {
                    ConfigurationSection ingSec = sec.getConfigurationSection("ingredients");
                    if (ingSec == null) continue;

                    ShapelessRecipe shapeless = new ShapelessRecipe(key, result);
                    List<RecipeChoice> choices = new ArrayList<>();
                    List<ItemStack> rawIngredients = new ArrayList<>();
                    for (String k : ingSec.getKeys(false)) {
                        ItemStack ing = deserializeItemFromConfig(ingSec.getConfigurationSection(k));
                        if (ing != null) {
                            RecipeChoice rc = toRecipeChoice(ing);
                            shapeless.addIngredient(rc);
                            choices.add(rc);
                            rawIngredients.add(ing);
                        }
                    }
                    bukkitRecipe = shapeless;
                    virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, choices, rawIngredients, result, station, 0, 0));

                } else if (station == StationType.FURNACE) {
                    ItemStack in = deserializeItemFromConfig(sec.getConfigurationSection("ingredient"));
                    if (in != null) {
                        int ct = sec.getInt("cooking-time", 200);
                        float exp = (float) sec.getDouble("experience", 0.7);
                        bukkitRecipe = new FurnaceRecipe(key, result, toRecipeChoice(in), exp, ct);
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in), result, station, ct, exp));
                    }
                } else if (station == StationType.BLAST_FURNACE) {
                    ItemStack in = deserializeItemFromConfig(sec.getConfigurationSection("ingredient"));
                    if (in != null) {
                        int ct = sec.getInt("cooking-time", 100);
                        float exp = (float) sec.getDouble("experience", 0.7);
                        bukkitRecipe = new BlastingRecipe(key, result, toRecipeChoice(in), exp, ct);
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in), result, station, ct, exp));
                    }
                } else if (station == StationType.SMOKER) {
                    ItemStack in = deserializeItemFromConfig(sec.getConfigurationSection("ingredient"));
                    if (in != null) {
                        int ct = sec.getInt("cooking-time", 100);
                        float exp = (float) sec.getDouble("experience", 0.7);
                        bukkitRecipe = new SmokingRecipe(key, result, toRecipeChoice(in), exp, ct);
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in), result, station, ct, exp));
                    }
                } else if (station == StationType.CAMPFIRE) {
                    ItemStack in = deserializeItemFromConfig(sec.getConfigurationSection("ingredient"));
                    if (in != null) {
                        int ct = sec.getInt("cooking-time", 600);
                        float exp = (float) sec.getDouble("experience", 0.7);
                        bukkitRecipe = new CampfireRecipe(key, result, toRecipeChoice(in), exp, ct);
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in), result, station, ct, exp));
                    }
                } else if (station == StationType.SMITHING) {
                    ItemStack t = deserializeItemFromConfig(sec.getConfigurationSection("template"));
                    ItemStack b = deserializeItemFromConfig(sec.getConfigurationSection("base"));
                    ItemStack a = deserializeItemFromConfig(sec.getConfigurationSection("addition"));
                    if (t != null && b != null && a != null) {
                        bukkitRecipe = new SmithingTransformRecipe(key, result, toRecipeChoice(t), toRecipeChoice(b), toRecipeChoice(a));
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(t, b, a), result, station, 0, 0));
                    }
                } else if (station == StationType.STONECUTTER) {
                    ItemStack in = deserializeItemFromConfig(sec.getConfigurationSection("ingredient"));
                    if (in != null) {
                        bukkitRecipe = new StonecuttingRecipe(key, result, toRecipeChoice(in));
                        virtualRecipes.add(new CraftRecipe(recipeId, key, plugin, null, null, result.getAmount(), false, null, null, null, List.of(in), result, station, 0, 0));
                    }
                }

                if (bukkitRecipe != null) {
                    try {
                        plugin.getServer().removeRecipe(key);
                    } catch (Throwable ignored) {}

                    if (plugin.getServer().addRecipe(bukkitRecipe)) {
                        registeredBukkitKeys.add(key);
                        loadedCount++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[CustomCraft] Ошибка загрузки сохранённого рецепта '" + keyStr + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("[CustomCraft] Загружено кастомных рецептов из recipes.yml: " + loadedCount);
    }

    // ===== СЕРИАЛИЗАЦИЯ И РАСПОЗНАВАНИЕ КАСТОМНЫХ ПРЕДМЕТОВ =====

    public String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (plugin.getItemHandler() != null) {
            String id = plugin.getItemHandler().getCustomItemId(item);
            if (id != null) return id;
        }
        return null;
    }

    public RecipeChoice toRecipeChoice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        String customId = getCustomId(item);

        if (customId != null || item.hasItemMeta()) {
            ItemStack exact = item.clone();
            exact.setAmount(1);
            return new RecipeChoice.ExactChoice(exact);
        }

        return new RecipeChoice.MaterialChoice(item.getType());
    }

    private void serializeItemToConfig(ConfigurationSection sec, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        String customId = getCustomId(item);

        sec.set("amount", item.getAmount());

        if (customId != null) {
            sec.set("custom-id", customId);
        }

        // Нативная сериализация Bukkit сохраняет 100% данных предмета: NBT, PDC, текстуры, модель, чары
        sec.set("item", item);

        if (!item.hasItemMeta()) {
            sec.set("material", item.getType().name());
        }
    }

    private ItemStack deserializeItemFromConfig(ConfigurationSection sec) {
        if (sec == null) return null;

        int amount = sec.getInt("amount", 1);

        // 1. Поиск по ID кастомного предмета Artifex (YAML и Java)
        if (sec.contains("custom-id")) {
            String customId = sec.getString("custom-id");
            if (plugin.getItemHandler() != null) {
                CustomItem ci = plugin.getItemHandler().getCustomItem(customId);
                if (ci != null) {
                    ItemStack st = ci.getItemStack().clone();
                    st.setAmount(amount);
                    return st;
                }
            }
            if (plugin.getApiItemRegistry() != null) {
                AbstractCustomItem ai = plugin.getApiItemRegistry().getItem(customId);
                if (ai != null) {
                    ItemStack st = ai.createItemStack();
                    st.setAmount(amount);
                    return st;
                }
            }
        }

        // 2. Полное восстановление через нативную сериализацию Bukkit ItemStack (сохраняет NBT, PDC, Lore, Enchants)
        if (sec.isItemStack("item")) {
            ItemStack st = sec.getItemStack("item");
            if (st != null) {
                st = st.clone();
                st.setAmount(amount);
                return st;
            }
        }

        // 3. Fallback: устаревший формат item-data
        if (sec.contains("item-data")) {
            try {
                ConfigurationSection dataSec = sec.getConfigurationSection("item-data");
                if (dataSec != null) {
                    ItemStack st = ItemStack.deserialize(dataSec.getValues(true));
                    st.setAmount(amount);
                    return st;
                }
            } catch (Exception ignored) {}
        }

        // 4. Обычный ванильный материал
        String matName = sec.getString("material", "STONE");
        Material mat = EnumCache.getMaterial(matName);
        if (mat == null) mat = Material.STONE;
        return new ItemStack(mat, amount);
    }

    private boolean isItemSimilarWithAmount(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    // ===== БЕЗОПАСНЫЙ ВОЗВРАТ ПРЕДМЕТОВ =====

    private void returnBuilderItems(BuilderSession session) {
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null) return;

        Inventory inv = session.inventory;
        List<ItemStack> toReturn = new ArrayList<>();

        if (session.station == StationType.WORKBENCH_SHAPED || session.station == StationType.WORKBENCH_SHAPELESS) {
            for (int s : GRID_3X3_SLOTS) {
                ItemStack st = inv.getItem(s);
                if (st != null && st.getType() != Material.AIR) {
                    toReturn.add(st);
                    inv.setItem(s, null);
                }
            }
        } else if (session.station == StationType.SMITHING) {
            int[] smithSlots = { 19, 20, 21 };
            for (int s : smithSlots) {
                ItemStack st = inv.getItem(s);
                if (st != null && st.getType() != Material.AIR) {
                    toReturn.add(st);
                    inv.setItem(s, null);
                }
            }
        } else {
            ItemStack in = inv.getItem(20);
            if (in != null && in.getType() != Material.AIR) {
                toReturn.add(in);
                inv.setItem(20, null);
            }
        }

        ItemStack res = inv.getItem(25);
        if (res != null && res.getType() != Material.AIR) {
            toReturn.add(res);
            inv.setItem(25, null);
        }

        giveOrDropItems(player, toReturn);
    }

    private void returnCraftItems(CraftView view) {
        Player player = Bukkit.getPlayer(view.playerId);
        if (player == null) return;

        List<ItemStack> toReturn = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slot = gridSlot(r, c);
                ItemStack st = view.inventory.getItem(slot);
                if (st != null && st.getType() != Material.AIR) {
                    toReturn.add(st);
                    view.inventory.setItem(slot, null);
                }
            }
        }
        view.inventory.setItem(craftResultSlot, null);
        giveOrDropItems(player, toReturn);
    }

    private void giveOrDropItems(Player player, List<ItemStack> items) {
        if (items.isEmpty()) return;
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(items.toArray(new ItemStack[0]));
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(colorize(config.getString("messages.inventory-full", "&cИнвентарь полон, предметы сброшены!")));
        }
    }

    // ===== ЛОГИКА ВИРТУАЛЬНОГО ВЕРСТАКА 3X3 (С КОЛИЧЕСТВАМИ) =====

    private void updateCraftResult(CraftView view) {
        ItemStack[][] grid = buildGridFromView(view);
        CraftRecipe match = null;

        for (CraftRecipe recipe : virtualRecipes) {
            if (recipe.matches(grid)) {
                match = recipe;
                break;
            }
        }

        view.currentRecipe = match;
        if (match != null) {
            view.inventory.setItem(craftResultSlot, match.createResult(plugin));
        } else {
            view.inventory.setItem(craftResultSlot, null);
        }
    }

    private void handleCraftResultClick(Player player, CraftView view, ClickType click) {
        if (view.currentRecipe == null) return;

        ItemStack result = view.inventory.getItem(craftResultSlot);
        if (result == null || result.getType() == Material.AIR) return;

        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!cursor.isSimilar(result) || (cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize())) {
                return;
            }
        }

        CraftRecipe recipe = view.currentRecipe;

        // Потребляем необходимое количество каждого ингредиента в сетке
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slot = gridSlot(r, c);
                ItemStack st = view.inventory.getItem(slot);
                if (st != null && st.getType() != Material.AIR) {
                    int required = 1;

                    if (recipe.isShaped && recipe.shapedMatrix != null) {
                        ItemStack exp = recipe.shapedMatrix[r][c];
                        if (exp != null && exp.getAmount() > 0) {
                            required = exp.getAmount();
                        }
                    } else if (!recipe.isShaped && recipe.rawIngredients != null) {
                        for (ItemStack exp : recipe.rawIngredients) {
                            if (recipe.matchesChoice(exp, st)) {
                                required = exp.getAmount();
                                break;
                            }
                        }
                    }

                    int remaining = st.getAmount() - required;
                    if (remaining > 0) {
                        st.setAmount(remaining);
                    } else {
                        view.inventory.setItem(slot, null);
                    }
                }
            }
        }

        if (cursor == null || cursor.getType() == Material.AIR) {
            player.setItemOnCursor(result.clone());
        } else {
            cursor.setAmount(cursor.getAmount() + result.getAmount());
            player.setItemOnCursor(cursor);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.3f);
        updateCraftResult(view);
    }

    private ItemStack[][] buildGridFromView(CraftView view) {
        ItemStack[][] grid = new ItemStack[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                grid[r][c] = view.inventory.getItem(gridSlot(r, c));
            }
        }
        return grid;
    }

    // ===== СЛОТЫ И РАЗМЕТКА =====

    private static final int[] GRID_3X3_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };

    private int getCatalogIndex(int rawSlot) {
        int[] catalogSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        for (int i = 0; i < catalogSlots.length; i++) {
            if (catalogSlots[i] == rawSlot) return i;
        }
        return -1;
    }

    private boolean isAllowedBuilderSlot(StationType station, int slot) {
        if (slot == 25) return true;

        if (station == StationType.WORKBENCH_SHAPED || station == StationType.WORKBENCH_SHAPELESS) {
            for (int s : GRID_3X3_SLOTS) {
                if (s == slot) return true;
            }
            return false;
        } else if (station == StationType.SMITHING) {
            return slot == 19 || slot == 20 || slot == 21;
        } else {
            return slot == 20;
        }
    }

    private boolean isGridSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 0 && row <= 2 && col >= 0 && col <= 2;
    }

    private int gridSlot(int row, int col) {
        return row * 9 + col;
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = createFiller();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private void fillBorder54(Inventory inv) {
        ItemStack filler = createFiller();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(fillerMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) list.add(colorize(l));
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void playErrorSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private String colorize(String text) {
        return ColorUtils.colorize(text);
    }

    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return map;
        }
        return null;
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        List<String> res = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) res.add(s);
        }
        return res;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ И ENUM =====

    public enum GuiType {
        MAIN_MENU,
        STATION_CHOOSER,
        BUILDER,
        VIRTUAL_WORKBENCH,
        RECIPE_CATALOG,
        RECIPE_DETAILS
    }

    public enum StationType {
        WORKBENCH_SHAPED("Форменный верстак (3x3)", Material.CRAFTING_TABLE),
        WORKBENCH_SHAPELESS("Бесформенный верстак", Material.FIREWORK_STAR),
        FURNACE("Обычная печь", Material.FURNACE),
        BLAST_FURNACE("Плавильня", Material.BLAST_FURNACE),
        SMOKER("Коптильня", Material.SMOKER),
        CAMPFIRE("Костёр", Material.CAMPFIRE),
        SMITHING("Кузнечный стол", Material.SMITHING_TABLE),
        STONECUTTER("Камнерез", Material.STONECUTTER);

        public final String displayName;
        public final Material icon;

        StationType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public static class CraftHolder implements InventoryHolder {
        public final UUID playerId;
        public final GuiType guiType;
        public final StationType stationType;
        public final int page;
        public final CraftRecipe targetRecipe;

        public CraftHolder(UUID playerId, GuiType guiType, StationType stationType, int page, CraftRecipe targetRecipe) {
            this.playerId = playerId;
            this.guiType = guiType;
            this.stationType = stationType;
            this.page = page;
            this.targetRecipe = targetRecipe;
        }

        @Override
        public Inventory getInventory() { return null; }
    }

    public static class BuilderSession {
        public final UUID playerId;
        public final StationType station;
        public final Inventory inventory;
        public int cookingTime = 200;
        public float experience = 0.7f;

        public BuilderSession(UUID playerId, StationType station, Inventory inventory) {
            this.playerId = playerId;
            this.station = station;
            this.inventory = inventory;
        }
    }

    public static class CraftView {
        public final UUID playerId;
        public final Inventory inventory;
        public CraftRecipe currentRecipe = null;

        public CraftView(UUID playerId, Inventory inventory) {
            this.playerId = playerId;
            this.inventory = inventory;
        }
    }

    // ===== КЛАСС РЕЦЕПТА (С КОЛИЧЕСТВАМИ И УДАЛЕНИЕМ) =====

    public static class CraftRecipe {
        public final String id;
        public final NamespacedKey namespacedKey;
        public final Main plugin;
        public final CustomItem customItem;
        public final AbstractCustomItem javaItem;
        public final int amount;
        public final boolean isShaped;
        public final ItemStack[][] shapedMatrix;
        public final Map<Character, RecipeChoice> shapedKeys;
        public final List<RecipeChoice> shapelessIngredients;
        public final List<ItemStack> rawIngredients;
        public final ItemStack customResult;
        public final StationType stationType;
        public final int cookingTime;
        public final float experience;

        public CraftRecipe(String id, NamespacedKey namespacedKey, Main plugin, CustomItem customItem, AbstractCustomItem javaItem,
                           int amount, boolean isShaped, ItemStack[][] shapedMatrix,
                           Map<Character, RecipeChoice> shapedKeys, List<RecipeChoice> shapelessIngredients,
                           List<ItemStack> rawIngredients, ItemStack customResult,
                           StationType stationType, int cookingTime, float experience) {
            this.id = id;
            this.namespacedKey = namespacedKey;
            this.plugin = plugin;
            this.customItem = customItem;
            this.javaItem = javaItem;
            this.amount = Math.max(1, amount);
            this.isShaped = isShaped;
            this.shapedMatrix = shapedMatrix;
            this.shapedKeys = shapedKeys;
            this.shapelessIngredients = shapelessIngredients;
            this.rawIngredients = rawIngredients;
            this.customResult = customResult;
            this.stationType = stationType != null ? stationType : (isShaped ? StationType.WORKBENCH_SHAPED : StationType.WORKBENCH_SHAPELESS);
            this.cookingTime = cookingTime;
            this.experience = experience;
        }

        public String getStationDisplayName() {
            return stationType != null ? stationType.displayName : "Верстак";
        }

        public ItemStack getResultPreview() {
            if (customResult != null) return customResult.clone();
            if (customItem != null) return customItem.getItemStack().clone();
            if (javaItem != null) return javaItem.createItemStack();
            return null;
        }

        public ItemStack createResult(Main plugin) {
            ItemStack res = getResultPreview();
            if (res != null) {
                res = res.clone();
                res.setAmount(this.amount);
            }
            return res;
        }

        public boolean matches(ItemStack[][] grid) {
            if (isShaped) {
                return matchesShaped(grid);
            } else {
                return matchesShapeless(grid);
            }
        }

        private boolean matchesShaped(ItemStack[][] grid) {
            if (shapedMatrix == null) return false;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack expected = shapedMatrix[r][c];
                    ItemStack actual = grid[r][c];

                    if (expected == null || expected.getType() == Material.AIR) {
                        if (actual != null && actual.getType() != Material.AIR) return false;
                    } else {
                        if (actual == null || actual.getType() == Material.AIR) return false;
                        if (actual.getAmount() < expected.getAmount()) return false;
                        if (!matchesChoice(expected, actual)) return false;
                    }
                }
            }
            return true;
        }

        private boolean matchesShapeless(ItemStack[][] grid) {
            if (rawIngredients == null || rawIngredients.isEmpty()) {
                if (shapelessIngredients == null) return false;
                List<ItemStack> actualItems = new ArrayList<>();
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        ItemStack st = grid[r][c];
                        if (st != null && st.getType() != Material.AIR) actualItems.add(st);
                    }
                }
                if (actualItems.size() != shapelessIngredients.size()) return false;
                boolean[] matched = new boolean[actualItems.size()];
                for (RecipeChoice choice : shapelessIngredients) {
                    boolean found = false;
                    for (int i = 0; i < actualItems.size(); i++) {
                        if (!matched[i] && choice.test(actualItems.get(i))) {
                            matched[i] = true;
                            found = true;
                            break;
                        }
                    }
                    if (!found) return false;
                }
                return true;
            }

            List<ItemStack> actualItems = new ArrayList<>();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    ItemStack st = grid[r][c];
                    if (st != null && st.getType() != Material.AIR) actualItems.add(st);
                }
            }

            if (actualItems.size() != rawIngredients.size()) return false;

            boolean[] matched = new boolean[actualItems.size()];
            for (ItemStack expected : rawIngredients) {
                boolean found = false;
                for (int i = 0; i < actualItems.size(); i++) {
                    if (!matched[i]) {
                        ItemStack actual = actualItems.get(i);
                        if (actual.getAmount() >= expected.getAmount() && matchesChoice(expected, actual)) {
                            matched[i] = true;
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) return false;
            }
            return true;
        }

        public boolean matchesChoice(ItemStack expected, ItemStack actual) {
            String expectedCustom = plugin.getItemHandler() != null ? plugin.getItemHandler().getCustomItemId(expected) : null;
            String actualCustom = plugin.getItemHandler() != null ? plugin.getItemHandler().getCustomItemId(actual) : null;

            if (expectedCustom != null) {
                return expectedCustom.equalsIgnoreCase(actualCustom);
            }
            if (expected.hasItemMeta()) {
                return expected.isSimilar(actual);
            }
            return expected.getType() == actual.getType();
        }

        public static CraftRecipe parseShaped(Main plugin, CustomItem item, Map<String, Object> map) {
            if (map == null) return null;
            try {
                Object patternObj = map.get("pattern");
                Object keysObj = map.get("keys");
                if (!(patternObj instanceof List) || !(keysObj instanceof Map)) return null;

                List<?> pattern = (List<?>) patternObj;
                Map<?, ?> keys = (Map<?, ?>) keysObj;
                ItemStack[][] matrix = new ItemStack[3][3];
                Map<Character, RecipeChoice> keyChoices = new HashMap<>();

                for (int r = 0; r < Math.min(3, pattern.size()); r++) {
                    String row = String.valueOf(pattern.get(r));
                    for (int c = 0; c < Math.min(3, row.length()); c++) {
                        char ch = row.charAt(c);
                        if (ch != ' ' && keys.containsKey(String.valueOf(ch))) {
                            String ingStr = String.valueOf(keys.get(String.valueOf(ch)));
                            ItemStack ing = resolveIngredientStack(plugin, ingStr);
                            if (ing != null) {
                                matrix[r][c] = ing;
                                keyChoices.put(ch, new RecipeChoice.ExactChoice(ing));
                            }
                        }
                    }
                }

                int amt = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
                return new CraftRecipe(null, null, plugin, item, null, amt, true, matrix, keyChoices, null, null, null, StationType.WORKBENCH_SHAPED, 0, 0);
            } catch (Exception ignored) {
                return null;
            }
        }

        public static CraftRecipe parseShapeless(Main plugin, CustomItem item, Map<String, Object> map) {
            if (map == null) return null;
            try {
                Object ingObj = map.get("ingredients");
                if (!(ingObj instanceof List)) return null;
                List<?> ings = (List<?>) ingObj;
                List<RecipeChoice> choices = new ArrayList<>();
                List<ItemStack> rawIngs = new ArrayList<>();

                for (Object o : ings) {
                    ItemStack st = resolveIngredientStack(plugin, String.valueOf(o));
                    if (st != null) {
                        choices.add(new RecipeChoice.ExactChoice(st));
                        rawIngs.add(st);
                    }
                }

                int amt = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
                return new CraftRecipe(null, null, plugin, item, null, amt, false, null, null, choices, rawIngs, null, StationType.WORKBENCH_SHAPELESS, 0, 0);
            } catch (Exception ignored) {
                return null;
            }
        }

        public static CraftRecipe parseJava(Main plugin, AbstractCustomItem javaItem, RecipeDef def) {
            if (def == null) return null;
            try {
                if (def.getType() == RecipeDef.Type.SHAPED) {
                    List<String> pattern = def.getPattern();
                    Map<Character, String> keys = def.getKeys();
                    ItemStack[][] matrix = new ItemStack[3][3];
                    Map<Character, RecipeChoice> keyChoices = new HashMap<>();

                    for (int r = 0; r < Math.min(3, pattern.size()); r++) {
                        String row = pattern.get(r);
                        for (int c = 0; c < Math.min(3, row.length()); c++) {
                            char ch = row.charAt(c);
                            if (ch != ' ' && keys.containsKey(ch)) {
                                String ingStr = keys.get(ch);
                                ItemStack ing = resolveIngredientStack(plugin, ingStr);
                                if (ing != null) {
                                    matrix[r][c] = ing;
                                    keyChoices.put(ch, new RecipeChoice.ExactChoice(ing));
                                }
                            }
                        }
                    }
                    return new CraftRecipe(null, null, plugin, null, javaItem, def.getAmount(), true, matrix, keyChoices, null, null, null, StationType.WORKBENCH_SHAPED, 0, 0);
                } else if (def.getType() == RecipeDef.Type.SHAPELESS) {
                    List<RecipeChoice> choices = new ArrayList<>();
                    List<ItemStack> rawIngs = new ArrayList<>();
                    for (String ingStr : def.getIngredients()) {
                        ItemStack ing = resolveIngredientStack(plugin, ingStr);
                        if (ing != null) {
                            choices.add(new RecipeChoice.ExactChoice(ing));
                            rawIngs.add(ing);
                        }
                    }
                    return new CraftRecipe(null, null, plugin, null, javaItem, def.getAmount(), false, null, null, choices, rawIngs, null, StationType.WORKBENCH_SHAPELESS, 0, 0);
                }
            } catch (Exception ignored) {}
            return null;
        }

        private static ItemStack resolveIngredientStack(Main plugin, String raw) {
            if (raw == null || raw.isEmpty()) return null;
            Material mat = EnumCache.getMaterial(raw);
            if (mat != null) return new ItemStack(mat);

            if (plugin.getItemHandler() != null) {
                CustomItem ci = plugin.getItemHandler().getCustomItem(raw);
                if (ci != null) return ci.getItemStack().clone();
            }
            if (plugin.getApiItemRegistry() != null) {
                AbstractCustomItem ai = plugin.getApiItemRegistry().getItem(raw);
                if (ai != null) return ai.createItemStack();
            }
            return null;
        }
    }
}
