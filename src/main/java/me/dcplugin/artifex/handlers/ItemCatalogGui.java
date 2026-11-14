package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Interactive chest GUI browser / catalog of all custom items in Artifex.
 */
public class ItemCatalogGui implements InventoryHolder, Listener {

    private static final int PAGE_SIZE = 36;
    private static final String GUI_TITLE_PREFIX = "§8Artifex §7Каталог";

    private final Main plugin;
    private final String category;
    private final int page;
    private final Inventory inventory;

    public ItemCatalogGui(Main plugin, String category, int page) {
        this.plugin = plugin;
        this.category = category != null ? category.toUpperCase(Locale.ROOT) : "ALL";
        this.page = Math.max(0, page);
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize(GUI_TITLE_PREFIX + " &8[" + this.category + "]"));
        buildGui();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(Player player, String category, int page) {
        Main plugin = Main.getInstance();
        if (plugin == null) return;
        ItemCatalogGui gui = new ItemCatalogGui(plugin, category, page);
        player.openInventory(gui.getInventory());
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private void buildGui() {
        // Top navigation bar
        inventory.setItem(0, createButton(Material.NETHERITE_SWORD, "&e✦ Все предметы", "&7Показать все зарегистрированные предметы", category.equals("ALL")));
        inventory.setItem(1, createButton(Material.DIAMOND_SWORD, "&c⚔ Оружие и инструменты", "&7Категория: TOOL", category.equals("TOOL")));
        inventory.setItem(2, createButton(Material.NETHERITE_CHESTPLATE, "&b🛡 Броня и экипировка", "&7Категория: ARMOR", category.equals("ARMOR")));
        inventory.setItem(3, createButton(Material.GOLDEN_APPLE, "&a🍏 Зелья и расходуемые", "&7Категория: CONSUMABLE", category.equals("CONSUMABLE")));
        inventory.setItem(4, createButton(Material.EMERALD, "&d✧ Руны и самоцветы", "&7Категория: RUNE", category.equals("RUNE")));
        inventory.setItem(5, createButton(Material.RAW_GOLD, "&6📦 Ресурсы и материалы", "&7Категория: MATERIAL", category.equals("MATERIAL")));
        inventory.setItem(6, createButton(Material.NOTE_BLOCK, "&d🧱 Кастомные блоки", "&7Категория: BLOCK", category.equals("BLOCK")));

        // Divider panes
        ItemStack divider = createDivider();
        inventory.setItem(7, divider);
        inventory.setItem(8, divider);
        inventory.setItem(6, divider);
        inventory.setItem(8, divider);

        // Filter items
        List<CustomItem> filtered = new ArrayList<>();
        if (plugin.getItemHandler() != null) {
            for (CustomItem item : plugin.getItemHandler().getAllCustomItems().values()) {
                if (category.equals("ALL") || item.getType().equalsIgnoreCase(category)) {
                    filtered.add(item);
                }
            }
        }
        filtered.sort(Comparator.comparing(CustomItem::getId));

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages - 1);

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());

        for (int i = start; i < end; i++) {
            CustomItem ci = filtered.get(i);
            ItemStack display = ci.getItemStack().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(" ");
                lore.add(ColorUtils.colorize("&8━━━━━━━━━━━━━━━━━━━━━━"));
                lore.add(ColorUtils.colorize("&7ID: &f" + ci.getId() + " &8| &7Тип: &a" + ci.getType()));
                lore.add(ColorUtils.colorize("&eЛКМ &8- &7Выдать 1 шт."));
                lore.add(ColorUtils.colorize("&eПКМ &8- &7Выдать 64 шт."));
                lore.add(ColorUtils.colorize("&eShift+Клик &8- &7Бросить под ноги"));
                lore.add(ColorUtils.colorize("&8━━━━━━━━━━━━━━━━━━━━━━"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            int slot = 9 + (i - start);
            inventory.setItem(slot, display);
        }

        // Bottom row
        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, divider);
        }

        if (currentPage > 0) {
            inventory.setItem(45, createButton(Material.ARROW, "&6⬅ Предыдущая страница", "&7Перейти на страницу " + currentPage, false));
        }

        inventory.setItem(49, createButton(Material.BOOK, "&eСтраница " + (currentPage + 1) + " / " + totalPages, "&7Всего предметов: &a" + filtered.size(), false));

        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createButton(Material.ARROW, "&6Следующая страница ➡", "&7Перейти на страницу " + (currentPage + 2), false));
        }
    }

    private ItemStack createButton(Material material, String name, String loreLine, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name + (active ? " &a✔" : "")));
            meta.setLore(List.of(ColorUtils.colorize(loreLine)));
            if (active) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDivider() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ItemCatalogGui gui)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= 54) return;

        // Category switches
        if (rawSlot == 0) { open(player, "ALL", 0); return; }
        if (rawSlot == 1) { open(player, "TOOL", 0); return; }
        if (rawSlot == 2) { open(player, "ARMOR", 0); return; }
        if (rawSlot == 3) { open(player, "CONSUMABLE", 0); return; }
        if (rawSlot == 4) { open(player, "RUNE", 0); return; }
        if (rawSlot == 5) { open(player, "MATERIAL", 0); return; }
        if (rawSlot == 6) { open(player, "BLOCK", 0); return; }

        // Pagination
        if (rawSlot == 45 && gui.page > 0) {
            open(player, gui.category, gui.page - 1);
            return;
        }
        if (rawSlot == 53) {
            open(player, gui.category, gui.page + 1);
            return;
        }

        // Custom item click
        if (rawSlot >= 9 && rawSlot <= 44) {
            ItemStack clicked = gui.inventory.getItem(rawSlot);
            if (clicked == null || clicked.getType().isAir()) return;

            String customId = gui.plugin.getItemHandler().getCustomItemId(clicked);
            if (customId == null) return;

            CustomItem customItem = gui.plugin.getItemHandler().getCustomItem(customId);
            if (customItem == null) return;

            ItemStack toGive = customItem.getItemStack().clone();
            if (event.isShiftClick()) {
                player.getWorld().dropItemNaturally(player.getLocation(), toGive);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.0f);
                player.sendMessage(ColorUtils.colorize("&aБрошен предмет: &f" + (customItem.hasDisplayName() ? customItem.getDisplayName() : customId)));
            } else if (event.isRightClick()) {
                toGive.setAmount(Math.min(64, toGive.getMaxStackSize() > 1 ? 64 : 1));
                player.getInventory().addItem(toGive);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
                player.sendMessage(ColorUtils.colorize("&aВыдан стек предметов: &f" + (customItem.hasDisplayName() ? customItem.getDisplayName() : customId)));
            } else {
                toGive.setAmount(1);
                player.getInventory().addItem(toGive);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f);
                player.sendMessage(ColorUtils.colorize("&aВыдан предмет: &f" + (customItem.hasDisplayName() ? customItem.getDisplayName() : customId)));
            }
        }
    }
}
