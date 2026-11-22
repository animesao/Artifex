# Installing Artifex 2.5.1

Artifex is a standalone modular Java plugin for Paper/Spigot 1.21.8+ running on Java 21.

## Requirements

- Paper or Spigot 1.21.8+
- Java 21 runtime
- Optional: PlaceholderAPI, Vault, DeluxeMenu

## Install

1. Download `Artifex-2.5.1.jar` and `SHA256SUMS.txt` from the
   [2.5.1 release](https://github.com/animesao/Artifex/releases/tag/v2.5.1).
2. Verify the JAR checksum:

   ```sh
   sha256sum -c SHA256SUMS.txt
   ```

3. Copy `Artifex-2.5.1.jar` into the server's `plugins/` directory.
4. Start the server once. Artifex creates `plugins/Artifex/config.yml`,
   `items/messages.java`, and (with the default configuration) `data.db`.
5. Stop the server before adding modules or editing configuration.

A fresh installation ships no example content. Examples live in the repository's `samples/`
directory and are copied manually into `items/` if needed.

## Verify startup

The log should show the database connecting before modules are discovered:

```
[Artifex] Database connected: SQLite
[Modules] Found N potential modules
[Modules] Loaded N modules
```

A severe message such as `[DB] Database is unavailable` indicates a broken startup and must be investigated.

## Upgrade from 2.4.0

1. Back up `plugins/Artifex/`, including `data.db` and `items/`.
2. Replace the old JAR with `Artifex-2.5.1.jar`.
3. Start the server. Existing SQLite data is preserved; no destructive migration is performed.

## Security note

Runtime Java compilation from `items/` executes arbitrary code on the server. Set
`java-compilation: false` in `config.yml` if untrusted users can write to the plugin folder.
See [SECURITY.md](SECURITY.md).

See also: [CONFIGURATION.md](CONFIGURATION.md), [ARCHITECTURE.md](ARCHITECTURE.md),
[docs/GETTING_STARTED_EN.md](docs/GETTING_STARTED_EN.md).
