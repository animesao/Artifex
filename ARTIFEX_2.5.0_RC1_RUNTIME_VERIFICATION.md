# Artifex 2.5.0-rc.1 — Final Runtime Verification

Date: 2026-11-22
**Final status: VERIFIED WITH LIMITATIONS** — every mandatory check PASS; explicitly documented limitations remain.
This is **not** a stable release declaration. Production readiness: **NOT READY**. No deployment was performed.

Operator authorization for this disposable launch was requested and granted before any Paper process started.
This report applies to the exact versioned release artifact, not to the pre-version-bump source used in P0.7.

## 1. Artifact and environment

| Item | Value |
| --- | --- |
| Artifact | `release/2.5.0-rc.1/Artifex-2.5.0-rc.1.jar` |
| Expected SHA-256 | `9989E32131F27FADB9FA8D77F1FB398A80184926B60BC08F4C5ADC3B4E26FDA6` |
| Actual SHA-256 before launch | identical |
| SHA-256 after all tests | identical — release artifact not modified |
| Installed plugin JAR | `plugins/Artifex-2.5.0-rc.1.jar` in both disposable copies (same hash) |
| Source for fresh copy | `D:\LETHAVARYN-RESTORE-TEST\2026-11-21_P0.3\artifex-dbtest\server` |
| Disposable root | `D:\LETHAVARYN-RESTORE-TEST\2026-11-22_RC1\paper-runtime` |
| Primary disposable server | `...\paper-runtime\server` |
| T17 disposable server | `...\paper-runtime\server-unavailable` — removed after the test |
| Evidence directory | `...\paper-runtime\results` |
| Main ports | game 25584, HTTP API 25573, RCON 25585 — loopback only |
| T17 ports | game 25586, HTTP API 25574, RCON 25587 — loopback only |
| Version observed in Paper | `2.5.0-rc.1` (`[Artifex] Current version: 2.5.0-rc.1` in both runs and T17) |
| Artifex config | `integration.http.enabled: true` in the disposable copy; host `127.0.0.1`; port as above |
| Token | synthetic, process environment only (`ARTIFEX_INTEGRATION_TOKEN`); never in config, logs, or report |
| Identity links | three synthetic numeric Discord IDs in `plugins\Artifex\links.yml` |
| Excluded content | DiscordSRV, LethavarynBootstrapper, `plugins\DbTestProbe.jar`, old `Artifex-2.4.0.jar`, `world\playerdata` |
| Empty state | `ops.json`, `usercache.json`, ban lists and whitelist reset to empty arrays |
| Preflight dry run | PASS, Java launch suppressed; `PREFLIGHT_RESULT=PASS` |

The release candidate JAR was copied **only** into the isolated disposable copies. No old build was substituted.

## 2. Startup evidence — real logs

Primary run 1 (`results\rc1-startup-1.log`, 538 lines):

```
[Artifex] Database connected: SQLite (plugins\Artifex\data.db)      line 416
[Artifex] [Modules] Found 14 potential modules                       line 417
[Artifex] [LETHAVARYN] Guilds system enabled. Loaded guilds: 0
[Artifex] [levels_levelsItem] RPG level system enabled. Table: artifex_levels
[Artifex] [LETHAVARYN] Race Skills enabled. 15 skill definitions loaded across 5 races.
[Artifex] [Modules] Loaded 14 modules                                line 485
[Artifex] [Integration] Read-only HTTP API listening on 127.0.0.1:25573  line 496
Done (283.697s)!                                                     line 520
```

Primary run 2 after clean restart (`results\rc1-startup-2.log`, 557 lines):

```
Database connected: SQLite                                          line 435
[Modules] Found 14 potential modules                                 line 436
[Modules] Loaded 14 modules                                          line 504
[Integration] Read-only HTTP API listening on 127.0.0.1:25573       line 515
Done (111.599s)!                                                     line 539
DB_BEFORE_MODULES=True
UNAVAILABLE_WARNINGS=0
```

Absent from both logs — the three defects this release fixes:

- `[LETHAVARYN] DatabaseManager unavailable, guilds will not persist.`
- `[ArtifexLevels] DatabaseManager is unavailable; progress will be kept in memory only.`
- `[LETHAVARYN] DatabaseManager unavailable. Race Skills cannot persist data.`

