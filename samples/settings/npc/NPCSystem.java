package me.dcplugin.artifex.items;

import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.utils.ColorUtils;
import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Полнофункциональная система NPC для Artifex.
 * Поддерживает аутентичные скины игроков (PLAYER / MANNEQUIN) со всеми 3D слоями одежды,
 * любых мобов, команды при клике, продвинутый визуальный GUI торговли и поворот головы за игроком.
 */
public class NPCSystem extends CustomCommand implements Listener {

    private static final String C_BLUE = "&#00C2FF";
    private static final String C_GRAY = "&7";
    private static final String C_WHITE = "&f";
    private static final String GUI_TITLE = ColorUtils.colorize("&8Настройка торговли NPC");

    // Слоты для отображения уже существующих трейдов в GUI
    private static final int[] TRADE_DISPLAY_SLOTS = {
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private Connection connection;
    private final NamespacedKey npcKey = new NamespacedKey("artifex", "custom_npc");

    // Кэши данных
    private final Map<String, String> commandCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> tradeCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> editingTrade = new ConcurrentHashMap<>();
    private final Map<UUID, String> npcSkins = new ConcurrentHashMap<>();
    private final Map<String, List<MerchantRecipe>> recipesCache = new ConcurrentHashMap<>();
    private final ExecutorService databaseWriter = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Artifex-NPC-Database");
        thread.setDaemon(true);
        return thread;
    });

    private org.bukkit.scheduler.BukkitTask trackerTask;

    public NPCSystem() {
        super("npc", "Управление NPC", "/npc <create|remove|command|type|skin|trade>", "artifex.admin");
        setupDatabase();
        loadAllDataToCache();

        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            startNPCTracker(plugin);
            // Исправляем невидимых NPC от предыдущих версий
            fixExistingNPCs(plugin);
        }
    }

    @Override
    public void onUnregister() {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        databaseWriter.shutdown();
        try {
            if (!databaseWriter.awaitTermination(5, TimeUnit.SECONDS)) databaseWriter.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            databaseWriter.shutdownNow();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable ignored) {}
            connection = null;
        }
        commandCache.clear();
        tradeCache.clear();
        clickCooldown.clear();
        editingTrade.clear();
        npcSkins.clear();
    }

    // =====================================================================
    // БАЗА ДАННЫХ
    // =====================================================================
    private void setupDatabase() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
            if (plugin == null) return;
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            File dbFile = new File(dataFolder, "items/settings/npc/npcs.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA busy_timeout=5000");
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS npcs (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "name TEXT, " +
                        "command TEXT, " +
                        "trade_enabled INTEGER DEFAULT 0, " +
                        "type TEXT DEFAULT 'VILLAGER', " +
                        "trades TEXT DEFAULT '', " +
                        "skin TEXT DEFAULT '')");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS npc_skins (" +
                        "skin_name TEXT PRIMARY KEY, " +
                        "texture TEXT, " +
                        "signature TEXT, " +
                        "updated_at INTEGER)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAllDataToCache() {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            // Загружаем NPC
            try (ResultSet rs = stmt.executeQuery("SELECT uuid, command, trade_enabled, skin, trades FROM npcs")) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    commandCache.put(uuid, rs.getString("command") != null ? rs.getString("command") : "");
                    tradeCache.put(uuid, rs.getInt("trade_enabled") == 1);
                    recipesCache.put(uuid, deserializeRecipes(rs.getString("trades")));
                    String skin = rs.getString("skin");
                    if (skin != null && !skin.isEmpty()) {
                        try {
                            npcSkins.put(UUID.fromString(uuid), skin);
                        } catch (Throwable ignored) {}
                    }
                }
            }
            // Загружаем кэшированные скины в память
            try (ResultSet rs = stmt.executeQuery("SELECT skin_name, texture, signature FROM npc_skins")) {
                while (rs.next()) {
                    String sName = rs.getString("skin_name");
                    String tex = rs.getString("texture");
                    String sig = rs.getString("signature");
                    if (sName != null && tex != null) {
                        NMSFakePlayer.SKIN_CACHE.put(sName.toLowerCase(Locale.ROOT), new String[]{tex, sig});
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Восстановление и фикс NPC, оставшихся невидимыми от предыдущих сборок.
     */
    private void fixExistingNPCs(Plugin plugin) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) continue;
                    String type = entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
                    if (type == null) continue;

                    // Гарантируем видимость
                    if (entity.isInvisible()) {
                        entity.setInvisible(false);
                    }

                    // Если NPC должен быть PLAYER, но заспавнен как обычный моб - заменяем на Mannequin
                    if (type.startsWith("PLAYER") && !(entity instanceof Mannequin)) {
                        String skinName = type.contains(":") ? type.split(":", 2)[1] : "Steve";
                        Location loc = entity.getLocation();
                        String name = entity.getCustomName();
                        String oldUUID = entity.getUniqueId().toString();

                        String cmd = commandCache.remove(oldUUID);
                        Boolean trade = tradeCache.remove(oldUUID);
                        List<MerchantRecipe> recipes = loadRecipes(oldUUID);

                        entity.remove();

                        Mannequin mannequin = (Mannequin) loc.getWorld().spawnEntity(loc, EntityType.MANNEQUIN);
                        mannequin.setAI(false);
                        mannequin.setInvulnerable(true);
                        mannequin.setSilent(true);
                        mannequin.setCustomName(name);
                        mannequin.setCustomNameVisible(true);
                        mannequin.setInvisible(false);
                        mannequin.setImmovable(true);
                        mannequin.setSkinParts(SkinParts.allParts());
                        mannequin.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, "PLAYER:" + skinName);

                        applyMannequinSkin(mannequin, skinName, null);

                        String newUUID = mannequin.getUniqueId().toString();
                        if (cmd != null) commandCache.put(newUUID, cmd);
                        if (trade != null) tradeCache.put(newUUID, trade);
                        if (!recipes.isEmpty()) saveRecipes(newUUID, recipes);
                        npcSkins.put(mannequin.getUniqueId(), skinName);

                        deleteNPCFromDB(oldUUID);
                        saveNPCToDB(newUUID, name != null ? name : "NPC", cmd != null ? cmd : "", "PLAYER", trade != null && trade, skinName);
                    } else if (entity instanceof Mannequin mannequin) {
                        String skinName = type.contains(":") ? type.split(":", 2)[1] : "Steve";
                        applyMannequinSkin(mannequin, skinName, null);
                    }
                }
            }
        }, 20L);
    }

    // =====================================================================
    // ТРЕКЕР: ПЛАВНЫЙ ПОВОРОТ ГОЛОВЫ NPC К БЛИЖАЙШЕМУ ИГРОКУ
    // =====================================================================
    private void startNPCTracker(Plugin plugin) {
        if (trackerTask != null) {
            trackerTask.cancel();
            trackerTask = null;
        }
        trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location pEye = player.getEyeLocation();
                for (Entity entity : player.getNearbyEntities(8.0, 5.0, 8.0)) {
                    if (!entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) continue;
                    try {
                        entity.lookAt(pEye.getX(), pEye.getY(), pEye.getZ(), LookAnchor.EYES);
                    } catch (Throwable ignored) {
                        Location eLoc = entity.getLocation();
                        Vector dir = pEye.toVector().subtract(eLoc.toVector().add(new Vector(0, entity.getHeight() * 0.85, 0)));
                        eLoc.setDirection(dir);
                        entity.setRotation(eLoc.getYaw(), eLoc.getPitch());
                    }
                }
            }
        }, 10L, 4L);
    }

    // =====================================================================
    // УСТАНОВКА СКИНА НА MANNEQUIN (NATIVE MINECRAFT 26.3)
    // =====================================================================
    public static class TradeEditorHolder implements org.bukkit.inventory.InventoryHolder {
        private final UUID npcUUID;
        public TradeEditorHolder(UUID npcUUID) {
            this.npcUUID = npcUUID;
        }
        public UUID getNpcUUID() {
            return npcUUID;
        }
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private void applyMannequinSkin(Mannequin mannequin, String skinName, Runnable onComplete) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin == null) return;

        mannequin.setSkinParts(SkinParts.allParts());

        String targetSkin = (skinName == null || skinName.isEmpty()) ? "Steve" : skinName;

        NMSFakePlayer.fetchSkinAsync(targetSkin, connection, skinData -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!mannequin.isValid()) return;
                try {
                    String[] data = skinData != null ? skinData : NMSFakePlayer.STEVE_SKIN;
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), targetSkin);
                    profile.setProperty(new ProfileProperty("textures", data[0], data[1] != null ? data[1] : ""));
                    mannequin.setSkinParts(SkinParts.allParts());
                    mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
                } catch (Throwable e) {
                    try {
                        PlayerProfile profile = Bukkit.createProfile(targetSkin);
                        profile.complete();
                        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
                    } catch (Throwable ignored) {}
                }
                if (onComplete != null) onComplete.run();
            });
        });
    }

    // =====================================================================
    // КОМАНДЫ /npc
    // =====================================================================
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        if (args.length == 0) {
            msg(player, ColorUtils.colorize(C_BLUE + "=== Artifex NPC ==="));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc create <имя> [PLAYER|моб] &7- Создать NPC"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc type <PLAYER|моб> &7- Сменить модель (PLAYER = 3D модель игрока)"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc skin <ник> &7- Установить скин игрока (Mojang API)"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc command <команда> &7- Задать команду при клике"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc trade &7- Визуальный GUI редактор торговли"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc trade <on|off> &7- Вкл/Выкл торговлю"));
            msg(player, ColorUtils.colorize(C_BLUE + "/npc remove &7- Удалить выбранного NPC"));
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                msg(player, ColorUtils.colorize("&cИспользование: /npc create <имя> [PLAYER|моб]"));
                return true;
            }
            String name = args[1];
            String coloredName = ColorUtils.colorize(name);

            boolean isPlayer = false;
            EntityType spawnType = EntityType.VILLAGER;
            if (args.length > 2) {
                String reqType = args[2].toUpperCase(Locale.ROOT);
                if (reqType.equals("PLAYER") || reqType.equals("MANNEQUIN")) {
                    isPlayer = true;
                    spawnType = EntityType.MANNEQUIN;
                } else {
                    try {
                        spawnType = EntityType.valueOf(reqType);
                    } catch (Exception ignored) {}
                }
            }

            Location pLoc = player.getLocation();
            org.bukkit.util.Vector dir = pLoc.getDirection().setY(0);
            if (dir.lengthSquared() < 0.01) {
                dir = new org.bukkit.util.Vector(0, 0, 1);
            } else {
                dir.normalize();
            }
            Location spawnLoc = pLoc.clone().add(dir.multiply(1.5));
            spawnLoc.setDirection(pLoc.toVector().subtract(spawnLoc.toVector()));

            LivingEntity npc = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, spawnType);
            npc.setAI(false);
            npc.setInvulnerable(true);
            npc.setSilent(true);
            npc.setCustomName(coloredName);
            npc.setCustomNameVisible(true);
            npc.setInvisible(false);

            String uuidStr = npc.getUniqueId().toString();
            commandCache.put(uuidStr, "");
            tradeCache.put(uuidStr, false);

            if (npc instanceof Mannequin mannequin) {
                String defaultSkin = player.getName();
                mannequin.setImmovable(true);
                mannequin.setSkinParts(SkinParts.allParts());
                applyMannequinSkin(mannequin, defaultSkin, null);
                npc.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, "PLAYER:" + defaultSkin);
                npcSkins.put(npc.getUniqueId(), defaultSkin);
                saveNPCToDB(uuidStr, coloredName, "", "PLAYER", false, defaultSkin);
            } else {
                if (npc instanceof Villager villager) {
                    villager.setProfession(Villager.Profession.NITWIT);
                }
                npc.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, spawnType.name());
                saveNPCToDB(uuidStr, coloredName, "", spawnType.name(), false, "");
            }

            msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "NPC &f" + coloredName + " &7успешно создан! (Модель: &f" + (isPlayer ? "PLAYER" : spawnType.name()) + "&7)"));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            return true;
        }

        Entity target = getTargetNPCEntity(player);

        if (args[0].equalsIgnoreCase("remove")) {
            if (target == null) {
                msg(player, ColorUtils.colorize("&cПодойдите и наведитесь на NPC."));
                return true;
            }
            String uuidStr = target.getUniqueId().toString();
            target.remove();
            commandCache.remove(uuidStr);
            tradeCache.remove(uuidStr);
            npcSkins.remove(target.getUniqueId());
            deleteNPCFromDB(uuidStr);
            msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "NPC успешно &cудален&7."));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
            return true;
        }

        if (args[0].equalsIgnoreCase("command")) {
            if (target == null) {
                msg(player, ColorUtils.colorize("&cПодойдите и наведитесь на NPC."));
                return true;
            }
            if (args.length < 2) {
                commandCache.put(target.getUniqueId().toString(), "");
                updateNPCCommandInDB(target.getUniqueId().toString(), "");
                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Команда NPC &cочищена&7."));
                return true;
            }
            String command = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (command.startsWith("/")) command = command.substring(1);
            commandCache.put(target.getUniqueId().toString(), command);
            updateNPCCommandInDB(target.getUniqueId().toString(), command);
            msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Команда NPC задана: &f/" + command));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            return true;
        }

        if (args[0].equalsIgnoreCase("type")) {
            if (target == null) {
                msg(player, ColorUtils.colorize("&cПодойдите и наведитесь на NPC."));
                return true;
            }
            if (args.length < 2) {
                msg(player, ColorUtils.colorize("&cИспользование: /npc type <PLAYER|моб>"));
                return true;
            }
            String typeName = args[1].toUpperCase(Locale.ROOT);
            Location loc = target.getLocation();
            String customName = target.getCustomName();
            String oldUUID = target.getUniqueId().toString();
            String cmd = commandCache.remove(oldUUID);
            Boolean trade = tradeCache.remove(oldUUID);
            List<MerchantRecipe> recipes = loadRecipes(oldUUID);

            // --- СМЕНА НА МОДЕЛЬ ИГРОКА (PLAYER / MANNEQUIN) ---
            if (typeName.equals("PLAYER") || typeName.equals("MANNEQUIN")) {
                String skinName = npcSkins.getOrDefault(target.getUniqueId(), "Steve");
                target.remove();

                Mannequin mannequin = (Mannequin) loc.getWorld().spawnEntity(loc, EntityType.MANNEQUIN);
                mannequin.setAI(false);
                mannequin.setInvulnerable(true);
                mannequin.setSilent(true);
                mannequin.setCustomName(customName);
                mannequin.setCustomNameVisible(true);
                mannequin.setInvisible(false);
                mannequin.setImmovable(true);
                mannequin.setSkinParts(SkinParts.allParts());
                mannequin.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, "PLAYER:" + skinName);

                applyMannequinSkin(mannequin, skinName, null);

                String newUUID = mannequin.getUniqueId().toString();
                if (cmd != null) commandCache.put(newUUID, cmd);
                if (trade != null) tradeCache.put(newUUID, trade);
                if (!recipes.isEmpty()) saveRecipes(newUUID, recipes);
                npcSkins.put(mannequin.getUniqueId(), skinName);

                deleteNPCFromDB(oldUUID);
                saveNPCToDB(newUUID, customName != null ? customName : "NPC", cmd != null ? cmd : "", "PLAYER", trade != null && trade, skinName);

                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Модель NPC изменена на &fPLAYER!"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                return true;
            }

            // --- СМЕНА НА ОБЫЧНОГО МОБА ---
            try {
                EntityType newType = EntityType.valueOf(typeName);
                Class<?> clazz = newType.getEntityClass();
                if (clazz == null || !LivingEntity.class.isAssignableFrom(clazz) || newType == EntityType.PLAYER) {
                    msg(player, ColorUtils.colorize("&cЭтот тип не поддерживается."));
                    return true;
                }

                target.remove();

                LivingEntity newEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, newType);
                newEntity.setAI(false);
                newEntity.setInvulnerable(true);
                newEntity.setSilent(true);
                newEntity.setCustomName(customName);
                newEntity.setCustomNameVisible(true);
                newEntity.setInvisible(false);
                if (newEntity instanceof Villager villager) {
                    villager.setProfession(Villager.Profession.NITWIT);
                }
                newEntity.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, newType.name());

                String newUUID = newEntity.getUniqueId().toString();
                if (cmd != null) commandCache.put(newUUID, cmd);
                if (trade != null) tradeCache.put(newUUID, trade);
                if (!recipes.isEmpty()) saveRecipes(newUUID, recipes);

                deleteNPCFromDB(oldUUID);
                saveNPCToDB(newUUID, customName != null ? customName : "NPC", cmd != null ? cmd : "", newType.name(), trade != null && trade, "");

                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Модель NPC изменена на &f" + newType.name()));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            } catch (Exception e) {
                msg(player, ColorUtils.colorize("&cНеверный тип моба. Пример: /npc type VILLAGER"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("skin")) {
            if (target == null) {
                msg(player, ColorUtils.colorize("&cПодойдите и наведитесь на NPC."));
                return true;
            }
            if (args.length < 2) {
                msg(player, ColorUtils.colorize("&cИспользование: /npc skin <ник игрока>"));
                return true;
            }
            String skinName = args[1];

            if (target instanceof Mannequin mannequin) {
                target.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, "PLAYER:" + skinName);
                npcSkins.put(target.getUniqueId(), skinName);
                updateSkinInDB(target.getUniqueId().toString(), skinName);

                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Загрузка скина &f" + skinName + "&7..."));
                applyMannequinSkin(mannequin, skinName, () -> {
                    msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Скин &a" + skinName + " &7успешно применён!"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                });
                return true;
            }

            // Для обычных мобов надеваем голову игрока
            if (target instanceof LivingEntity livingNpc) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(skinName));
                    head.setItemMeta(meta);
                }
                livingNpc.getEquipment().setHelmet(head);
                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Голова скина &f" + skinName + " &7надета на моба."));
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("trade")) {
            if (target == null) {
                msg(player, ColorUtils.colorize("&cПодойдите и наведитесь на NPC."));
                return true;
            }

            String targetUUID = target.getUniqueId().toString();

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("on")) {
                    tradeCache.put(targetUUID, true);
                    updateTradeStatusInDB(targetUUID, true);
                    msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Торговля NPC: &aВКЛЮЧЕНА"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1f);
                    return true;
                }
                if (args[1].equalsIgnoreCase("off")) {
                    tradeCache.put(targetUUID, false);
                    updateTradeStatusInDB(targetUUID, false);
                    msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Торговля NPC: &cВЫКЛЮЧЕНА"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return true;
                }
            }

            // Автоматически включаем торговлю при открытии редактора
            tradeCache.put(targetUUID, true);
            updateTradeStatusInDB(targetUUID, true);

            openTradeGUI(player, target);
            return true;
        }

        return true;
    }

    // =====================================================================
    // GUI РЕДАКТОР ТОРГОВЛИ
    // =====================================================================
    private void openTradeGUI(Player player, Entity npc) {
        TradeEditorHolder holder = new TradeEditorHolder(npc.getUniqueId());
        Inventory gui = Bukkit.createInventory(holder, 54, GUI_TITLE);

        ItemStack darkGlass = createNamedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, darkGlass);
        }

        // Пустые слоты под предметы оплаты и результат
        gui.setItem(10, null);
        gui.setItem(12, null);
        gui.setItem(16, null);

        // Информационные иконки
        gui.setItem(9, createNamedItem(Material.PAPER, ColorUtils.colorize("&#00C2FF| &7Предмет для оплаты (1)")));
        gui.setItem(11, createNamedItem(Material.PAPER, ColorUtils.colorize("&#00C2FF| &7Предмет для оплаты (2) &8(опционально)")));
        gui.setItem(15, createNamedItem(Material.PAPER, ColorUtils.colorize("&#00C2FF| &7Что получит игрок")));

        // Кнопка добавления трейда
        gui.setItem(49, createNamedItem(Material.EMERALD_BLOCK, ColorUtils.colorize("&#00C2FF&lДОБАВИТЬ ТРЕЙД")));

        // Кнопка удаления всех трейдов
        gui.setItem(45, createNamedItem(Material.BARRIER, ColorUtils.colorize("&c&lУДАЛИТЬ ВСЕ ТРЕЙДЫ")));

        // Заголовок текущих трейдов
        gui.setItem(22, createNamedItem(Material.BOOK, ColorUtils.colorize("&#00C2FF&lТЕКУЩИЕ ТРЕЙДЫ NPC")));

        // Заполняем текущие трейды
        List<MerchantRecipe> recipes = loadRecipes(npc.getUniqueId().toString());
        populateTradesInGUI(gui, recipes);

        editingTrade.put(player.getUniqueId(), npc.getUniqueId());
        player.openInventory(gui);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
    }

    private void populateTradesInGUI(Inventory gui, List<MerchantRecipe> recipes) {
        ItemStack emptyGlass = createNamedItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : TRADE_DISPLAY_SLOTS) {
            gui.setItem(slot, emptyGlass);
        }

        for (int i = 0; i < recipes.size() && i < TRADE_DISPLAY_SLOTS.length; i++) {
            MerchantRecipe recipe = recipes.get(i);
            ItemStack display = recipe.getResult().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&8------------------------"));
                ItemStack in1 = recipe.getIngredients().get(0);
                lore.add(ColorUtils.colorize("&7Оплата (1): &f" + in1.getAmount() + "x " + formatItemName(in1)));
                if (recipe.getIngredients().size() > 1) {
                    ItemStack in2 = recipe.getIngredients().get(1);
                    lore.add(ColorUtils.colorize("&7Оплата (2): &f" + in2.getAmount() + "x " + formatItemName(in2)));
                }
                lore.add(ColorUtils.colorize("&aРезультат: &f" + display.getAmount() + "x " + formatItemName(display)));
                lore.add(" ");
                lore.add(ColorUtils.colorize("&c▶ Нажмите ЛКМ, чтобы удалить трейд"));
                lore.add(ColorUtils.colorize("&8------------------------"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            gui.setItem(TRADE_DISPLAY_SLOTS[i], display);
        }
    }

    private String formatItemName(ItemStack item) {
        if (item == null) return "Ничего";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private ItemStack createNamedItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeEditorHolder holder)) return;
        UUID npcUUID = holder.getNpcUUID();

        // Если клик в рюкзаке игрока
        if (event.getClickedInventory() == player.getInventory()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    if (top.getItem(10) == null || top.getItem(10).getType() == Material.AIR) {
                        top.setItem(10, clicked.clone());
                        event.setCurrentItem(null);
                    } else if (top.getItem(12) == null || top.getItem(12).getType() == Material.AIR) {
                        top.setItem(12, clicked.clone());
                        event.setCurrentItem(null);
                    } else if (top.getItem(16) == null || top.getItem(16).getType() == Material.AIR) {
                        top.setItem(16, clicked.clone());
                        event.setCurrentItem(null);
                    }
                }
            }
            return;
        }

        // Если клик в окне редактора торговли
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            int slot = event.getSlot();

            // Слоты 10, 12, 16 - полностью интерактивны
            if (slot == 10 || slot == 12 || slot == 16) {
                return;
            }

            event.setCancelled(true);

            // Кнопка 49: ДОБАВИТЬ ТРЕЙД
            if (slot == 49) {
                Inventory inv = event.getView().getTopInventory();
                ItemStack in1 = inv.getItem(10);
                ItemStack in2 = inv.getItem(12);
                ItemStack out = inv.getItem(16);

                if (in1 == null || in1.getType() == Material.AIR || out == null || out.getType() == Material.AIR) {
                    msg(player, ColorUtils.colorize("&cПоложите предмет оплаты (1) и предмет получения!"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }

                MerchantRecipe recipe = new MerchantRecipe(out.clone(), 9999);
                recipe.addIngredient(in1.clone());
                if (in2 != null && in2.getType() != Material.AIR) {
                    recipe.addIngredient(in2.clone());
                }

                List<MerchantRecipe> recipes = loadRecipes(npcUUID.toString());
                recipes.add(recipe);
                saveRecipes(npcUUID.toString(), recipes);

                inv.setItem(10, null);
                inv.setItem(12, null);
                inv.setItem(16, null);

                populateTradesInGUI(inv, recipes);
                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Трейд успешно &aдобавлен&7!"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                return;
            }

            // Кнопка 45: УДАЛИТЬ ВСЕ ТРЕЙДЫ
            if (slot == 45) {
                saveRecipes(npcUUID.toString(), new ArrayList<>());
                Inventory inv = event.getView().getTopInventory();
                populateTradesInGUI(inv, Collections.emptyList());
                msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Все трейды &cудалены&7!"));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                return;
            }

            // Удаление одного трейда по клику
            for (int i = 0; i < TRADE_DISPLAY_SLOTS.length; i++) {
                if (TRADE_DISPLAY_SLOTS[i] == slot) {
                    List<MerchantRecipe> recipes = loadRecipes(npcUUID.toString());
                    if (i < recipes.size()) {
                        recipes.remove(i);
                        saveRecipes(npcUUID.toString(), recipes);
                        populateTradesInGUI(event.getView().getTopInventory(), recipes);
                        msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "Трейд &cудален&7!"));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1.5f);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof TradeEditorHolder holder)) return;
        UUID npcUUID = holder.getNpcUUID();

        // Возвращаем предметы из слотов оплаты и результата
        for (int slot : new int[]{10, 12, 16}) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
        editingTrade.remove(player.getUniqueId());
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 1f, 1.2f);
    }

    @EventHandler
    public void onGUIDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof TradeEditorHolder)) return;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 54) {
                if (rawSlot != 10 && rawSlot != 12 && rawSlot != 16) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // =====================================================================
    // ВЗАИМОДЕЙСТВИЕ С NPC (КЛИК ПРАВОЙ КНОПКОЙ)
    // =====================================================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), event.getHand());
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onNPCInteractAt(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), event.getHand());
        event.setCancelled(true);
    }

    private void handleInteract(Player player, Entity entity, EquipmentSlot hand) {
        if (hand != EquipmentSlot.HAND) return;
        if (!entity.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) return;

        long now = System.currentTimeMillis();
        if (now - clickCooldown.getOrDefault(player.getUniqueId(), 0L) < 500) return;
        clickCooldown.put(player.getUniqueId(), now);

        String uuidStr = entity.getUniqueId().toString();
        boolean isTradeEnabled = tradeCache.getOrDefault(uuidStr, false);

        // 1. Открытие торговли (работает для MANNEQUIN, VILLAGER и всех мобов)
        if (isTradeEnabled) {
            List<MerchantRecipe> recipes = recipesCache.get(uuidStr);
            if (recipes != null && !recipes.isEmpty()) {
                recipes = cloneRecipes(recipes);
                String title = entity.getCustomName() != null ? entity.getCustomName() : "Торговец";
                Merchant merchant = Bukkit.createMerchant(ColorUtils.colorize(title));
                merchant.setRecipes(recipes);
                player.openMerchant(merchant, true);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
                return;
            }
        }

        // 2. Выполнение команды при клике
        String command = commandCache.getOrDefault(uuidStr, "");
        if (!command.isEmpty()) {
            String finalCmd = command.replace("%player%", player.getName());
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
            if (plugin != null) {
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(finalCmd));
            }
        } else {
            msg(player, ColorUtils.colorize(C_BLUE + "| " + C_GRAY + "У этого NPC пока нет настроенных команд или торговли."));
        }
    }

    @EventHandler
    public void onNPCDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    // =====================================================================
    // УМНЫЙ ПОИСК NPC ПРИ НАВЕДЕНИИ
    // =====================================================================
    private Entity getTargetNPCEntity(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                6.0,
                0.6,
                e -> e != player && e.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)
        );
        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity();
        }

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(4.5, 4.5, 4.5)) {
            if (e.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) {
                double d = e.getLocation().distanceSquared(player.getLocation());
                if (d < closestDist) {
                    closestDist = d;
                    closest = e;
                }
            }
        }
        return closest;
    }

    // =====================================================================
    // РЕЦЕПТЫ ТОРГОВЛИ (СЕРИАЛИЗАЦИЯ И ДЕСЕРИАЛИЗАЦИЯ)
    // =====================================================================
    private List<MerchantRecipe> loadRecipes(String npcUUID) {
        List<MerchantRecipe> cached = recipesCache.get(npcUUID);
        if (cached != null) return cloneRecipes(cached);

        List<MerchantRecipe> recipes = new ArrayList<>();
        if (connection == null) return recipes;
        try (PreparedStatement ps = connection.prepareStatement("SELECT trades FROM npcs WHERE uuid = ?")) {
            ps.setString(1, npcUUID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String tradesStr = rs.getString("trades");
                    if (tradesStr != null && !tradesStr.isEmpty()) {
                        String[] parts = tradesStr.split(";;");
                        for (String part : parts) {
                            String[] items = part.split("\\|\\|");
                            if (items.length >= 2) {
                                ItemStack in1 = deserializeItem(items[0]);
                                ItemStack out = deserializeItem(items[1]);
                                ItemStack in2 = items.length > 2 ? deserializeItem(items[2]) : null;
                                if (in1 != null && out != null) {
                                    MerchantRecipe recipe = new MerchantRecipe(out, 9999);
                                    recipe.addIngredient(in1);
                                    if (in2 != null && in2.getType() != Material.AIR) recipe.addIngredient(in2);
                                    recipes.add(recipe);
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        recipesCache.put(npcUUID, cloneRecipes(recipes));
        return cloneRecipes(recipes);
    }

    private List<MerchantRecipe> deserializeRecipes(String tradesStr) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        if (tradesStr == null || tradesStr.isEmpty()) return recipes;
        for (String part : tradesStr.split(";;")) {
            String[] items = part.split("\\|\\|");
            if (items.length < 2) continue;
            ItemStack in1 = deserializeItem(items[0]);
            ItemStack out = deserializeItem(items[1]);
            ItemStack in2 = items.length > 2 ? deserializeItem(items[2]) : null;
            if (in1 == null || out == null) continue;
            MerchantRecipe recipe = new MerchantRecipe(out, 9999);
            recipe.addIngredient(in1);
            if (in2 != null && in2.getType() != Material.AIR) recipe.addIngredient(in2);
            recipes.add(recipe);
        }
        return recipes;
    }

    private List<MerchantRecipe> cloneRecipes(List<MerchantRecipe> recipes) {
        List<MerchantRecipe> copy = new ArrayList<>(recipes.size());
        for (MerchantRecipe recipe : recipes) {
            MerchantRecipe clone = new MerchantRecipe(recipe.getResult().clone(), recipe.getMaxUses());
            clone.setUses(recipe.getUses());
            for (ItemStack ingredient : recipe.getIngredients()) clone.addIngredient(ingredient.clone());
            copy.add(clone);
        }
        return copy;
    }

    private void saveRecipes(String npcUUID, List<MerchantRecipe> recipes) {
        recipesCache.put(npcUUID, cloneRecipes(recipes));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            sb.append(serializeItem(recipe.getIngredients().get(0))).append("||")
              .append(serializeItem(recipe.getResult())).append("||")
              .append(recipe.getIngredients().size() > 1 ? serializeItem(recipe.getIngredients().get(1)) : "");
            if (i < recipes.size() - 1) sb.append(";;");
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            databaseWriter.execute(() -> {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE npcs SET trades = ? WHERE uuid = ?")) {
                    ps.setString(1, sb.toString());
                    ps.setString(2, npcUUID);
                    ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    private String serializeItem(ItemStack item) {
        if (item == null) return "";
        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
            os.writeObject(item);
            os.flush();
            return Base64.getEncoder().encodeToString(io.toByteArray());
        } catch (Exception e) { return ""; }
    }

    private ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            ByteArrayInputStream io = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream is = new BukkitObjectInputStream(io);
            return (ItemStack) is.readObject();
        } catch (Exception e) { return null; }
    }

    private void saveNPCToDB(String uuid, String name, String command, String type, boolean tradeEnabled, String skin) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO npcs (uuid, name, command, type, trade_enabled, skin) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, uuid); ps.setString(2, name); ps.setString(3, command); ps.setString(4, type);
                    ps.setInt(5, tradeEnabled ? 1 : 0); ps.setString(6, skin); ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    private void updateNPCCommandInDB(String uuid, String command) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE npcs SET command = ? WHERE uuid = ?")) {
                    ps.setString(1, command); ps.setString(2, uuid); ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    private void updateTradeStatusInDB(String uuid, boolean enabled) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE npcs SET trade_enabled = ? WHERE uuid = ?")) {
                    ps.setInt(1, enabled ? 1 : 0); ps.setString(2, uuid); ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    private void updateSkinInDB(String uuid, String skin) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (PreparedStatement ps = connection.prepareStatement("UPDATE npcs SET skin = ? WHERE uuid = ?")) {
                    ps.setString(1, skin); ps.setString(2, uuid); ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    private void deleteNPCFromDB(String uuid) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM npcs WHERE uuid = ?")) {
                    ps.setString(1, uuid); ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            });
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, int cursor) {
        if (args.length == 1) {
            return List.of("create", "remove", "command", "type", "skin", "trade").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
            List<String> types = new ArrayList<>();
            types.add("PLAYER");
            for (EntityType t : EntityType.values()) {
                Class<?> clazz = t.getEntityClass();
                if (clazz != null && LivingEntity.class.isAssignableFrom(clazz) && t != EntityType.PLAYER) {
                    types.add(t.name());
                }
            }
            return types.stream().filter(name -> name.startsWith(args[1].toUpperCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("trade")) {
            return List.of("on", "off").stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return Collections.emptyList();
    }
}
