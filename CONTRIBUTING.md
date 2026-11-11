# Contributing to Artifex

Thank you for your interest in improving Artifex! This guide will help you contribute.

## How to Contribute

### 1. Fork and Clone

```bash
git clone https://github.com/YOUR_USERNAME/Artifex.git
cd Artifex
```

### 2. Create a Branch

```bash
git checkout -b feature/your-feature
```

### 3. Build

```bash
mvn package
```

The jar will be in `target/Artifex-*.jar`

### 4. Make Changes

- Follow existing code style
- Add Javadoc to public methods
- Add/update unit tests for changed logic (`mvn test` must pass)
- Test on Paper 1.21.8+

### 5. Commit and Push

Use [Conventional Commits](https://www.conventionalcommits.org/) so release
notes and changelogs stay readable:

```bash
git checkout -b feature/your-feature   # или fix/your-fix
git add .
git commit -m "feat: add charge cooldown to triggers"
git commit -m "fix: attribute removal on unequip"
git push origin feature/your-feature
```

Commit types: `feat` (новая фича), `fix` (исправление), `docs`, `test`,
`refactor`, `chore`. Ветки: `feature/*`, `fix/*`, `docs/*`.

### 6. Create a Pull Request

- Describe the changes
- Mention the issue it solves
- Add screenshots (if applicable)

---

## Project Structure

```
src/main/java/me/dcplugin/dcustomitems/
├── Main.java                    # Entry point (delegates to PluginBootstrap)
├── bootstrap/
│   ├── PluginBootstrap.java     # Component initialization
│   ├── CommandRegistrar.java    # Dynamic command registration
│   └── ConfigMigrator.java      # Config migration
├── handlers/
│   ├── CustomItemHandler.java   # Custom item manager
│   ├── ItemLoader.java          # YAML loading
│   ├── LoreManager.java         # Lore templates
│   ├── UsesManager.java         # Uses counter
│   └── EquippedItemsChecker.java # Global equipment checker
├── managers/
│   ├── EffectManager.java       # Potion effects
│   ├── AttributeManager.java    # Item attributes
│   └── ArmorSetManager.java     # Armor sets
├── api/                         # Java API
├── listeners/                   # Event listeners
├── models/                      # Data models
└── utils/
    ├── EnumCache.java           # Bukkit enum caching
    ├── ColorUtils.java          # Colors + PAPI
    └── ItemBuilder.java         # Item builder
```

## Code Rules

### Java

- Use Java 21 (target of the build; Paper 1.21.x requires Java 21 at runtime)
- Follow Google Java Style Guide
- Add Javadoc to public methods
- Use `EnumCache` for all Bukkit enum lookups (Material, Particle, Sound, etc.)
- Prefer global BukkitRunnable over per-player tasks

### YAML

- Use 2 spaces for indentation
- Add comments for complex parameters
- Group related settings

### Modules

- Place new modules in `src/main/resources/items/<module-name>/`
- Include `config.yml` with `enabled: true/false`
- Include a README.md with usage instructions

---

## Building

```bash
# Compile
mvn compile

# Package
mvn package

# Clean + Package
mvn clean package
```

---

## Testing

1. Build the plugin: `mvn package`
2. Copy `target/DC-CustomItems-*.jar` to your server's `plugins/`
3. Restart the server
4. Test with `/ci reload` and `/give`

---

## Communities

- **GitHub Issues**: For bug reports and suggestions
- **Discord**: For community chat

---

## License

By contributing, you agree that your works will be licensed under the MIT License.