## 3. Runtime cases — all PASS

| # | Case | Result | Real evidence |
| --- | --- | --- | --- |
| 1 | Startup initialization order | **PASS** | DB connected line 416 precedes modules-found line 417; `DB_BEFORE_MODULES=True` in both runs |
| 2 | Database connection | **PASS** | `Database connected: SQLite (plugins\Artifex\data.db)` |
| 3 | Module initialization | **PASS** | 14 modules loaded; guilds, levels, raceskills enabled; zero unavailable warnings |
| 4 | Guilds loading | **PASS** | API returned persisted `guildTag`, `guildId`, `guildState=VERIFIED` (see L1) |
| 5 | Levels persistence | **PASS** | API returned `level=7`, `currentXp=25`, `totalXp=325`, `progressionState=VERIFIED` |
| 6 | Race Skills persistence | **PASS** | API returned `raceId=humans`, `selectedSkillId=human_will`, `raceState=VERIFIED` |
| 7 | Clean restart persistence | **PASS** | RCON stop → 0 Java, 0 listeners → relaunch → identical values; `Done (111.599s)` |
| 8 | HTTP authentication | **PASS** | 401 without token |
| 9 | Invalid token | **PASS** | 401 with wrong token |
| 10 | Invalid UUID | **PASS** | 400 `invalid_uuid` |
| 11 | Discord identity mismatch | **PASS** | 403 `not_linked` |
| 12 | Missing identity link | **PASS** | 403 `not_linked` |
| 13 | GET endpoint | **PASS** | 200 `state=READY`, `source=database`, all persisted fields verified |
| 14 | POST rejection | **PASS** | 405 `method_not_allowed` |
| 15 | No-store header | **PASS** | `Cache-Control: no-store` |
| 16 | nosniff header | **PASS** | `X-Content-Type-Options: nosniff` |
| 17 | Module unavailable response | **PASS** | Broken-DB copy: 200 + body `state=UNAVAILABLE`, all values `null`, `source=none` |
| 18 | API shutdown and clean exit | **PASS** | RCON `stop`; 0 Java processes, 0 listeners on all test ports |

### Release-specific checks

| Check | Result |
| --- | --- |
| Paper reports plugin version | **PASS** — `2.5.0-rc.1` |
| Runtime JAR hash equals release hash | **PASS** — both copies |
| API disabled by default in the shipped config | **PASS** — default remains `false`; enabled only in the disposable config |
| No `/artifex reload` needed for persistence | **PASS** — values survived a full stop/restart without reload |
| New artifact not substituted with an old build | **PASS** — only `Artifex-2.5.0-rc.1.jar` installed; old JAR removed |

## 4. API responses (sanitized)

Successful GET (`results\response_GET_valid.json`, `results\response_GET_after_restart.json`):

```json
{"uuid":"<SYNTH-UUID-01>","state":"READY","observedAt":<timestamp>,"username":null,
 "usernameState":"UNAVAILABLE","level":7,"currentXp":25,"totalXp":325,
 "progressionState":"VERIFIED","raceId":"humans","selectedSkillId":"human_will",
 "raceState":"VERIFIED","activeCooldowns":[],"guildTag":"SYN","guildId":2,
 "guildState":"VERIFIED","source":"database"}
```

The post-restart response is identical except for `observedAt`. Error cases:

| Request | HTTP | Body |
| --- | --- | --- |
| no token | 401 | `{"state":"UNAVAILABLE","error":"unauthorized"}` |
| wrong token | 401 | `{"state":"UNAVAILABLE","error":"unauthorized"}` |
| malformed UUID | 400 | `{"state":"UNAVAILABLE","error":"invalid_uuid"}` |
| wrong Discord ID | 403 | `{"state":"UNAVAILABLE","error":"not_linked"}` |
| unknown Discord ID | 403 | `{"state":"UNAVAILABLE","error":"not_linked"}` |
| POST | 405 | `{"state":"UNAVAILABLE","error":"method_not_allowed"}` |

T17 (`results\response_T17_unavailable.json`) — **HTTP 200** with an explicit unavailable body:

