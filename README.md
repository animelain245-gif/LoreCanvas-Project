# LoreCanvas (Android)

An offline-first worldbuilding and story-planning studio, native Android (Kotlin + Jetpack Compose).

**Status: Phase 7 — testing pass, and a real performance bug found and fixed.** This round focused on the testing categories the roadmap called for at the core layer (unit/repository/storage/integration), plus Batch Commands and Template categories/built-ins. The most consequential result: an actual performance test at scale caught a genuine O(n²)-shaped bug, not a false alarm.

## The performance finding (read this one first)

Adding Timeline events one at a time re-serializes and rewrites the *entire* Timeline file on every single add. Measured directly: **100 events → 270ms, 500 → 905ms, 1,000 → 2,727ms** — clearly super-linear, not the flat per-event cost a well-behaved design would have. At the stated target scale (20,000 Timeline Events) this would be genuinely unusable.

Fixed with `TimelineRepository.addEvents()` — a bulk variant that validates everything, mutates everything in memory, then saves **once**. Measured: **2,000 events in one bulk call → 190ms** (versus 2,727ms for half that many one-at-a-time). This also directly satisfies this phase's "batch operations" ask — it's the same fix either way.

**What this doesn't fix:** a user adding events one at a time by hand through the UI still hits the per-add save cost — that's inherent to "one event, one edit, one save" and is fine at human-typing speed. The bulk path matters for Import (already wired if Import ever adds many events at once — currently Import writes Timelines directly via Storage, not through `addEvents`, so this is available but not yet the load-bearing path for Import specifically) and any future bulk-creation feature. A deeper fix (per-event files instead of one growing embedded array) would remove the ceiling entirely but is a real storage-format change, not attempted this pass.

## Honest scope of "Performance Tests" and "UI Tests" in this phase's ask

This environment has no Android SDK, no emulator, and no device — so "smooth scrolling," "no ANRs," "no memory leaks," and any on-device UI test genuinely cannot be verified here, full stop. What *was* tested for real: repository/storage/search/graph behavior against synthetic data at meaningful scale (5,000 Nodes, 10,000 Cards, 5,000 Relationships tested directly; Timeline events tested up to 2,000 in the bulk path after the fix), measuring real wall-clock time against real files on a real filesystem. That's a genuine, if partial, answer to "does this scale" — it is not a substitute for profiling the actual app on a device, which only Android Studio can do.

## This session's other changes

- **Batch/Compound Commands.** `CompoundCommand` groups several `Command`s into one undo/redo step, executing in order and undoing in reverse. `CommandHistory` gained `executeBatch()`, plus `nextUndoLabel`/`nextRedoLabel`/`executedLabels()` for a future "Command History" UI list — none of that introspection existed before.
- **Template categories + built-in templates.** `Template` gained a `category` field (old template files without one default to "Custom" — a real backward-compatibility path, tested). `BuiltInTemplates` ships three ready-made templates (Character/Location/Organization) that appear in every project without being created, can be applied like any other template, and are protected from deletion — `TemplateRepository.list()` merges them with user-created ones.
- **Storage corruption and missing-file handling tested across every entity type**, not just Node/Project as before: a corrupted Card/Relationship/Timeline/Template file is skipped (not crashed on) by every list operation and returns `null`/a clean `NOT_FOUND` from single-item lookups; a file deleted out from under a live in-memory object fails its next save cleanly instead of throwing.

## Project layout

```
core-common/       Json, Markdown, Logger, Config, LcResult, UuidService.
core-domain/        Entity, Project, Node, Card, Relationship+RelationshipContext,
                     Timeline+TimelineEvent, Template (+category, +BuiltInTemplates).
core-validation/    Rules + EntityValidators (one validator per entity).
core-events/        EventBus, DomainEvent.
core-commands/      Command, CompoundCommand (new), CommandHistory (+ introspection).
core-storage/       FileManager, one Storage/FileStorage/Serializer trio per entity,
                     ExportBundleSerializer (version-tagged).
core-repository/    Transaction, RepositoryError, one Repository per entity,
                     NodeCommands.kt, TimelineRepository.addEvents() (new, fixes the
                     measured perf bug), ImportExportRepository (version-checked).
core-search/        SearchIndex + SearchIndexCache (Repository-backed, event-driven).
core-graph/         GraphBuilder + GraphCache (Repository-backed, event-driven).
core-plugin/        LoreCanvasPlugin/PluginContext/PluginRegistry + StatisticsPlugin.
app/                Compose UI, unchanged in shape this session — no UI work this round;
                     all effort went into testing and the performance fix above.
```

## What's actually been verified, and what hasn't

- **Verified for real:** all 11 `core-*` modules compile clean. Seven smoke tests now run together with zero regressions — six from prior sessions, plus a new one (`SmokeTestPhase7`) covering exactly this session's claims: `CompoundCommand` executing/undoing/redoing as one atomic step; built-in templates present without creation, correctly categorized, protected from deletion, still applicable; a user template's category persisting to and reading back from disk; and — the most thorough round of this particular check yet — a corrupted file and a missing file for **every single entity type** (Card, Relationship, Timeline, Template, plus Node), confirming each is handled cleanly rather than crashing. The performance numbers quoted above are real measurements from actual runs against real files in this sandbox, not estimates.
- **Still not verified by me, and can't be from here:** anything requiring a real Android runtime — UI tests, Compose rendering/scrolling performance, memory leak detection, ANR detection, the actual Gradle/Android build itself, lint, and static analysis tooling that needs the Android Gradle Plugin. These all need Android Studio or a real device; that boundary hasn't moved across any session because the constraint is environmental, not something more effort here would resolve.

## Requirements

JDK 17, Android SDK Platform 36 / Build-Tools 36.0.0, minSdk 26, targetSdk/compileSdk 36.

## Next milestone

Correction to my own earlier draft of this note: I initially planned to say Import/Export needed to be updated to use the new `addEvents()` bulk path — I checked before finalizing, and it doesn't. `ImportExportRepository` already writes each imported Timeline as a single `createTimeline` call with all its events already embedded in that one object (reconstructed whole from the export bundle's JSON), so it was never affected by the per-event bottleneck in the first place. `addEvents()`'s real beneficiary is any future bulk-creation path — a "paste multiple events at once" UI feature, or programmatic bulk population — not Import, which was already fine.

The actual next milestone, unchanged from last session: Commands exist and are tested at the Repository layer, but the UI still doesn't call them — `NodeEditorScreen`'s handlers call the repository directly instead of routing through `commandHistory.execute()`, and there's no Undo/Redo button anywhere. That's still the biggest gap between "the infrastructure works" and "a user can press undo."
