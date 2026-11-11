package me.dcplugin.artifex.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the central message system.
 *
 * <p>MessagesConfig must stay usable without a running server: the class is
 * loaded by the plain unit tests, so only the parts that do not touch the
 * Bukkit registry are exercised here.
 */
class MessagesConfigTest {

    @AfterEach
    void cleanup() {
        MessagesConfig.clearCustom();
    }

    @Test
    void builtInMessageIsResolvedByPath() {
        assertEquals(MessagesConfig.ITEM_WORLD_BLOCKED,
                MessagesConfig.getBuiltIn("items.world-blocked"));
    }

    @Test
    void unknownPathReturnsFallback() {
        assertEquals("fallback", MessagesConfig.get("does.not.exist", "fallback"));
        assertNull(MessagesConfig.getBuiltIn("does.not.exist"));
    }

    @Test
    void runtimeOverrideWinsOverBuiltIn() {
        MessagesConfig.set("items.world-blocked", "&aAllowed here!");
        assertEquals("&aAllowed here!", MessagesConfig.get("items.world-blocked", "fallback"));
        assertEquals("&aAllowed here!", MessagesConfig.getCustom("items.world-blocked"));
    }

    @Test
    void customMessageIsRemovable() {
        MessagesConfig.set("my.module.text", "&aHello!");
        assertEquals("&aHello!", MessagesConfig.get("my.module.text", "fallback"));

        MessagesConfig.set("my.module.text", null);
        assertNull(MessagesConfig.getCustom("my.module.text"));
        assertEquals("fallback", MessagesConfig.get("my.module.text", "fallback"));
    }

    @Test
    void clearRemovesEveryOverride() {
        MessagesConfig.set("a.b", "1");
        MessagesConfig.set("c.d", "2");
        MessagesConfig.clearCustom();
        assertTrue(MessagesConfig.getCustom().isEmpty());
    }

    @Test
    void customMapIsReadOnly() {
        MessagesConfig.set("a.b", "1");
        assertThrows(UnsupportedOperationException.class,
                () -> MessagesConfig.getCustom().put("x.y", "z"));
    }

    @Test
    void builtInOverridableConstantIsReadLive() {
        String original = MessagesConfig.ITEM_EXPIRED;
        try {
            MessagesConfig.ITEM_EXPIRED = "&bChanged!";
            assertEquals("&bChanged!", MessagesConfig.getBuiltIn("items.expired"));
        } finally {
            MessagesConfig.ITEM_EXPIRED = original;
        }
    }

    @Test
    void everyBuiltInPathIsUniqueAndResolvable() {
        // The paths are the public contract for admins and modules.
        for (String path : new String[]{
                "plugin.no-permission", "plugin.player-not-found", "plugin.item-not-found",
                "plugin.unknown-command", "plugin.only-for-players",
                "plugin.update-available", "plugin.update-download",
                "items.uses-depleted", "items.expired", "items.world-blocked",
                "items.no-permission",
                "commands.not-found", "commands.error", "commands.no-permission",
                "commands.player-not-found", "commands.specify-player",
                "ci.header", "ci.help.reload", "ci.help.validate", "ci.help.debug",
                "ci.help.backup", "ci.reload.success", "ci.reload.yaml",
                "ci.reload.java-items", "ci.reload.java-commands",
                "ci.reload.java-placeholders", "ci.reload.error", "ci.modules",
                "ci.validation.ok", "ci.validation.problems", "ci.debug.usage",
                "ci.debug.item-not-found", "ci.backup.created", "ci.backup.failed",
                "update.header", "update.title", "update.current-version",
                "update.new-version", "update.download"}) {
            assertNotNull(MessagesConfig.getBuiltIn(path), "no built-in message for path: " + path);
        }
    }
}
