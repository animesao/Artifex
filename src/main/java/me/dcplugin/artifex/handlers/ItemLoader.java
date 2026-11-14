package me.dcplugin.artifex.handlers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import me.dcplugin.artifex.utils.EnumCache;
import me.dcplugin.artifex.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

/**
 * Loading of custom items from config.yml and the items/ folder.
 * Extracted from CustomItemHandler to separate responsibilities.
 */
public class ItemLoader {

    private final Main plugin;
    private final NamespacedKey customItemKey;
    private final NamespacedKey itemModelKey;

    // Valid values
    private static final List<String> VALID_TYPES = List.of("RUNE", "TOOL", "ARMOR", "CONSUMABLE", "MATERIAL", "BLOCK");
    private static final List<String> VALID_SLOTS = List.of("HAND", "OFFHAND", "HEAD", "CHEST", "LEGS", "FEET");

    public ItemLoader(Main plugin, NamespacedKey customItemKey, NamespacedKey itemModelKey) {
        this.plugin = plugin;
        this.customItemKey = customItemKey;
        this.itemModelKey = itemModelKey;
    }

    /**
     * Load all items from config.yml and the items/ folder
     */
    public Map<String, CustomItem> loadAll() {
        Map<String, CustomItem> items = new HashMap<>();

        // 1. config.yml
        loadFromConfig(items);

        // 2. items/ folder
        loadFromFolder(items);

        return items;
    }

    // ===== Loading from config.yml =====

    // Known non-item keys in config.yml
    private static final Set<String> CONFIG_SKIP_KEYS = Set.of(
            "debug-mode", "database", "set-bonuses",
            "update-available", "latest-version",
            "disable-worlds", "whitelist-blocks",
            "java-compilation", "backup", "extract-default-modules",
            "file-logging", "crafting", "items", "cooldowns", "security",
            "custom-blocks"
    );

