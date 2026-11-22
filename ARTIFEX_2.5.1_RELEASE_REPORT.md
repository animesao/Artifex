# Artifex 2.5.1 — Stable Release Report

Publication date: 2026-11-22
Status: **PUBLISHED — stable Latest Release.** Not a pre-release.

## 1. Publication details

| Item | Value |
| --- | --- |
| Repository | `https://github.com/animesao/Artifex` |
| Release branch | `release/2.5.1` |
| Release commit (tag target) | `24d2be6ea0d7ba6eae1ecb2906ff755c0a89bc49` |
| Annotated tag | `v2.5.1` |
| Tag object | `955a586c249091bed86dd6a54787f66e69b6574e` |
| Release page | `https://github.com/animesao/Artifex/releases/tag/v2.5.1` |
| Release ID | `454368032` |
| Release name | `Artifex 2.5.1` |
| `target_commitish` | `release/2.5.1` |
| Pre-release | **false** |
| Draft | false |
| Latest stable release | **Yes** — `/releases/latest` resolves to this release |
| Release author | `animesao` via authenticated GitHub REST API |
| Published at | `2026-11-22T10:32:35Z` |

## 2. Branch, tag and commit provenance

- Stable base: `release/2.5.0` at `49f59aca791ab27d1e4c4aabdbea91cb0d410276`.
- Functional release commit: `fa48f266e08cd682f7d20287f6a1503fd8c7a117` — `fix: complete Java module shutdown lifecycle`.
- Checksum alignment commit: `24d2be6ea0d7ba6eae1ecb2906ff755c0a89bc49` — `chore: align release checksum with CI build`.
- Annotated tag `v2.5.1` points to `24d2be6ea0d7ba6eae1ecb2906ff755c0a89bc49`.
- Main was advanced from stale local `8204bbd` to remote `23aa224` by fast-forward, then merged with the release line.
- Post-publication workflow fix: `b191fa033187a116d2ed4f3ecf4a6dd1ebd89810` — `fix: read release checksum from correct field`.
- Final main head at publication: `e01e9f365f151dbb605ee96797700cf23dcabab2`.
- No `git reset`, `git clean`, `git stash`, force push, history rewrite or tag deletion was performed.

## 3. Pre-publication Git audit

- Git root: `D:/Artifex`; remote: `https://github.com/animesao/Artifex.git`.
- Audited worktrees: `D:/Artifex` (`main`), `D:/Artifex-worktrees/2026-11-22-init-api` (`release/2.5.0`), `D:/Artifex-worktrees/module-lifecycle-shutdown` (`fix/module-lifecycle-shutdown`), `D:/Artifex-worktrees/release-2.5.1` (`release/2.5.1`).
- Audited refs: `main`, `release/2.5.0`, `v2.5.0`, `fix/module-lifecycle-shutdown`, post-tag commits and all uncommitted changes.
- GitHub audit: repository public, default branch `main`, authenticated user `animesao`, admin/push permissions present.
- `v2.5.0` and `v2.5.0-rc.1` tags and releases were verified and left unchanged.
- `git diff --check` returned exit code 0 on the final release tree.

## 4. Lifecycle fix in Artifex Core

- `PluginBootstrap.disable()` now calls `ModuleManager.unloadAll()` after dynamic command unregistration and before placeholder, registry and database shutdown.
- `ModuleManager.unloadAll()` isolates `Throwable` failures per module, logs each failure, and clears the registry in `finally`.
- `Module.enable()` rolls back `enabled` state and unregisters commands registered before a failed `onEnable()`.
- `CommandMapAccessor` retries failed lookups, resets on enable/disable and does not throw in unit tests without Bukkit.
- Dynamic command unregistration falls back to `cmd.unregister(commandMap)` when `knownCommands` is unavailable and always clears tracked collections.
- `DatabaseManager.shutdownAsync()` drains async work, completes pending futures exceptionally on timeout and owns `disconnect()`.
- `ItemRegistry.shutdown()` unregisters custom commands/placeholders, clears registries and compiler state, and closes classloaders last.
- Every cleanup step in full shutdown is `Throwable`-safe; recipe cleanup clears both YAML and Java recipes.
- No WebBridge, LETHAVARYN, Discord, OAuth or private integration code was added to Artifex Core.