```json
{"uuid":"<SYNTH-UUID-01>","state":"UNAVAILABLE","observedAt":0,"username":null,
 "usernameState":"UNAVAILABLE","level":null,"currentXp":null,"totalXp":null,
 "progressionState":"UNAVAILABLE","raceId":null,"selectedSkillId":null,
 "raceState":"UNAVAILABLE","activeCooldowns":[],"guildTag":null,"guildId":null,
 "guildState":"UNAVAILABLE","source":"none"}
```

The T17 log confirms the database was genuinely unreachable, not merely empty:

```
[Artifex] SQLite connection error: [SQLITE_CANTOPEN_ISDIR] The file is really a directory
[Artifex] Could not acquire database connection: SQLite is not connected
```

No cached or invented value was exposed. Clients must inspect the body `state` on every 2xx response.

## 5. Database results

Read-only check of the disposable database before launch: `artifex_guilds` 3 rows,
`artifex_guild_members` 3, `artifex_guild_invites` 0, `artifex_levels` 3,
`artifex_race_skill_state` 3, `artifex_race_skill_cooldowns` 3, `artifex_races` 3 —
all synthetic. No production database was opened for write.

## 6. Limitations

- **L1 — guild module cache:** the module prints `Loaded guilds: 0` before its asynchronous DB load completes.
  The module's internal cache count was not directly probed. The authoritative Discord-facing path was verified:
  the API reads persisted guild rows directly and returned `guildState=VERIFIED`.
- **L2 — username:** `username` is `UNAVAILABLE` by design; the Artifex tables used here do not store names.
- **L3 — cooldowns:** `activeCooldowns` is empty because the synthetic expiry had passed. The query filters
  `cooldown_until > now` correctly.
- **L4 — availability semantics:** a disconnected DatabaseManager returns HTTP 200 with body
  `state=UNAVAILABLE`; HTTP 503 is reserved for query exceptions. This is documented contract behavior.
- **L5 — update checker:** Paper log shows `Current version: 2.5.0-rc.1, Latest version: 2.4.0` followed by
  `You are using the latest version!`. This is the upstream checker's numeric comparison treating the `-rc`
  suffix as an extra numeric segment; it does not affect functionality and no update was downloaded.
- **L6 — intentional T17 errors:** SQLITE_CANTOPEN_ISDIR and connection failures in the second copy are the
  test condition, not regressions.
- **L7 — unrelated warnings:** Iris Nether history warning and DeluxeMenus NMS hook warning come from
  pre-existing disposable content; they are unrelated to this release.
- **L8 — no interactive play:** no real or synthetic player joined the world; persistence was verified from
  stored rows and API reads, not in-game actions.
- **L9 — unresolved scope:** account-linking provisioning is not complete, no Discord client is included,
  and Velocity integration remains a separate open question.
- **L10 — console encoding:** Cyrillic module messages appear as mojibake in the captured Windows console;
  this is a code-page artifact, not a functional failure.

## 7. Safety confirmations

- No production Minecraft start, no production command, no `/artifex reload` in production.
- Production and master backup hash comparison after all runtime work: **0 mismatches** across launcher,
  server JAR, EULA, properties, all plugin JARs, and both Artifex databases.
- Release artifact hash unchanged; no JAR copied to production or any production plugin folder.
- Both disposable servers stopped with the RCON `stop` command. Final state: **0 Java processes** and
  **0 listeners** across ports 25571–25574 and 25579–25587.
- Broken-DB disposable copy removed after the test.
- No push, tag, or GitHub Release performed. No stable release declared.
- Text-only secret scan of the disposable root: **57,802 files scanned, 0 secret-like patterns**. The two
  earlier hits were binary DLLs in the Iris native cache, not credentials.
- Raw synthetic values are retained only inside the isolated test root; this report is sanitized.

## 8. Conclusion

The exact `Artifex 2.5.0-rc.1` binary passed all 18 runtime cases, the clean-restart persistence check,
and the release-specific version/hash checks on disposable Paper. Status: **VERIFIED WITH LIMITATIONS**.
This does not authorize production deployment, and it does not promote the Release Candidate to a stable release.
