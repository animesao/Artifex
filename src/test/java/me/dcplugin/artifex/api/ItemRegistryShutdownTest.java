package me.dcplugin.artifex.api;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import me.dcplugin.artifex.api.placeholders.PlaceholderManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemRegistryShutdownTest {

    @Test
    void shutdownUnregistersCustomCommandsAndPlaceholdersBeforeClosingRegistries() throws Exception {
        Main plugin = mock(Main.class);
        PlaceholderManager placeholders = mock(PlaceholderManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ItemRegistryShutdownTest-" + UUID.randomUUID()));
        when(plugin.getDataFolder()).thenReturn(Path.of("target", "item-registry-shutdown").toFile());
        when(plugin.getPlaceholderManager()).thenReturn(placeholders);

        CustomCommand command = mock(CustomCommand.class);
        when(command.getName()).thenReturn("shutdownprobe");
        CustomPlaceholder placeholder = mock(CustomPlaceholder.class);
        when(placeholder.getIdentifier()).thenReturn("shutdownprobe_value");

        ItemRegistry registry = new ItemRegistry(plugin);
        mapField(registry, "registeredCommands").put("shutdownprobe", command);
        mapField(registry, "registeredPlaceholders").put("shutdownprobe_value", placeholder);

        registry.shutdown();

        verify(plugin).unregisterCustomCommands();
        verify(command).onUnregister();
        verify(placeholder).onUnregister();
        verify(placeholders).unregisterCustom("shutdownprobe_value");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapField(ItemRegistry registry, String name) throws Exception {
        Field field = ItemRegistry.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, Object>) field.get(registry);
    }
}