## 5. Universal module lifecycle invariants

- Every active module receives `onDisable()` exactly once during full shutdown.
- One failing module cannot prevent other modules from disabling.
- Repeated `disable()` calls are idempotent and do not call `onDisable()` twice.
- Database, registry, placeholders and module folders remain available during `onDisable()`.
- A module may write pending queue state to disk during shutdown before the database is disconnected.
- The public Java module contract is unchanged: `extends Module`, `(Main, String, File)` constructor, `onEnable()`, `onDisable()` and `registerDynamicCommand(...)`.

## 6. Unit and integration test suite

- Final clean Maven run: `mvn -B clean package`, `BUILD SUCCESS`.
- Surefire totals: **16 suites, 98 tests, 0 failures, 0 errors, 0 skipped**.
- New/updated coverage:
  - `PluginBootstrapShutdownTest`: 8 tests for ordering, exactly-once shutdown, error isolation, idempotence, dependency availability, recipe cleanup and disk spill during `onDisable()`.
  - `CommandCleanupTest`: 3 tests for dynamic-command fallback cleanup and failed-enable rollback.
  - `DatabaseManagerTest`: async shutdown drain and waiter completion.
  - `ItemRegistryShutdownTest`: custom command/placeholder cleanup.
  - `ConfigMigratorTest`: `performance` is not misclassified as a legacy item.
- Mockito self-attach and JaCoCo warnings were non-fatal; the build and suite passed.

## 7. Build reproducibility and checksum alignment

- Two local clean builds under the same JDK produced the same JAR SHA-256: `22115841b118753d33b6ec0f09ab80db2fa6d117b66910dd4c3d74262dfe2b77`.
- CI builds with JDK 21; local verification used JDK 25. The observed difference is expected from `Build-Jdk-Spec` and compiler output, not source divergence.
- CI artifact SHA-256, verified across two CI runs: `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83`.
- `release/2.5.1/SHA256SUMS.txt` was aligned to the CI-built artifact before tagging.
- The release workflow now fails if the committed checksum file is missing or does not match the built JAR.

## 8. Disposable Paper 26.3 runtime verification

- Environment: `D:\LETHAVARYN-RESTORE-TEST\2026-11-22_ARTIFEX-251\paper-runtime\server`.
- Binding: `127.0.0.1:25594`, RCON `127.0.0.1:25595`, synthetic RCON password only.
- Artifact tested: the exact CI-built JAR with SHA-256 `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83`.
- Sequence: fresh prepare → startup → `/ci reload` → full stop → second startup → second stop → verifier.
- Verifier result: **26/26 PASS, 0 failures**.
- Confirmed: plugin enabled as v2.5.1, runtime Java compiler discovered 2 sources, one module discovered and loaded, dynamic command registered, reload called `onDisable()` once and `onEnable()` again, shutdown called `onDisable()` exactly once while SQLite was still connected, database disconnected only after module disable, queue spill persisted to disk, second start clean, final listeners `0`.
- No `EXAMPLE-webbridge` was activated in the disposable runtime.

## 9. Configuration migration check

- Added `performance` to the known configuration section set in `ConfigMigrator`.
- `ConfigMigratorTest` verifies a legacy item key is reported while `performance` is not.
- Disposable startup produced no false item-migration warning for `performance`.
- Existing configurations, items, worlds, player data and database content were not migrated or reset.

## 10. Module compatibility and WebBridge harness

