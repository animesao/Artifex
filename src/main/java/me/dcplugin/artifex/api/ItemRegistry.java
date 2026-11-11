package me.dcplugin.artifex.api;

import me.dcplugin.artifex.Main;
import me.dcplugin.artifex.api.commands.CustomCommand;
import me.dcplugin.artifex.api.modules.Module;
import me.dcplugin.artifex.api.placeholders.CustomPlaceholder;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ItemRegistry {

    private final Main plugin;
    // ConcurrentHashMap: getItem() вызывается из потока PlaceholderAPI,
    // а главный поток пересобирает карту при /ci reload.
    private final Map<String, AbstractCustomItem> registeredItems;
    private final Map<String, CustomCommand> registeredCommands;
    private final Map<String, CustomPlaceholder> registeredPlaceholders;
    private final Map<String, Module> registeredModules;

    /**
     * Mirror of the item IDs in a thread-safe set.
     *
     * Needed for calls coming from the PlaceholderAPI thread: iterating the keySet() of a plain
     * LinkedHashMap during a reload would throw a ConcurrentModificationException,
     * and copying the map for every placeholder request would be a needless allocation.
     */
    private final Set<String> registeredItemIds = ConcurrentHashMap.newKeySet();
    private final List<URLClassLoader> jarClassLoaders = new ArrayList<>();
    private final JavaItemCompiler compiler;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final ExecutorService reloadExecutor;

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        this.registeredItems = new java.util.concurrent.ConcurrentHashMap<>();
        this.registeredCommands = new java.util.concurrent.ConcurrentHashMap<>();
        this.registeredPlaceholders = new java.util.concurrent.ConcurrentHashMap<>();
        this.registeredModules = new java.util.concurrent.ConcurrentHashMap<>();
        this.compiler = new JavaItemCompiler(plugin);
        this.reloadExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Artifex-Reload");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Initial load. Must run on the Bukkit main thread. */
    public void loadAll() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("ItemRegistry.loadAll() must run on the Bukkit main thread");
        }

        closeJarClassLoaders();
        registeredItems.clear();
        registeredItemIds.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        registerJavaRecipes();
        applyRuntimeMessages();

        // The startup path must register commands and placeholders the same way
        // as reload does: otherwise onRegister() was never called and Java placeholders
        // did not show up until the first /ci reload.
        registerCustomCommandsAndPlaceholders();

        logStats();
    }

    /**
     * Calls onRegister() on the commands and placeholders and registers
     * the placeholders in the PlaceholderManager.
     */
    private void registerCustomCommandsAndPlaceholders() {
        for (CustomCommand cmd : registeredCommands.values()) {
            try {
                cmd.onRegister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering command " + cmd.getName(), exception);
            }
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try {
                if (plugin.getPlaceholderManager() != null) {
                    plugin.getPlaceholderManager().registerCustom(ph.getIdentifier(), ph::getValue);
                }
                ph.onRegister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering placeholder " + ph.getIdentifier(), exception);
            }
        }
    }

    /**
     * Asynchronous API kept for callers outside the server thread.
     * Bukkit/plugin operations are marshalled back to the main thread.
     */
    public CompletableFuture<Void> reloadAsync() {
        if (!loading.compareAndSet(false, true)) {
            plugin.getLogger().info("[API] Already loading, skipping...");
            return CompletableFuture.completedFuture(null);
        }
        if (Bukkit.isPrimaryThread()) {
            try {
                reloadSynchronously();
                return CompletableFuture.completedFuture(null);
            } finally {
                loading.set(false);
            }
        }

        // A dedicated thread instead of the shared ForkJoin pool: a blocking
        // .get() wait would occupy a pool thread and could lead to
        // a deadlock with the server main thread.
        if (reloadExecutor.isShutdown()) {
            loading.set(false);
            throw new IllegalStateException("Plugin is shutting down - reload is unavailable");
        }
        return CompletableFuture.runAsync(() -> {
            try {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    reloadSynchronously();
                    return null;
                }).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Reload interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Reload failed", e.getCause());
            } finally {
                loading.set(false);
            }
        }, reloadExecutor);
    }

    /**
     * Stops the reload thread and closes jar classloaders (on plugin shutdown).
     */
    public void shutdown() {
        reloadExecutor.shutdownNow();
        closeJarClassLoaders();
    }

    /** Synchronous reload used by /ci reload on the main Bukkit thread. */
    public void reload() {
        if (!loading.compareAndSet(false, true)) {
            plugin.getLogger().info("[API] Already loading, skipping...");
            return;
        }

        try {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    reloadSynchronously();
                    return null;
                }).get();
            } else {
                reloadSynchronously();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reload interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Reload failed", e.getCause());
        } finally {
            loading.set(false);
        }
    }

    private void reloadSynchronously() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Reload must run on the Bukkit main thread");
        }

        if (plugin.getModuleManager() != null) {
            plugin.getModuleManager().unloadAll();
        }

        // Remove dynamic Bukkit command nodes before dropping the old classloader.
        // Otherwise a reload can retain command objects backed by stale Java classes.
        plugin.unregisterCustomCommands();
        for (CustomCommand cmd : registeredCommands.values()) {
            try {
                cmd.onUnregister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error unregistering command " + cmd.getName(), exception);
            }
        }
        for (CustomPlaceholder ph : registeredPlaceholders.values()) {
            try {
                ph.onUnregister();
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error unregistering placeholder " + ph.getIdentifier(), exception);
            }
            // Unregister the placeholder from the manager as well: otherwise old implementations
            // (and with them the old classes and classloader) stayed
            // in the PlaceholderManager map forever.
            if (plugin.getPlaceholderManager() != null) {
                plugin.getPlaceholderManager().unregisterCustom(ph.getIdentifier());
            }
        }

        closeJarClassLoaders();
        registeredItems.clear();
        registeredItemIds.clear();
        registeredCommands.clear();
        registeredPlaceholders.clear();
        registeredModules.clear();
        compiler.clear();

        File itemsDir = new File(plugin.getDataFolder(), "items");
        if (!itemsDir.exists()) itemsDir.mkdirs();
        loadJarFiles(itemsDir);
        loadJavaFiles(itemsDir);
        registerJavaRecipes();
        applyRuntimeMessages();

        registerCustomCommandsAndPlaceholders();
        plugin.registerCustomCommands();

        // The item definitions changed — the cached equipment state is stale
        // (the hash is built from item IDs, which did not change).
        if (plugin.getEquippedItemsChecker() != null) {
            plugin.getEquippedItemsChecker().invalidateAll();
        }
        if (plugin.getModuleManager() != null) {
            plugin.getModuleManager().loadAll();
        }
        logStats();
    }

    /**
     * Recipes of Java API items (getRecipes() -> RecipeDef) are registered
     * in vanilla crafting. The method clears the old java keys by itself before adding,
     * so it is safe on repeated reloads without reloading the YAML.
     *
     * Public method: on server start the recipes are re-registered after
     * the YAML items are loaded — otherwise a Java recipe referencing a YAML item
     * could not resolve its ingredient.
     */
    public void registerJavaRecipes() {
        try {
            if (plugin.getItemHandler() != null) {
                plugin.getItemHandler().getRecipeManager()
                        .registerJavaItems(registeredItems.values());
            }
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "[API] Error registering recipes for Java items", exception);
        }
    }

    /**
     * items/messages.java is now an ACTIVE message config: after the compilation
     * (the compiler renames the class to items.messagesItem) its static load()
     * is called, which overwrites MessagesConfig.*.
     * Called on startup and on /ci reload.
     */
    private void applyRuntimeMessages() {
        try {
            Class<?> clazz = compiler.loadClass("items.messagesItem");
            if (clazz == null) return; // no file — the built-in MessagesConfig is used
            // Сбрасываем предыдущие переопределения: строка, удалённая из
            // items/messages.java, не должна оставаться в силе.
            me.dcplugin.artifex.api.config.MessagesConfig.clearCustom();
            clazz.getMethod("load").invoke(null);
            plugin.getLogger().info("[Messages] items/messages.java applied (runtime load())");
        } catch (java.lang.reflect.InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            plugin.getLogger().warning("[Messages] items/messages.java was not applied: " + e.getMessage());
        }
    }

    private void loadJavaFiles(File itemsDir) {
        // Security: runtime compilation of .java files means executing arbitrary
        // code. Keep it disabled when several people administer the server
        // or there is a risk of external writes into the plugin folder.
        if (!plugin.getConfig().getBoolean("java-compilation", true)) {
            plugin.getLogger().info(
                    "[API] Runtime Java compilation disabled (java-compilation: false)"
                    + " — .java files in items/ are skipped.");
            return;
        }
        List<File> files = new ArrayList<>();
        collectJavaFiles(itemsDir, files);
        if (files.isEmpty()) return;

        plugin.getLogger().info("[API] Compiling " + files.size() + " .java files...");
        JavaItemCompiler.CompileResult result = compiler.compileAll();

        for (String cn : result.items) {
            AbstractCustomItem item = compiler.createItemInstance(cn);
            if (item != null) {
                registeredItems.put(item.getId(), item);
                registeredItemIds.add(item.getId());
            }
        }
        for (String cn : result.commands) {
            CustomCommand cmd = compiler.createCommandInstance(cn);
            if (cmd != null) registeredCommands.put(cmd.getName(), cmd);
        }
        for (String cn : result.modules) {
            Module module = compiler.createModuleInstance(cn);
            if (module != null) registeredModules.put(module.getId().toLowerCase(), module);
        }
        for (String cn : result.placeholders) {
            CustomPlaceholder ph = compiler.createPlaceholderInstance(cn);
            if (ph != null) registeredPlaceholders.put(ph.getIdentifier(), ph);
        }

        for (CustomCommand cmd : registeredCommands.values()) {
            plugin.getLogger().info("[API] Command: /" + cmd.getName());
        }
    }

    private void collectJavaFiles(File directory, List<File> files) {
        File[] children = directory.listFiles();
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getPath));
        for (File child : children) {
            if (child.isDirectory()) {
                // Sample and reference folders (EXAMPLE-*, EXAMPLES/, _template/)
                // are not compiled — they are reference material.
                String name = child.getName();
                if (name.startsWith("EXAMPLE-")
                        || name.equalsIgnoreCase("EXAMPLES")
                        || name.equalsIgnoreCase("_template")) continue;
                collectJavaFiles(child, files);
            } else if (child.getName().endsWith(".java") && !child.getName().startsWith("EXAMPLE-")) {
                files.add(child);
            }
        }
    }

    /**
     * Loads .jar files from the items/ folder.
     *
     * <p>Loading a jar means running its classes in the server JVM, so it is
     * controlled by the same {@code java-compilation} switch as the .java
     * files. Otherwise {@code java-compilation: false} — the mitigation
     * promised in SECURITY.md — would leave the RCE path open.
     */
    private void loadJarFiles(File itemsDir) {
        if (!plugin.getConfig().getBoolean("java-compilation", true)) {
            plugin.getLogger().info("[API] Runtime Java loading disabled (java-compilation: false)"
                    + " - .jar files in items/ are skipped.");
            return;
        }
        File[] jars = itemsDir.listFiles((d, n) -> n.endsWith(".jar"));
        if (jars != null) for (File jar : jars) loadFromJar(jar);
    }

    private void loadFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            List<String> classNames = new ArrayList<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    classNames.add(entry.getName().replace("/", ".").replace(".class", ""));
                }
            }
            URL jarUrl = jarFile.toURI().toURL();
            // URLClassLoader must remain open while registered items are in use.
            // It is tracked in jarClassLoaders and closed on reload/shutdown.
            URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, plugin.getClass().getClassLoader());
            jarClassLoaders.add(cl);
            for (String cn : classNames) {
                try {
                    Class<?> clazz = Class.forName(cn, false, cl);
                    if (AbstractCustomItem.class.isAssignableFrom(clazz)) {
                        AbstractCustomItem item = (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
                        registeredItems.put(item.getId(), item);
                        registeredItemIds.add(item.getId());
                    }
                } catch (Exception exception) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "[API] Failed to load class " + cn + " from " + jarFile.getName(), exception);
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "[API] Failed to read Java item jar " + jarFile.getName(), exception);
        }
    }

    private void closeJarClassLoaders() {
        for (URLClassLoader cl : jarClassLoaders) {
            try {
                cl.close();
            } catch (IOException ignored) {}
        }
        jarClassLoaders.clear();
    }

    private void logStats() {
        plugin.getLogger().info("[API] Loaded: " + registeredItems.size() + " items, "
            + registeredCommands.size() + " commands, " + registeredPlaceholders.size() + " placeholders"
            + ", " + registeredModules.size() + " modules");
    }

    public AbstractCustomItem getItem(String id) { return registeredItems.get(id); }

    /**
     * Snapshot of all registered Java items (read-only).
     *
     * Previously a live unmodifiable view of the map was returned: any
     * external iteration could fail with a ConcurrentModificationException during
     * a reload and could modify the contents of the map.
     */
    public Map<String, AbstractCustomItem> getAllItems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredItems));
    }

    public Set<String> getAllIds() { return new LinkedHashSet<>(registeredItems.keySet()); }

    /**
     * View of the item IDs without copying (thread-safe for iteration).
     */
    public Set<String> getItemIdsView() { return Collections.unmodifiableSet(registeredItemIds); }
    public int getItemCount() { return registeredItems.size(); }
    public CustomCommand getCommand(String name) { return registeredCommands.get(name.toLowerCase()); }

    /**
     * Snapshot of all registered commands (read-only).
     */
    public Map<String, CustomCommand> getAllCommands() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredCommands));
    }

    public int getCommandCount() { return registeredCommands.size(); }
    public CustomPlaceholder getPlaceholder(String id) { return registeredPlaceholders.get(id.toLowerCase()); }

    /**
     * Snapshot of all registered placeholders (read-only).
     */
    public Map<String, CustomPlaceholder> getAllPlaceholders() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredPlaceholders));
    }

    public int getPlaceholderCount() { return registeredPlaceholders.size(); }

    /**
     * Snapshot of all registered modules (read-only).
     */
    public Map<String, Module> getAllModules() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredModules));
    }

    public Module getModule(String id) { return registeredModules.get(id.toLowerCase()); }
    public int getModuleCount() { return registeredModules.size(); }
    public JavaItemCompiler getCompiler() { return compiler; }
    public int getCount() { return getItemCount(); }
    public boolean isLoading() { return loading.get(); }
}
