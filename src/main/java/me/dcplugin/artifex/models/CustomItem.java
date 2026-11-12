package me.dcplugin.artifex.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomItem {
    private final String id;
    private final ItemStack itemStack;
    private final String type;
    private final String activationSlot;
    private final boolean placeable;
    private final List<String> effects;
    private volatile Map<PotionEffectType, Integer> parsedEffects;
    private final Map<String, Double> attributes;
    private final String armorSetId;
    private final boolean hasSetBonus;
    private final List<String> leftClickActions;
    private final List<String> rightClickActions;
    private final long clickCooldown;
    private final int maxUses;
    private final List<String> loreTemplate;
    private final int customModelData;
    private final String permission;
    private final List<String> equipParticles;
    private final List<String> equipSounds;
    private final List<String> unequipParticles;
    private final List<String> unequipSounds;
    private final List<String> triggerActions;
    private final String equipMessage;
    private final String unequipMessage;
    private final String cooldownMessage;
    private final String activationMessage;
    private final String deactivationMessage;
    private final String usesDepletedMessage;

    // === New fields ===
    private final long duration;                    // Item lifetime (seconds, -1 = permanent)
    private final String maxDurationMessage;         // The message when the lifetime expires
    private final List<String> trailParticles;       // Trail particles while moving
    private final int trailParticleInterval;         // Particle spawn interval (ticks)
    private final List<String> allowedWorlds;        // Allowed worlds (empty = all)
    private final List<String> disabledWorlds;       // Disabled worlds
    private final String itemModelVariant;           // Model variant (for animated textures)
    private final Map<String, Object> recipes;        // The recipes section from YAML (shaped/shapeless/furnace)
    private final Map<org.bukkit.entity.EntityType, Double> mobDrops;   // Mob drop chances (0.0 to 1.0)
    private final Map<org.bukkit.Material, Double> blockDrops;          // Block break drop chances (0.0 to 1.0)

    private CustomItem(Builder b) {
        this.id = b.id;
        this.itemStack = b.itemStack;
        // The type and the slot are normalized to upper case: the configs were written
        // as "consumable"/"hand", while the comparisons in the code used
        // "CONSUMABLE"/"HAND".
        this.type = b.type == null ? "RUNE" : b.type.toUpperCase(java.util.Locale.ROOT);
        this.activationSlot = b.activationSlot == null
                ? "HAND"
                : b.activationSlot.toUpperCase(java.util.Locale.ROOT);
        this.placeable = b.placeable;
        this.effects = immutableList(b.effects);
        this.attributes = immutableMap(b.attributes);
        this.armorSetId = b.armorSetId;
        this.hasSetBonus = b.hasSetBonus;
        this.leftClickActions = immutableList(b.leftClickActions);
        this.rightClickActions = immutableList(b.rightClickActions);
        this.clickCooldown = b.clickCooldown;
        this.maxUses = b.maxUses;
        this.loreTemplate = immutableList(b.loreTemplate);
        this.customModelData = b.customModelData;
        this.permission = b.permission;
        this.equipParticles = immutableList(b.equipParticles);
        this.equipSounds = immutableList(b.equipSounds);
        this.unequipParticles = immutableList(b.unequipParticles);
        this.unequipSounds = immutableList(b.unequipSounds);
        this.triggerActions = immutableList(b.triggerActions);
        this.equipMessage = b.equipMessage;
        this.unequipMessage = b.unequipMessage;
        this.cooldownMessage = b.cooldownMessage;
        this.activationMessage = b.activationMessage;
        this.deactivationMessage = b.deactivationMessage;
        this.usesDepletedMessage = b.usesDepletedMessage;
        this.duration = b.duration;
        this.maxDurationMessage = b.maxDurationMessage;
        this.trailParticles = immutableList(b.trailParticles);
        this.trailParticleInterval = b.trailParticleInterval;
        this.allowedWorlds = immutableList(b.allowedWorlds);
        this.disabledWorlds = immutableList(b.disabledWorlds);
        this.itemModelVariant = b.itemModelVariant;
        this.recipes = b.recipes == null ? null : immutableMap(b.recipes);
        this.mobDrops = immutableMap(b.mobDrops);
        this.blockDrops = immutableMap(b.blockDrops);
    }

    // Collections.copyOf/List.copyOf forbid null elements, while in YAML
    // a value can be empty. Therefore wrappers are used that allow null
    // but forbid modification.

    private static List<String> immutableList(List<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        return java.util.Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        if (source == null || source.isEmpty()) return Map.of();
        return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(source));
    }

    // ===== BUILDER =====

    public static Builder builder(String id, ItemStack itemStack) {
        return new Builder(id, itemStack);
    }

    public static class Builder {
        // Required
        private final String id;
        private final ItemStack itemStack;

        // Defaults
        private String type = "RUNE";
        private String activationSlot = "HAND";
        private boolean placeable = false;
        private List<String> effects = new ArrayList<>();
        private Map<String, Double> attributes = new HashMap<>();
        private String armorSetId = null;
        private boolean hasSetBonus = false;
        private List<String> leftClickActions = new ArrayList<>();
        private List<String> rightClickActions = new ArrayList<>();
        private long clickCooldown = 0;
        private int maxUses = -1;
        private List<String> loreTemplate = new ArrayList<>();
        private int customModelData = -1;
        private String permission = null;
        private List<String> equipParticles = new ArrayList<>();
        private List<String> equipSounds = new ArrayList<>();
        private List<String> unequipParticles = new ArrayList<>();
        private List<String> unequipSounds = new ArrayList<>();
        private List<String> triggerActions = new ArrayList<>();
        private String equipMessage = null;
        private String unequipMessage = null;
        private String cooldownMessage = null;
        private String activationMessage = null;
        private String deactivationMessage = null;
        private String usesDepletedMessage = null;

        // New fields
        private long duration = -1;                    // Lifetime (seconds, -1 = permanent)
        private String maxDurationMessage = null;
        private List<String> trailParticles = new ArrayList<>();
        private int trailParticleInterval = 5;         // Ticks between particle spawns
        private List<String> allowedWorlds = new ArrayList<>();
        private List<String> disabledWorlds = new ArrayList<>();
        private String itemModelVariant = null;
        private Map<String, Object> recipes = null;
        private Map<org.bukkit.entity.EntityType, Double> mobDrops = new HashMap<>();
        private Map<org.bukkit.Material, Double> blockDrops = new HashMap<>();

        private Builder(String id, ItemStack itemStack) {
            this.id = id;
            this.itemStack = itemStack;
        }

        public Builder type(String type) { this.type = type; return this; }
        public Builder activationSlot(String slot) { this.activationSlot = slot; return this; }
        public Builder placeable(boolean placeable) { this.placeable = placeable; return this; }
        public Builder effects(List<String> effects) { this.effects = effects != null ? effects : new ArrayList<>(); return this; }
        public Builder attributes(Map<String, Double> attributes) { this.attributes = attributes != null ? attributes : new HashMap<>(); return this; }
        public Builder armorSet(String armorSetId, boolean hasSetBonus) { this.armorSetId = armorSetId; this.hasSetBonus = hasSetBonus; return this; }
        public Builder leftClickActions(List<String> actions) { this.leftClickActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder rightClickActions(List<String> actions) { this.rightClickActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder clickCooldown(long ms) { this.clickCooldown = ms; return this; }
        public Builder maxUses(int maxUses) { this.maxUses = maxUses; return this; }
        public Builder loreTemplate(List<String> lore) { this.loreTemplate = lore != null ? lore : new ArrayList<>(); return this; }
        public Builder customModelData(int data) { this.customModelData = data; return this; }
        public Builder permission(String permission) { this.permission = permission; return this; }
        public Builder equipParticles(List<String> particles) { this.equipParticles = particles != null ? particles : new ArrayList<>(); return this; }
        public Builder equipSounds(List<String> sounds) { this.equipSounds = sounds != null ? sounds : new ArrayList<>(); return this; }
        public Builder unequipParticles(List<String> particles) { this.unequipParticles = particles != null ? particles : new ArrayList<>(); return this; }
        public Builder unequipSounds(List<String> sounds) { this.unequipSounds = sounds != null ? sounds : new ArrayList<>(); return this; }
        public Builder triggerActions(List<String> actions) { this.triggerActions = actions != null ? actions : new ArrayList<>(); return this; }
        public Builder equipMessage(String msg) { this.equipMessage = msg; return this; }
        public Builder unequipMessage(String msg) { this.unequipMessage = msg; return this; }
        public Builder cooldownMessage(String msg) { this.cooldownMessage = msg; return this; }
        public Builder activationMessage(String msg) { this.activationMessage = msg; return this; }
        public Builder deactivationMessage(String msg) { this.deactivationMessage = msg; return this; }
        public Builder usesDepletedMessage(String msg) { this.usesDepletedMessage = msg; return this; }
        public Builder duration(long seconds) { this.duration = seconds; return this; }
        public Builder maxDurationMessage(String msg) { this.maxDurationMessage = msg; return this; }
        public Builder trailParticles(List<String> particles) { this.trailParticles = particles != null ? particles : new ArrayList<>(); return this; }
        public Builder trailParticleInterval(int ticks) { this.trailParticleInterval = ticks; return this; }
        public Builder allowedWorlds(List<String> worlds) { this.allowedWorlds = worlds != null ? worlds : new ArrayList<>(); return this; }
        public Builder disabledWorlds(List<String> worlds) { this.disabledWorlds = worlds != null ? worlds : new ArrayList<>(); return this; }
        public Builder itemModelVariant(String variant) { this.itemModelVariant = variant; return this; }
        public Builder recipes(Map<String, Object> recipes) { this.recipes = recipes != null ? recipes : new HashMap<>(); return this; }
        public Builder mobDrops(Map<org.bukkit.entity.EntityType, Double> drops) { this.mobDrops = drops != null ? drops : new HashMap<>(); return this; }
        public Builder blockDrops(Map<org.bukkit.Material, Double> drops) { this.blockDrops = drops != null ? drops : new HashMap<>(); return this; }

        public CustomItem build() {
            return new CustomItem(this);
        }
    }

    // ===== Getters =====

    public String getId() { return id; }
    public ItemStack getItemStack() { return itemStack; }
    public boolean hasDisplayName() {
        return itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName();
    }
    public String getDisplayName() {
        return hasDisplayName() ? itemStack.getItemMeta().getDisplayName() : id;
    }
    public String getType() { return type; }
    public String getActivationSlot() { return activationSlot; }
    public boolean isPlaceable() { return placeable; }
    public List<String> getEffects() { return effects; }
    public Map<String, Double> getAttributes() { return attributes; }
    public String getArmorSetId() { return armorSetId; }
    public boolean hasSetBonus() { return hasSetBonus; }
    public List<String> getLeftClickActions() { return leftClickActions; }
    public List<String> getRightClickActions() { return rightClickActions; }
    public long getClickCooldown() { return clickCooldown; }
    public int getMaxUses() { return maxUses; }
    public boolean hasMaxUses() { return maxUses > 0; }
    public List<String> getLoreTemplate() { return loreTemplate; }
    public int getCustomModelData() { return customModelData; }
    public boolean hasCustomModelData() { return customModelData > 0; }
    public String getPermission() { return permission; }
    public boolean hasPermission() { return permission != null && !permission.isEmpty(); }

    /**
     * May the given player use this item?
     *
     * <p>Used for every path that gives the item its power: clicking,
     * wearing it (effects, attributes) and triggers. Before, the permission
     * was only checked on click, so a player without the right could still
     * benefit from the item's passive bonuses and triggers.
     */
    public boolean isUsableBy(org.bukkit.entity.Player player) {
        if (!hasPermission()) return true;
        return player != null && player.hasPermission(permission);
    }
    public List<String> getEquipParticles() { return equipParticles; }
    public List<String> getEquipSounds() { return equipSounds; }
    public List<String> getUnequipParticles() { return unequipParticles; }
    public List<String> getUnequipSounds() { return unequipSounds; }
    public List<String> getTriggerActions() { return triggerActions; }
    public boolean hasTriggerActions() { return triggerActions != null && !triggerActions.isEmpty(); }
    public String getEquipMessage() { return equipMessage; }
    public String getUnequipMessage() { return unequipMessage; }
    public String getCooldownMessage() { return cooldownMessage; }
    public String getActivationMessage() { return activationMessage; }
    public String getDeactivationMessage() { return deactivationMessage; }
    public String getUsesDepletedMessage() { return usesDepletedMessage; }

    public Map<PotionEffectType, Integer> getParsedEffects() {
        // The field is already immutable (setParsedEffects stores Map.copyOf),
        // so copying it again only allocated garbage in the hot path.
        return parsedEffects;
    }
    public synchronized void setParsedEffects(Map<PotionEffectType, Integer> parsedEffects) {
        this.parsedEffects = parsedEffects == null ? null : Map.copyOf(parsedEffects);
    }

    public boolean hasEffect(PotionEffectType effectType) {
        return parsedEffects != null && parsedEffects.containsKey(effectType);
    }

    public int getEffectLevel(PotionEffectType effectType) {
        if (parsedEffects != null && parsedEffects.containsKey(effectType)) {
            return parsedEffects.get(effectType);
        }
        return 0;
    }

    public boolean hasEquipMessage() { return equipMessage != null && !equipMessage.trim().isEmpty(); }
    public boolean hasUnequipMessage() { return unequipMessage != null && !unequipMessage.trim().isEmpty(); }
    public boolean hasCooldownMessage() { return cooldownMessage != null && !cooldownMessage.trim().isEmpty(); }
    public boolean hasActivationMessage() { return activationMessage != null && !activationMessage.trim().isEmpty(); }
    public boolean hasDeactivationMessage() { return deactivationMessage != null && !deactivationMessage.trim().isEmpty(); }
    public boolean hasUsesDepletedMessage() { return usesDepletedMessage != null && !usesDepletedMessage.trim().isEmpty(); }

    // === New getters ===
    public long getDuration() { return duration; }
    public boolean hasDuration() { return duration > 0; }
    public String getMaxDurationMessage() { return maxDurationMessage; }
    public boolean hasMaxDurationMessage() { return maxDurationMessage != null && !maxDurationMessage.trim().isEmpty(); }
    public List<String> getTrailParticles() { return trailParticles; }
    public boolean hasTrailParticles() { return trailParticles != null && !trailParticles.isEmpty(); }
    public int getTrailParticleInterval() { return trailParticleInterval; }
    public List<String> getAllowedWorlds() { return allowedWorlds; }
    public boolean hasAllowedWorlds() { return allowedWorlds != null && !allowedWorlds.isEmpty(); }
    public List<String> getDisabledWorlds() { return disabledWorlds; }
    public boolean hasDisabledWorlds() { return disabledWorlds != null && !disabledWorlds.isEmpty(); }
    public String getItemModelVariant() { return itemModelVariant; }
    public boolean hasItemModelVariant() { return itemModelVariant != null && !itemModelVariant.trim().isEmpty(); }
    public Map<String, Object> getRecipes() { return recipes; }
    public boolean hasRecipes() { return recipes != null && !recipes.isEmpty(); }
    public Map<org.bukkit.entity.EntityType, Double> getMobDrops() { return mobDrops; }
    public boolean hasMobDrops() { return mobDrops != null && !mobDrops.isEmpty(); }
    public Map<org.bukkit.Material, Double> getBlockDrops() { return blockDrops; }
    public boolean hasBlockDrops() { return blockDrops != null && !blockDrops.isEmpty(); }

    /**
     * Checks whether the item is allowed in the given world
     */
    public boolean isAllowedInWorld(String worldName) {
        if (hasAllowedWorlds() && !allowedWorlds.contains(worldName)) return false;
        if (hasDisabledWorlds() && disabledWorlds.contains(worldName)) return false;
        return true;
    }

    public enum ItemType { RUNE, TOOL, ARMOR, CONSUMABLE }
    public enum ActivationSlot { HAND, OFFHAND, HEAD, CHEST, LEGS, FEET }

    @Override
    public String toString() {
        return "CustomItem{" +
               "id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", activationSlot='" + activationSlot + '\'' +
               ", placeable=" + placeable +
               ", effects=" + effects +
               '}';
    }
}
