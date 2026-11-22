# Database Initialization Order Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Artifex connect DatabaseManager before module `onEnable()` callbacks run.

**Architecture:** Add a tiny package-private helper in `PluginBootstrap` that executes database initialization before module loading. Unit-test the helper with ordered callbacks, then call it from `enable()`. No production server, production database, or released JAR is touched.

**Tech Stack:** Java 21, Maven, JUnit 5.

---

## File structure

- Modify: `src/main/java/me/dcplugin/artifex/bootstrap/PluginBootstrap.java`
- Create: `src/test/java/me/dcplugin/artifex/bootstrap/PluginBootstrapTest.java`

## Tasks

### Task 1: Failing order test

**Files:**
- Test: `src/test/java/me/dcplugin/artifex/bootstrap/PluginBootstrapTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run the test and verify RED**

Run: `mvn -q -Dtest=PluginBootstrapTest test`
Expected: compilation failure because `initializeDatabaseBeforeModules` does not exist.

### Task 2: Minimal implementation

**Files:**
- Modify: `src/main/java/me/dcplugin/artifex/bootstrap/PluginBootstrap.java`

- [ ] **Step 1: Add the helper and use it in enable()**

Replace:

```java
        moduleManager.loadAll();
```

with:

```java
        initializeDatabaseBeforeModules(this::initDatabase, () -> moduleManager.loadAll());
```

and remove the later standalone `initDatabase();` call under the `// Database` comment.

Add this package-private static method before `initDatabase()`:

```java
    static void initializeDatabaseBeforeModules(Runnable initDatabase, Runnable loadModules) {
        initDatabase.run();
        loadModules.run();
    }
```

- [ ] **Step 2: Run the test and verify GREEN**

Run: `mvn -q -Dtest=PluginBootstrapTest test`
Expected: PASS.

### Task 3: Full build and disposable validation

- [ ] **Step 1: Run all tests**

Run: `mvn -q test`
Expected: PASS with no failures.

- [ ] **Step 2: Build the JAR**

Run: `mvn -q -DskipTests package`
Expected: `target/Artifex-2.5.0.jar` exists.

- [ ] **Step 3: Validate only on the existing disposable server**

Copy the newly built JAR into the disposable test copy only. Start through its isolated launcher, confirm no `[DB] Database is unavailable` warnings at startup, confirm `Database connected: SQLite` appears before module initialization, and confirm synthetic module records load without `/artifex reload`.

Do not copy the JAR to production or the master backup.
