# Artifex 2.5.0-rc.1 — GitHub Release Report

Publication date: 2026-11-22
Status: **PUBLISHED — GitHub Pre-release.** Not a stable release.

## 1. Publication details

| Item | Value |
| --- | --- |
| Repository | `https://github.com/animesao/Artifex` |
| Repository visibility | Public |
| Default branch | `main` |
| Release branch | `release/2.5.0-rc.1` |
| Remote branch commit | `d3df9fa1b82706b42f67225fe8259bde47698dcd` |
| Release tag | `v2.5.0-rc.1` |
| Tag type | annotated (`tag` object), message `Artifex 2.5.0-rc.1 Release Candidate` |
| Tag target commit | `d3df9fa1b82706b42f67225fe8259bde47698dcd` |
| Release page | `https://github.com/animesao/Artifex/releases/tag/v2.5.0-rc.1` |
| Release ID | `454235096` |
| Release name | `Artifex 2.5.0-rc.1` |
| `target_commitish` | `release/2.5.0-rc.1` |
| Pre-release | **true** |
| Draft | false |
| Latest stable release | **No** — explicitly excluded by `prerelease: true` |

## 2. Assets

| Asset | Size (bytes) | SHA-256 / digest |
| --- | --- | --- |
| `Artifex-2.5.0-rc.1.jar` | 19,138,321 | `9989E32131F27FADB9FA8D77F1FB398A80184926B60BC08F4C5ADC3B4E26FDA6` |
| `SHA256SUMS.txt` | 827 | `04074B846413209377B1E3BC99B5BB03543A0FA7E399F0BF11EA0CD8C44BA509` |

Verification performed after upload:

- GitHub-reported digest for the JAR matches the local verified artifact.
- The JAR was downloaded from the public asset URL and re-hashed locally: identical SHA-256.
- The downloaded `SHA256SUMS.txt` is byte-identical to the local copy.
- No logs, synthetic databases, disposable runtime folders, player data, or private test evidence were uploaded.

## 3. Pre-publication verification

- Remote confirmed as the intended Artifex repository; the LETHAVARYN server repository was not used.
- `gh` CLI absent; no token environment variables; GitHub access verified via cached Git credentials.
  Authenticated API login resolved to the repository owner. Tokens were never printed or stored in files.
- Repository found to be public with an established release convention (`v<version>` tags, JAR assets).
- Local branch `release/2.5.0-rc.1` at expected commit `d3df9fa`; runtime verification commit `3ea6723` in history.
- Working tree clean; verified JAR hash matched expected value before publication.
- CHANGELOG, README, release report and API contract reviewed.
- Tracked-file scan found no `.env`, database, log, player-data, backup, token or password files.
  The only filename matches were ordinary source files `IntegrationTokenVerifier.java` and `BackupManager.java`.
- CI configuration already treats `-rc` versions as prereleases; no CI or security checks were weakened.

## 4. Publication actions

1. Normal (non-force) push of `release/2.5.0-rc.1` — branch created remotely.
2. Annotated tag `v2.5.0-rc.1` created at `d3df9fa` and pushed — tag created remotely.
3. GitHub Pre-release published with the supplied release notes, adapted to repository style.
4. Release assets attached and checksum-verified.

No force push, no history rewrite, no stable-release announcement.

## 5. CI status

Build workflow run `44991192324` (event `push`, head `d3df9fa`) completed with conclusion `success`.

The workflow automatically created a release when the tag was pushed. Its generated JAR had a different
SHA-256 (`1f14d59f…`) from the verified artifact, its notes still claimed Paper verification had not been
done, its `target_commitish` was `main`, and it lacked `SHA256SUMS.txt`. That auto-generated asset was
deleted and replaced with the verified artifact; release metadata and notes were corrected via the API.
No CI configuration was modified to achieve this.

## 6. Security and verification status

- 105 automated tests passed; 0 failures, 0 errors.
- 18/18 disposable Paper runtime cases passed on the exact versioned artifact.
- Clean restart persistence, T17 unavailable-database behavior and API auth checks verified.
- Secret scan: 57,802 text files, zero secret-like patterns.
- Production/master integrity: 0 hash mismatches. No production deployment or database change.

## 7. Warnings and limitations

- This is a Release Candidate, not a stable production release.
- Discord account-linking provisioning and the Discord bot client are not implemented.
- Minecraft username is unavailable through the current profile data source.
- Velocity forwarding/authentication integration remains under evaluation.
- The guild module's internal asynchronous cache counter was not directly inspected in the final runtime test.
- `SHA256SUMS.txt` states "No GitHub Release or push performed"; that line was written before publication and
  was intentionally left unchanged because the file is part of the verified release metadata.
- The release author is `github-actions[bot]` because CI created the release on tag push; the corrective
  metadata update and asset upload were performed through the authenticated repository-owner session.
- This report is a post-publication documentation commit on the release branch. It does not change the
  verified code artifact or the release tag target.

## 8. Final state

- Remote branch and remote tag both verified.
- Both release assets present with correct names and checksums.
- Working tree clean; no production modification; no master backup modification; no deployment.
