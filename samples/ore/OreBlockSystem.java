package items;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.AbstractCustomItem;
import me.dcplugin.artifex.api.ItemAPI;
import me.dcplugin.artifex.managers.CustomBlockManager;
import me.dcplugin.artifex.models.CustomItem;
import me.dcplugin.artifex.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * 🧱 АВТОМАТИЧЕСКАЯ СИСТЕМА КАСТОМНЫХ БЛОКОВ И РУД ARTIFEX
 * Превращает установленные предметы из ores.yml в полноценные 3D-блоки (NoteBlock states),
 * синхронизирует сетевые пакеты sendBlockChange, реализует честную добычу, прочность,
 * требования к кирке, Шелковое касание, Удачу, частицы и реалистичные звуки.
 */
public class OreBlockSystem extends AbstractCustomItem implements Listener {

    public static class BlockDef {
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
        public final boolean isNether;
        public final boolean isBlock;
        public final Color sparkleColor;
        public final String displayName;

        public BlockDef(String blockId, Instrument instrument, int note, boolean powered,
                        String dropItemId, int minDrop, int maxDrop, int expDrop,
                        Material minTool, boolean isDeepslate, boolean isNether, boolean isBlock,
                        Color sparkleColor, String displayName) {
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
            this.isNether = isNether;
            this.isBlock = isBlock;
            this.sparkleColor = sparkleColor;
            this.displayName = displayName;
        }

        public String getStateKey() {
            return instrument.name() + ":" + note + ":" + powered;
        }
    }