- WebBridge compatibility suite was run only from a disposable copy under `D:\LETHAVARYN-RESTORE-TEST\2026-11-22_ARTIFEX-251\webbridge-compat`.
- Results against the exact CI artifact: `ArtifexWebBridgeTest` **3/3 passed**, `WebBridgeRuntimeTest` **4/4 passed**, `WebBridgeContractTest` **6/6 passed**.
- Runtime Java compilation, YAML item loading, PlaceholderAPI/Vault-facing behavior, GUI, crafting and dynamic commands remain compatible.
- The harness copied files read-only; no production WebBridge file was modified.
- No WebBridge-specific module or integration was activated in the release runtime.

## 11. Secret and sensitive-path scan

- Broad text scan: 243 files; 71 pattern matches were reviewed.
- All broad matches were documentation placeholders, env-var names, GitHub Actions token references or dangerous-call identifiers in source hygiene code.
- Focused scan for production paths, LETHAVARYN/WebBridge/Discord/OAuth strings and credential formats: 7 matches, all in the historical `ARTIFEX_2.5.0_STABLE_RELEASE_REPORT.md`; no live secrets and no core code references.
- No token was printed or written to a repository file; cached Git credentials were held only in PowerShell variables.

## 12. Version target audit

- `VERSION`: `2.5.1`.
- `pom.xml`: `<version>2.5.1</version>`.
- `src/main/resources/plugin.yml`: `version: 2.5.1`.
- README badge, installation URL, documentation, changelog and release scripts target `2.5.1`.
- Remaining `2.5.0` strings are historical changelog/report/checksum references and were intentionally preserved.

## 13. CI status and checksum guard

| Run | Head | Conclusion | Note |
| --- | --- | --- | --- |
| `45010973226` | `9d2af16c93f` | success | First main push; build succeeded, checksum mismatch investigation began |
| `45011702836` | `0ef935db63a` | success | Committed checksum aligned to CI artifact; artifact hash verified as `cdfcc33...` |
| `45012106986` | `24d2be6ea0d` | failure | Tag run release job failed because the workflow parsed the checksum line with `$NF` |
| `45012875060` | `e01e9f365f1` | success | Parser fixed to read `$1`; build job success, release job skipped on non-tag push |

- The tag workflow failure was a workflow parsing defect, not a source or artifact defect.
- The final workflow fix is on `release/2.5.1` (`b191fa0`) and `main` (`e01e9f3`).
- The tagged release was therefore created manually through the GitHub REST API using the verified CI artifact.

## 14. Release assets and download verification

| Asset | ID | Size (bytes) | SHA-256 |
| --- | --- | --- | --- |
| `Artifex-2.5.1.jar` | `778695679` | 19,119,951 | `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83` |
| `SHA256SUMS.txt` | `778695838` | 85 | `938baf6cfd5852701d8b955228872a55ff16d17b74b8b6cd0463efb8c430441d` |

- Both assets were downloaded from public `v2.5.1` URLs and re-hashed locally.
- Downloaded JAR SHA-256 matches the committed checksum and the CI artifact: `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83`.
- Downloaded `SHA256SUMS.txt` is byte-identical to the local copy.
- `SHA256SUMS.txt` contains exactly: `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83  Artifex-2.5.1.jar`.
- Exactly two assets are attached; no logs, databases, runtime files, player data or private material were uploaded.

## 15. Safety confirmations

- Production server path `C:\Users\Anime\Desktop\server` was not modified.
- No production JAR, config, database, world, playerdata, inventory, start script, proxy or Iris setting was changed.
- The production server was not started, stopped or connected to by RCON.
- Master backups were not modified.
- `v2.5.0`, `v2.5.0-rc.1`, their tags and their GitHub releases were left unchanged.
- `EXAMPLE-webbridge` remained inactive.
- All development and builds occurred in `D:\Artifex-worktrees\release-2.5.1`; all runtime/harness work occurred under `D:\LETHAVARYN-RESTORE-TEST`.

## 16. Preserved behavior and exclusions

