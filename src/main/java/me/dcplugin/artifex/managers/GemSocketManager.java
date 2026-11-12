package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.events.CustomItemSocketEvent;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles gem socketing, rune inlaying, and socket visualization for custom items.
 */
public class GemSocketManager implements Listener {

    private final Main plugin;
    private final NamespacedKey keyMaxSockets;
    private final NamespacedKey keyUsedSockets;
    private final NamespacedKey keyIsGem;
    private final NamespacedKey keyGemName;
    private final NamespacedKey keyGemBonus;

    public GemSocketManager(Main plugin) {
        this.plugin = plugin;
        this.keyMaxSockets = new NamespacedKey(plugin, "max_sockets");
        this.keyUsedSockets = new NamespacedKey(plugin, "used_sockets");
        this.keyIsGem = new NamespacedKey(plugin, "is_gem");
        this.keyGemName = new NamespacedKey(plugin, "gem_name");
        this.keyGemBonus = new NamespacedKey(plugin, "gem_bonus");
    }

    public int getMaxSockets(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer val = pdc.get(keyMaxSockets, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public int getUsedSockets(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer val = pdc.get(keyUsedSockets, PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public boolean isSocketable(ItemStack item) {
        return getMaxSockets(item) > 0;
    }

    public boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Byte val = pdc.get(keyIsGem, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public String getGemName(ItemStack item) {
        if (!isGem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyGemName, PersistentDataType.STRING);
    }

    public String getGemBonus(ItemStack item) {
        if (!isGem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(keyGemBonus, PersistentDataType.STRING);
    }

    public boolean canInsertGem(ItemStack target, ItemStack gem) {
        if (!isSocketable(target) || !isGem(gem)) return false;
        return getUsedSockets(target) < getMaxSockets(target);
    }

    /**
     * Initializes an item with a given number of open sockets.
     */
    public ItemStack setSockets(ItemStack item, int maxSockets) {
        if (item == null) return null;
        ItemStack stack = item.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.getPersistentDataContainer().set(keyMaxSockets, PersistentDataType.INTEGER, maxSockets);
        meta.getPersistentDataContainer().set(keyUsedSockets, PersistentDataType.INTEGER, 0);

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(renderSocketsLine(maxSockets, 0));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Converts an item into a socketable Gem.
     */
    public ItemStack makeGem(ItemStack item, String gemName, String bonusDescription) {
        if (item == null) return null;
        ItemStack stack = item.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.getPersistentDataContainer().set(keyIsGem, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(keyGemName, PersistentDataType.STRING, gemName);
        meta.getPersistentDataContainer().set(keyGemBonus, PersistentDataType.STRING, bonusDescription);

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(ColorUtils.colorize("&6✦ Самоцвет для инкрустации"));
        lore.add(ColorUtils.colorize("&7Бонус: &a" + bonusDescription));
        lore.add(ColorUtils.colorize("&8Перетащите на предмет с сокетом"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Sockets a gem into a target item.
     */
    public ItemStack insertGem(Player player, ItemStack target, ItemStack gem) {
        if (!canInsertGem(target, gem)) return null;

        String gemName = getGemName(gem);
        if (gemName == null || gemName.isEmpty()) {
            gemName = gem.hasItemMeta() && gem.getItemMeta().hasDisplayName()
                    ? gem.getItemMeta().getDisplayName() : "Самоцвет";
        }
        String gemBonus = getGemBonus(gem);
        if (gemBonus == null || gemBonus.isEmpty()) {
            gemBonus = "Усиление характеристик";
        }

        CustomItemSocketEvent event = new CustomItemSocketEvent(player, target, gem, gemName, gemBonus);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;

        ItemStack stack = target.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;

        int max = getMaxSockets(stack);
        int used = getUsedSockets(stack) + 1;
        meta.getPersistentDataContainer().set(keyUsedSockets, PersistentDataType.INTEGER, used);

        NamespacedKey entryKey = new NamespacedKey(plugin, "socket_gem_" + used);
        meta.getPersistentDataContainer().set(entryKey, PersistentDataType.STRING, gemName + " (" + gemBonus + ")");

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Update sockets bar line
        boolean replaced = false;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Сокеты:") || lore.get(i).contains("Sockets:")) {
                lore.set(i, renderSocketsLine(max, used));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lore.add(renderSocketsLine(max, used));
        }

        lore.add(ColorUtils.colorize("&8  • &f" + gemName + " &7[" + gemBonus + "]"));
        meta.setLore(lore);
        stack.setItemMeta(meta);

        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 25, 0.4, 0.4, 0.4, 0.1);
            player.sendMessage(ColorUtils.colorize("&a✔ &f" + gemName + " &aуспешно инкрустирован в предмет!"));
        }

        return stack;
    }

    private String renderSocketsLine(int max, int used) {
        StringBuilder sb = new StringBuilder("&7Сокеты: &8[ ");
        for (int i = 0; i < max; i++) {
            if (i < used) {
                sb.append("&a● ");
            } else {
                sb.append("&7○ ");
            }
        }
        sb.append("&8]");
        return ColorUtils.colorize(sb.toString());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType().isAir()) return;
        if (current == null || current.getType().isAir()) return;

        if (isGem(cursor) && isSocketable(current)) {
            if (canInsertGem(current, cursor)) {
                event.setCancelled(true);
                ItemStack updated = insertGem(player, current, cursor);
                if (updated != null) {
                    event.setCurrentItem(updated);
                    cursor.setAmount(cursor.getAmount() - 1);
                    if (cursor.getAmount() <= 0) {
                        player.setItemOnCursor(null);
                    } else {
                        player.setItemOnCursor(cursor);
                    }
                }
            } else if (getUsedSockets(current) >= getMaxSockets(current)) {
                player.sendMessage(ColorUtils.colorize("&cВ этом предмете больше нет свободных сокетов!"));
            }
        }
    }
}
