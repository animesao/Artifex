# Runtime Verification Plan — Artifex 2.5.0-rc.1

Status: **PLAN ONLY. Requires separate operator authorization before any Paper launch.**

The P0.7 result (18/18 PASS) applies to commit `00147f0`, not to this versioned artifact. This plan exists
to verify the exact binary `release/2.5.0-rc.1/Artifex-2.5.0-rc.1.jar` (SHA-256 recorded in
`SHA256SUMS.txt`) before any further release decision.

## Preconditions

1. Authorization: explicit operator approval to start a disposable Paper server. Without it, stop here.
2. Source artifact: use only `release/2.5.0-rc.1/Artifex-2.5.0-rc.1.jar`. Do not rebuild for the test.
3. Production and master backup remain read-only throughout.
4. Use the verified candidate launcher (`start.bat` with read-only preflight), never the production launcher.
5. Fresh isolated copy under a new disposable root; never reuse an earlier test server.
6. Keep LethavarynBootstrapper and DiscordSRV absent/quarantined.
7. Use only synthetic records and synthetic identity links. No real account, token, or player data.
8. Loopback-only game, API, and RCON ports, distinct from previous runs to avoid collisions:
   game 25584, HTTP API 25573, RCON 25585.
9. Set `ARTIFEX_INTEGRATION_TOKEN` in the process environment only; never in config or reports.

## Cases

| # | Case | Evidence |
| --- | --- | --- |
| 1 | Startup initialization order | `Database connected` precedes `[Modules] Found` in real log |
| 2 | Database connection | SQLite connection line for the disposable database |
| 3 | Module initialization | 14 modules enabled; zero `DatabaseManager unavailable` warnings |
| 4 | Guilds loading | Module enabled with DB; persisted guild readable via API |
| 5 | Levels persistence | API returns persisted synthetic level/xp/total_xp |
| 6 | Race Skills persistence | API returns persisted synthetic race and selected skill |
| 7 | Clean restart persistence | RCON stop, relaunch, identical persisted values |
| 8 | HTTP authentication | 401 without token |
| 9 | Invalid token | 401 with wrong token |
| 10 | Invalid UUID | 400 `invalid_uuid` |
| 11 | Discord identity mismatch | 403 `not_linked` |
| 12 | Missing identity link | 403 `not_linked` |
| 13 | GET endpoint | 200 `READY` with verified fields |
| 14 | POST rejection | 405 `method_not_allowed` |
| 15 | No-store header | `Cache-control: no-store` on the GET response |
| 16 | nosniff header | `X-content-type-options: nosniff` on the GET response |
| 17 | Module unavailable response | Second disposable copy with unreachable DB: 200 + body `state=UNAVAILABLE`, all values null |
| 18 | API shutdown and clean exit | RCON `stop`; zero Java processes and zero listeners on all test ports |

## Additional release-specific checks

- Confirm `plugin.yml` version reported by Paper is `2.5.0-rc.1`.
- Confirm the JAR hash used at runtime equals the hash in `SHA256SUMS.txt`.
- Confirm the API default remains disabled unless explicitly enabled in the disposable config.
- Confirm no `/artifex reload` is required for module persistence.

## Completion rules

- Unit tests do not replace this runtime verification.
- Do not mark the versioned artifact as runtime-verified until real logs and HTTP responses are captured.
- Do not copy the JAR to production or publish a GitHub Release as part of this test.
- After testing, stop the server, confirm no Java process/listener remains, and sanitize all reports.