- `/ci reload`, runtime Java compilation and dynamic commands remain functional.
- SQLite and MySQL support, YAML item loading, config loading, PlaceholderAPI, Vault, GUI and crafting remain functional.
- The Java module API signatures and constructor contract were not changed.
- No Discord API, OAuth, account linking, identity validation, private HTTP integration, WebBridge code or LETHAVARYN-specific behavior was introduced.
- Published artifacts are unchanged by this report.

## 17. Final state

- Remote branch `release/2.5.1` and annotated tag `v2.5.1` are present.
- `main` is synchronized with the release line and the post-publication workflow fix.
- GitHub Release `v2.5.1` is published, not a draft, not a pre-release, and is the current `Latest` release.
- The release has exactly two checksum-verified public assets.
- Local release and main working trees are clean apart from this final report commit.
- This report documents the completed 2.5.1 release and does not alter the release artifact or tag target.

## 18. Independent post-publication verification (2026-11-22)

This section records a second, independent verification performed after publication from a fresh detached worktree at `main` (`818934f32152298c4cce10ce67285fd8adec7c3e`). No source code, tag, GitHub Release, asset, production server or deployed environment was changed by this verification.

### 18.1 Git and source

- Git root: `D:/Artifex`; remote: `https://github.com/animesao/Artifex.git` (fetch and push URLs identical).
- Worktrees audited: `D:/Artifex` (`main`), `D:/Artifex-worktrees/2026-11-22-init-api` (`release/2.5.0`), `D:/Artifex-worktrees/module-lifecycle-shutdown` (`fix/module-lifecycle-shutdown`), `D:/Artifex-worktrees/release-2.5.1` (`release/2.5.1`), plus a temporary detached verification worktree.
- `fix/module-lifecycle-shutdown` still contains three uncommitted local changes (`ModuleManager.java`, `PluginBootstrap.java`, untracked `PluginBootstrapShutdownTest.java`). They are an earlier, narrower version of the fix and were deliberately preserved untouched; they are not required because the complete fix is already in `main` and in tag `v2.5.1`.
- `git diff --check 818934f` returned exit code 0.
- `VERSION`, `pom.xml` and `src/main/resources/plugin.yml` all report `2.5.1`. Remaining `2.5.0` references are historical changelog/report/checksum records and one old internal plan.

### 18.2 Source components reviewed directly

- `PluginBootstrap.disable()` calls `moduleManager.unloadAll()` after command unregistration and before placeholder, API-registry, database, cooldown and log shutdown; every step is wrapped in `runCleanup(..., Throwable)`.
- `ModuleManager.unloadAll()` isolates per-module `Throwable`, logs each with a stack trace, and clears its registry in `finally`; `loadAll()` begins by `unloadAll()`, so reload is idempotent.
- `Module.enable()`/`disable()` guard on `enabled`; failed `onEnable()` rolls state back and unregisters commands; `disable()` calls `onDisable()` once and unregisters dynamic commands in `finally`.
- `DatabaseManager.shutdownAsync()` owns connection shutdown; `ItemRegistry.shutdown()` stops the reload executor, unregisters listeners/commands/placeholders and closes classloaders last.
- The public Java module contract (`extends Module`, `(Main, String, File)`, `onEnable()`, `onDisable()`, `registerDynamicCommand(...)`) is unchanged.

### 18.3 Changed Java files in 2.5.1 relative to v2.5.0

- `src/main/java/me/dcplugin/artifex/api/ItemRegistry.java`
- `src/main/java/me/dcplugin/artifex/api/JavaItemCompiler.java`
- `src/main/java/me/dcplugin/artifex/api/database/DatabaseManager.java`
- `src/main/java/me/dcplugin/artifex/api/modules/Module.java`
- `src/main/java/me/dcplugin/artifex/api/modules/ModuleManager.java`
- `src/main/java/me/dcplugin/artifex/bootstrap/CommandRegistrar.java`
- `src/main/java/me/dcplugin/artifex/bootstrap/ConfigMigrator.java`
- `src/main/java/me/dcplugin/artifex/bootstrap/PluginBootstrap.java`
- `src/main/java/me/dcplugin/artifex/handlers/ItemLoader.java`
- `src/main/java/me/dcplugin/artifex/utils/CommandMapAccessor.java`
- Tests: `ItemRegistryShutdownTest.java`, `DatabaseManagerTest.java`, `CommandCleanupTest.java`, `ConfigMigratorTest.java`, `PluginBootstrapShutdownTest.java`.

