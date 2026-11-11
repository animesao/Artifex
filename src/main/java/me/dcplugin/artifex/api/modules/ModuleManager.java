package me.dcplugin.artifex.api.modules;

import me.dcplugin.artifex.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.*;

/**
 * Module manager
 * 
 * Loads modules from the subfolders of items/
 * 
 * Structure:
 * items/
 * ├── shop/
 * │   ├── config.yml
 * │   ├── items.yml
 * │   └── shop.java
 * └── arena/
 *     ├── config.yml
 *     └── arena.java
 */
public class ModuleManager {

    private final Main plugin;
    private final File modulesDir;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(Main plugin) {
        this.plugin = plugin;
        this.modulesDir = new File(plugin.getDataFolder(), "items");
    }

    /**
     * Loads all modules from the subfolders
     */
    public void loadAll() {
        unloadAll();
        
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
            return;
        }

        // Look for subfolders (each subfolder = one module)
        File[] subfolders = modulesDir.listFiles(File::isDirectory);
        if (subfolders == null || subfolders.length == 0) {
            return;
        }

        plugin.getLogger().info("[Modules] Found " + subfolders.length + " potential modules");

        for (File folder : subfolders) {
            // Sample and reference folders (EXAMPLE-*, EXAMPLES/, _template/)
            // are not treated as modules — they are reference material.
            String folderName = folder.getName();
            if (folderName.startsWith("EXAMPLE-")
                    || folderName.equalsIgnoreCase("EXAMPLES")
                    || folderName.equalsIgnoreCase("_template")) {
                continue;
            }
            try {
                loadModule(folder);
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[Modules] Error loading module: " + folder.getName() + " - " + e.getMessage(), e);
            }
        }

        plugin.getLogger().info("[Modules] Loaded " + modules.size() + " modules");
    }

    /**
     * Loads a module from a folder
     */
    private void loadModule(File folder) {
        String moduleId = folder.getName();
        
        // Check whether config.yml exists
        File configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            // Create a default config
            createDefaultConfig(folder, moduleId);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String moduleName = config.getString("name", moduleId);
        
        plugin.getLogger().info("[Modules] Loading module: " + moduleName + " (" + moduleId + ")");

        // Java modules are already compiled by the ItemRegistry together with the commands.
        Module javaModule = plugin.getApiItemRegistry() == null
            ? null : plugin.getApiItemRegistry().getModule(moduleId);
        if (javaModule != null) {
            modules.put(moduleId.toLowerCase(), javaModule);
            if (config.getBoolean("enabled", true)) javaModule.enable();
            return;
        }

        // If there is no Java module, fall back to a simple YAML module.
        loadSimpleModule(folder, moduleId, config);
    }

    /**
     * Loads a simple YAML module
     */
    private void loadSimpleModule(File folder, String moduleId, YamlConfiguration config) {
        SimpleModule module = new SimpleModule(plugin, moduleId, folder, config);
        // The key is registered in lower case — same as for Java modules.
        // Previously moduleId was used without normalisation, which made a module
        // whose folder name is in upper case impossible to find.
        modules.put(moduleId.toLowerCase(java.util.Locale.ROOT), module);
        module.enable();
    }

    /**
     * Creates a default config
     */
    private void createDefaultConfig(File folder, String moduleId) {
        try {
            File configFile = new File(folder, "config.yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("name", moduleId);
            config.set("version", "1.0");
            config.set("enabled", true);
            config.set("commands", Collections.emptyList());
            config.set("permissions", Collections.emptyList());
            
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "[Modules] Error creating config: " + e.getMessage(), e);
        }
    }

    /**
     * Unloads all modules
     */
    public void unloadAll() {
        for (Module module : modules.values()) {
            try {
                module.disable();
            } catch (Exception e) {
                plugin.getLogger().warning("[Modules] Error disabling: " + module.getId());
            }
        }
        modules.clear();
    }

    /**
     * Reloads all modules
     */
    public void reloadAll() {
        unloadAll();
        loadAll();
    }

    /**
     * Gets a module by ID
     */
    public Module getModule(String id) {
        return modules.get(id.toLowerCase());
    }

    /**
     * Gets all modules
     */
    public Map<String, Module> getAllModules() {
        return Collections.unmodifiableMap(modules);
    }

    /**
     * Number of modules
     */
    public int getModuleCount() {
        return modules.size();
    }
}
