package me.dcplugin.artifex.bootstrap;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    void performanceIsNotReportedAsLegacyItem() {
        Main plugin = mock(Main.class);
        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("performance.enabled", true);
        config.set("legacySword.material", "DIAMOND_SWORD");
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getConfig()).thenReturn(config);

        new ConfigMigrator(plugin).checkConfigMigration();

        verify(logger).info("  - legacySword");
        verify(logger, never()).info("  - performance");
    }
}
