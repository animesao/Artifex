# Artifex Plugin Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Artifex efficient under normal and high player counts while exposing safe runtime tuning through `config.yml` instead of source edits.

**Architecture:** Keep Bukkit API work on the main thread and database/compiler work off it. Reduce repeated work by indexing runtime Java items by equipped ID, gating inventory scans by loaded features, and making scheduler intervals configurable. Preserve existing APIs and behavior unless a setting explicitly changes performance frequency.

**Tech Stack:** Java 21, Paper 1.21.8, Bukkit scheduler, Maven, JUnit 5, SQLite/MySQL.

## File map

- Modify `src/main/resources/config.yml` — documented performance settings.
- Modify `src/main/java/me/dcplugin/artifex/handlers/EquippedItemsChecker.java` — configurable intervals and indexed periodic processing.
- Modify `src/main/java/me/dcplugin/artifex/api/ItemAPI.java` — avoid duplicate PDC reads.
- Modify `src/main/java/me/dcplugin/artifex/utils/ActionParser.java` — safe sequence scheduling limits and correct millisecond-to-tick conversion.
- Modify `samples/settings/npc/NPCSystem.java` — recipe cache and serialized SQLite writes.
- Modify `docs/README_RU.md` or the main README — explain configuration knobs and operational profiling.
- Add focused unit tests where logic can be tested without a live Paper server.

## Task 1: Add safe performance configuration

- [ ] Add `performance` defaults to `src/main/resources/config.yml`: equipment refresh cooldown, duration/world check intervals, NPC tracker interval, maximum action sequence steps, and maximum action sequence delay.
- [ ] Read values defensively with minimum bounds and preserve current defaults when keys are absent.
- [ ] Document that lower intervals increase CPU usage and that settings require reload/restart as appropriate.

## Task 2: Optimize equipped Java periodic items

- [ ] Add a failing unit-level test for selecting only periodic Java items whose IDs are present in the six equipped slots.
- [ ] Replace the `periodicJavaItems` full-map loop with direct slot-ID lookups while retaining activation-slot validation and event cancellation.
- [ ] Keep periodic interval semantics unchanged.
- [ ] Run all tests and package the plugin.

## Task 3: Remove unnecessary item metadata work

- [ ] Keep `ItemAPI.isHolding` and `isEquipped` using one `getCustomItemId` call per inspected stack.
- [ ] Add null-safe behavior tests for AIR/null stacks where feasible.
- [ ] Verify public API behavior remains inventory-wide for `isEquipped`.

## Task 4: Make action sequences bounded and scheduler-safe

- [ ] Add configurable maximum sequence steps and total delay.
- [ ] Reject excess steps with one debug/warning message instead of creating unbounded scheduler tasks.
- [ ] Fix sub-50ms delays to round up to one tick rather than silently becoming zero and normalize negative delays.
- [ ] Keep action syntax and ordering compatible.
- [ ] Add parser tests for delay conversion and bounds.

## Task 5: Finish NPC sample hardening

- [ ] Keep recipes in memory after startup and clone them before handing them to Bukkit inventories/merchants.
- [ ] Serialize SQLite writes through one executor and wait briefly during unregister before closing the connection.
- [ ] Use SQLite busy timeout/WAL where supported.
- [ ] Avoid database reads during ordinary NPC interaction.
- [ ] Validate the sample source with the project build/test checks.

## Task 6: Verification and documentation

- [ ] Run `mvn test` and `mvn package`.
- [ ] Run `git diff --check`.
- [ ] Review changed code for main-thread Bukkit violations and resource leaks.
- [ ] Update user documentation with the performance section, Spark profiling commands, and configuration examples.
- [ ] Report exact verification results and any limits that still require a live server profile.
