package me.dcplugin.artifex.items;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Высокопроизводительный NMS-обработчик фейковых игроков для Paper 1.21.4 / 26.3+.
 * Поддерживает кастомные скины (Mojang API, онлайн-игроки, SQLite кэш),
 * скрытие из TAB без деспавна скина, поворот головы и правильную маску слоев скина (0x7F).
 */
public final class NMSFakePlayer {

    private static final String NMS = "net.minecraft.";
    public static final Map<String, String[]> SKIN_CACHE = new ConcurrentHashMap<>();

    public static final String[] STEVE_SKIN = new String[]{
        "ewogICJ0aW1lc3RhbXAiIDogMTYwMDAwMDAwMDAwMCwKICAicHJvZmlsZUlkIiA6ICI4NmM5MWZiNTkzNDk0ODhhYjdkMzk1YTBkNjFiYTJmNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGV2ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IGZhbHNlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMGEyYjY2ZTljYzc3ZTY4Y2E4OWYwMTY5YTQ5YmE4ZGM0Zjc3NTMwNzg4NmFhN2Q1OTkzNWUyNjc0YjhiYSIKICAgIH0KICB9Cn0=",
        ""
    };

    private NMSFakePlayer() {}

    /**
     * Создание или обновление фейкового игрока на клиенте зрителя.
     */
    public static void spawn(
            Player viewer,
            Location loc,
            int entityId,
            UUID uuid,
            String name,
            String skinName,
            Connection dbConn
    ) {
        if (viewer == null || !viewer.isOnline() || loc == null) return;

        try {
            // 1. Получаем NMS ServerPlayer зрителя и его сетевое соединение
            Object craftPlayer = viewer.getClass().getMethod("getHandle").invoke(viewer);
            Field connField = craftPlayer.getClass().getField("connection");
            Object connection = connField.get(craftPlayer);
            Class<?> packetClass = Class.forName(NMS + "network.protocol.Packet");
            Method sendMethod = connection.getClass().getMethod("send", packetClass);

            // 2. Удаляем старую сущность на клиенте, чтобы не было конфликта ID
            try {
                Class<?> removeClass = Class.forName(NMS + "network.protocol.game.ClientboundRemoveEntitiesPacket");
                Constructor<?> removeCtor = removeClass.getConstructor(int[].class);
                Object removePacket = removeCtor.newInstance(new int[]{ entityId });
                sendMethod.invoke(connection, removePacket);
            } catch (Throwable ignored) {}

            // 3. Подготавливаем GameProfile и скины
            Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
            String visibleName = name == null ? "Artifex NPC" : name;
            String rawName = ChatColor.stripColor(visibleName).replaceAll("[^A-Za-z0-9_]", "");
            if (rawName.isEmpty()) rawName = "ArtifexNPC";
            if (rawName.length() > 16) rawName = rawName.substring(0, 16);

            Object profile = profileClass.getConstructor(UUID.class, String.class).newInstance(uuid, rawName);

            // Применяем скин из кэша / БД если задан
            String[] skinData = getSkinData(skinName, dbConn);
            if (skinData != null && skinData[0] != null && !skinData[0].isEmpty()) {
                try {
                    Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
                    Object prop = propClass.getConstructor(String.class, String.class, String.class)
                            .newInstance("textures", skinData[0], skinData[1] != null ? skinData[1] : "");
                    Object properties = profileClass.getMethod("getProperties").invoke(profile);
                    Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
                    putMethod.invoke(properties, "textures", prop);
                } catch (Throwable t) {
                    Bukkit.getLogger().warning("[Artifex NPC] Ошибка применения скина: " + t.getMessage());
                }
            }

            // 4. Подготавливаем ClientInformation (0x7F = 127 включает второй слой скина: шляпу, куртку, штаны, рукава)
            Class<?> clientInfoClass = Class.forName(NMS + "server.level.ClientInformation");
            Object clientInfo = null;
            try {
                Class<?> chatVisClass = Class.forName(NMS + "world.entity.player.ChatVisiblity");
                Class<?> armClass = Class.forName(NMS + "world.entity.HumanoidArm");
                Class<?> partClass = Class.forName(NMS + "server.level.ParticleStatus");

                Constructor<?> infoCtor = clientInfoClass.getConstructor(
                        String.class, int.class, chatVisClass, boolean.class, int.class,
                        armClass, boolean.class, boolean.class, partClass
                );
                Object chatVis = chatVisClass.getField("FULL").get(null);
                Object arm = armClass.getField("RIGHT").get(null);
                Object part = partClass.getField("ALL").get(null);
                clientInfo = infoCtor.newInstance("en_us", 2, chatVis, true, 127, arm, false, true, part);
            } catch (Throwable fallback) {
                clientInfo = clientInfoClass.getMethod("createDefault").invoke(null);
            }

            // 5. Создаем настоящий NMS ServerPlayer
            Object minecraftServer = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object serverLevel = loc.getWorld().getClass().getMethod("getHandle").invoke(loc.getWorld());

            Class<?> serverPlayerClass = Class.forName(NMS + "server.level.ServerPlayer");
            Constructor<?> spCtor = null;
            for (Constructor<?> c : serverPlayerClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 4 && params[2].getName().contains("GameProfile") && params[3].getName().contains("ClientInformation")) {
                    spCtor = c;
                    break;
                }
            }
            if (spCtor == null) {
                throw new NoSuchMethodException("Не найден конструктор ServerPlayer(Server, Level, GameProfile, ClientInformation)");
            }

            Object fakePlayer = spCtor.newInstance(minecraftServer, serverLevel, profile, clientInfo);
            try {
                fakePlayer.getClass().getMethod("setId", int.class).invoke(fakePlayer, entityId);
            } catch (Throwable ignored) {}

            // Позиционирование
            fakePlayer.getClass().getMethod("setPos", double.class, double.class, double.class)
                    .invoke(fakePlayer, loc.getX(), loc.getY(), loc.getZ());
            fakePlayer.getClass().getMethod("setYRot", float.class).invoke(fakePlayer, loc.getYaw());
            fakePlayer.getClass().getMethod("setXRot", float.class).invoke(fakePlayer, loc.getPitch());
            fakePlayer.getClass().getMethod("setYHeadRot", float.class).invoke(fakePlayer, loc.getYaw());

            // Имя над головой
            try {
                Class<?> compClass = Class.forName(NMS + "network.chat.Component");
                Object comp = compClass.getMethod("literal", String.class).invoke(null, visibleName);
                fakePlayer.getClass().getMethod("setCustomName", compClass).invoke(fakePlayer, comp);
                fakePlayer.getClass().getMethod("setCustomNameVisible", boolean.class).invoke(fakePlayer, true);
            } catch (Throwable ignored) {}

            // Включаем отображение всех слоев скина в EntityData (0x7F = 127)
            try {
                Field custField = null;
                Class<?> cur = serverPlayerClass;
                while (cur != null && cur != Object.class) {
                    try {
                        custField = cur.getField("DATA_PLAYER_MODE_CUSTOMISATION");
                        break;
                    } catch (NoSuchFieldException ignored) {
                        cur = cur.getSuperclass();
                    }
                }
                if (custField != null) {
                    Object accessor = custField.get(null);
                    Object entityData = fakePlayer.getClass().getMethod("getEntityData").invoke(fakePlayer);
                    for (Method m : entityData.getClass().getMethods()) {
                        if (m.getName().equals("set") && m.getParameterCount() == 2) {
                            m.invoke(entityData, accessor, (byte) 127);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // 6. Пакет ClientboundPlayerInfoUpdatePacket (с флагом listed=false, чтобы не висел в TAB!)
            try {
                Class<?> infoPacketClass = Class.forName(NMS + "network.protocol.game.ClientboundPlayerInfoUpdatePacket");
                Class<?> actionClass = Class.forName(NMS + "network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
                Class<?> entryClass = Class.forName(NMS + "network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
                Class<?> gameTypeClass = Class.forName(NMS + "world.level.GameType");
                Object survivalMode = gameTypeClass.getField("SURVIVAL").get(null);

                Constructor<?> bestEntryCtor = null;
                for (Constructor<?> ctor : entryClass.getConstructors()) {
                    Class<?>[] pt = ctor.getParameterTypes();
                    if (pt.length >= 2 && pt[0] == UUID.class) {
                        if (bestEntryCtor == null || pt.length > bestEntryCtor.getParameterTypes().length) {
                            bestEntryCtor = ctor;
                        }
                    }
                }

                if (bestEntryCtor != null) {
                    Class<?>[] pTypes = bestEntryCtor.getParameterTypes();
                    Object[] ctorArgs = new Object[pTypes.length];
                    boolean firstBool = true;
                    for (int i = 0; i < pTypes.length; i++) {
                        Class<?> pt = pTypes[i];
                        if (pt == UUID.class) {
                            ctorArgs[i] = uuid;
                        } else if (pt.getName().contains("GameProfile")) {
                            ctorArgs[i] = profile;
                        } else if (pt == boolean.class || pt == Boolean.class) {
                            if (firstBool) {
                                ctorArgs[i] = false; // listed = false (НЕ отображать в TAB!)
                                firstBool = false;
                            } else {
                                ctorArgs[i] = true;  // showHat = true
                            }
                        } else if (pt == int.class || pt == Integer.class) {
                            ctorArgs[i] = 0; // latency / listOrder
                        } else if (pt.getName().contains("GameType")) {
                            ctorArgs[i] = survivalMode;
                        } else {
                            ctorArgs[i] = null; // Component, chatSession, etc.
                        }
                    }
                    Object entry = bestEntryCtor.newInstance(ctorArgs);

                    EnumSet actions = EnumSet.of(
                            (Enum) Enum.valueOf((Class) actionClass, "ADD_PLAYER"),
                            (Enum) Enum.valueOf((Class) actionClass, "UPDATE_LISTED"),
                            (Enum) Enum.valueOf((Class) actionClass, "UPDATE_GAME_MODE"),
                            (Enum) Enum.valueOf((Class) actionClass, "UPDATE_LATENCY"),
                            (Enum) Enum.valueOf((Class) actionClass, "UPDATE_HAT")
                    );

                    Constructor<?> infoPacketCtor = null;
                    for (Constructor<?> ctor : infoPacketClass.getConstructors()) {
                        Class<?>[] pt = ctor.getParameterTypes();
                        if (pt.length == 2 && pt[0] == EnumSet.class && (pt[1] == List.class || pt[1] == Collection.class)) {
                            infoPacketCtor = ctor;
                            break;
                        }
                    }
                    if (infoPacketCtor != null) {
                        Object infoPacket = infoPacketCtor.newInstance(actions, Collections.singletonList(entry));
                        sendMethod.invoke(connection, infoPacket);
                    }
                }
            } catch (Throwable infoErr) {
                Bukkit.getLogger().warning("[Artifex NPC] Ошибка отправки PlayerInfo: " + infoErr.getMessage());
            }

            // 7. Пакет ClientboundAddEntityPacket (появление сущности PLAYER)
            Class<?> addPacketClass = Class.forName(NMS + "network.protocol.game.ClientboundAddEntityPacket");
            Class<?> entityTypeClass = Class.forName(NMS + "world.entity.EntityType");
            Object playerType = null;
            try {
                playerType = fakePlayer.getClass().getMethod("getType").invoke(fakePlayer);
            } catch (Throwable t) {
                try {
                    playerType = Class.forName(NMS + "world.entity.EntityTypes").getField("PLAYER").get(null);
                } catch (Throwable t2) {
                    playerType = entityTypeClass.getField("PLAYER").get(null);
                }
            }
            Class<?> vec3Class = Class.forName(NMS + "world.phys.Vec3");
            Object zeroVec = vec3Class.getField("ZERO").get(null);

            Constructor<?> addCtor = addPacketClass.getConstructor(
                    int.class, UUID.class, double.class, double.class, double.class,
                    float.class, float.class, entityTypeClass, int.class, vec3Class, double.class
            );
            Object addPacket = addCtor.newInstance(
                    entityId, uuid, loc.getX(), loc.getY(), loc.getZ(),
                    loc.getPitch(), loc.getYaw(), playerType, 0, zeroVec, (double) loc.getYaw()
            );
            sendMethod.invoke(connection, addPacket);

            // 8. Пакет ClientboundRotateHeadPacket (поворот головы)
            try {
                Class<?> rotateClass = Class.forName(NMS + "network.protocol.game.ClientboundRotateHeadPacket");
                Constructor<?> rotateCtor = rotateClass.getConstructor(Class.forName(NMS + "world.entity.Entity"), byte.class);
                byte headYaw = (byte) ((loc.getYaw() * 256.0F) / 360.0F);
                Object rotatePacket = rotateCtor.newInstance(fakePlayer, headYaw);
                sendMethod.invoke(connection, rotatePacket);
            } catch (Throwable ignored) {}

            // 9. Пакет ClientboundSetEntityDataPacket (метаданные: слои скина и имя)
            try {
                Object entityData = fakePlayer.getClass().getMethod("getEntityData").invoke(fakePlayer);
                List<?> packed = (List<?>) entityData.getClass().getMethod("packAll").invoke(entityData);
                Class<?> metaClass = Class.forName(NMS + "network.protocol.game.ClientboundSetEntityDataPacket");
                Constructor<?> metaCtor = metaClass.getConstructor(int.class, List.class);
                Object metaPacket = metaCtor.newInstance(entityId, packed);
                sendMethod.invoke(connection, metaPacket);
            } catch (Throwable ignored) {}

        } catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Artifex] Ошибка спавна FakePlayer: " + e.getMessage(), e);
        }
    }

    /**
     * Удаление фейкового игрока на клиенте зрителя.
     */
    public static void despawn(Player viewer, int entityId, UUID uuid) {
        if (viewer == null || !viewer.isOnline()) return;
        try {
            Object craftPlayer = viewer.getClass().getMethod("getHandle").invoke(viewer);
            Field connField = craftPlayer.getClass().getField("connection");
            Object connection = connField.get(craftPlayer);
            Class<?> packetClass = Class.forName(NMS + "network.protocol.Packet");
            Method sendMethod = connection.getClass().getMethod("send", packetClass);

            // Удаляем сущность
            Class<?> removeClass = Class.forName(NMS + "network.protocol.game.ClientboundRemoveEntitiesPacket");
            Constructor<?> removeCtor = removeClass.getConstructor(int[].class);
            Object removePacket = removeCtor.newInstance(new int[]{ entityId });
            sendMethod.invoke(connection, removePacket);

            // Удаляем из player info если требуется
            if (uuid != null) {
                try {
                    Class<?> removeInfoClass = Class.forName(NMS + "network.protocol.game.ClientboundPlayerInfoRemovePacket");
                    Constructor<?> removeInfoCtor = removeInfoClass.getConstructor(List.class);
                    Object removeInfoPacket = removeInfoCtor.newInstance(Collections.singletonList(uuid));
                    sendMethod.invoke(connection, removeInfoPacket);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Плавный поворот тела и головы NPC в сторону точки (в глаза игрока).
     */
    public static void lookAt(Player viewer, int entityId, Location npcLoc, Location targetLoc) {
        if (viewer == null || !viewer.isOnline() || npcLoc == null || targetLoc == null) return;
        try {
            double dx = targetLoc.getX() - npcLoc.getX();
            double dy = targetLoc.getY() - (npcLoc.getY() + 1.62);
            double dz = targetLoc.getZ() - npcLoc.getZ();
            double distXZ = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Math.toDegrees(-Math.atan2(dx, dz));
            float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

            Object craftPlayer = viewer.getClass().getMethod("getHandle").invoke(viewer);
            Field connField = craftPlayer.getClass().getField("connection");
            Object connection = connField.get(craftPlayer);
            Class<?> packetClass = Class.forName(NMS + "network.protocol.Packet");
            Method sendMethod = connection.getClass().getMethod("send", packetClass);

            byte byteYaw = (byte) ((yaw * 256.0F) / 360.0F);
            byte bytePitch = (byte) ((pitch * 256.0F) / 360.0F);

            // 1. Поворот тела и наклон головы (Pitch)
            try {
                Class<?> rotClass = Class.forName(NMS + "network.protocol.game.ClientboundMoveEntityPacket$Rot");
                Constructor<?> rotCtor = rotClass.getConstructor(int.class, byte.class, byte.class, boolean.class);
                Object rotPacket = rotCtor.newInstance(entityId, byteYaw, bytePitch, true);
                sendMethod.invoke(connection, rotPacket);
            } catch (Throwable ignored) {}

            // 2. Поворот головы (Yaw)
            Object serverLevel = npcLoc.getWorld().getClass().getMethod("getHandle").invoke(npcLoc.getWorld());
            Object serverEntity = serverLevel.getClass().getMethod("getEntity", int.class).invoke(serverLevel, entityId);
            if (serverEntity != null) {
                Class<?> rotateClass = Class.forName(NMS + "network.protocol.game.ClientboundRotateHeadPacket");
                Constructor<?> rotateCtor = rotateClass.getConstructor(Class.forName(NMS + "world.entity.Entity"), byte.class);
                Object rotatePacket = rotateCtor.newInstance(serverEntity, byteYaw);
                sendMethod.invoke(connection, rotatePacket);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Синхронное получение скина из памяти или БД.
     */
    public static String[] getSkinData(String skinName, Connection dbConn) {
        if (skinName == null || skinName.isBlank()) return null;
        String lower = skinName.toLowerCase(Locale.ROOT);
        String[] cached = SKIN_CACHE.get(lower);
        if (cached != null) return cached;

        // Поиск в SQLite
        if (dbConn != null) {
            try (PreparedStatement ps = dbConn.prepareStatement("SELECT texture, signature FROM npc_skins WHERE skin_name = ?")) {
                ps.setString(1, lower);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String[] data = new String[]{ rs.getString("texture"), rs.getString("signature") };
                        SKIN_CACHE.put(lower, data);
                        return data;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Поиск у онлайн-игроков на сервере
        Player online = Bukkit.getPlayerExact(skinName);
        if (online != null && online.isOnline()) {
            try {
                Object cp = online.getClass().getMethod("getHandle").invoke(online);
                Object gp = cp.getClass().getMethod("getGameProfile").invoke(cp);
                Object props = gp.getClass().getMethod("getProperties").invoke(gp);
                Method getMethod = props.getClass().getMethod("get", Object.class);
                Collection<?> col = (Collection<?>) getMethod.invoke(props, "textures");
                if (col != null && !col.isEmpty()) {
                    Object p = col.iterator().next();
                    String val = (String) p.getClass().getMethod("value").invoke(p);
                    String sig = (String) p.getClass().getMethod("signature").invoke(p);
                    if (val != null && !val.isEmpty()) {
                        String[] data = new String[]{ val, sig };
                        SKIN_CACHE.put(lower, data);
                        saveSkinToDB(dbConn, lower, val, sig);
                        return data;
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Асинхронная загрузка скина через официальный Mojang API.
     */
    public static void fetchSkinAsync(String skinName, Connection dbConn, Consumer<String[]> callback) {
        if (skinName == null || skinName.isBlank()) {
            callback.accept(null);
            return;
        }

        String lower = skinName.toLowerCase(Locale.ROOT);
        String[] cached = getSkinData(lower, dbConn);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        if (plugin == null) {
            callback.accept(null);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] result = null;

            // 1. Быстрый запрос скина через Ely.by Skins API (поддерживает скины Ely.by, TLauncher и др.)
            try {
                URL uEly = new URL("http://skinsystem.ely.by/textures/signed/" + skinName);
                HttpURLConnection cEly = (HttpURLConnection) uEly.openConnection();
                cEly.setRequestProperty("User-Agent", "Mozilla/5.0");
                cEly.setConnectTimeout(3000);
                cEly.setReadTimeout(3000);
                if (cEly.getResponseCode() == 200) {
                    String jsonEly = new String(cEly.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String val = extractJson(jsonEly, "value");
                    String sig = extractJson(jsonEly, "signature");
                    if (val != null && !val.isEmpty()) {
                        result = new String[]{ val, sig != null ? sig : "" };
                    }
                }
            } catch (Throwable ignored) {}

            // 2. Официальный Mojang API если не найден на Ely.by
            if (result == null) {
                try {
                    // Запрос UUID по нику
                    URL u1 = new URL("https://api.mojang.com/users/profiles/minecraft/" + skinName);
                    HttpURLConnection c1 = (HttpURLConnection) u1.openConnection();
                    c1.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    c1.setConnectTimeout(3000);
                    c1.setReadTimeout(3000);
                    if (c1.getResponseCode() == 200) {
                        String json1 = new String(c1.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        String id = extractJson(json1, "id");
                        if (id != null && !id.isEmpty()) {
                            // Запрос текстур и подписи по UUID
                            URL u2 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + id + "?unsigned=false");
                            HttpURLConnection c2 = (HttpURLConnection) u2.openConnection();
                            c2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                            c2.setConnectTimeout(3000);
                            c2.setReadTimeout(3000);
                            if (c2.getResponseCode() == 200) {
                                String json2 = new String(c2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                                String val = extractJson(json2, "value");
                                String sig = extractJson(json2, "signature");
                                if (val != null && !val.isEmpty()) {
                                    result = new String[]{ val, sig != null ? sig : "" };
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 3. Fallback на качественный Steve скин если нигде не найден
            if (result == null && (skinName.equalsIgnoreCase("Steve") || skinName.equalsIgnoreCase("default"))) {
                result = STEVE_SKIN;
            }

            final String[] finalRes = result;
            if (finalRes != null) {
                SKIN_CACHE.put(lower, finalRes);
                saveSkinToDB(dbConn, lower, finalRes[0], finalRes[1]);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalRes));
        });
    }

    public static void saveSkinToDB(Connection dbConn, String skinName, String texture, String signature) {
        if (dbConn == null || skinName == null || texture == null) return;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Artifex");
        Runnable task = () -> {
            try (PreparedStatement ps = dbConn.prepareStatement(
                    "INSERT INTO npc_skins (skin_name, texture, signature) VALUES (?, ?, ?) " +
                    "ON CONFLICT(skin_name) DO UPDATE SET texture=excluded.texture, signature=excluded.signature")) {
                ps.setString(1, skinName.toLowerCase(Locale.ROOT));
                ps.setString(2, texture);
                ps.setString(3, signature != null ? signature : "");
                ps.executeUpdate();
            } catch (Throwable ignored) {}
        };
        if (plugin != null && Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            task.run();
        }
    }

    private static String extractJson(String json, String key) {
        if (json == null || key == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }
}