### 18.4 Fresh build and tests

- Command: `mvn -B clean package` in the detached verification worktree under JDK 25.0.2 / Maven 3.9.15.
- Result: `BUILD SUCCESS`; 63 main sources and 16 test sources compiled; Surefire: **16 suites, 98 tests, 0 failures, 0 errors, 0 skipped**.
- `PluginBootstrapShutdownTest`: 8/8 passed, including single/multiple modules, exactly-once `onDisable()`, error isolation, repeated `disable()`, dependency availability, recipe cleanup and disk spill before database shutdown.
- Mockito self-attach, native-access and shade overlap warnings were non-fatal; no compilation error occurred.

### 18.5 Build hash difference and published assets

- Fresh local JDK 25 JAR SHA-256: `22115841b118753d33b6ec0f09ab80db2fa6d117b66910dd4c3d74262dfe2b77`.
- Published JAR SHA-256 (GitHub API, asset digest, downloaded file and committed `SHA256SUMS.txt`): `cdfcc33bd65e9d5f2924b38fb152b36db8dd91609511fea49b9d943bf0ebee83`.
- Published `SHA256SUMS.txt` SHA-256: `938baf6cfd5852701d8b955228872a55ff16d17b74b8b6cd0463efb8c430441d`.
- Both assets were downloaded again from the public release URLs and re-hashed locally; all values matched the GitHub API metadata exactly.
- Cause of the local/CI difference was confirmed from JAR manifests: published JAR has `Build-Jdk-Spec: 21` (CI uses JDK 21 per `.github/workflows/build.yml`), fresh local JAR has `Build-Jdk-Spec: 25`. This is an environment/compiler-output difference, not a source divergence. No byte-for-byte equality between local and CI JAR is claimed.

### 18.6 GitHub state confirmed via REST API

- `GET /repos/animesao/Artifex/releases/latest` resolves to `v2.5.1`, release ID `454368032`, `draft: false`, `prerelease: false`.
- Release page: `https://github.com/animesao/Artifex/releases/tag/v2.5.1`; `target_commitish: release/2.5.1`.
- Exactly two assets are attached: `Artifex-2.5.1.jar` and `SHA256SUMS.txt` with the sizes/digests listed above.
- `v2.5.0` and `v2.5.0-rc.1` were not modified; no new tag or replacement asset was created.

### 18.7 Documentation changes in 2.5.1

- `VERSION`, `pom.xml`, `plugin.yml`, `README.md`, `CHANGELOG.md`, `INSTALLATION.md`, `ARCHITECTURE.md`, `CONFIGURATION.md`, `SECURITY.md`, `docs/`, `wiki/`, `.github/workflows/build.yml` and `release/2.5.1/SHA256SUMS.txt` were updated to the 2.5.1 release line; historical 2.5.0/RC1 records were preserved.

### 18.8 Production-server confirmation

- `C:\Users\Anime\Desktop\server` was not read for modification, written to, started, stopped, RCON-connected to, or deployed to by this verification.
- No plugin JAR, config, database, world, playerdata, inventory, proxy setting, Iris setting or start script under that path was touched. `EXAMPLE-webbridge` remained inactive.

### 18.9 What remains for future work

- No blocking work remains for the 2.5.1 stable release.
- Optional follow-ups: run the disposable Paper runtime suite on a future CI JDK 21 artifact after any new source change; extend module tests to cover additional third-party modules if they are added to the public repository; consider adding Mockito as a JVM agent to silence the self-attach warning; keep monitoring `main` versus the latest stable release whenever a new release is cut.
