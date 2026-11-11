package me.dcplugin.artifex.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CooldownStoreTest {

    @TempDir
    Path tempDir;

    private CooldownStore store;

    @BeforeEach
    void setUp() {
        store = new CooldownStore(tempDir.toFile());
    }

    @Test
    void testCooldownActiveAndRemaining() {
        long now = System.currentTimeMillis();
        String player = "player-uuid-123";
        String item = "fire_sword";

        assertFalse(store.isOnCooldown(player, item, now));
        assertEquals(0, store.remaining(player, item, now));

        store.put(player, item, now + 5000);

        assertTrue(store.isOnCooldown(player, item, now));
        assertTrue(store.remaining(player, item, now) > 0);
        assertTrue(store.remaining(player, item, now) <= 5000);

        // After cooldown expires
        assertFalse(store.isOnCooldown(player, item, now + 6000));
        assertEquals(0, store.remaining(player, item, now + 6000));
    }

    @Test
    void testRemoveCooldown() {
        long now = System.currentTimeMillis();
        String player = "player-uuid-456";
        String item = "speed_boots";

        store.put(player, item, now + 10000);
        assertTrue(store.isOnCooldown(player, item, now));

        store.remove(player, item);
        assertFalse(store.isOnCooldown(player, item, now));
        assertEquals(0, store.remaining(player, item, now));
    }

    @Test
    void testPersistenceSaveAndLoad() {
        long now = System.currentTimeMillis();
        String player = "player-789";
        String item = "magic_wand";

        store.put(player, item, now + 60000);
        store.save();

        File cooldownFile = new File(tempDir.toFile(), "cooldowns.yml");
        assertTrue(cooldownFile.exists());

        // Create new store instance pointing to same directory
        CooldownStore reloadedStore = new CooldownStore(tempDir.toFile());
        assertTrue(reloadedStore.isOnCooldown(player, item, now));
        assertTrue(reloadedStore.remaining(player, item, now) > 0);
    }
}