    private void loadFromConfig(Map<String, CustomItem> items) {
        ConfigurationSection config = plugin.getConfig();
        for (String itemId : config.getKeys(false)) {
            try {
                if (CONFIG_SKIP_KEYS.contains(itemId)) continue;
                CustomItem item = parseCustomItem(itemId, config.getConfigurationSection(itemId));
                if (item != null) {
                    items.put(itemId, item);
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Error while loading item " + itemId + ": " + e.getMessage(), e);
            }
        }
    }

    // ===== Loading from items/ folder =====

    private void loadFromFolder(Map<String, CustomItem> items) {
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            plugin.getLogger().info("Created the items/ folder for custom items");
            return;
        }

        File[] files = itemsFolder.listFiles((dir, name) ->
                (name.endsWith(".yml") || name.endsWith(".yaml")) && !name.startsWith("EXAMPLE-"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("No .yml items in items/ yet. "
                    + "Copy an example from the repository (samples/) into items/ and run /ci reload.");
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
                for (String itemId : fileConfig.getKeys(false)) {
                    try {
                        if ("set-bonuses".equals(itemId)) continue;
                        CustomItem item = parseCustomItem(itemId, fileConfig.getConfigurationSection(itemId));
                        if (item != null) {
                            items.put(itemId, item);
                            plugin.getLogger().fine("[items] Loaded item '" + itemId + "' from " + file.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Error while loading item " + itemId + " from " + file.getName() + ": " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Error while reading file " + file.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    // ===== Parsing a single item =====

    /**
     * Parsing of a single YAML section into a CustomItem object
     */
    public CustomItem parseCustomItem(String itemId, ConfigurationSection section) {
        if (section == null || "set-bonuses".equals(itemId)) return null;

        try {
            // === Basic params ===
            String type = validateType(section.getString("type", "RUNE"), itemId);
            String activationSlot = validateSlot(section.getString("activation-slot", "HAND"), itemId);
            boolean placeable = section.getBoolean("placeable", false);
            List<String> effects = section.getStringList("effects");
            if (effects == null) effects = new ArrayList<>();

            // === Item section ===
            ConfigurationSection itemSection = section.getConfigurationSection("item");
            if (itemSection == null) {
                plugin.getLogger().warning("Section 'item' not found for item: " + itemId);
                return null;
            }

            // === Build ItemStack ===
            ItemStack itemStack = buildItemStack(itemSection, itemId);
            if (itemStack == null) return null;

            // Mark as custom item
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, itemId);
                itemStack.setItemMeta(meta);
            }

            // === Lore processing ===
            int maxUses = section.getInt("max-uses", -1);
            List<String> lorePre = section.getStringList("lore");
            List<String> processedLore = new ArrayList<>();
            List<String> loreTemplate = new ArrayList<>();

            if (!lorePre.isEmpty()) {
                for (String line : lorePre) {
                    String processed = line.replace("%cooldown%", "0.0");
                    if (maxUses > 0) {
                        processed = processed.replace("%uses%", String.valueOf(maxUses));
                    } else {
                        processed = processed.replace("%uses%", "∞");
                    }
                    processedLore.add(ColorUtils.colorize(processed));

                    String templateLine = line;
                    if (maxUses > 0) {
                        templateLine = templateLine.replace("%uses%", String.valueOf(maxUses));
                    } else if (line.contains("%uses%")) {
                        templateLine = templateLine.replace("%uses%", "∞");
                    }
                    loreTemplate.add(templateLine);
                }
                meta = itemStack.getItemMeta();
                if (meta != null) {
                    meta.setLore(processedLore);
                    itemStack.setItemMeta(meta);
                }
            }

            // === Attributes ===
            // Supported both at the top level of the item and inside the
            // item: section — the top level wins.
            Map<String, Double> attributes = loadAttributes(section, itemSection);

            // === Armor set ===
            // Supported both at the top level of the item and inside the
            // item: section — the top level wins.
            String armorSetId = section.getString("armor-set",
                    itemSection.getString("armor-set", null));
            boolean hasSetBonus = section.getBoolean("has-set-bonus",
                    itemSection.getBoolean("has-set-bonus", false));
            if (armorSetId != null) {
                plugin.getLogger().fine("[LOAD] Item '" + itemId + "': armor-set=" + armorSetId);
            }

            // === Actions ===
            List<String> leftClickActions = section.getStringList("left-click-actions");
            List<String> rightClickActions = section.getStringList("right-click-actions");
            long clickCooldown = section.getLong("click-cooldown", 0);

            // === Model data ===
            int customModelData = itemSection.getInt("custom-model-data", -1);
            String itemModel = itemSection.getString("item-model", itemSection.getString("model", null));
            String itemModelVariant = itemSection.getString("item-model-variant", null);
            applyModelData(itemStack, customModelData, itemModel, itemId, itemModelVariant);

            // === Trigger actions (old + new format) ===
            List<String> triggerActions = loadTriggerActions(section);

            // === Messages ===
            String equipMessage = section.getString("equip-message", null);
            String unequipMessage = section.getString("unequip-message", null);
            String cooldownMessage = section.getString("cooldown-message", null);
            String activationMessage = section.getString("activation-message", null);
            String deactivationMessage = section.getString("deactivation-message", null);
            String usesDepletedMessage = section.getString("uses-depleted-message", null);

            // === Permission ===
            String permission = section.getString("permission", null);

            // === Particles & Sounds ===
            List<String> equipParticles = section.getStringList("equip-particles");
            List<String> equipSounds = section.getStringList("equip-sounds");
            List<String> unequipParticles = section.getStringList("unequip-particles");
            List<String> unequipSounds = section.getStringList("unequip-sounds");

            // === World restrictions ===
            List<String> allowedWorlds = section.getStringList("allowed-worlds");
            List<String> disabledWorlds = section.getStringList("disabled-worlds");

            // === Duration ===
            long duration = section.getLong("duration", -1);
            String maxDurationMessage = section.getString("max-duration-message", null);

            // === Trail particles ===
            List<String> trailParticles = section.getStringList("trail-particles");
            int trailParticleInterval = section.getInt("trail-particle-interval", 5);

            // === Recipes (shaped / shapeless / furnace) ===
            Map<String, Object> recipes = null;
            ConfigurationSection recipesSection = section.getConfigurationSection("recipes");
            if (recipesSection != null) {
                recipes = toPlainMap(recipesSection);
            }

            // === Mob & Block Drops ===
            Map<org.bukkit.entity.EntityType, Double> mobDrops = new HashMap<>();
            Map<Material, Double> blockDrops = new HashMap<>();
            ConfigurationSection dropsSec = section.getConfigurationSection("drops");
            if (dropsSec != null) {
                ConfigurationSection mobsSec = dropsSec.getConfigurationSection("mobs");
                if (mobsSec != null) {
                    for (String mobName : mobsSec.getKeys(false)) {
                        try {
                            org.bukkit.entity.EntityType et = org.bukkit.entity.EntityType.valueOf(mobName.toUpperCase(Locale.ROOT));
                            mobDrops.put(et, mobsSec.getDouble(mobName, 0.0));
                        } catch (Exception ignored) {}
                    }
                }
                ConfigurationSection blocksSec = dropsSec.getConfigurationSection("blocks");
                if (blocksSec != null) {
                    for (String blkName : blocksSec.getKeys(false)) {
                        Material mat = EnumCache.getMaterial(blkName);
                        if (mat != null) {
                            blockDrops.put(mat, blocksSec.getDouble(blkName, 0.0));
                        }
                    }
                }
            }

            // === Build CustomItem ===
            if (section.contains("buy-price") || section.contains("sell-price")) {
                plugin.getLogger().warning("[LOAD] '" + itemId + "': buy-price/sell-price were removed in 1.328 - the key is ignored.");
            }
            CustomItem customItem = CustomItem.builder(itemId, itemStack)
                    .type(type)
                    .activationSlot(activationSlot)
                    .placeable(placeable)
                    .effects(effects)
                    .attributes(attributes)
                    .armorSet(armorSetId, hasSetBonus)
                    .leftClickActions(leftClickActions)
                    .rightClickActions(rightClickActions)
                    .clickCooldown(clickCooldown)
                    .maxUses(maxUses)
                    .loreTemplate(loreTemplate)
                    .customModelData(customModelData)
                    .permission(permission)
                    .equipParticles(equipParticles)
                    .equipSounds(equipSounds)
                    .unequipParticles(unequipParticles)
                    .unequipSounds(unequipSounds)
                    .triggerActions(triggerActions)
                    .equipMessage(equipMessage)
                    .unequipMessage(unequipMessage)
                    .cooldownMessage(cooldownMessage)
                    .activationMessage(activationMessage)
                    .deactivationMessage(deactivationMessage)
                    .usesDepletedMessage(usesDepletedMessage)
                    .duration(duration)
                    .maxDurationMessage(maxDurationMessage)
                    .trailParticles(trailParticles)
                    .trailParticleInterval(trailParticleInterval)
                    .allowedWorlds(allowedWorlds)
                    .disabledWorlds(disabledWorlds)
                    .itemModelVariant(itemModelVariant)
                    .recipes(recipes)
                    .mobDrops(mobDrops)
                    .blockDrops(blockDrops)
                    .build();

            // Parse effects
            if (!effects.isEmpty()) {
                customItem.setParsedEffects(plugin.getEffectManager().parseEffects(effects));
            }

            return customItem;

        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Error while creating item " + itemId + ": " + e.getMessage(), e);
            return null;
        }
    }

    // ===== Helpers =====

    private ItemStack buildItemStack(ConfigurationSection itemSection, String itemId) {
        String materialName = itemSection.getString("type", "STONE");
        Material material = EnumCache.getMaterial(materialName);
        if (material == null || EnumCache.isAir(material)) {
            plugin.getLogger().warning((material == null ? "Unknown" : "Invalid (AIR)") + " material: " + materialName + " for item: " + itemId);
            return null;
        }

        ItemBuilder builder = new ItemBuilder(material, plugin);

        String title = itemSection.getString("title");
        if (title != null) builder.setDisplayName(title);

        List<String> lore = itemSection.contains("lore") ? itemSection.getStringList("lore") : new ArrayList<>();
        if (!lore.isEmpty()) builder.setLore(lore);

        int amount = itemSection.getInt("amount", 1);
        builder.setAmount(amount);

        if (itemSection.getBoolean("glowing", false)) builder.setGlowing(true);
        if (itemSection.getBoolean("unbreakable", false)) builder.setUnbreakable(true);

        // Enchantments
        ConfigurationSection enchSection = itemSection.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                try {
                    Enchantment ench = EnumCache.getEnchantment(enchName);
                    if (ench != null) builder.addEnchantment(ench, enchSection.getInt(enchName));
                } catch (Exception e) {
                    plugin.getLogger().warning("Unknown enchantment: " + enchName);
                    // Enchantment not found — already logged by EnumCache
                }
            }
        }

        // Item flags
        for (String flagName : itemSection.getStringList("item-flags")) {
            ItemFlag flag = EnumCache.getItemFlag(flagName);
            if (flag != null) {
                builder.addItemFlags(flag);
            } else {
                plugin.getLogger().warning("Unknown item flag: " + flagName);
            }
        }

        // Skull texture
        String texture = itemSection.getString("texture");
        if (texture != null && material == Material.PLAYER_HEAD) {
            builder.setSkullTexture(texture);
        }

        // Armor trim (1.20+)
        if (itemSection.isConfigurationSection("trim")) {
            ConfigurationSection trimSec = itemSection.getConfigurationSection("trim");
            String pattern = trimSec != null ? trimSec.getString("pattern") : null;
            String trimMat = trimSec != null ? trimSec.getString("material") : null;
            if (pattern != null && trimMat != null) {
                builder.setArmorTrim(pattern, trimMat);
            }
        }

        // Glint override (1.20.5+)
        if (itemSection.contains("glint")) {
            builder.setGlintOverride(itemSection.getBoolean("glint"));
        }

        // Color (Leather armor / Potion)
        String colorStr = itemSection.getString("color");
        if (colorStr != null && !colorStr.isBlank()) {
            builder.setColor(colorStr);
        }

        return builder.build();
    }

    private void applyModelData(ItemStack itemStack, int customModelData, String itemModel, String itemId) {
        applyModelData(itemStack, customModelData, itemModel, itemId, null);
    }

    private void applyModelData(ItemStack itemStack, int customModelData, String itemModel, String itemId, String itemModelVariant) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        boolean hasModel = itemModel != null && !itemModel.isEmpty();
        boolean hasVariant = itemModelVariant != null && !itemModelVariant.isEmpty();

        if (hasModel || hasVariant) {
            if (hasModel) {
                try {
                    NamespacedKey modelKey = itemModel.contains(":")
                        ? NamespacedKey.fromString(itemModel)
                        : new NamespacedKey("minecraft", itemModel.toLowerCase(java.util.Locale.ROOT));
                    if (modelKey != null) {
                        meta.setItemModel(modelKey);
                    }
                } catch (Throwable ignored) {
                    // Fallback for non-Paper or older version
                }
            }
            CustomModelDataComponent cmdComponent = meta.getCustomModelDataComponent();
            List<String> strings = new ArrayList<>();
            if (hasModel) strings.add(itemModel);
            // item-model-variant is added as an additional string
            if (hasVariant) strings.add(itemModelVariant);
            cmdComponent.setStrings(strings);
            meta.setCustomModelDataComponent(cmdComponent);
            if (hasModel) {
                meta.getPersistentDataContainer().set(itemModelKey, PersistentDataType.STRING, itemModel);
            }
            plugin.getLogger().fine("[LOAD] Applied custom_model_data strings: " + strings + " for " + itemId);
        } else if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
            plugin.getLogger().fine("[LOAD] Applied custom_model_data: " + customModelData + " for " + itemId);
        }

        itemStack.setItemMeta(meta);
    }

    /**
     * Reads the attributes of an item.
     *
     * <p>The section is looked up first at the top level of the item and
     * then inside the {@code item:} block — the top level wins. Previously
     * only {@code item:} was read, so every item that declared its
     * attributes at the top level (as all the samples do) silently lost
     * them.
     */
    private Map<String, Double> loadAttributes(ConfigurationSection section, ConfigurationSection itemSection) {
        Map<String, Double> attributes = new HashMap<>();

        ConfigurationSection attrSection = section.getConfigurationSection("attributes");
        if (attrSection == null) {
            attrSection = itemSection.getConfigurationSection("attributes");
        }
        if (attrSection == null) return attributes;

        for (String attrKey : attrSection.getKeys(false)) {
            Object attrValue = attrSection.get(attrKey);
            double value;
            if (attrValue instanceof Number) {
                value = ((Number) attrValue).doubleValue();
            } else if (attrValue instanceof ConfigurationSection) {
                value = ((ConfigurationSection) attrValue).getDouble("value", 0);
            } else {
                continue;
            }
            attributes.put(attrKey.toUpperCase(), value);
        }
        return attributes;
    }

    private List<String> loadTriggerActions(ConfigurationSection section) {
        if (section.contains("trigger-actions")) {
            plugin.getLogger().severe("[LOAD] 'trigger-actions' was removed in 1.328 - use the 'triggers:' section instead. The key is ignored.");
        }

        // New format: triggers: { on_click_left: ['particles:FLAME:10'] }
        ConfigurationSection triggersSection = section.getConfigurationSection("triggers");
        List<String> triggerActions = new ArrayList<>();
        if (triggersSection == null) return triggerActions;
        for (String triggerName : triggersSection.getKeys(false)) {
            for (String action : triggersSection.getStringList(triggerName)) {
                triggerActions.add(triggerName + ":" + action);
            }
        }
        return triggerActions;
    }

    // ===== Plain value conversion (recipes) =====

    /**
     * Converts a config section into plain Map/List so that the item model
     * does not depend on the Bukkit ConfigurationSection.
     */
    private Map<String, Object> toPlainMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key, toPlainValue(section.get(key)));
        }
        return map;
    }

    private Object toPlainValue(Object value) {
        if (value instanceof ConfigurationSection) {
            return toPlainMap((ConfigurationSection) value);
        }
        if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object o : (List<?>) value) {
                list.add(toPlainValue(o));
            }
            return list;
        }
        return value;
    }

    private String validateType(String type, String itemId) {
        String normalized = type == null ? "" : type.toUpperCase(java.util.Locale.ROOT);
        if (!VALID_TYPES.contains(normalized)) {
            plugin.getLogger().warning("Invalid item type: " + type + " for item: " + itemId);
            return "RUNE";
        }
        return normalized;
    }

    private String validateSlot(String slot, String itemId) {
        String normalized = slot == null ? "" : slot.toUpperCase(java.util.Locale.ROOT);
        if (!VALID_SLOTS.contains(normalized)) {
            plugin.getLogger().warning("Invalid activation slot: " + slot + " for item: " + itemId);
            return "HAND";
        }
        return normalized;
    }
}