    private static final Map<String, BlockDef> BY_ID = new HashMap<>();
    private static final Map<String, BlockDef> BY_STATE = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        registerDef(new BlockDef("amber_ore", Instrument.IRON_XYLOPHONE, 5, false, "raw_amber", 1, 2, 3, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 170, 0), "&#FFAA00Янтарная руда"));
        registerDef(new BlockDef("amethyst_ore", Instrument.IRON_XYLOPHONE, 6, false, "amethyst", 2, 4, 4, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(200, 85, 255), "&#C855FFАметистовая руда"));
        registerDef(new BlockDef("bloodstone_ore", Instrument.IRON_XYLOPHONE, 7, false, "raw_bloodstone", 1, 2, 5, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 34, 68), "&#FF2244Кровавая руда"));
        registerDef(new BlockDef("crystalite_ore", Instrument.IRON_XYLOPHONE, 8, false, "raw_crystalite", 1, 2, 5, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(0, 255, 221), "&#00FFDDКристаллитовая руда"));
        registerDef(new BlockDef("deepslate_amber_ore", Instrument.IRON_XYLOPHONE, 9, false, "raw_amber", 1, 2, 4, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(255, 170, 0), "&#FFAA00Глубинная янтарная руда"));
        registerDef(new BlockDef("deepslate_amethyst_ore", Instrument.IRON_XYLOPHONE, 10, false, "amethyst", 2, 4, 5, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(200, 85, 255), "&#C855FFГлубинная аметистовая руда"));
        registerDef(new BlockDef("deepslate_iridium_ore", Instrument.IRON_XYLOPHONE, 11, false, "iridium", 1, 1, 9, Material.DIAMOND_PICKAXE, true, false, false, Color.fromRGB(240, 240, 255), "&#FFFFFFГлубинная иридиевая руда"));
        registerDef(new BlockDef("deepslate_jade_ore", Instrument.IRON_XYLOPHONE, 12, false, "raw_jade", 1, 2, 5, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(85, 255, 85), "&#55FF55Глубинная нефритовая руда"));
        registerDef(new BlockDef("deepslate_malachite_ore", Instrument.IRON_XYLOPHONE, 13, false, "malachite", 1, 3, 5, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(17, 187, 102), "&#11BB66Глубинная малахитовая руда"));
        registerDef(new BlockDef("deepslate_mythril_ore", Instrument.IRON_XYLOPHONE, 14, false, "raw_mythril", 1, 2, 9, Material.DIAMOND_PICKAXE, true, false, false, Color.fromRGB(51, 224, 255), "&#33E0FFГлубинная мифриловая руда"));
        registerDef(new BlockDef("deepslate_ruby_ore", Instrument.IRON_XYLOPHONE, 15, false, "ruby", 1, 2, 7, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(255, 51, 102), "&#FF3366Глубинная рубиновая руда"));
        registerDef(new BlockDef("deepslate_silver_ore", Instrument.IRON_XYLOPHONE, 16, false, "silver", 1, 2, 4, Material.IRON_PICKAXE, true, false, false, Color.fromRGB(221, 238, 255), "&#DDEEFFГлубинная серебряная руда"));
        registerDef(new BlockDef("galenite_ore", Instrument.IRON_XYLOPHONE, 17, false, "raw_galenite", 1, 2, 4, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(112, 128, 144), "&#708090Галенитовая руда"));
        registerDef(new BlockDef("iridium_ore", Instrument.IRON_XYLOPHONE, 18, false, "iridium", 1, 1, 8, Material.DIAMOND_PICKAXE, false, false, false, Color.fromRGB(240, 240, 255), "&#FFFFFFИридиевая руда"));
        registerDef(new BlockDef("jade_ore", Instrument.IRON_XYLOPHONE, 19, false, "raw_jade", 1, 2, 4, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(85, 255, 85), "&#55FF55Нефритовая руда"));
        registerDef(new BlockDef("malachite_ore", Instrument.IRON_XYLOPHONE, 20, false, "malachite", 1, 3, 4, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(17, 187, 102), "&#11BB66Малахитовая руда"));
        registerDef(new BlockDef("mythril_ore", Instrument.IRON_XYLOPHONE, 21, false, "raw_mythril", 1, 2, 8, Material.DIAMOND_PICKAXE, false, false, false, Color.fromRGB(51, 224, 255), "&#33E0FFМифриловая руда"));
        registerDef(new BlockDef("oresvol1_amber_ore", Instrument.IRON_XYLOPHONE, 22, false, "raw_amber", 1, 2, 3, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 136, 0), "&#FF8800Древнеянтарная руда"));
        registerDef(new BlockDef("ruby_ore", Instrument.IRON_XYLOPHONE, 23, false, "ruby", 1, 2, 6, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 51, 102), "&#FF3366Рубиновая руда"));
        registerDef(new BlockDef("silver_ore", Instrument.IRON_XYLOPHONE, 24, false, "silver", 1, 2, 3, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(221, 238, 255), "&#DDEEFFСеребряная руда"));
        registerDef(new BlockDef("amber_end_ore", Instrument.COW_BELL, 0, false, "raw_amber", 1, 2, 5, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 170, 0), "&#FFAA00Янтарная руда Энда"));
        registerDef(new BlockDef("amber_nether_ore", Instrument.COW_BELL, 1, false, "raw_amber", 1, 2, 5, Material.IRON_PICKAXE, false, true, false, Color.fromRGB(255, 170, 0), "&#FFAA00Янтарная руда Незера"));
        registerDef(new BlockDef("bloodstone_end_ore", Instrument.COW_BELL, 2, false, "raw_bloodstone", 1, 2, 7, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 34, 68), "&#FF2244Кровавая руда Энда"));
        registerDef(new BlockDef("bloodstone_nether_ore", Instrument.COW_BELL, 3, false, "raw_bloodstone", 1, 2, 7, Material.IRON_PICKAXE, false, true, false, Color.fromRGB(255, 34, 68), "&#FF2244Кровавая руда Незера"));
        registerDef(new BlockDef("crystalite_end_ore", Instrument.COW_BELL, 4, false, "raw_crystalite", 1, 2, 7, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(0, 255, 221), "&#00FFDDКристаллитовая руда Энда"));
        registerDef(new BlockDef("crystalite_nether_ore", Instrument.COW_BELL, 5, false, "raw_crystalite", 1, 2, 7, Material.IRON_PICKAXE, false, true, false, Color.fromRGB(0, 255, 221), "&#00FFDDКристаллитовая руда Незера"));
        registerDef(new BlockDef("galenite_end_ore", Instrument.COW_BELL, 6, false, "raw_galenite", 1, 2, 6, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(112, 128, 144), "&#708090Галенитовая руда Энда"));
        registerDef(new BlockDef("galenite_nether_ore", Instrument.COW_BELL, 7, false, "raw_galenite", 1, 2, 6, Material.IRON_PICKAXE, false, true, false, Color.fromRGB(112, 128, 144), "&#708090Галенитовая руда Незера"));
        registerDef(new BlockDef("oresvol1_amber_ingot", Instrument.COW_BELL, 8, false, "raw_amber", 1, 2, 5, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 136, 0), "&#FF8800Древнеянтарная руда"));
        registerDef(new BlockDef("oresvol1_amber_nugget", Instrument.COW_BELL, 9, false, "raw_amber", 1, 2, 5, Material.IRON_PICKAXE, false, false, false, Color.fromRGB(255, 136, 0), "&#FF8800Древнеянтарная руда"));
        registerDef(new BlockDef("amber_block", Instrument.GUITAR, 6, false, "amber_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 170, 0), "&#FFAA00Янтарный блок"));
        registerDef(new BlockDef("amethyst_block", Instrument.GUITAR, 7, false, "amethyst_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(200, 85, 255), "&#C855FFАметистовый блок"));
        registerDef(new BlockDef("bloodstone_block", Instrument.GUITAR, 8, false, "bloodstone_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 34, 68), "&#FF2244Кровавый блок"));
        registerDef(new BlockDef("crystalite_block", Instrument.GUITAR, 9, false, "crystalite_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(0, 255, 221), "&#00FFDDКристаллитовый блок"));
        registerDef(new BlockDef("galenite_block", Instrument.GUITAR, 10, false, "galenite_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(112, 128, 144), "&#708090Галенитовый блок"));
        registerDef(new BlockDef("iridium_block", Instrument.GUITAR, 11, false, "iridium_block", 1, 1, 0, Material.DIAMOND_PICKAXE, false, false, true, Color.fromRGB(240, 240, 255), "&#FFFFFFИридиевый блок"));
        registerDef(new BlockDef("jade_block", Instrument.GUITAR, 12, false, "jade_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(85, 255, 85), "&#55FF55Нефритовый блок"));
        registerDef(new BlockDef("malachite_block", Instrument.GUITAR, 13, false, "malachite_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(17, 187, 102), "&#11BB66Малахитовый блок"));
        registerDef(new BlockDef("mythril_block", Instrument.GUITAR, 14, false, "mythril_block", 1, 1, 0, Material.DIAMOND_PICKAXE, false, false, true, Color.fromRGB(51, 224, 255), "&#33E0FFМифриловый блок"));
        registerDef(new BlockDef("oresvol1_amber_block", Instrument.GUITAR, 15, false, "oresvol1_amber_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 136, 0), "&#FF8800Древнеянтарный блок"));
        registerDef(new BlockDef("ruby_block", Instrument.GUITAR, 16, false, "ruby_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 51, 102), "&#FF3366Рубиновый блок"));
        registerDef(new BlockDef("silver_block", Instrument.GUITAR, 17, false, "silver_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(221, 238, 255), "&#DDEEFFСеребряный блок"));
        registerDef(new BlockDef("raw_amber_block", Instrument.DIDGERIDOO, 0, false, "raw_amber_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 170, 0), "&#FFAA00Блок сырого янтаря"));
        registerDef(new BlockDef("raw_amethyst_block", Instrument.DIDGERIDOO, 1, false, "raw_amethyst_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(200, 85, 255), "&#C855FFБлок сырого аметиста"));
        registerDef(new BlockDef("raw_iridium_block", Instrument.DIDGERIDOO, 2, false, "raw_iridium_block", 1, 1, 0, Material.DIAMOND_PICKAXE, false, false, true, Color.fromRGB(240, 240, 255), "&#FFFFFFБлок сырого иридия"));
        registerDef(new BlockDef("raw_jade_block", Instrument.DIDGERIDOO, 3, false, "raw_jade_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(85, 255, 85), "&#55FF55Блок сырого нефрита"));
        registerDef(new BlockDef("raw_malachite_block", Instrument.DIDGERIDOO, 4, false, "raw_malachite_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(17, 187, 102), "&#11BB66Блок сырого малахита"));
        registerDef(new BlockDef("raw_mythril_block", Instrument.DIDGERIDOO, 5, false, "raw_mythril_block", 1, 1, 0, Material.DIAMOND_PICKAXE, false, false, true, Color.fromRGB(51, 224, 255), "&#33E0FFБлок сырого мифрила"));
        registerDef(new BlockDef("raw_ruby_block", Instrument.DIDGERIDOO, 6, false, "raw_ruby_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(255, 51, 102), "&#FF3366Блок сырого рубина"));
        registerDef(new BlockDef("raw_silver_block", Instrument.DIDGERIDOO, 7, false, "raw_silver_block", 1, 1, 0, Material.IRON_PICKAXE, false, false, true, Color.fromRGB(221, 238, 255), "&#DDEEFFБлок сырого серебра"));
    }

    private static void registerDef(BlockDef def) {
        BY_ID.put(def.blockId.toLowerCase(Locale.ROOT), def);
        BY_STATE.put(def.getStateKey(), def);
    }

    public OreBlockSystem() {
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
            if (plugin != null) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
            }
        } catch (Throwable ignored) {}

        // Также регистрируем в CustomBlockManager ядра Artifex при наличии
        try {
            if (Main.getInstance() != null && Main.getInstance().getCustomBlockManager() != null) {
                CustomBlockManager cbm = Main.getInstance().getCustomBlockManager();
                for (BlockDef def : BY_ID.values()) {
                    cbm.register(new CustomBlockManager.CustomBlockDef(
                        def.blockId, def.instrument, def.note, def.powered,
                        def.dropItemId, def.minDrop, def.maxDrop, def.expDrop,
                        def.minTool, def.isDeepslate, def.sparkleColor, def.displayName
                    ));
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public String getId() {
        return "ore_block_system";
    }

    @Override
    public String getDisplayName() {
        return "&#FF8800&lСИСТЕМА КАСТОМНЫХ БЛОКОВ";
    }

    @Override
    public Material getMaterial() {
        return Material.BARRIER;
    }

    @Override
    public boolean isPlaceable() {
        return false;
    }

    @Override
    public List<String> getLore() {
        return List.of("&8Служебный системный класс обработки кастомных руд и блоков");
    }

    public static BlockDef getByBlock(Block block) {
        if (block == null || !(block.getBlockData() instanceof NoteBlock nb)) return null;
        String key = nb.getInstrument().name() + ":" + nb.getNote().getId() + ":" + nb.isPowered();
        return BY_STATE.get(key);
    }

    public static BlockDef getById(String id) {
        if (id == null) return null;
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    // ===== РАЗМЕЩЕНИЕ БЛОКА =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String customId = getCustomId(item);
        if (customId == null) {
            ItemStack off = event.getPlayer().getInventory().getItemInOffHand();
            customId = getCustomId(off);
        }
        if (customId == null) return;

        BlockDef def = getById(customId);
        if (def == null) return;

        Block block = event.getBlockPlaced();
        setBlockData(block, def);
        event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());

        // Синхронизация на следующем тике предотвращает фантомное замещение ванильным камнем
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
            if (plugin != null && plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (block.getType() == Material.NOTE_BLOCK) {
                        setBlockData(block, def);
                        for (Player p : block.getWorld().getPlayers()) {
                            if (p.getLocation().distanceSquared(block.getLocation()) < 64 * 64) {
                                p.sendBlockChange(block.getLocation(), block.getBlockData());
                            }
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        if (def.isDeepslate) {
            block.getWorld().playSound(loc, Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.9f);
        } else if (def.isNether) {
            block.getWorld().playSound(loc, Sound.BLOCK_NETHERRACK_PLACE, 1.0f, 0.9f);
        } else if (def.isBlock) {
            block.getWorld().playSound(loc, Sound.BLOCK_METAL_PLACE, 1.0f, 1.0f);
        } else {
            block.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.9f);
        }
        block.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.6f, 1.3f);
    }

    // ===== ПРОВЕРКА ИНСТРУМЕНТА И УСТАЛОСТЬ ДОБЫЧИ =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        BlockDef def = getByBlock(block);
        if (def == null) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        if (!isToolEligible(tool, def.minTool)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 2, false, false, false));
            player.sendActionBar(ColorUtils.colorize("&c[Artifex] &7Требуется &e" + getToolName(def.minTool) + " &7для добычи!"));
            player.playSound(loc, Sound.BLOCK_STONE_HIT, 0.8f, 0.7f);
        } else {
            Sound hitSound = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_HIT : Sound.BLOCK_STONE_HIT;
            player.playSound(loc, hitSound, 0.9f, 1.0f);
        }
    }

    // ===== РАЗРУШЕНИЕ, ЧАСТИЦЫ И ДРОП =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockDef def = getByBlock(block);
        if (def == null) return;

        event.setDropItems(false);
        event.setExpToDrop(0);

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

        if (!isToolEligible(tool, def.minTool)) {
            Sound failSound = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_BREAK : Sound.BLOCK_STONE_BREAK;
            loc.getWorld().playSound(loc, failSound, 1.0f, 0.6f);
            player.sendActionBar(ColorUtils.colorize("&c[Artifex] &7Руда рассыпалась в пыль без &e" + getToolName(def.minTool) + "&7!"));
            return;
        }

        Sound baseBreak = def.isDeepslate ? Sound.BLOCK_DEEPSLATE_BREAK : Sound.BLOCK_STONE_BREAK;
        loc.getWorld().playSound(loc, baseBreak, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.9f, 1.2f);

        Material stoneParticle = def.isDeepslate ? Material.DEEPSLATE : Material.STONE;
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 30, 0.3, 0.3, 0.3, stoneParticle.createBlockData());
        if (def.sparkleColor != null) {
            Particle.DustOptions dust = new Particle.DustOptions(def.sparkleColor, 1.2f);
            loc.getWorld().spawnParticle(Particle.DUST, loc, 25, 0.35, 0.35, 0.35, dust);
        }

        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        if (hasSilkTouch) {
            ItemStack blockDrop = getCustomItemStack(def.blockId, 1);
            if (blockDrop != null) {
                loc.getWorld().dropItemNaturally(loc, blockDrop);
            }
        } else {
            int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
            int multiplier = 1;
            if (fortune > 0) {
                int bonus = RANDOM.nextInt(fortune + 2) - 1;
                if (bonus > 0) multiplier += bonus;
            }
            int baseCount = def.minDrop + (def.maxDrop > def.minDrop ? RANDOM.nextInt(def.maxDrop - def.minDrop + 1) : 0);
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

    // ===== ПРЕДОТВРАЩЕНИЕ НАСТРОЙКИ НОТЫ И ИНСПЕКЦИЯ =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        BlockDef def = getByBlock(block);
        if (def != null) {
            event.setCancelled(true);
            if (event.getPlayer().isSneaking()) {
                event.getPlayer().sendActionBar(ColorUtils.colorize("&6✦ &f" + def.displayName + " &8| &7Добыча: &e" + getToolName(def.minTool)));
            }
        }
    }

    // ===== ЗАЩИТА ОТ РЕДСТОУНА, ПОРШНЕЙ И ФИЗИКИ =====

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (getByBlock(event.getBlock()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.NOTE_BLOCK && getByBlock(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.NOTE_BLOCK && getByBlock(b) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.NOTE_BLOCK && getByBlock(b) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNeighborBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        for (BlockFace face : new BlockFace[]{
                BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
        }) {
            Block neighbor = broken.getRelative(face);
            if (neighbor.getType() == Material.NOTE_BLOCK) {
                BlockDef def = getByBlock(neighbor);
                if (def != null) {
                    try {
                        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
                        if (plugin != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (neighbor.getType() == Material.NOTE_BLOCK) {
                                    setBlockData(neighbor, def);
                                }
                            });
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private static void setBlockData(Block block, BlockDef def) {
        block.setType(Material.NOTE_BLOCK, false);
        if (block.getBlockData() instanceof NoteBlock nb) {
            nb.setInstrument(def.instrument);
            nb.setNote(new Note(def.note));
            nb.setPowered(def.powered);
            block.setBlockData(nb, false);
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        try {
            String id = ItemAPI.getCustomItemId(item);
            if (id != null && !id.isEmpty()) return id;
        } catch (Throwable ignored) {}
        try {
            if (Main.getInstance() != null && Main.getInstance().getItemHandler() != null) {
                String id = Main.getInstance().getItemHandler().getCustomItemId(item);
                if (id != null && !id.isEmpty()) return id;
            }
        } catch (Throwable ignored) {}
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("ID: ")) {
                        int idx = line.indexOf("ID: ");
                        String id = line.substring(idx + 4).trim();
                        id = ChatColor.stripColor(id).trim();
                        return id;
                    }
                }
            }
        }
        return null;
    }

    private ItemStack getCustomItemStack(String id, int amount) {
        if (id == null) return null;
        try {
            if (Main.getInstance() != null && Main.getInstance().getItemHandler() != null) {
                CustomItem ci = Main.getInstance().getItemHandler().getCustomItem(id);
                if (ci != null) {
                    ItemStack stack = ci.getItemStack();
                    if (stack != null) {
                        stack = stack.clone();
                        stack.setAmount(amount);
                        return stack;
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            if (Main.getInstance() != null && Main.getInstance().getApiItemRegistry() != null) {
                var javaItem = Main.getInstance().getApiItemRegistry().getItem(id);
                if (javaItem != null) {
                    ItemStack stack = javaItem.createItemStack();
                    if (stack != null) {
                        stack = stack.clone();
                        stack.setAmount(amount);
                        return stack;
                    }
                }
            }
        } catch (Throwable ignored) {}
        Material mat = Material.matchMaterial(id);
        if (mat != null) {
            return new ItemStack(mat, amount);
        }
        return null;
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
}
