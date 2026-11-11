package me.dcplugin.artifex.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты EnumCache.
 *
 * Внимание: Material.valueOf работает без сервера, а Sound/Particle
 * инициализируются через Bukkit Registry — их valueOf требует живой
 * сервер. Здесь покрывается то, что тестируется в чистом JVM: materials,
 * нормализация имён и null-безопасность всех lookup'ов.
 */
class EnumCacheTest {

    @Test
    void materialValid() {
        assertEquals(Material.DIAMOND, EnumCache.getMaterial("DIAMOND"));
    }

    @Test
    void materialLowercase() {
        assertEquals(Material.IRON_INGOT, EnumCache.getMaterial("iron_ingot"));
    }

    @Test
    void materialWithNamespace() {
        assertEquals(Material.DIAMOND_SWORD, EnumCache.getMaterial("minecraft:diamond_sword"));
    }

    @Test
    void materialWithDashes() {
        assertEquals(Material.DIAMOND_SWORD, EnumCache.getMaterial("diamond-sword"));
    }

    @Test
    void materialInvalidReturnsNull() {
        assertNull(EnumCache.getMaterial("NOT_A_REAL_MATERIAL_123"));
    }

    @Test
    void materialNullAndEmpty() {
        assertNull(EnumCache.getMaterial(null));
        assertNull(EnumCache.getMaterial(""));
    }

    @Test
    void particleNullAndEmptyNoServerNeeded() {
        assertNull(EnumCache.getParticle(null));
        assertNull(EnumCache.getParticle(""));
    }

    @Test
    void soundNullAndEmptyNoServerNeeded() {
        assertNull(EnumCache.getSound(null));
        assertNull(EnumCache.getSound(""));
    }

    /**
     * Поиск атрибута не должен бросать исключение, когда сервера нет
     * (реестр атрибутов недоступен) и когда имя заведомо неверное.
     */
    @Test
    void attributeLookupNeverThrows() {
        assertDoesNotThrow(() -> EnumCache.getAttribute(null));
        assertDoesNotThrow(() -> EnumCache.getAttribute(""));
        assertDoesNotThrow(() -> EnumCache.getAttribute("GENERIC_ATTACK_DAMAGE"));
        assertDoesNotThrow(() -> EnumCache.getAttribute("definitely-not-an-attribute"));
    }

    @Test
    void attributeNullAndEmpty() {
        assertNull(EnumCache.getAttribute(null));
        assertNull(EnumCache.getAttribute(""));
    }

    @Test
    void itemFlagNormalizesDashes() {
        // ItemFlag — обычный enum, доступен без сервера
        assertNotNull(EnumCache.getItemFlag("hide-enchants"));
    }
}
