package me.dcplugin.artifex.managers;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Custom Block Engine for Minecraft 26.3 Paper.
 * Remaps NoteBlock states to custom fantasy RPG ores, crystals, and storage blocks.
 * Enforces real stone/ore hardness, tool requirements, authentic mining sounds & particles.
 */
public class CustomBlockManager implements Listener {

    public static class CustomBlockDef {
        public final String blockId;
        public final Instrument instrument;
        public final int note;
        public final boolean powered;
        public final String dropItemId;
        public final int minDrop;
        public final int maxDrop;
        public final int expDrop;
        public final Material minTool;
        public final boolean isDeepslate;
        public final Color sparkleColor;
        public final String displayName;

        public CustomBlockDef(String blockId, Instrument instrument, int note, boolean powered,
                              String dropItemId, int minDrop, int maxDrop, int expDrop,
                              Material minTool, boolean isDeepslate, Color sparkleColor, String displayName) {
            this.blockId = blockId;
            this.instrument = instrument;
            this.note = note;
            this.powered = powered;
            this.dropItemId = dropItemId;
            this.minDrop = minDrop;
            this.maxDrop = maxDrop;
            this.expDrop = expDrop;
            this.minTool = minTool;
            this.isDeepslate = isDeepslate;
            this.sparkleColor = sparkleColor;
            this.displayName = displayName;
        }

        public String getStateKey() {
            return instrument.name() + ":" + note + ":" + powered;
        }
    }

    private final Main plugin;
    private final Map<String, CustomBlockDef> byId = new HashMap<>();
    private final Map<String, CustomBlockDef> byState = new HashMap<>();
    private final Random random = new Random();

