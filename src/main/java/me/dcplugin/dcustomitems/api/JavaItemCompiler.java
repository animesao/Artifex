package me.dcplugin.dcustomitems.api;

import me.dcplugin.dcustomitems.Main;
import me.dcplugin.dcustomitems.api.commands.CustomCommand;
import me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optimized Runtime compiler for .java files
 *
 * Optimizations:
 * - Caching of the compiled classes
 * - Skipping of the unchanged files
 * - Asynchronous compilation
 * - Minimal server load
 */
public class JavaItemCompiler {

    private final Main plugin;
    private final Path itemsDir;
    private final Path compiledDir;
    private final Path cacheDir;
    private final Map<String, byte[]> compiledClasses = new ConcurrentHashMap<>();
    private final Map<String, String> fileHashes = new ConcurrentHashMap<>(); // file -> hash
    private final Map<String, String> sourceClassAliases = new ConcurrentHashMap<>();
    private final Map<String, File> moduleSourceFolders = new ConcurrentHashMap<>();
    private CustomClassLoader classLoader;
    private final AtomicBoolean compiling = new AtomicBoolean(false);
    
    private static final String PACKAGE = "items";
    private static final long MAX_FILE_BYTES = 512 * 1024;
    private static final int MAX_FILES = 300;
    private static final String[] DANGEROUS_TOKENS = {
        "System.exit", "Runtime.getRuntime().exec", "ProcessBuilder", ".setAccessible("
    };

    /**
     * The imports that are added automatically if they are missing in the source.
     * They are checked by name: previously the import block was only added if
     * the file did not contain the line "import me.dcplugin.dcustomitems.api" at all,
     * and a file with a single import (for example RecipeDef) was not compiled.
     */
    private static final String[] DEFAULT_IMPORTS = {
        "me.dcplugin.dcustomitems.api.AbstractCustomItem",
        "me.dcplugin.dcustomitems.api.ItemAPI",
        "me.dcplugin.dcustomitems.api.commands.CustomCommand",
        "me.dcplugin.dcustomitems.api.placeholders.CustomPlaceholder"
    };

    /** The compiled templates for the class name replacement (one per alias). */
    private final List<Map.Entry<Pattern, String>> aliasPatterns = new ArrayList<>();

    public JavaItemCompiler(Main plugin) {
        this.plugin = plugin;
        this.itemsDir = plugin.getDataFolder().toPath().resolve("items");
        this.compiledDir = plugin.getDataFolder().toPath().resolve("compiled");
        this.cacheDir = plugin.getDataFolder().toPath().resolve("cache");
    }

