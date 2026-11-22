package me.dcplugin.artifex.bootstrap;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.ItemRegistry;
import me.dcplugin.artifex.api.database.DatabaseManager;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.api.modules.ModuleManager;
import me.dcplugin.artifex.api.placeholders.PlaceholderManager;
import me.dcplugin.artifex.handlers.CustomItemHandler;
import me.dcplugin.artifex.handlers.RecipeManager;
import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginBootstrapShutdownTest {

    @TempDir
    Path tempDir;

    private Main plugin;
    private PluginBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        plugin = mock(Main.class);
        Logger logger = Logger.getLogger("PluginBootstrapShutdownTest-" + UUID.randomUUID());
        when(plugin.getLogger()).thenReturn(logger);
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(logger);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptyList());
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        bootstrap = new PluginBootstrap(plugin);
    }

    @Test
    void disableUnloadsSingleActiveModule() throws Exception {
        List<String> events = new ArrayList<>();
        RecordingModule module = module("single", events, () -> { });
        ModuleManager manager = managerWith(module);
        setField(bootstrap, "moduleManager", manager);

        assertDoesNotThrow(bootstrap::disable);

        assertEquals(List.of("enable:single", "disable:single"), events);
        assertEquals(0, manager.getModuleCount());
        assertFalse(module.isEnabled());
    }

    @Test
    void disableUnloadsAllActiveModules() throws Exception {
        List<String> events = new ArrayList<>();
        RecordingModule first = module("first", events, () -> { });
        RecordingModule second = module("second", events, () -> { });
        RecordingModule third = module("third", events, () -> { });
        ModuleManager manager = managerWith(first, second, third);
        setField(bootstrap, "moduleManager", manager);

        assertDoesNotThrow(bootstrap::disable);

        assertEquals(3, Collections.frequency(events, "disable:first") + Collections.frequency(events, "disable:second")
                + Collections.frequency(events, "disable:third"));
        assertEquals(0, manager.getModuleCount());
        assertFalse(first.isEnabled());
        assertFalse(second.isEnabled());
        assertFalse(third.isEnabled());
    }

    @Test
    void disableCallsOnDisableExactlyOnce() throws Exception {
        List<String> events = new ArrayList<>();
        RecordingModule module = module("once", events, () -> { });
        setField(bootstrap, "moduleManager", managerWith(module));

        bootstrap.disable();

        assertEquals(1, Collections.frequency(events, "disable:once"));
    }

    @Test
    void disableContinuesWhenModuleOnDisableFails() throws Exception {
        List<String> events = new ArrayList<>();
        RecordingModule failing = module("failing", events, () -> {
            throw new AssertionError("module shutdown failed");
        });
        RecordingModule healthy = module("healthy", events, () -> { });
        ModuleManager manager = managerWith(failing, healthy);
        setField(bootstrap, "moduleManager", manager);

        assertDoesNotThrow(bootstrap::disable);

        assertEquals(1, Collections.frequency(events, "disable:failing"));
        assertEquals(1, Collections.frequency(events, "disable:healthy"));
        assertEquals(0, manager.getModuleCount());
    }

    @Test
    void repeatedDisableDoesNotCallOnDisableTwice() throws Exception {
        List<String> events = new ArrayList<>();
        RecordingModule module = module("repeat", events, () -> { });
        setField(bootstrap, "moduleManager", managerWith(module));

        assertDoesNotThrow(bootstrap::disable);
        assertDoesNotThrow(bootstrap::disable);

        assertEquals(1, Collections.frequency(events, "disable:repeat"));
    }

    @Test
    void dependenciesAreAvailableWhileModulesAreDisabled() throws Exception {
        ItemRegistry registry = mock(ItemRegistry.class);
        PlaceholderManager placeholders = mock(PlaceholderManager.class);
        DatabaseManager database = mock(DatabaseManager.class);
        when(plugin.getApiItemRegistry()).thenReturn(registry);
        when(plugin.getPlaceholderManager()).thenReturn(placeholders);
        when(plugin.getDatabaseManager()).thenReturn(database);

        AtomicBoolean registryAvailable = new AtomicBoolean();
        AtomicBoolean placeholdersAvailable = new AtomicBoolean();
        AtomicBoolean databaseAvailable = new AtomicBoolean();
        List<String> events = new ArrayList<>();
        RecordingModule module = module("dependencies", events, () -> {
            registryAvailable.set(plugin.getApiItemRegistry() == registry);
            placeholdersAvailable.set(plugin.getPlaceholderManager() == placeholders);
            databaseAvailable.set(plugin.getDatabaseManager() == database);
        });
        ModuleManager manager = spy(managerWith(module));
        setField(bootstrap, "moduleManager", manager);
        setField(bootstrap, "apiItemRegistry", registry);
        setField(bootstrap, "placeholderManager", placeholders);
        setField(bootstrap, "databaseManager", database);

        bootstrap.disable();

        assertTrue(registryAvailable.get());
        assertTrue(placeholdersAvailable.get());
        assertTrue(databaseAvailable.get());
        assertEquals(1, Collections.frequency(events, "disable:dependencies"));

        var order = inOrder(manager, placeholders, registry, database);
        order.verify(manager).unloadAll();
        order.verify(placeholders).unregisterExpansion();
        order.verify(registry).shutdown();
        order.verify(database).shutdownAsync();
    }

    @Test
    void disableClearsYamlAndJavaRecipes() throws Exception {
        CustomItemHandler itemHandler = mock(CustomItemHandler.class);
        RecipeManager recipes = mock(RecipeManager.class);
        when(itemHandler.getRecipeManager()).thenReturn(recipes);
        setField(bootstrap, "itemHandler", itemHandler);

        assertDoesNotThrow(bootstrap::disable);

        verify(recipes).clear();
        verify(recipes).clearJava();
    }

    @Test
    void moduleCanPersistPendingStateDuringDisable() throws Exception {
        List<String> events = new ArrayList<>();
        Path spillFile = tempDir.resolve("queued-state").resolve("spill").resolve("pending.json");
        RecordingModule module = module("queued-state", events, () -> {
            try {
                Files.createDirectories(spillFile.getParent());
                Files.writeString(spillFile, "{\"id\":\"queued\"}");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        setField(bootstrap, "moduleManager", managerWith(module));

        bootstrap.disable();

        assertTrue(Files.exists(spillFile));
        assertEquals("{\"id\":\"queued\"}", Files.readString(spillFile));
        assertEquals(1, Collections.frequency(events, "disable:queued-state"));
    }

    private RecordingModule module(String id, List<String> events, Runnable onDisable) {
        return new RecordingModule(plugin, tempDir.resolve(id).toFile(), id, events, onDisable);
    }

    private ModuleManager managerWith(Module... modules) throws Exception {
        ModuleManager manager = new ModuleManager(plugin);
        for (Module module : modules) {
            modulesOf(manager).put(module.getId().toLowerCase(Locale.ROOT), module);
            module.enable();
        }
        return manager;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Module> modulesOf(ModuleManager manager) throws Exception {
        Field field = ModuleManager.class.getDeclaredField("modules");
        field.setAccessible(true);
        return (Map<String, Module>) field.get(manager);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static final class RecordingModule extends Module {
        private final String eventId;
        private final List<String> events;
        private final Runnable onDisable;

        private RecordingModule(Main plugin, File folder, String id, List<String> events, Runnable onDisable) {
            super(plugin, id, folder);
            this.eventId = id;
            this.events = events;
            this.onDisable = onDisable;
        }

        @Override
        protected void onEnable() {
            events.add("enable:" + eventId);
        }

        @Override
        protected void onDisable() {
            events.add("disable:" + eventId);
            onDisable.run();
        }
    }
}
