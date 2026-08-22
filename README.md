# LoreCanvas (Android)

An offline-first worldbuilding and story-planning studio, native Android (Kotlin + Jetpack Compose).

Where i am currently (Using AI to summarise the current progress stage)

**Status: Command/Undo-Redo system complete at the Repository layer, and now wired into the UI.** Node, Card, Relationship, and Timeline all have full Command coverage (create/edit/undo/redo), `CompoundCommand` batches multi-field edits into one atomic undo step, and — the change that closes out the biggest gap from the previous phase — the Compose UI in `LoreCanvasApp.kt` now routes its mutations through `CommandHistory` instead of calling repositories directly. All of this is backed by a real Gradle-discoverable JUnit5 test suite: **28/28 passing, VERIFIED BY EXECUTION.**

The Android/Compose application itself has **NOT yet been verified by a real build** — see "Android build status" below.

## The performance finding (still true, from last phase — read this one first)

Adding Timeline events one at a time re-serializes and rewrites the *entire* Timeline file on every single add. Measured directly: **100 events → 270ms, 500 → 905ms, 1,000 → 2,727ms** — clearly super-linear, not the flat per-event cost a well-behaved design would have. At the stated target scale (20,000 Timeline Events) this would be genuinely unusable.

Fixed with `TimelineRepository.addEvents()` — a bulk variant that validates everything, mutates everything in memory, then saves **once**. Measured: **2,000 events in one bulk call → 190ms** (versus 2,727ms for half that many one-at-a-time). This also directly satisfies that phase's "batch operations" ask — it's the same fix either way.

**What this doesn't fix:** a user adding events one at a time by hand through the UI still hits the per-add save cost — that's inherent to "one event, one edit, one save" and is fine at human-typing speed. The bulk path matters for Import (already wired if Import ever adds many events at once — currently Import writes Timelines directly via Storage, not through `addEvents`, so this is available but not yet the load-bearing path for Import specifically) and any future bulk-creation feature. A deeper fix (per-event files instead of one growing embedded array) would remove the ceiling entirely but is a real storage-format change, not attempted yet.

## Command / Undo-Redo system (this phase's main work)

- **Node/Card/Relationship/Timeline Commands.** Every entity type now has a full set of Commands (`NodeCommands.kt`, `CardCommands.kt`, `RelationshipCommands.kt`, `TimelineCommands.kt`) covering create, field edits, and — for Timeline — its Events (add/update/remove). Delete is deliberately *not* wrapped in a Command for any entity: `create()` always mints a fresh id, so there's no identity-preserving way to undo a delete without a new Storage capability this phase didn't build. Relationship's `addContext` is similarly left unwrapped, matching its own documented history-preservation model (no `removeContext` capability exists).
- **A real redo bug, found and fixed.** `CommandHistory.redo()` re-invokes `execute()`, but every Create-type command's `execute()` unconditionally called `repository.create()`, which mints a fresh id every time — so undo-then-redo silently produced a *different* entity than the one undone. Fixed by giving every repository a `restore(entity)` method (re-inserts an already-built entity at its existing id, distinct from `create()`, which mints fresh, and `save()`, which requires the id already exist) and having each Create command's redo path call it.
- **`CompoundCommand`** (from the prior phase) groups several Commands into one atomic undo/redo step. This phase puts it to real use: `NodeEditCommands.kt` / `CardEditCommands.kt` / `RelationshipEditCommands.kt` / `TimelineEditCommands.kt` are new plain-Kotlin bridge files that diff a field snapshot (taken when an editor screen opens) against the live, already-edited object at Save time, and batch every changed field into one `CompoundCommand` — so Save becomes exactly one undo step, not one per field.
- **UI Command Integration.** `LoreCanvasApp.kt` now calls `commandHistory.execute(...)` for Node/Card/Relationship/Timeline creation, Timeline event create/edit/remove, and all three "batch edit → Save" flows — 11 call sites total. Delete stays a direct repository call everywhere, matching the architecture decision above. A real bug was fixed along the way: `TimelineEditorScreen`'s rename field mutated `Timeline.name` in memory on every keystroke but nothing ever called `timelineRepository.save()` for it — unlike Node/Card/Relationship, Timeline's screen has no Save button, so a renamed Timeline was silently never persisted. Fixed by committing the rename on navigating back.
- **Validation error propagation.** Every Create-type Command now exposes a `lastError: RepositoryError?`, populated on failure instead of silently discarded, so the UI can show the real validation message again instead of a generic one.
- **`WorkspaceContext` owns a single shared `CommandHistory`**, threaded through every screen — scoped per-project/per-editing-session, not per-screen, which is also the correct scope for the eventual Undo/Redo UI buttons (not built yet — see "Next milestone").

