package me.dcplugin.dcustomitems.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты разбора и нормализации строк действий ActionParser.
 * Проверяют нормализацию типа (нижний регистр, дефисы = подчёркивания),
 * отделение значения и безопасность разбора пустых строк.
 */
class ActionParserTest {

    @Test
    void nullActionReturnsNull() {
        assertNull(ActionParser.splitAction(null));
    }

    @Test
    void emptyActionReturnsNull() {
        assertNull(ActionParser.splitAction(""));
        assertNull(ActionParser.splitAction("   "));
    }

    @Test
    void simpleActionKeepsValue() {
        String[] parsed = ActionParser.splitAction("effect:SPEED:10:2");
        assertNotNull(parsed);
        assertEquals("effect", parsed[0]);
        assertEquals("SPEED:10:2", parsed[1]);
    }

    @Test
    void typeIsLowercased() {
        assertEquals("message", ActionParser.splitAction("MESSAGE:hello")[0]);
    }

    @Test
    void hyphensAreNormalizedToUnderscores() {
        assertEquals("damage_mobs", ActionParser.splitAction("damage-mobs:5")[0]);
        assertEquals("effect_nearby", ActionParser.splitAction("effect-nearby:SPEED:5:1:3")[0]);
    }

    @Test
    void mixedDashUnderscoreNormalizes() {
        assertEquals("damage_mobs", ActionParser.splitAction("damage-MOBS:5")[0]);
    }

    @Test
    void actionWithoutValueHasEmptyValue() {
        String[] parsed = ActionParser.splitAction("sound");
        assertNotNull(parsed);
        assertEquals("sound", parsed[0]);
        assertEquals("", parsed[1]);
    }

    @Test
    void valueMayContainColons() {
        String[] parsed = ActionParser.splitAction("title:&6Title|&7Sub:10:40:10");
        assertEquals("title", parsed[0]);
        assertEquals("&6Title|&7Sub:10:40:10", parsed[1]);

        String[] cmdSeq = ActionParser.splitAction("command_sequence:minecraft:give %player% diamond 1:100;minecraft:say hello:50");
        assertEquals("command_sequence", cmdSeq[0]);
        assertEquals("minecraft:give %player% diamond 1:100;minecraft:say hello:50", cmdSeq[1]);
    }

    /**
     * Пустое (пробельное) действие раньше приводило к NPE вне блока try:
     * splitAction возвращал null, а код сразу обращался к parsed[0].
     */
    @Test
    void blankActionDoesNotThrow() {
        assertDoesNotThrow(() -> ActionParser.execute(null, "   "));
        assertDoesNotThrow(() -> ActionParser.execute(null, ""));
        assertDoesNotThrow(() -> ActionParser.execute(null, null));
    }

    @Test
    void commandSequenceWithColonsDoesNotThrow() {
        assertDoesNotThrow(() -> ActionParser.execute(null, "command_sequence:minecraft:give %player% diamond 1:100"));
    }
}
