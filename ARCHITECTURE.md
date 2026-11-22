# Artifex Architecture

Artifex 2.5.1 is an independent, universal modular Java core for Minecraft servers. It provides
the engine and shared infrastructure; project-specific products are built on top of it as
separate projects and modules.

## Core responsibilities

- Plugin bootstrap and lifecycle (`PluginBootstrap`, `Main`).
- `ModuleManager` discovery, loading, enabling and disabling of dynamic modules.
- `DatabaseManager` connection lifecycle, SQLite/MySQL access, query helpers and connection-health checks.
- Commands, events, services, validation, configuration and general-purpose Java APIs.
- Security controls for runtime Java compilation.

## Startup order

```
1. Configuration and managers
2. DatabaseManager construction
3. SQLite/MySQL connection and `isConnected()` readiness verification
4. Dynamic Java module loading
5. Module onEnable callbacks
6. Commands, listeners and update check
```

`initializeDatabaseBeforeModules(...)` enforces steps 2–5. A module never enables before its
database dependency is available. Regression tests cover this ordering.

## Storage boundaries

`DatabaseManager` is the single shared persistence mechanism. Modules use its API instead of
opening their own database files or duplicating schema logic. YAML files remain the format for
configuration and content definitions; they are not a replacement for Java business logic.

## Extension model

- Java modules implement behavior, events, handlers and services.
- YAML/JSON files describe configuration, content, parameters and templates.
- Dynamic Java modules are loaded at runtime through `ModuleManager` after database readiness.

## Scope

Artifex does not include project-specific integrations, bot clients, external profile products
or RPG-specific modules. Those belong to downstream projects built on Artifex.
