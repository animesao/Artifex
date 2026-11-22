# Artifex 2.5.0-rc.1 — Release Candidate Report

Date: 2026-11-22
Status: **Release Candidate prepared.** Not a stable release. Not deployed. Production readiness: **NOT READY.**

## 1. Versions

| Item | Value |
| --- | --- |
| Previous version | 2.4.0 |
| New version | 2.5.0-rc.1 |
| Base (verified) commit | `00147f04ceb988f174e3b58635d55fa287299d17` on `feature/database-init-order-and-discord-api` |
| Release branch | `release/2.5.0-rc.1` |
| Included fixes | `b7146f5` DatabaseManager init order, `32ec070` STALE state, `ffca972` API docs, `00147f0` unavailable semantics |

Independent version strings were deliberately left unchanged: documentation version `1.325.0`, wiki version
examples, module config versions, and third-party dependency versions. Historical `2.4.0` references in
plan documents were also preserved.

## 2. Changed files

| File | Change |
| --- | --- |
| `pom.xml` | Maven artifact version → `2.5.0-rc.1` |
| `src/main/resources/plugin.yml` | Plugin version → `2.5.0-rc.1` |
| `VERSION` | Canonical version source → `2.5.0-rc.1` |
| `README.md` | Version badge, Release Candidate notice, integration/configuration guidance |
| `CHANGELOG.md` | New `## v2.5.0-rc.1` section |
| `docs/DISCORD_READONLY_HTTP_API.md` | RC status, limitations, explicit body-`state` client rule |
| `.github/workflows/build.yml` | `-rc` versions are marked as prereleases in release automation |

Release metadata created in `release/2.5.0-rc.1/`: `Artifex-2.5.0-rc.1.jar` and `SHA256SUMS.txt`.
The JAR is ignored by Git (`*.jar`); the metadata text is committed.

## 3. Commits on the release branch

```
eef65d1 docs: add 2.5.0-rc.1 changelog and release guidance
02d0cdb chore: bump version to 2.5.0-rc.1
```

Plus the release metadata commit containing this report and `release/2.5.0-rc.1/SHA256SUMS.txt`.
No historical commits were rewritten. No push, tag, or GitHub Release was created.

## 4. Included functionality

### DatabaseManager fix
`PluginBootstrap.enable()` initializes the database before `moduleManager.loadAll()`. The former
`DatabaseManager == null` state in module `onEnable()` is removed while the existing module architecture
is retained.

### Discord read-only HTTP API
`GET /v1/player/{uuid}`, loopback-only binding, bearer authentication, token from
`ARTIFEX_INTEGRATION_TOKEN` only, constant-time comparison, Discord snowflake validation, identity-link
verification, parameterized SQL, read-only operations, `READY`/`STALE`/`NOT_FOUND`/`UNAVAILABLE` states,
per-field `VERIFIED`/`UNAVAILABLE`, JSON `null` for unavailable values, `no-store`, `nosniff`, and 405
for unsupported methods. The API contract was not changed for this release.

A disconnected DatabaseManager intentionally returns **HTTP 200 with body `state: UNAVAILABLE`**;
HTTP 503 is reserved for query exceptions. Clients must inspect the body `state` on every 2xx response.

## 5. Testing and build

| Check | Result |
| --- | --- |
| `mvn -o -q clean test` | PASS — 105 tests, 0 failures, 0 errors |
| `mvn -o -q -DskipTests package` | PASS |
| `mvn -o -q clean package -DskipTests` (reproducibility) | PASS |
| `plugin.yml` version inside JAR | `2.5.0-rc.1` |
| `META-INF/maven/me.dcplugin/artifex/pom.properties` | version `2.5.0-rc.1` |
| HTTP API classes inside JAR | 11 integration classes, all required classes present |
| Documentation present | `docs/DISCORD_READONLY_HTTP_API.md`, CHANGELOG section, README notice |
| Embedded token/secret scan of JAR resources | 0 matches |
| Source correspondence | All 103 classes from `target/classes` present in JAR; clean rebuild byte-identical for every entry except `META-INF/MANIFEST.MF` |
| Offline build | PASS — no dependency downloads performed |

The single class-hash difference between the raw build output and the shaded JAR is
`DatabaseManager.class`, caused by the expected HikariCP package relocation performed by the shade plugin.

## 6. Artifact

| Item | Value |
| --- | --- |
| Artifact | `release/2.5.0-rc.1/Artifex-2.5.0-rc.1.jar` |
| Size | 19,138,321 bytes |
| SHA-256 | `9989E32131F27FADB9FA8D77F1FB398A80184926B60BC08F4C5ADC3B4E26FDA6` |
| Git commit | `eef65d15becd076c2a53c2e4a28a8256f942a56b` |
| Build date | 2026-11-22T10:08:59+05:00 |
| Build runtime | Maven 3.9.15, Java 25.0.2 (Eclipse Adoptium), compiler source/target 21 |

## 7. API configuration summary

```yaml
integration:
  http:
    enabled: false
    host: loopback (configured host ignored unless exactly loopback)
    port: 25570
    require-player-link: true
    max-age-ms: 30000
```

- Token: environment variable `ARTIFEX_INTEGRATION_TOKEN` only. Never read from config, never logged,
  never included in documentation or artifacts. No real token exists in this workspace.
- Identity links: `plugins/Artifex/links.yml`, numeric Discord snowflake keys mapped to UUID values.
- API starts only when `enabled: true` and a non-blank token is present.

## 8. Known limitations

- Release Candidate only; not a stable or supported production release.
- Account-linking provisioning is not complete — `links.yml` is a development fallback, not a secure flow.
- No Discord client implementation is included in this artifact.
- Velocity forwarding/authentication mode remains an unresolved, separate topic.
- Username is reported `UNAVAILABLE` because the used Artifex tables do not store player names.
- Module in-memory caches are never exposed as current profile data; all values are read from persistence.
- Existing LETHAVARYN modules (guilds, levels, raceskills) live in `plugins/Artifex/items/...` and were not
  modified or rewritten for this release.
- No automatic database reset or migration is performed.

## 9. Paper runtime verification status of this exact artifact

**NOT VERIFIED.** The versioned `2.5.0-rc.1` JAR has not been started in Paper. The earlier P0.7
verification (18/18 runtime cases PASS) was performed on the functionally identical pre-version-bump
source at commit `00147f0`. It must not be presented as verification of this newly versioned binary.

A separate operator-authorized runtime verification plan for this exact artifact is required; no new
Paper runtime was launched for this release preparation.

## 10. Safety confirmations

- No production Minecraft start; no `/artifex reload` in production; no production database change.
- Production and master backup hash comparison: **0 mismatches** across launcher, server JAR, EULA,
  server properties, all plugin JARs, and both Artifex databases.
- New JAR was copied only into the isolated release directory. It was **not** copied to production or
  any server plugin folder. No deployment, push, tag, or GitHub Release was performed.
- No Java process or test listener remained after all checks.
