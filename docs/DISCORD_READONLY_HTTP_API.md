# Discord Read-Only HTTP API

**Status:** implemented in isolated worktree `D:\Artifex-worktrees\2026-11-22-init-api` on branch
`feature/database-init-order-and-discord-api`. **Not deployed to production. Production readiness: NOT READY.**

## Purpose

Give a Discord bot verified Artifex profile fields without granting it direct access to any SQLite file.
The Minecraft plugin owns the database connection and answers authenticated, read-only, loopback-only
HTTP queries. The bot never opens `data.db`, never writes, and cannot trigger `/artifex reload`.

## Transport and binding

- `GET /v1/player/{uuid}` on loopback only.
- Configured host is ignored unless it is exactly `127.0.0.1`; a warning is logged otherwise.
- Default port: `25570`. Invalid ports disable the API.
- All responses include `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`.
- Only `GET` is allowed; other methods return `405`.

## Authentication and authorization

1. Service token is read **only** from the `ARTIFEX_INTEGRATION_TOKEN` environment variable. It is never
   read from `config.yml`, logged, or returned in a response. If missing, the API does not start.
2. Requests must send `Authorization: Bearer <token>`. Comparison is constant-time.
3. Requests must send `X-Discord-User-Id` containing a numeric Discord snowflake (17–20 digits).
4. By default the Discord ID must be linked to the requested Minecraft UUID in `links.yml`.
   A mismatch returns `403 not_linked`. `require-player-link: false` disables only the link check;
   it never removes token or Discord-ID validation.

`plugins/Artifex/links.yml` format (synthetic example):

```yaml
100000000000000001: 00000000-0000-0000-0000-0000000000a1
```

Keys must be numeric Discord IDs. Invalid keys or invalid UUID values are ignored.

## Configuration

```yaml
integration:
  http:
    enabled: false
    host: 127.0.0.1
    port: 25570
    require-player-link: true
    max-age-ms: 30000
```

`enabled: false` is the default. The HTTP server is stopped on plugin disable.

## Response states

| State | Meaning | Bot behavior |
| --- | --- | --- |
| `READY` | All fields read on demand from the database at `observedAt` | Render normally |
| `STALE` | Verified snapshot older than `max-age-ms` | Show "last verified" marker; do not present as current |
| `NOT_FOUND` | No persisted level row for that UUID | Show "no profile data"; do not invent values |
| `UNAVAILABLE` | DatabaseManager missing/disconnected, query failed, or internal error | Show explicit unavailable state; retry later |

Per-field states are `VERIFIED` or `UNAVAILABLE`; absent values are JSON `null`, never invented strings.

## Field mapping (profile card v1)

Response fields and their source:

| Field | Source | Priority |
| --- | --- | --- |
| `uuid` | Requested, link-verified UUID | Required |
| `state`, `observedAt` | Query service freshness check | Required |
| `username`, `usernameState` | Not stored in Artifex persistence tables; reported `UNAVAILABLE` until a verified source exists | Explicit unavailable |
| `level`, `currentXp`, `totalXp`, `progressionState` | `artifex_levels` (`level`, `xp`, `total_xp`) | Verified DB read |
| `raceId`, `selectedSkillId`, `raceState` | `artifex_race_skill_state` | Verified DB read |
| `activeCooldowns` | `artifex_race_skill_cooldowns` where `cooldown_until > now`; skill IDs only | Public-safe subset |
| `guildTag`, `guildId`, `guildState` | `artifex_guild_members` joined to `artifex_guilds` | Verified DB read |
| `source` | Always `database` for persisted values, `none` for `NOT_FOUND`/`UNAVAILABLE` | Required |

Classes, equipment, achievements, character statistics, and any race not actually persisted are out of
scope for v1 and must not be fabricated.

Example synthetic response:

```json
{"uuid":"00000000-0000-0000-0000-0000000000a1","state":"READY","observedAt":1763800000000,"username":null,"usernameState":"UNAVAILABLE","level":7,"currentXp":25,"totalXp":325,"progressionState":"VERIFIED","raceId":"humans","selectedSkillId":"human_will","raceState":"VERIFIED","activeCooldowns":["human_will"],"guildTag":"SYN","guildId":2,"guildState":"VERIFIED","source":"database"}
```

## Error responses

| HTTP | `error` | Cause |
| --- | --- | --- |
| 200 | *(none)* | Successful query. The body's `state` may still be `READY`, `STALE`, `NOT_FOUND`, or `UNAVAILABLE`. |
| 400 | `invalid_discord_id` | Missing or malformed `X-Discord-User-Id` |
| 400 | `invalid_uuid` | Malformed UUID path segment |
| 400 | `bad_request` | Malformed request path |
| 401 | `unauthorized` | Missing/invalid service token |
| 403 | `not_linked` | Discord ID not linked to that UUID |
| 405 | `method_not_allowed` | Non-GET request |
| 503 | *(state `UNAVAILABLE`)* | A database query threw an exception |

Two distinct unavailability cases exist and the bot must treat both identically:

1. `DatabaseManager` is missing or disconnected → **HTTP 200** with `state: UNAVAILABLE`, all value fields
   `null`, per-field states `UNAVAILABLE`, and `source: none`. This is graceful degradation, not an error.
2. A query future completes exceptionally → **HTTP 503** with the same `UNAVAILABLE` body.

In neither case is a cached value or invented placeholder returned.

## Readiness, async initialization, and stale data

- Readiness is `DatabaseManager.isConnected()`. If it is false, the API returns HTTP 200 with body
  `state: UNAVAILABLE` and all values `null` rather than cached values. This was verified on a disposable
  server whose SQLite file was deliberately replaced by a directory: the connection failed at startup,
  no module reported usable data, and the API returned no invented values.
- Guild membership is read from `artifex_guild_members`/`artifex_guilds` on every request. The module's
  asynchronous in-memory cache is **not** treated as current data, so a not-yet-populated cache cannot
  produce a false "no guild" answer.
- Levels and Race Skills are likewise read on demand from their tables; player-event caches are not used.
- `observedAt` plus `max-age-ms` produces `STALE`. A stale profile is never presented as current.
- All four SQL statements are parameterized; the UUID is never string-concatenated into SQL.

## Testing

From the worktree:

```
mvn -o -q test
mvn -o -q -DskipTests package
```

The suite includes a real loopback HTTP test and a real SQLite test using only synthetic records
(`SYNTH-OP-01`, `SYNTH-EMPTY-01`, guild tag `SYN`, level `7`, xp `25`, total xp `325`).
No production database, production plugin folder, or real account is read or written.

## Deployment gate

Do not copy the built JAR to production or the master backup. A Paper/disposable-server runtime check
with the verified launcher and synthetic token remains a separate approval step. `/artifex reload` is
not part of this integration and must not be automated by Discord.