    /**
     * Asynchronous compilation of all .java files
     * Returns a CompletableFuture with the result
     */
    public CompletableFuture<CompileResult> compileAllAsync() {
        if (compiling.get()) {
            plugin.getLogger().info("[JavaCompiler] Already compiling, skipping...");
            return CompletableFuture.completedFuture(new CompileResult());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            compiling.set(true);
            try {
                return compileAllInternal();
            } finally {
                compiling.set(false);
            }
        });
    }

    /**
     * Synchronous compilation (for use at startup)
     */
    public CompileResult compileAll() {
        return compileAllInternal();
    }

    private CompileResult compileAllInternal() {
        long startTime = System.currentTimeMillis();
        compiledClasses.clear();
        moduleSourceFolders.clear();
        CompileResult result = new CompileResult();

        try {
            Files.createDirectories(compiledDir);
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Cannot create directories: " + e.getMessage(), e);
            return result;
        }

        // Load the file hash cache
        loadFileHashesCache();

        // Look for .java files recursively, including the module subfolders.
        List<File> javaFiles = findJavaFiles(itemsDir.toFile());
        if (javaFiles.isEmpty()) {
            return result;
        }
        if (javaFiles.size() > MAX_FILES) {
            plugin.getLogger().severe("[JavaCompiler] Too many .java files: " + javaFiles.size() + " (limit " + MAX_FILES + "). Check items/.");
            result.errors.add("Too many .java files: " + javaFiles.size());
            return result;
        }

        // First the name table is built. This allows the files of one module
        // to reference each other after the compiler renames the classes.
        sourceClassAliases.clear();
        for (File javaFile : javaFiles) {
            String source = readFileContent(javaFile);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("public\\s+(?:final\\s+)?class\\s+(\\w+)")
                .matcher(source);
            if (matcher.find()) {
                String generatedName = getGeneratedClassName(javaFile);
                sourceClassAliases.put(matcher.group(1), generatedName);
                if (detectClassType(source) == ClassType.MODULE) {
                    moduleSourceFolders.put(PACKAGE + "." + generatedName, javaFile.getParentFile());
                }
            }
        }

        // The class name replacement patterns are compiled once, not for every file
        // (previously Pattern.compile was called for every alias in every file).
        buildAliasPatterns();

        plugin.getLogger().info("[JavaCompiler] Found " + javaFiles.size() + " .java files");

        int skipped = 0;
        int compiled = 0;

        List<File> toCompile = new ArrayList<>();

        for (File javaFile : javaFiles) {
            // Check whether the file has changed
            String currentHash = getFileHash(javaFile);
            String cacheKey = getCacheKey(javaFile);
            String cachedHash = fileHashes.get(cacheKey);
            
            if (currentHash.equals(cachedHash)) {
                // The file has not changed - skip the compilation
                String fullClassName = PACKAGE + "." + getGeneratedClassName(javaFile);
                
                // The whole class bundle is loaded from the cache: the main class and its
                // nested classes (for example Outer$MenuItem). Previously only
                // Outer.class was loaded here, which made the modules with
                // nested classes fail after a restart in the Skipped mode.
                Map<String, byte[]> cachedBundle = loadClassBundleFromCache(
                    fullClassName,
                    readFileContent(javaFile)
                );
                if (!cachedBundle.isEmpty()) {
                    compiledClasses.putAll(cachedBundle);
                    ClassType classType = detectClassType(readFileContent(javaFile));
                    addToResult(result, fullClassName, classType);
                    skipped++;
                    continue;
                }
            }

            toCompile.add(javaFile);
        }

        if (!toCompile.isEmpty()) {
            if (toCompile.size() > 1) {
                // Try batch compilation first
                if (batchCompile(toCompile, result)) {
                    compiled += toCompile.size();
                    for (File f : toCompile) {
                        fileHashes.put(getCacheKey(f), getFileHash(f));
                    }
                } else {
                    // Fall back to compiling one by one
                    for (File javaFile : toCompile) {
                        if (compileFile(javaFile, result)) {
                            compiled++;
                            fileHashes.put(getCacheKey(javaFile), getFileHash(javaFile));
                        }
                    }
                }
            } else {
                File javaFile = toCompile.get(0);
                if (compileFile(javaFile, result)) {
                    compiled++;
                    fileHashes.put(getCacheKey(javaFile), getFileHash(javaFile));
                }
            }
        }

        // Save the hash cache
        saveFileHashesCache();

        // Load the classes
        // A new loader is always created: otherwise the old classLoader with the
        // old classes remained after all .java files were deleted.
        CustomClassLoader loader = new CustomClassLoader(getClass().getClassLoader());
        for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
            loader.addClass(entry.getKey(), entry.getValue());
        }
        classLoader = loader;

        long elapsed = System.currentTimeMillis() - startTime;
        plugin.getLogger().info("[JavaCompiler] Compiled: " + compiled + ", Skipped: " + skipped + " (" + elapsed + "ms)");

        return result;
    }

    private boolean batchCompile(List<File> javaFiles, CompileResult result) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return false;
            }

            List<PreparedSource> prepared = new ArrayList<>(javaFiles.size());
            List<JavaFileObject> sources = new ArrayList<>(javaFiles.size());

            for (File javaFile : javaFiles) {
                if (javaFile.length() > MAX_FILE_BYTES) {
                    return false;
                }
                String sourceCode = readFileContent(javaFile);
                sourceCode = stripPackageDeclaration(sourceCode);
                for (String token : DANGEROUS_TOKENS) {
                    if (sourceCode.contains(token)) {
                        plugin.getLogger().warning("[JavaCompiler] ⚠ " + javaFile.getName() + " contains a dangerous call '" + token + "' - check the file manually (java-compilation executes arbitrary code).");
                        break;
                    }
                }

                sourceCode = injectDefaultImports(sourceCode);
                ClassType classType = detectClassType(sourceCode);
                String generatedClassName = getGeneratedClassName(javaFile);
                sourceCode = replaceClassNames(sourceCode);

                String finalSourceCode = "package " + PACKAGE + ";\n" + sourceCode;
                String fullClassName = PACKAGE + "." + generatedClassName;

                prepared.add(new PreparedSource(javaFile, generatedClassName, fullClassName, classType, finalSourceCode));

                sources.add(new SimpleJavaFileObject(
                    URI.create("string:///" + generatedClassName + ".java"),
                    JavaFileObject.Kind.SOURCE
                ) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return finalSourceCode;
                    }
                });
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            try {
                final Map<String, byte[]> classBytesHolder = new HashMap<>();

                JavaFileManager virtualFileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {
                    @Override
                    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                               JavaFileObject.Kind kind, FileObject sibling) {
                        return new SimpleJavaFileObject(
                            URI.create("mem:///" + className.replace('.', '/') + ".class"),
                            JavaFileObject.Kind.CLASS
                        ) {
                            @Override
                            public OutputStream openOutputStream() {
                                return new ByteArrayOutputStream() {
                                    @Override
                                    public void close() throws IOException {
                                        super.close();
                                        classBytesHolder.put(className, toByteArray());
                                    }
                                };
                            }
                        };
                    }
                };

                List<String> options = List.of(
                    "-classpath", getClasspath(),
                    "-d", compiledDir.toString(),
                    "-proc:none"
                );

                JavaCompiler.CompilationTask task = compiler.getTask(
                    null, virtualFileManager, diagnostics, options, null, sources
                );

                boolean success = task.call();
                if (success) {
                    compiledClasses.putAll(classBytesHolder);

                    for (Map.Entry<String, byte[]> entry : classBytesHolder.entrySet()) {
                        saveToCache(entry.getKey(), entry.getValue());
                        saveToCompiledDir(entry.getKey(), entry.getValue());
                    }

                    for (PreparedSource p : prepared) {
                        addToResult(result, p.fullClassName, p.classType);
                    }
                    return true;
                } else {
                    return false;
                }
            } finally {
                try {
                    fileManager.close();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static class PreparedSource {
        final File file;
        final String generatedClassName;
        final String fullClassName;
        final ClassType classType;
        final String finalSourceCode;

        PreparedSource(File file, String generatedClassName, String fullClassName, ClassType classType, String finalSourceCode) {
            this.file = file;
            this.generatedClassName = generatedClassName;
            this.fullClassName = fullClassName;
            this.classType = classType;
            this.finalSourceCode = finalSourceCode;
        }
    }

    private boolean compileFile(File javaFile, CompileResult result) {
        try {
            if (javaFile.length() > MAX_FILE_BYTES) {
                String err = javaFile.getName() + ": file >512KB - rejected (compiler overload protection)";
                plugin.getLogger().warning("[JavaCompiler] ❌ " + err);
                result.errors.add(err);
                return false;
            }
            String sourceCode = readFileContent(javaFile);
            // The compiler adds its own package declaration — the user's own
            // declaration has to be removed, otherwise the source would
            // contain two package statements and javac would refuse to build it.
            sourceCode = stripPackageDeclaration(sourceCode);
            for (String token : DANGEROUS_TOKENS) {
                if (sourceCode.contains(token)) {
                    plugin.getLogger().warning("[JavaCompiler] ⚠ " + javaFile.getName() + " contains a dangerous call '" + token + "' - check the file manually (java-compilation executes arbitrary code).");
                    break;
                }
            }
            String fileName = javaFile.getName().replace(".java", "");

            // Add the missing plugin API imports
            sourceCode = injectDefaultImports(sourceCode);

            ClassType classType = detectClassType(sourceCode);
            String generatedClassName = getGeneratedClassName(javaFile);
            sourceCode = replaceClassNames(sourceCode);

            final String finalSourceCode = "package " + PACKAGE + ";\n" + sourceCode;
            final String fullClassName = PACKAGE + "." + generatedClassName;

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                plugin.getLogger().severe("[JavaCompiler] JavaCompiler not found! JDK required.");
                return false;
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            try {
                JavaFileObject source = new SimpleJavaFileObject(
                    URI.create("string:///" + generatedClassName + ".java"),
                    JavaFileObject.Kind.SOURCE
                ) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return finalSourceCode;
                    }
                };

                final Map<String, byte[]> classBytesHolder = new HashMap<>();

                JavaFileManager virtualFileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(fileManager) {
                    @Override
                    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                               JavaFileObject.Kind kind, FileObject sibling) {
                        return new SimpleJavaFileObject(
                            URI.create("mem:///" + className.replace('.', '/') + ".class"),
                            JavaFileObject.Kind.CLASS
                        ) {
                            @Override
                            public OutputStream openOutputStream() {
                                return new ByteArrayOutputStream() {
                                    @Override
                                    public void close() throws IOException {
                                        super.close();
                                        classBytesHolder.put(className, toByteArray());
                                    }
                                };
                            }
                        };
                    }
                };

                List<String> options = List.of(
                    "-classpath", getClasspath(),
                    "-d", compiledDir.toString(),
                    "-proc:none"
                );

                JavaCompiler.CompilationTask task = compiler.getTask(
                    null, virtualFileManager, diagnostics, options, null, List.of(source)
                );

                boolean success = task.call();

                if (success) {
                    compiledClasses.putAll(classBytesHolder);

                    // Save to the cache
                    for (Map.Entry<String, byte[]> entry : classBytesHolder.entrySet()) {
                        saveToCache(entry.getKey(), entry.getValue());
                        // The class files are left on disk, so that the next source file
                        // from the same folder can use the already compiled class.
                        saveToCompiledDir(entry.getKey(), entry.getValue());
                    }

                    addToResult(result, fullClassName, classType);
                    return true;
                } else {
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                        String error = javaFile.getName() + ":" + diagnostic.getLineNumber() + " - " + diagnostic.getMessage(null);
                        plugin.getLogger().warning("[JavaCompiler] ❌ " + error);
                        result.errors.add(error);
                    }
                    return false;
                }
            } finally {
                // The FileManager holds resources — it is always closed, otherwise
                // an exception inside the compilation left open file descriptors.
                try {
                    fileManager.close();
                } catch (Exception ignored) {
                    // Closing must not break the compilation
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Error: " + javaFile.getName() + ": " + e.getMessage(), e);
            result.errors.add(javaFile.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private void addToResult(CompileResult result, String fullClassName, ClassType classType) {
        switch (classType) {
            case ITEM:
                result.items.add(fullClassName);
                break;
            case COMMAND:
                result.commands.add(fullClassName);
                break;
            case PLACEHOLDER:
                result.placeholders.add(fullClassName);
                break;
            case MODULE:
                result.modules.add(fullClassName);
                break;
            case UTILITY:
                // Compiled, but not registered (messages.java and similar)
                break;
        }
    }

    private String readFileContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("[JavaCompiler] Error reading file: " + file.getName());
            return "";
        }
    }

    private ClassType detectClassType(String sourceCode) {
        // The order matters: the non-utility files may mention the names of other types
        if (sourceCode.contains("extends Module")) return ClassType.MODULE;
        if (sourceCode.contains("extends CustomCommand")) return ClassType.COMMAND;
        if (sourceCode.contains("extends CustomPlaceholder")) return ClassType.PLACEHOLDER;
        if (sourceCode.contains("extends AbstractCustomItem")) return ClassType.ITEM;
        // The utilities (for example messages.java) are compiled, but not registered
        return ClassType.UTILITY;
    }

    private String replaceClassNames(String sourceCode) {
        if (aliasPatterns.isEmpty()) return sourceCode;
        String result = sourceCode;
        for (Map.Entry<Pattern, String> alias : aliasPatterns) {
            result = alias.getKey().matcher(result).replaceAll(Matcher.quoteReplacement(alias.getValue()));
        }
        return result;
    }

    /**
     * Compiles the templates for the class name replacement (the longer names
     * first, so that "Foo" does not swallow "FooBar").
     */
    private void buildAliasPatterns() {
        aliasPatterns.clear();
        List<Map.Entry<String, String>> aliases = new ArrayList<>(sourceClassAliases.entrySet());
        aliases.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> alias : aliases) {
            aliasPatterns.add(Map.entry(
                    Pattern.compile("\\b" + Pattern.quote(alias.getKey()) + "\\b"),
                    alias.getValue()));
        }
    }

    /**
     * Adds the missing plugin API imports.
     *
     * The check is done by name: if the file contains any import from the api
     * package, the old code did not add the others — and the file was not compiled.
     *
     * The imports are inserted AFTER the package declaration (if there is one):
     * they cannot go at the beginning of the file — javac requires the
     * package to be the first statement.
     */
    private String injectDefaultImports(String sourceCode) {
        List<String> imports = new ArrayList<>();
        for (String fqcn : DEFAULT_IMPORTS) {
            String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            // If the user declared a class with the same name, the API import is not needed
            // (and after the name replacement it would break).
            if (sourceClassAliases.containsKey(simpleName)) continue;
            if (sourceCode.contains("import " + fqcn + ";")) continue;
            imports.add(fqcn);
        }
        return insertImports(sourceCode, imports);
    }

    /**
     * Inserts the imports into the source.
     *
     * The imports are inserted AFTER the package declaration (if there is one):
     * they cannot go at the beginning of the file — javac requires the
     * package to be the first statement.
     */
    static String insertImports(String sourceCode, List<String> imports) {
        if (imports == null || imports.isEmpty()) return sourceCode;

        StringBuilder block = new StringBuilder();
        for (String fqcn : imports) {
            block.append("import ").append(fqcn).append(";\n");
        }

        int insertAt = findPackageDeclarationEnd(sourceCode);
        return sourceCode.substring(0, insertAt) + block + sourceCode.substring(insertAt);
    }

    /**
     * Removes the package declaration from the source (the compiler adds its own).
     *
     * Takes the BOM into account (a UTF-8 file with a BOM starts with U+FEFF)
     * and does not touch the lines inside block comments: the package must be
     * the first line of the file, not a random word in the text.
     */
    static String stripPackageDeclaration(String sourceCode) {
        String source = sourceCode;
        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            source = source.substring(1);
        }
        // The start-of-line anchor + only spaces/tabs before "package":
        // a multi-line \s* could capture a line inside a comment.
        return source.replaceFirst("(?m)^[ \\t]*package[ \\t]+[\\w.]+[ \\t]*;[^\\S\\n]*\\n?", "");
    }

    /**
     * Returns the index of the character right after the package declaration
     * (and its line break). If there is no declaration — 0.
     */
    static int findPackageDeclarationEnd(String sourceCode) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?m)^\\s*package\\s+[\\w.]+\\s*;")
                .matcher(sourceCode);
        if (!matcher.find()) return 0;
        int end = matcher.end();
        // The insertion is moved to the next line, so that the import is not
        // glued to the semicolon.
        int newline = sourceCode.indexOf('\n', end);
        return newline >= 0 ? newline + 1 : end;
    }

    private List<File> findJavaFiles(File directory) {
        List<File> files = new ArrayList<>();
        File[] children = directory.listFiles();
        if (children == null) return files;
        Arrays.sort(children, Comparator.comparing(File::getPath));
        for (File child : children) {
            if (child.isDirectory()) {
                // The sample and reference folders (EXAMPLE-*, EXAMPLES/, _template/)
                // are not compiled — they are reference material.
                String name = child.getName();
                if (name.startsWith("EXAMPLE-")
                        || name.equalsIgnoreCase("EXAMPLES")
                        || name.equalsIgnoreCase("_template")) continue;
                files.addAll(findJavaFiles(child));
            } else if (child.getName().endsWith(".java")
                    && !child.getName().startsWith("Abstract")
                    && !child.getName().startsWith("Custom")
                    && !child.getName().startsWith("EXAMPLE-")) {
                files.add(child);
            }
        }
        return files;
    }

    /**
     * The name of the generated class based on the file path.
     *
     * The name must be a valid Java identifier: previously the file
     * "1abc.java" produced the class "1abcItem", and "my.item.java" produced
     * "my.itemItem", and such a source was not compiled.
     */
    public String getGeneratedClassName(File javaFile) {
        String relative;
        try {
            relative = itemsDir.relativize(javaFile.toPath()).toString();
        } catch (Exception e) {
            relative = javaFile.getName();
        }
        return sanitizeGeneratedClassName(relative);
    }

    /**
     * Turns the relative source path into a valid Java identifier.
     */
    static String sanitizeGeneratedClassName(String relativePath) {
        String base = relativePath == null ? "" : relativePath.replace('\\', '_').replace('/', '_');
        if (base.toLowerCase(Locale.ROOT).endsWith(".java")) {
            base = base.substring(0, base.length() - ".java".length());
        }

        StringBuilder sb = new StringBuilder(base.length() + 5);
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            boolean valid = Character.isLetterOrDigit(c) || c == '_';
            sb.append(valid ? c : '_');
        }
        sb.append("Item");

        // A Java identifier cannot start with a digit
        if (sb.length() > 0 && Character.isDigit(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.toString();
    }

    private String getCacheKey(File javaFile) {
        try {
            return itemsDir.relativize(javaFile.toPath()).toString().replace('\\', '/');
        } catch (Exception e) {
            return javaFile.getName();
        }
    }

    // ===== CACHING =====

    private String getFileHash(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(file.lastModified());
        }
    }

    private void loadFileHashesCache() {
        try {
            Path cacheFile = cacheDir.resolve("file_hashes.properties");
            if (Files.exists(cacheFile)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(cacheFile));
                for (String key : props.stringPropertyNames()) {
                    fileHashes.put(key, props.getProperty(key));
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveFileHashesCache() {
        try {
            Path cacheFile = cacheDir.resolve("file_hashes.properties");
            Properties props = new Properties();
            props.putAll(fileHashes);
            props.store(Files.newOutputStream(cacheFile), "DC-CustomItems File Hashes Cache");
        } catch (Exception ignored) {}
    }

    private Map<String, byte[]> loadClassBundleFromCache(String mainClassName, String sourceCode) {
        Map<String, byte[]> bundle = new LinkedHashMap<>();
        try {
            Path mainClassPath = cacheDir.resolve(mainClassName.replace('.', '/') + ".class");
            if (!Files.exists(mainClassPath)) return bundle;

            String classPrefix = mainClassName + "$";
            Path packageDirectory = mainClassPath.getParent();
            if (packageDirectory == null || !Files.exists(packageDirectory)) return bundle;

            try (java.util.stream.Stream<Path> paths = Files.walk(packageDirectory)) {
                paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .forEach(path -> {
                        String relative = cacheDir.relativize(path).toString()
                            .replace(File.separatorChar, '.')
                            .replace('/', '.');
                        String className = relative.substring(0, relative.length() - ".class".length());
                        if (className.equals(mainClassName) || className.startsWith(classPrefix)) {
                            try {
                                bundle.put(className, Files.readAllBytes(path));
                            } catch (IOException ignored) {
                                // The whole bundle will be treated as unavailable below.
                            }
                        }
                    });
            }

            // A legacy cache can contain Outer.class but miss Outer$Inner.class.
            // Validate every named nested declaration before accepting Skipped.
            java.util.regex.Matcher nestedMatcher = java.util.regex.Pattern
                .compile("\\b(?:class|interface|enum|record)\\s+(\\w+)")
                .matcher(sourceCode);
            boolean firstDeclaration = true;
            while (nestedMatcher.find()) {
                if (firstDeclaration) {
                    firstDeclaration = false;
                    continue;
                }
                String nestedClassName = mainClassName + "$" + nestedMatcher.group(1);
                if (!bundle.containsKey(nestedClassName)) {
                    bundle.clear();
                    return bundle;
                }
            }
        } catch (Exception ignored) {
            bundle.clear();
        }
        return bundle;
    }

    private void saveToCache(String className, byte[] bytes) {
        try {
            Path cacheFile = cacheDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, bytes);
        } catch (Exception ignored) {}
    }

    private void saveToCompiledDir(String className, byte[] bytes) {
        try {
            Path classFile = compiledDir.resolve(className.replace('.', '/') + ".class");
            Files.createDirectories(classFile.getParent());
            Files.write(classFile, bytes);
        } catch (Exception ignored) {}
    }

    // ===== CLASSPATH =====

    private String getClasspath() {
        StringBuilder classpath = new StringBuilder();

        try {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.append(pluginFile.getAbsolutePath());
        } catch (Exception ignored) {
            classpath.append("plugins/DC-CustomItems.jar");
        }

        // The server jar: Paper/Spigot/Folia/Purpur and so on. It is searched in the
        // server root (plugins/..), in the current folder and by relative paths.
        File serverRoot = plugin.getDataFolder().getParentFile() != null
                ? plugin.getDataFolder().getParentFile().getParentFile() : null;
        List<String> candidates = new ArrayList<>();
        for (String name : new String[]{"server.jar", "paper.jar", "spigot.jar", "purpur.jar", "folia.jar", "paperclip.jar"}) {
            candidates.add(name);
            candidates.add("../" + name);
            if (serverRoot != null) {
                candidates.add(new File(serverRoot, name).getAbsolutePath());
            }
        }
        for (String path : candidates) {
            File serverJar = new File(path);
            if (serverJar.exists()) {
                classpath.append(File.pathSeparator).append(serverJar.getAbsolutePath());
                break;
            }
        }

        String[] libPaths = {"libraries", "../libraries"};
        for (String libPath : libPaths) {
            File libsDir = new File(libPath);
            if (libsDir.exists() && libsDir.isDirectory()) {
                addJarsFromClassDir(libsDir, classpath);
            }
        }

        // The plugins/DC-CustomItems/libs/ folder — any third-party libraries
        // (jars) that your Java modules need. Put a jar there and the
        // compiler will pick it up on the next /ci reload.
        File dataLibsDir = new File(plugin.getDataFolder(), "libs");
        if (dataLibsDir.exists() && dataLibsDir.isDirectory()) {
            addJarsFromClassDir(dataLibsDir, classpath);
        }

        // The jars of the installed plugins (VaultAPI, PlaceholderAPI, DeluxeMenus and so on).
        // This allows the modules to be compiled against the API of other plugins. If the
        // required plugin is missing, only its module is compiled and the error is limited to it.
        File pluginsDir = plugin.getDataFolder().getParentFile(); // .../plugins
        if (pluginsDir != null && pluginsDir.exists() && pluginsDir.isDirectory()) {
            addJarsFromClassDir(pluginsDir, classpath);
        }

        classpath.append(File.pathSeparator).append(compiledDir.toString());
        return classpath.toString();
    }

    private void addJarsFromClassDir(File dir, StringBuilder classpath) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    addJarsFromClassDir(file, classpath);
                } else if (file.getName().endsWith(".jar")) {
                    classpath.append(File.pathSeparator).append(file.getAbsolutePath());
                }
            }
        }
    }

    // ===== CLASS LOADING =====

    public Class<?> loadClass(String fullClassName) {
        if (classLoader == null) return null;
        try {
            return classLoader.findClass(fullClassName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public AbstractCustomItem createItemInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && AbstractCustomItem.class.isAssignableFrom(clazz)) {
                return (AbstractCustomItem) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating item: " + fullClassName);
        }
        return null;
    }

    public CustomCommand createCommandInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && CustomCommand.class.isAssignableFrom(clazz)) {
                CustomCommand cmd = (CustomCommand) clazz.getDeclaredConstructor().newInstance();
                cmd.setPlugin(plugin);
                return cmd;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating command: " + fullClassName);
        }
        return null;
    }

    public me.dcplugin.dcustomitems.api.modules.Module createModuleInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && me.dcplugin.dcustomitems.api.modules.Module.class.isAssignableFrom(clazz)) {
                File moduleFolder = moduleSourceFolders.get(fullClassName);
                if (moduleFolder == null) {
                    moduleFolder = new File(plugin.getDataFolder(), "items/" + moduleIdFromClassName(fullClassName));
                }
                String moduleId = moduleFolder.getName();
                return (me.dcplugin.dcustomitems.api.modules.Module) clazz
                    .getDeclaredConstructor(Main.class, String.class, File.class)
                    .newInstance(plugin, moduleId, moduleFolder);
            }
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[JavaCompiler] Error creating module: " + fullClassName + " - " + e.getMessage(), e);
        }
        return null;
    }

    private String moduleIdFromClassName(String fullClassName) {
        String simpleName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        if (simpleName.endsWith("_Item")) simpleName = simpleName.substring(0, simpleName.length() - 5);
        int separator = simpleName.indexOf('_');
        return separator > 0 ? simpleName.substring(0, separator) : simpleName;
    }

    public CustomPlaceholder createPlaceholderInstance(String fullClassName) {
        try {
            Class<?> clazz = loadClass(fullClassName);
            if (clazz != null && CustomPlaceholder.class.isAssignableFrom(clazz)) {
                CustomPlaceholder ph = (CustomPlaceholder) clazz.getDeclaredConstructor().newInstance();
                ph.setPlugin(plugin);
                return ph;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[JavaCompiler] Error creating placeholder: " + fullClassName);
        }
        return null;
    }

    public void clear() {
        compiledClasses.clear();
        fileHashes.clear();
        sourceClassAliases.clear();
        moduleSourceFolders.clear();
        aliasPatterns.clear();
        classLoader = null;
    }

    public boolean isCompiling() {
        return compiling.get();
    }

    // ===== CLASSES =====

    public enum ClassType { ITEM, COMMAND, PLACEHOLDER, MODULE, UTILITY }

    public static class CompileResult {
        public int compiled = 0;
        public List<String> items = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
        public List<String> placeholders = new ArrayList<>();
        public List<String> modules = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
    }

    private static class CustomClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytes = new HashMap<>();

        CustomClassLoader(ClassLoader parent) { super(parent); }

        void addClass(String name, byte[] bytes) {
            classBytes.put(name, bytes);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes.get(name);
            if (bytes != null) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            throw new ClassNotFoundException(name);
        }
    }
}