## Honest scope of "Performance Tests" and "UI Tests"

This environment has no Android SDK, no emulator, and no device — so "smooth scrolling," "no ANRs," "no memory leaks," and any on-device UI test genuinely cannot be verified here, full stop. What *was* tested for real, this phase: the entire Command/Undo-Redo system, including the UI-Save-flow bridge logic (`*EditCommands.kt`), against real file storage — 28 tests, all passing, via real JUnit5. That's not a substitute for a real Android build, which only a networked Gradle/Android environment can do.

## Last phase's other changes (still true, untouched this phase)

- **Batch/Compound Commands.** `CommandHistory` has `executeBatch()`, plus `nextUndoLabel`/`nextRedoLabel`/`executedLabels()` for a future "Command History" UI list.
- **Template categories + built-in templates.** `Template` has a `category` field (old template files without one default to "Custom" — a real backward-compatibility path, tested). `BuiltInTemplates` ships three ready-made templates (Character/Location/Organization) that appear in every project without being created, can be applied like any other template, and are protected from deletion.
- **Storage corruption and missing-file handling tested across every entity type**: a corrupted Card/Relationship/Timeline/Template file is skipped (not crashed on) by every list operation and returns `null`/a clean `NOT_FOUND` from single-item lookups; a file deleted out from under a live in-memory object fails its next save cleanly instead of throwing.

## Project layout

```
core-common/       Json, Markdown, Logger, Config, LcResult, UuidService.
core-domain/        Entity, Project, Node, Card, Relationship+RelationshipContext,
                     Timeline+TimelineEvent, Template (+category, +BuiltInTemplates).
core-validation/    Rules + EntityValidators (one validator per entity).
core-events/        EventBus, DomainEvent.
core-commands/      Command, CompoundCommand, CommandHistory (+ introspection).
core-storage/       FileManager, one Storage/FileStorage/Serializer trio per entity,
                     ExportBundleSerializer (version-tagged).
core-repository/    Transaction, RepositoryError, one Repository per entity
                     (each with restore(), fixing the Create-redo bug),
                     NodeCommands.kt / CardCommands.kt / RelationshipCommands.kt /
                     TimelineCommands.kt (this phase's main new work),
                     NodeEditCommands.kt / CardEditCommands.kt /
                     RelationshipEditCommands.kt / TimelineEditCommands.kt
                     (UI Save-flow bridges, new), ImportExportRepository
                     (version-checked).
                     src/test/kotlin/: 28 real JUnit5 tests (new this phase) —
                     CommandTestFixture, NodeCommandsTest, CardCommandsTest,
                     RelationshipCommandsTest, TimelineCommandsTest,
                     CompoundCommandTest, UiBoundaryTest.
core-search/        SearchIndex + SearchIndexCache (Repository-backed, event-driven).
core-graph/         GraphBuilder + GraphCache (Repository-backed, event-driven).
core-plugin/        LoreCanvasPlugin/PluginContext/PluginRegistry + StatisticsPlugin.
app/                Compose UI. LoreCanvasApp.kt now routes Node/Card/Relationship/
                     Timeline mutations through commandHistory.execute() instead of
                     calling repositories directly (this phase's UI work). Delete
                     operations remain direct repository calls by design. No visual/
                     navigation redesign this phase, and no Undo/Redo buttons yet.
```

