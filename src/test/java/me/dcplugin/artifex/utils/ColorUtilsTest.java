package me.dcplugin.artifex.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorUtilsTest {

    @Test
    void testLegacyColorize() {
        assertEquals("§cRed text", ColorUtils.colorize("&cRed text"));
        assertEquals("§6Gold §aGreen", ColorUtils.colorize("&6Gold &aGreen"));
    }

    @Test
    void testHexColorize() {
        String colorized = ColorUtils.colorize("&#FF0000Hex text");
        assertNotNull(colorized);
        assertTrue(colorized.contains("Hex text"));
    }

    @Test
    void testMiniMessageColorize() {
        String result = ColorUtils.colorize("<red>MiniMessage text</red>");
        assertNotNull(result);
        assertTrue(result.contains("MiniMessage text"));
        assertTrue(result.contains("§c") || result.contains("§4"));

        String gradient = ColorUtils.colorize("<gradient:#ff0000:#0000ff>Gradient</gradient>");
        assertNotNull(gradient);
        assertTrue(ColorUtils.stripColor(gradient).contains("Gradient"));
    }

    @Test
    void testFormatTime() {
        assertEquals("30s", ColorUtils.formatTime(30));
        assertEquals("1m 15s", ColorUtils.formatTime(75));
        assertEquals("1h 0m 0s", ColorUtils.formatTime(3600));
        assertEquals("2h 30m 10s", ColorUtils.formatTime(9010));
    }

    @Test
    void testStripColor() {
        String colored = "§cRed §6Gold";
        assertEquals("Red Gold", ColorUtils.stripColor(colored));
    }
}