    public CustomBlockManager(Main plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    private void registerDefaults() {
        // Base Elemental Ores
        register(new CustomBlockDef("ember_ore", Instrument.IRON_XYLOPHONE, 0, false, "ember_shard", 1, 3, 5, Material.IRON_PICKAXE, false, Color.fromRGB(255, 61, 0), "Эмбер Руда"));
        register(new CustomBlockDef("frost_ore", Instrument.IRON_XYLOPHONE, 1, false, "frost_crystal_shard", 1, 3, 6, Material.IRON_PICKAXE, false, Color.fromRGB(0, 229, 255), "Ледяная Руда"));
        register(new CustomBlockDef("storm_ore", Instrument.IRON_XYLOPHONE, 2, false, "storm_shard", 1, 2, 7, Material.IRON_PICKAXE, false, Color.fromRGB(255, 235, 59), "Громовая Руда"));
        register(new CustomBlockDef("void_ore", Instrument.IRON_XYLOPHONE, 3, false, "void_shard", 1, 2, 12, Material.DIAMOND_PICKAXE, true, Color.fromRGB(156, 39, 176), "Руда Бездны"));

        // Gemstone Ores
        register(new CustomBlockDef("ruby_ore", Instrument.BELL, 0, false, "ruby_rough", 1, 2, 8, Material.IRON_PICKAXE, false, Color.fromRGB(255, 23, 68), "Рубиновая Жила"));
        register(new CustomBlockDef("sapphire_ore", Instrument.BELL, 1, false, "sapphire_rough", 1, 2, 8, Material.IRON_PICKAXE, false, Color.fromRGB(41, 121, 255), "Сапфировая Жила"));
        register(new CustomBlockDef("topaz_ore", Instrument.BELL, 2, false, "topaz_rough", 1, 2, 8, Material.IRON_PICKAXE, false, Color.fromRGB(255, 167, 38), "Топазовая Жила"));

        // Deep/Mythic Ores
        register(new CustomBlockDef("adamantite_ore", Instrument.BELL, 3, false, "adamantite_ore_raw", 1, 1, 10, Material.DIAMOND_PICKAXE, true, Color.fromRGB(233, 30, 99), "Адамантитовая Жила"));
        register(new CustomBlockDef("mithril_ore", Instrument.BELL, 4, false, "mithril_ore_raw", 1, 2, 9, Material.DIAMOND_PICKAXE, false, Color.fromRGB(128, 222, 234), "Мифриловая Жила"));

        // Ancient & Relic
        register(new CustomBlockDef("ancient_runestone", Instrument.FLUTE, 0, false, "runestone_slate", 1, 2, 12, Material.DIAMOND_PICKAXE, true, Color.fromRGB(171, 71, 188), "Древний Рунный Камень"));

        // Storage Blocks
        register(new CustomBlockDef("ember_block", Instrument.GUITAR, 0, false, "ember_block", 1, 1, 0, Material.IRON_PICKAXE, false, Color.fromRGB(255, 87, 34), "Блок Пламени"));
        register(new CustomBlockDef("frost_block", Instrument.GUITAR, 1, false, "frost_block", 1, 1, 0, Material.IRON_PICKAXE, false, Color.fromRGB(79, 195, 247), "Блок Вечной Мерзлоты"));
        register(new CustomBlockDef("storm_block", Instrument.GUITAR, 2, false, "storm_block", 1, 1, 0, Material.IRON_PICKAXE, false, Color.fromRGB(255, 214, 0), "Блок Грозовой Стали"));
        register(new CustomBlockDef("void_block", Instrument.GUITAR, 3, false, "void_block", 1, 1, 0, Material.DIAMOND_PICKAXE, true, Color.fromRGB(106, 27, 154), "Блок Бездны"));
        register(new CustomBlockDef("ruby_block", Instrument.GUITAR, 4, false, "ruby_block", 1, 1, 0, Material.IRON_PICKAXE, false, Color.fromRGB(213, 0, 0), "Блок Рубинов"));
        register(new CustomBlockDef("sapphire_block", Instrument.GUITAR, 5, false, "sapphire_block", 1, 1, 0, Material.IRON_PICKAXE, false, Color.fromRGB(13, 71, 161), "Блок Сапфиров"));
    }

    public void register(CustomBlockDef def) {
        byId.put(def.blockId.toLowerCase(), def);
        byState.put(def.getStateKey(), def);
    }

    public CustomBlockDef getByBlock(Block block) {
        if (block == null || !(block.getBlockData() instanceof NoteBlock nb)) return null;
        String key = nb.getInstrument().name() + ":" + nb.getNote().getId() + ":" + nb.isPowered();
        return byState.get(key);
    }

    public CustomBlockDef getById(String id) {
        return byId.get(id.toLowerCase());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("custom-blocks.enabled", true)) return;
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        String customId = plugin.getItemHandler().getCustomItemId(item);
        if (customId == null) return;

        CustomBlockDef def = getById(customId);
        if (def == null) return;

        Block block = event.getBlockPlaced();
        block.setType(Material.NOTE_BLOCK, false);
        if (block.getBlockData() instanceof NoteBlock nb) {
            nb.setInstrument(def.instrument);
            nb.setNote(new Note(def.note));
            nb.setPowered(def.powered);
            block.setBlockData(nb, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        CustomBlockDef def = getByBlock(event.getBlock());
        if (def == null) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

        boolean canHarvest = isToolEligible(tool, def.minTool);
        if (!canHarvest) {
            // Apply Mining Fatigue to prevent instant breaking with bare fist or axe
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 2, false, false, false));
            player.sendActionBar(ColorUtils.colorize("&c[Artifex] &7Требуется &e" + getToolName(def.minTool) + " &7для добычи!"));
            player.playSound(loc, Sound.BLOCK_STONE_HIT, 0.8f, 0.7f);
        } else {
            Sound hitSound = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_HIT : Sound.BLOCK_STONE_HIT;
            player.playSound(loc, hitSound, 0.9f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("custom-blocks.enabled", true)) return;
        Block block = event.getBlock();
        CustomBlockDef def = getByBlock(block);
        if (def == null) return;

        event.setDropItems(false);
        event.setExpToDrop(0);

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        // Remove mining fatigue upon breaking
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        // Check tool tier
        boolean canHarvest = isToolEligible(tool, def.minTool);
        if (!canHarvest) {
            Sound failSound = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_BREAK : Sound.BLOCK_STONE_BREAK;
            loc.getWorld().playSound(loc, failSound, 1.0f, 0.6f);
            player.sendActionBar(ColorUtils.colorize("&c[Artifex] &7Руда рассыпалась в пыль без &e" + getToolName(def.minTool) + "&7!"));
            return;
        }

        // Realistic stone/deepslate break sound + crystal chime
        Sound baseBreak = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_BREAK : Sound.BLOCK_STONE_BREAK;
        loc.getWorld().playSound(loc, baseBreak, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);

        // Spawn realistic stone/deepslate fracture particles
        Material stoneParticle = def.isDeepslate ? Material.DEEPSLATE : Material.STONE;
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 35, 0.3, 0.3, 0.3, stoneParticle.createBlockData());

        // Spawn colored sparkle dust matching the ore color
        if (def.sparkleColor != null) {
            Particle.DustOptions dust = new Particle.DustOptions(def.sparkleColor, 1.3f);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 25, 0.35, 0.35, 0.35, dust);
        }

        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        if (hasSilkTouch) {
            ItemStack blockDrop = getCustomItemStack(def.blockId, 1);
            if (blockDrop != null) {
                loc.getWorld().dropItemNaturally(loc, blockDrop);
            }
        } else {
            boolean allowFortune = plugin.getConfig().getBoolean("custom-blocks.enable-fortune", true);
            int fortune = allowFortune ? tool.getEnchantmentLevel(Enchantment.FORTUNE) : 0;
            int multiplier = 1;
            if (fortune > 0) {
                int bonus = random.nextInt(fortune + 2) - 1;
                if (bonus > 0) multiplier += bonus;
            }
            int baseCount = def.minDrop + (def.maxDrop > def.minDrop ? random.nextInt(def.maxDrop - def.minDrop + 1) : 0);
            int total = baseCount * multiplier;

            ItemStack resourceDrop = getCustomItemStack(def.dropItemId, total);
            if (resourceDrop != null) {
                loc.getWorld().dropItemNaturally(loc, resourceDrop);
            }

            if (def.expDrop > 0) {
                loc.getWorld().spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(def.expDrop));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("custom-blocks.prevent-tuning", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlockDef def = getByBlock(block);
        if (def != null) {
            // Cancel tuning NoteBlock
            event.setCancelled(true);
            
            // Show ore inspect info on shift-click
            if (event.getPlayer().isSneaking()) {
                event.getPlayer().sendActionBar(ColorUtils.colorize("&6✦ &f" + def.displayName + " &8| &7Добыча: &e" + getToolName(def.minTool)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (!plugin.getConfig().getBoolean("custom-blocks.mute-notes", true)) return;
        CustomBlockDef def = getByBlock(event.getBlock());
        if (def != null) {
            event.setCancelled(true);
        }
    }

    private boolean isToolEligible(ItemStack tool, Material minTool) {
        if (minTool == null) return true;
        if (tool == null) return false;
        Material type = tool.getType();
        String name = type.name();
        if (!name.endsWith("_PICKAXE")) return false;

        int playerTier = getPickaxeTier(type);
        int requiredTier = getPickaxeTier(minTool);
        return playerTier >= requiredTier;
    }

    private int getPickaxeTier(Material m) {
        if (m == Material.WOODEN_PICKAXE || m == Material.GOLDEN_PICKAXE) return 1;
        if (m == Material.STONE_PICKAXE) return 2;
        if (m == Material.IRON_PICKAXE) return 3;
        if (m == Material.DIAMOND_PICKAXE) return 4;
        if (m == Material.NETHERITE_PICKAXE) return 5;
        return 0;
    }

    private String getToolName(Material m) {
        if (m == Material.WOODEN_PICKAXE) return "Деревянная кирка";
        if (m == Material.STONE_PICKAXE) return "Каменная кирка";
        if (m == Material.IRON_PICKAXE) return "Железная кирка";
        if (m == Material.DIAMOND_PICKAXE) return "Алмазная кирка";
        if (m == Material.NETHERITE_PICKAXE) return "Незеритовая кирка";
        return "Кирка";
    }

    private ItemStack getCustomItemStack(String id, int amount) {
        if (id == null) return null;
        CustomItem ci = plugin.getItemHandler().getCustomItem(id);
        if (ci != null) {
            ItemStack stack = ci.getItemStack();
            if (stack != null) {
                stack = stack.clone();
                stack.setAmount(amount);
                return stack;
            }
        }
        if (plugin.getApiItemRegistry() != null) {
            var javaItem = plugin.getApiItemRegistry().getItem(id);
            if (javaItem != null) {
                ItemStack stack = javaItem.createItemStack();
                if (stack != null) {
                    stack = stack.clone();
                    stack.setAmount(amount);
                    return stack;
                }
            }
        }
        Material mat = org.bukkit.Material.matchMaterial(id);
        if (mat != null) {
            return new ItemStack(mat, amount);
        }
        return null;
    }
}