## What's actually been verified, and what hasn't

- **VERIFIED BY COMPILATION AND EXECUTION:** all 11 `core-*` modules compile clean together. The 28-test suite (`core-repository/src/test`, real JUnit5, Gradle-discoverable via the already-configured `useJUnitPlatform()`) passes in full: every Command for every entity type, `CompoundCommand`'s batching and reverse-order undo, the Create-redo identity bug fix, and the UI Save-flow bridge logic (`*EditCommands.kt`) simulating the exact call sequence the real Compose screens make — multi-field edit → Save → Undo (atomic) → Redo — plus a regression test for the Timeline-rename-never-persisted bug.
- **VERIFIED BY STATIC INSPECTION only:** the actual `LoreCanvasApp.kt` wiring (Compose code itself). Confirmed by direct inspection that `commandHistory.execute(...)` is called at the intended 11 mutation sites, and that Delete remains a direct repository call everywhere, matching the documented architecture. This has *not* been confirmed by real compilation — see below.
- **BLOCKED BY ENVIRONMENT — the Android/Compose build itself.** This sandbox has no network path to Google's Maven, Maven Central, or the Gradle Plugin Portal, so the actual `./gradlew build test` has never been run, and the Compose UI code has never been compiled for real. This is a sandbox/environment constraint, not a project defect — confirmed blocked on multiple separate occasions, not a one-off.
- **NOT YET VERIFIED, and can't be from here:** anything requiring a real Android runtime — UI tests, Compose rendering/scrolling performance, memory leak detection, ANR detection, lint, and any static analysis tooling that needs the Android Gradle Plugin. These need Android Studio or a real device.

## Android build status — read before assuming the app builds

**The Android/Compose application has NOT been compiled or run.** Everything above is Repository-layer and plain-Kotlin verification (`core-*` modules only). The `app` module's Compose code has only ever been statically inspected, never actually built by Gradle/AGP.

**Prerequisites, pulled directly from the project's own Gradle files:**
- JDK 17
- Gradle 8.11.1 (declared in `gradle/wrapper/gradle-wrapper.properties`)
- Android Gradle Plugin 8.9.1
- Kotlin 2.0.21 (`org.jetbrains.kotlin.android` / `.jvm` / `.plugin.compose`, all pinned identically)
- Compose BOM `2026.06.00`
- compileSdk 36, targetSdk 36, minSdk 26
- Repositories needed: `google()`, `mavenCentral()`, `gradlePluginPortal()`

**Note: the `gradlew` wrapper script itself is not currently committed to this repository** — only `gradle-wrapper.properties` is present. A real build environment will need to run `gradle wrapper` once (or let Android Studio generate it) before `./gradlew build test` will work.

## Requirements

JDK 17, Android SDK Platform 36 / Build-Tools 36.0.0, minSdk 26, targetSdk/compileSdk 36.

## Next milestone

The UI Command Integration gap from last phase is closed: `LoreCanvasApp.kt`'s Node/Card/Relationship/Timeline mutations now go through `commandHistory.execute()`, not straight to the repository.

What's actually next, in order:

1. **Get a real Android/Gradle build running somewhere with network access to Google's Maven and Maven Central**, and run `./gradlew build test` for real. This is the single biggest open item — nothing about the Compose UI code has been verified by actual compilation yet, only by static inspection.
2. **Undo/Redo UI buttons.** `CommandHistory` already exposes `canUndo`/`canRedo`/`nextUndoLabel`/`nextRedoLabel`/`executedLabels()`, and already lives at the right scope (`WorkspaceContext`, shared across the whole editing session) — the infrastructure is ready, but no button exists anywhere in the app yet. Deliberately not started until (1) above succeeds.
