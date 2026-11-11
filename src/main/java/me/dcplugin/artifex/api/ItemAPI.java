package me.dcplugin.artifex.api;

import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Utility class for working with custom items.
 *
 * Provides static methods for:
 * - Creating items
 * - Potion effects
 * - Particles and sounds
 * - Messages and titles
 * - Teleportation and damage
 * - Item checks
 */
public class ItemAPI {

    private static NamespacedKey customItemKey;
    private static NamespacedKey itemModelKey;

    private static final NamespacedKey[] LEGACY_CUSTOM_ITEM_KEYS = new NamespacedKey[] {
        new NamespacedKey("dcustomitems", "custom_item_id"),
        new NamespacedKey("dc-customitems", "custom_item_id")
    };

    /**
     * Plugin namespace used for the attribute modifier keys.
     *
     * It equals plugin.getName().toLowerCase() — for the current
     * plugin.yml that is "artifex".
     */
    private static String pluginNamespace = "artifex";

    /**
     * Initialises the keys (called when the plugin is loaded).
     */
    public static void init(org.bukkit.plugin.java.JavaPlugin plugin) {
        customItemKey = new NamespacedKey(plugin, "custom_item_id");
        itemModelKey = new NamespacedKey(plugin, "item_model");
        pluginNamespace = plugin.getName().toLowerCase(java.util.Locale.ROOT);
    }

    // ===== ITEM CREATION =====

