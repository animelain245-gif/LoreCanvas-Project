# Core Application Model

## Final Requirement

LoreCanvas does **not** use separate domain classes for each worldbuilding
category (no separate `Character`, `Location`, `Species`, `MagicSystem`,
`Organization` classes). Instead, every entity in a project — regardless
of what kind of thing it represents — is a single, generic **Node**, and
its specific kind is just a `type` string field on that Node.

**This is the single most important architectural decision in the whole
historical record**, and it directly explains why the current
implementation's domain model (`Node`, `Card`, `Relationship`, `Timeline`)
looks smaller than the long worldbuilding category list that appears
earlier in the design conversation (World, Locations, Maps, Lore,
Species, Magic System, Chronicle, PlotThread, etc. — see
`LORECANVAS-DESIGN-INDEX.md`'s note on this).

## User Purpose

A fixed set of hardcoded entity classes can never cover every kind of
thing a writer wants to track. A generic Node with a free-text `type`
field can represent anything a writer needs, without the application
needing a new class (and new UI, new storage format, new validation)
every time someone wants to track a new kind of thing.

## Required Behavior

- A Node is "the canonical representation of an entity inside a
  LoreCanvas project. Every entity that exists inside the project is
  represented by exactly one Node." (direct paraphrase of the Node
  Engine's stated purpose)
- Examples explicitly given for what a Node's `type` can represent:
  Characters, Cities, Kingdoms, Organizations, Species, Magic Systems,
  Artifacts, Historical Events.
- Cards attach to a Node to hold its actual content (see
  `05-card-system.md`) — the Node itself is identity only, not content.
- Relationships connect two Nodes regardless of their `type` — a
  Character-to-Location relationship uses the exact same Relationship
  mechanism as a Character-to-Character one.

## Data / Domain Requirements

- Node: id (identity, never reused), `type` (free string — this is the
  field that stands in for "Character," "Location," etc.), name,
  summary, status, tags.
- No separate storage table/file-category per worldbuilding category —
  all Nodes live in one Node collection regardless of `type`.

## Relationships With Other Systems

- `04-node-system.md` — the Node entity itself in more detail
- `05-card-system.md` — how content attaches to a Node
- `07-relationship-system.md` — how Nodes connect to each other

## Historical Evolution

### Earlier Proposal
The very first version of this idea (message #17) was narrower and
map-specific: **"Map Nodes"** — points placed on a map, each
representing "almost anything" (Castle, City, Forest, Mountain, Road,
Bridge, Battlefield, Monster Lair, Dungeon Entrance, Port, Mine, Magic
Circle, Custom Point), with three detail levels (Quick Note / Standard
Node / and a third, unconfirmed level).

### Revision
The list of worldbuilding categories that appears earlier in the
conversation and in the audit script's own Phase 3 category list (World,
Locations, Maps, Lore, Species, Magic System, Chronicle, PlotThread,
Organizations, History) was never implemented as separate systems —
these became `type` values on the generic Node instead.

### Final Decision
"Chapter 10 — Node Engine" (message #436, part of what was then called
**LAR-001, Revision B1: Core Engine Atlas**) states the generic-Node
model directly and lists Species and Magic Systems explicitly as *Node
types*, not separate systems — confirming this was the resolved, final
architecture, not an unresolved tension.

## Status

**FINAL**

## Historical Evidence

- `history/08_2026-07-18_to_2026-07-21.md`, message #436 ("Chapter 10 —
  Node Engine")
- `history/01_2026-07-14_to_2026-07-14.md`, message #17 (earliest "Map
  Node" precursor idea)
