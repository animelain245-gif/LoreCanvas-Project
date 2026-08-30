# Writing Workflow (Entity Highlighting / Contextual Sidebar)

## Final Requirement

While writing story text, a writer can select a word or phrase and tell
LoreCanvas which existing entity (Node) it represents (e.g. highlight
"Elara" → Character, "Arclight" → Location). LoreCanvas then recognizes
those entities in the current scene and surfaces them in a **contextual
sidebar**, without interrupting the writing flow.

## User Purpose

Direct quote, the user's own words (message #877): *"during the writing
process, you could highlight certain words of characters, locations and
such so that if they are present in the main screen during the actual
story writing, it can seperate or put them in a list."* Confirmed
explicitly as a core writing feature, not a tangential idea (message
#878: *"This fits extremely well with the Story Workspace and contextual
sidebar we just designed. I'd make this a core writing feature."*)

## Required Behavior

- Writer selects a word/phrase in the story text.
- Writer tells LoreCanvas what entity it represents (links the selected
  text to an existing Node).
- LoreCanvas tracks which entities are "in the current scene" based on
  these highlights.
- Those entities appear in a contextual sidebar (separate UI surface
  from the writing area itself — does not interrupt writing).
- Sidebar groups entities by category (example shown: Characters,
  Locations, as a nested list under "Current scene").

## Data / Domain Requirements

Implies some form of story-text-to-Node reference/link needs to exist
(a highlighted span of text associated with a specific Node id) —
exact data model not independently traced beyond this conceptual
description in this pass.

## UI Requirements

- A writing surface (main story text editor).
- A separate "contextual sidebar" surface, shown alongside the writing
  surface without covering/interrupting it.
- Described explicitly as fitting into the already-designed "Story
  Workspace" concept (message #878) — this pass did not independently
  trace the full Story Workspace specification beyond this reference.

## Relationships With Other Systems

- Directly depends on the Node system (`04-node-system.md`) — highlighted
  text must resolve to a real Node.
- Related to, but distinct from, general search (`FEATURE-INVENTORY.md`
  — Search row) — this is inline, in-context entity recognition while
  writing, not a search query.

## Historical Evolution

### Final Decision
Introduced by the user directly, late in the design conversation
(message #877, explicitly called "a final idea" by the user), and
immediately accepted and elaborated by the assistant as a core feature
(message #878) — this reads as a genuinely late addition to the design,
not something revised through multiple earlier iterations.

## Status

**FINAL** (the core interaction and purpose). UI-level detail beyond the
initial sketch (message #878's mockup) and the underlying data model:
**UNCERTAIN** — not independently traced further in this pass.

## Historical Evidence

- `history/09_2026-07-21_to_2026-08-02.md`, messages #877 (user's
  original idea), #878 (assistant's elaboration, "Entity Highlighting")
