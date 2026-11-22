# Module Development

This guide covers dynamic modules loaded by Artifex `ModuleManager`. For custom item classes,
see [JAVA_API_EN.md](JAVA_API_EN.md).

## Lifecycle guarantees

Artifex completes database initialization before loading modules:

1. `DatabaseManager` is constructed and connected.
2. Connection readiness is verified with `isConnected()`.
3. Modules are discovered and loaded.
4. Module `onEnable` callbacks execute.

Inside `onEnable`, `Main.getInstance().getDatabaseManager()` is available and connected. Do not
call `connect()` yourself and do not open a separate SQLite file. Use the shared manager for
tables, queries and transactions.

## Rules for modules

- Keep business logic in Java; use YAML/JSON for settings, content definitions and templates.
- Register listeners through the plugin instance and unregister them in `onDisable`.
- Guard optional dependencies; a module must not assume another module is enabled.
- Use async database methods for slow queries.
- Do not duplicate `DatabaseManager` schema initialization logic.

## Database unavailable

If the database cannot be initialized, Artifex reports an explicit unavailable state instead of
passing a half-initialized manager to modules. Modules should handle a failed query or an
unavailable manager without throwing during enable/disable.

## Reload

`/artifex reload` recompiles and reloads modules. Disable state and listeners correctly so that
reloads do not leak tasks or duplicate registrations.