    /**
     * Creates an ItemStack from an AbstractCustomItem.
     */
    public static ItemStack createItem(AbstractCustomItem item) {
        ItemStack stack = new ItemStack(item.getMaterial(), item.getAmount());
        ItemMeta meta = stack.getItemMeta();

        if (meta == null) return stack;

        // Display name
        meta.setDisplayName(ColorUtils.colorize(item.getDisplayName()));

        // Description
        List<String> lore = item.getLore();
        if (!lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtils.colorize(line));
            }
            meta.setLore(coloredLore);
        }

        // Unbreakable
        meta.setUnbreakable(item.isUnbreakable());

        // Enchantment glint
        if (item.isGlowing()) {
            // UNBREAKING gives no bonus. SHARPNESS would add +1 damage
            // and would break the item balance.
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Item Model (1.21.4+)
        String itemModel = item.getItemModel();
        if (itemModel != null && !itemModel.isEmpty()) {
            try {
                NamespacedKey modelKey = itemModel.contains(":")
                    ? NamespacedKey.fromString(itemModel)
                    : new NamespacedKey("minecraft", itemModel.toLowerCase(java.util.Locale.ROOT));
                if (modelKey != null) {
                    meta.setItemModel(modelKey);
                }
            } catch (Throwable ignored) {}
            org.bukkit.inventory.meta.components.CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
            cmd.setStrings(List.of(itemModel));
            meta.setCustomModelDataComponent(cmd);
            meta.getPersistentDataContainer().set(itemModelKey, PersistentDataType.STRING, itemModel);
        } else if (item.getCustomModelData() > 0) {
            meta.setCustomModelData(item.getCustomModelData());
        }

        // Custom ID
        meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, item.getId());

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Creates an item with attribute modifiers.
     */
    public static ItemStack createItem(AbstractCustomItem item, Map<Attribute, Double> attributes) {
        ItemStack stack = createItem(item);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<Attribute, Double> entry : attributes.entrySet()) {
                // The modifier key must be unique per item:
                // addAttributeModifier rejects duplicate keys, and a shared
                // key for all items would break the creation of the second item.
                // The old constructor taking a UUID is deprecated and slated for removal.
                meta.addAttributeModifier(entry.getKey(),
                    new AttributeModifier(modifierKey(item.getId(), entry.getKey()), entry.getValue(),
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Attribute modifier key inside the plugin namespace.
     * Characters outside [a-z0-9._-] are replaced with "_" — otherwise NamespacedKey
     * would throw an IllegalArgumentException.
     */
    private static org.bukkit.NamespacedKey modifierKey(String itemId, Attribute attribute) {
        org.bukkit.NamespacedKey attributeKey = attribute.getKey();
        StringBuilder sb = new StringBuilder();
        sb.append("custom_").append(itemId).append('_')
                .append(attributeKey != null ? attributeKey.getKey() : "attribute");
        StringBuilder safe = new StringBuilder(sb.length());
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.';
            safe.append(valid ? c : '_');
        }
        return new org.bukkit.NamespacedKey(pluginNamespace, safe.toString());
    }

    // ===== CHECKS =====

    /**
     * Checks whether an ItemStack is a custom item.
     */
    public static boolean isCustomItem(ItemStack item) {
        return getCustomItemId(item) != null;
    }

    /**
     * Gets the ID of a custom item.
     */
    public static String getCustomItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        var pdc = meta.getPersistentDataContainer();
        if (customItemKey != null) {
            String id = pdc.get(customItemKey, PersistentDataType.STRING);
            if (id != null) return id;
        }
        for (NamespacedKey legacyKey : LEGACY_CUSTOM_ITEM_KEYS) {
            String id = pdc.get(legacyKey, PersistentDataType.STRING);
            if (id != null) return id;
        }
        return null;
    }

    /**
     * Checks whether the player holds an item with the given ID.
     */
    public static boolean isHolding(Player player, String itemId) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (isCustomItem(main) && itemId.equals(getCustomItemId(main))) ||
               (isCustomItem(off) && itemId.equals(getCustomItemId(off)));
    }

    /**
     * Checks whether an item is equipped (in hand or as armour).
     */
    public static boolean isEquipped(Player player, String itemId) {
        // getContents() already includes armour — no separate pass is needed.
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCustomItem(item) && itemId.equals(getCustomItemId(item))) return true;
        }
        return false;
    }

    // ===== EFFECTS =====

    /**
     * Adds a potion effect to the player.
     *
     * @param player the player
     * @param type the effect type
     * @param duration duration in seconds
     * @param amplifier the level (1 = normal, 2 = amplified, etc.)
     */
    public static void effect(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration * 20, amplifier - 1, false, false));
    }

    /**
     * Adds the speed effect.
     */
    public static void speed(Player player, int duration, int level) {
        effect(player, PotionEffectType.SPEED, duration, level);
    }

    /**
     * Adds the strength effect.
     */
    public static void strength(Player player, int duration, int level) {
        effect(player, PotionEffectType.STRENGTH, duration, level);
    }

    /**
     * Makes the player invisible.
     */
    public static void vanish(Player player, int seconds) {
        effect(player, PotionEffectType.INVISIBILITY, seconds, 1);
    }

    /**
     * Makes the player glowing.
     */
    public static void glow(Player player, int seconds) {
        effect(player, PotionEffectType.GLOWING, seconds, 1);
    }

    /**
     * Heals the player.
     *
     * @param amount amount of hearts (1 heart = 2 HP)
     */
    public static void heal(Player player, double amount) {
        if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
            double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(Math.min(player.getHealth() + amount * 2, max));
        }
    }

    /**
     * Damages the player.
     */
    public static void damage(Player player, double amount) {
        player.damage(amount);
    }

    /**
     * Deals AoE damage in an area.
     */
    public static void damageNearby(Player player, double amount, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                ((Player) entity).damage(amount, player);
            }
        }
    }

    /**
     * Heals the nearest players.
     */
    public static void healNearby(Player player, double amount, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (target.getAttribute(Attribute.MAX_HEALTH) != null) {
                    double max = target.getAttribute(Attribute.MAX_HEALTH).getValue();
                    target.setHealth(Math.min(target.getHealth() + amount * 2, max));
                }
            }
        }
    }

    // ===== PARTICLES AND SOUNDS =====

    /**
     * Spawns particles at the player.
     */
    public static void particles(Player player, Particle particle, int count) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count);
    }

    /**
     * Spawns particles at the given location.
     */
    public static void particlesAt(Location loc, Particle particle, int count) {
        loc.getWorld().spawnParticle(particle, loc, count);
    }

    /**
     * Spawns particles with a custom offset.
     */
    public static void particlesCustom(Player player, Particle particle, int count,
                                        double offsetX, double offsetY, double offsetZ) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), count,
            offsetX, offsetY, offsetZ);
    }

    /**
     * Plays a sound for the player.
     */
    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Plays a sound at a location for everyone nearby.
     */
    public static void soundAt(Location loc, Sound sound, float volume, float pitch) {
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    // ===== MESSAGES =====

    /**
     * Sends a message to the player (with colour codes + PlaceholderAPI).
     */
    public static void message(Player player, String msg) {
        if (msg != null && !msg.trim().isEmpty()) {
            player.sendMessage(ColorUtils.processMessage(player, msg));
        }
    }

    /**
     * Sends a title (large centred text) with PlaceholderAPI.
     */
    public static void title(Player player, String title, String subtitle) {
        String[] processed = ColorUtils.processTitle(player, title, subtitle);
        player.sendTitle(processed[0], processed[1], 10, 40, 10);
    }

    /**
     * Sends an action bar message (above the hotbar) with PlaceholderAPI.
     */
    public static void actionbar(Player player, String text) {
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ColorUtils.processMessage(player, text))
        );
    }

    /**
     * Broadcasts a message to all players (with PlaceholderAPI for each player).
     */
    public static void broadcast(String msg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ColorUtils.processMessage(player, msg));
        }
    }

    // ===== TELEPORTATION =====

    /**
     * Teleports the player to the given coordinates.
     */
    public static void teleport(Player player, double x, double y, double z) {
        player.teleport(new Location(player.getWorld(), x, y, z));
    }

    /**
     * Teleports the player relative to the current position.
     */
    public static void teleportRelative(Player player, double dx, double dy, double dz) {
        Location loc = player.getLocation();
        player.teleport(new Location(loc.getWorld(), loc.getX() + dx, loc.getY() + dy, loc.getZ() + dz));
    }

    /**
     * Teleports the player to the nearest player.
     */
    public static void teleportToNearest(Player player, double maxRange) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other == player) continue;
            // The distance between different worlds is undefined — Location.distance()
            // throws an IllegalArgumentException. Only compare players in the same world.
            if (!other.getWorld().equals(player.getWorld())) continue;
            double dist = player.getLocation().distanceSquared(other.getLocation());
            if (dist < nearestDist && dist <= maxRange * maxRange) {
                nearestDist = dist;
                nearest = other;
            }
        }

        if (nearest != null) {
            player.teleport(nearest.getLocation());
        }
    }

    // ===== COMBAT =====

    /**
     * Strikes lightning where the player is looking.
     */
    public static void lightningForward(Player player, int distance) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Location target = eyeLoc.clone().add(direction.multiply(distance));
        player.getWorld().strikeLightning(target);
    }

    /**
     * Strikes lightning at the given location.
     */
    public static void lightning(Location loc) {
        loc.getWorld().strikeLightning(loc);
    }

    /**
     * Knocks back the nearest players.
     */
    public static void knockbackNearby(Player player, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                Vector dir = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector());
                // A zero vector after normalize() produces NaN — the velocity becomes
                // invalid and the player gets "stuck". On zero — simply launch upwards.
                if (dir.lengthSquared() < 1.0E-6) {
                    entity.setVelocity(entity.getVelocity().add(new Vector(0, 0.5, 0)));
                } else {
                    dir.normalize();
                    entity.setVelocity(dir.multiply(1.5).setY(0.5));
                }
            }
        }
    }

    /**
     * Launches the nearest players upwards.
     */
    public static void launchNearby(Player player, double power, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                entity.setVelocity(entity.getVelocity().add(new Vector(0, power, 0)));
            }
        }
    }

    // ===== ITEMS =====

    /**
     * Gives an item to the player.
     *
     * If the inventory is full, the remainder drops on the ground instead of vanishing.
     */
    public static void giveItem(Player player, Material material, int amount) {
        if (player == null || material == null || amount <= 0) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(material, amount));
        for (ItemStack stack : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
    }

    /**
     * Removes an item from the player.
     *
     * {@code Inventory#getContents()} returns copies of the stacks, so the
     * changes have to be written back with {@code setContents}.
     */
    public static boolean removeItem(Player player, Material material, int amount) {
        if (player == null || material == null || amount <= 0) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material && remaining > 0) {
                int toRemove = Math.min(item.getAmount(), remaining);
                int left = item.getAmount() - toRemove;
                remaining -= toRemove;
                contents[i] = left > 0 ? item : null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
        }
        return remaining == 0;
    }

    /**
     * Grants experience points.
     */
    public static void giveExp(Player player, int amount) {
        player.giveExp(amount);
    }

    /**
     * Grants experience levels.
     */
    public static void giveLevels(Player player, int amount) {
        player.giveExpLevels(amount);
    }

    // ===== FIRE =====

    /**
     * Sets the player on fire.
     */
    public static void setOnFire(Player player, int ticks) {
        player.setFireTicks(ticks);
    }

    /**
     * Sets the nearest players on fire.
     */
    public static void setNearbyOnFire(Player player, int ticks, double radius) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                entity.setFireTicks(ticks);
            }
        }
    }
}
