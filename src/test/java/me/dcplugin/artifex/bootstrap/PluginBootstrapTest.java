package me.dcplugin.artifex.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginBootstrapTest {
    @Test
    void initializesDatabaseBeforeLoadingModules() {
        List<String> calls = new ArrayList<>();

        PluginBootstrap.initializeDatabaseBeforeModules(
                () -> calls.add("database"),
                () -> calls.add("modules"));

        assertEquals(List.of("database", "modules"), calls);
    }
}
