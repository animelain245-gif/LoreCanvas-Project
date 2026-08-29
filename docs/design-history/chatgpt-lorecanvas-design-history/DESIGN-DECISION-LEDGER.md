# LoreCanvas — Design Decision Ledger

Compact record of major design decisions, so this doesn't need to be
re-derived by searching the full transcript every time. Only decisions
actually supported by the historical record are listed.

| ID | Topic | Decision | Status | Supersedes | Evidence |
|---|---|---|---|---|---|
| D-001 | Product name | Named "LoreCanvas," chosen by the user directly from a shortlist (LoreForge, ChronoForge, StorySmith, InkAtlas, WorldWeaver, ThreadForge, WriterHouse) | FINAL | working name "Story Studio" | `history/01_...md` #45-46 |
| D-002 | Offline-first | Application must never assume an internet connection exists, for any Version-1 feature | FINAL | — | `history/09_...md` #588; restated dozens of times, see `final-requirements/25-offline-first-requirements.md` |
| D-003 | Entity model | No separate domain classes per worldbuilding category (Character, Location, Species, MagicSystem, etc.). One generic Node with a free `type` field represents everything. | FINAL | earlier "Map Node" (map-specific) concept, and the long worldbuilding-category list implied by earlier planning | `history/08_...md` #436, Chapter 10 "Node Engine" |
| D-004 | Platform | Kotlin + Jetpack Compose, native Android | FINAL | **Electron + TypeScript + React, Windows desktop** (the original target) | Current repo's actual `build.gradle.kts`/`app/` module supersedes; original target documented at `history/01_...md` #56, #512, #530. Exact pivot point not independently located in this pass. |
| D-005 | Relationship terminology | "Relationship" | FINAL | "Link" (original term) | `history/08_...md` #462 ("Link Model Specification"); current repo uses `Relationship`/`RelationshipRepository` throughout |
| D-006 | Login/accounts | No login required for core functionality | FINAL | — | `history/01_...md` #56 ("No login") |
| D-007 | Cloud sync | Explicitly deferred to a future version; never required for Version 1 | FUTURE | — | `history/01_...md` #56, #106, #108; revisited later at `history/13_...md` #770 as an *optional* addition that must preserve offline-first as default |
| D-008 | AI features | Explicitly excluded from Version 1 | FUTURE | — | `history/09_...md` #524, "Version 1 excludes: AI features" |
| D-009 | Real-time collaboration | Explicitly excluded from Version 1 | FUTURE | — | `history/09_...md` #524 |
| D-010 | Plugin marketplace | Explicitly excluded from Version 1; plugin *architecture* was designed but deferred | FUTURE | — | `history/09_...md` #524; `history/09_...md` #450 references "ADR-007: Plugins Designed but Deferred" |
| D-011 | UI complexity | Two modes: Simple Mode and Complex Mode | FINAL | — | `history/09_...md` #836; cross-confirmed by this repo's own `Instructions_for_LoreCanvas` |
| D-012 | Command/Undo-Redo historical spec | Not independently located as a dedicated named chapter in this pass — closest related evidence is the Repository's "Transaction Manager" (atomic operations, rollback) | UNCERTAIN | — | `history/09_...md` #612-614; see `final-requirements/22-command-undo-redo.md` |
| D-013 | Timeline detailed spec | Confirmed in-scope for Version 1; a dedicated deep-dive chapter (parallel to the Node/Card/Link engine specs) not independently located in this pass | UNCERTAIN | — | `history/08_...md` #524; see `final-requirements/08-timeline-system.md` |
| D-014 | Import security | Imported data must be treated as untrusted; explicit requirement to prevent path traversal during file operations | FINAL | — | `history/09_...md` #638, #648 — directly matches a real vulnerability found and fixed in this repository's actual implementation (see this repo's own commit history, `ImportExportRepository.kt` path-traversal fix) |
| D-015 | Document numbering scheme | See `LCD-INDEX.md` for the full LAR-001 → LCD-XXX (two passes) evolution | SUPERSEDED (twice) | — | `LCD-INDEX.md` |
