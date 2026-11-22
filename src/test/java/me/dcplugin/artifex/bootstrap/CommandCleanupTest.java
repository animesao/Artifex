package me.dcplugin.artifex.bootstrap;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.utils.CommandMapAccessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandCleanupTest {

    @TempDir
    Path tempDir;

    private Main plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Main.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("CommandCleanupTest-" + UUID.randomUUID()));
        when(plugin.getName()).thenReturn("Artifex");
    }

    @AfterEach
    void tearDown() {
        CommandMapAccessor.reset();
    }

    @Test
    void registrarUnregistersTrackedCommandsWhenKnownCommandsIsUnavailable() throws Exception {
        cacheCommandMap(mock(CommandMap.class));
        CommandRegistrar registrar = new CommandRegistrar(plugin);
        RecordingCommand command = new RecordingCommand("registrar-probe");
        commandsOf(registrar).add(command);

        registrar.unregisterAll();

        assertTrue(command.unregistered);
        assertTrue(commandsOf(registrar).isEmpty());
    }

    @Test
    void moduleUnregistersDynamicCommandsWhenKnownCommandsIsUnavailable() throws Exception {
        cacheCommandMap(mock(CommandMap.class));
        TestModule module = new TestModule(plugin, tempDir.resolve("disable").toFile(), false);
        RecordingCommand command = new RecordingCommand("module-probe");
        commandsOf(module).add(command);

        module.enable();
        module.disable();

        assertTrue(command.unregistered);
        assertTrue(commandsOf(module).isEmpty());
        assertFalse(module.isEnabled());
    }

    @Test
    void failedEnableRollsBackStateAndUnregistersCommands() throws Exception {
        cacheCommandMap(mock(CommandMap.class));
        TestModule module = new TestModule(plugin, tempDir.resolve("failed").toFile(), true);
        RecordingCommand command = new RecordingCommand("failed-enable-probe");
        commandsOf(module).add(command);

        assertThrows(IllegalStateException.class, module::enable);

        assertFalse(module.isEnabled());
        assertTrue(command.unregistered);
        assertTrue(commandsOf(module).isEmpty());
    }

    private void cacheCommandMap(CommandMap commandMap) throws Exception {
        setStatic(CommandMapAccessor.class, "commandMap", commandMap);
        setStatic(CommandMapAccessor.class, "knownCommands", null);
        setStatic(CommandMapAccessor.class, "resolved", true);
    }

    @SuppressWarnings("unchecked")
    private static Collection<Command> commandsOf(Object target) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("dynamicCommands");
                field.setAccessible(true);
                return (Collection<Command>) field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("dynamicCommands");
    }

    private static void setStatic(Class<?> target, String name, Object value) throws Exception {
        Field field = target.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class TestModule extends Module {
        private final boolean failOnEnable;

        private TestModule(Main plugin, File folder, boolean failOnEnable) {
            super(plugin, "probe", folder);
            this.failOnEnable = failOnEnable;
        }

        @Override
        protected void onEnable() {
            if (failOnEnable) {
                throw new IllegalStateException("synthetic enable failure");
            }
        }

        @Override
        protected void onDisable() {
        }
    }

    private static final class RecordingCommand extends Command {
        private boolean unregistered;

        private RecordingCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }

        @Override
        public boolean unregister(CommandMap commandMap) {
            unregistered = true;
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return List.of();
        }
    }
}
