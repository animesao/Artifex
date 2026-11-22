# Configuring Artifex 2.5.0

The single configuration file is `plugins/Artifex/config.yml`. Content definitions live in
`plugins/Artifex/items/` as YAML (`.yml`) or Java (`.java`) files. Messages are overridable in
`items/messages.java`.

## Top-level sections

| Section | Purpose |
| --- | --- |
| `java-compilation` | Enables or disables runtime compilation of `.java`/`.jar` modules |
| `security.strict-mode` | Rejects dangerous system calls in runtime-compiled Java |
| `debug-mode` | Verbose diagnostics; keep disabled on production |
| `performance` | Refresh intervals and action limits |
| `file-logging` | Writes plugin messages to `logs/latest.log` instead of the console |
| `backup` | Backups of `items/`, `config.yml` and `data.db` before `/artifex reload` |
| `crafting` | Protections for custom items in vanilla recipes |
| `database` | SQLite/MySQL storage selection and connection settings |
| `set-bonuses` | Effects granted by complete armor sets |

## Database

SQLite is the default and requires no external service:

```yaml
database:
  type: sqlite
```

MySQL is optional. Credentials should be supplied through environment variables rather than
committed configuration. See [docs/DATABASE_EN.md](docs/DATABASE_EN.md) and
[docs/DATABASE_RU.md](docs/DATABASE_RU.md).

Database initialization always completes before dynamic modules load. Modules receive a
connected `DatabaseManager` in `onEnable`; they must not create independent database files.

## Content boundaries

- YAML/JSON: configuration, content definitions, parameters, module settings, templates.
- Java: business logic, events, handlers, services, validation, persistence, API, security.
- DatabaseManager: connection lifecycle, connection-health checks, queries and transactions.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the lifecycle and module boundaries.
