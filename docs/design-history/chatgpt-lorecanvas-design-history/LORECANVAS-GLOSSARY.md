# LoreCanvas — Terminology Glossary

## Node
**Meaning:** The generic identity/existence unit. Every entity in a
project — Character, Location, Species, Magic System, Organization,
Artifact, Historical Event, anything — is a Node, distinguished only by
its `type` field.
**Historical aliases:** "Map Node" (earliest, narrower precursor concept).
**Do not confuse with:** a UI element, or a generic programming/tree
"node" — this is a specific domain entity.
**Evidence:** `history/08_...md` #436; `final-requirements/04-node-system.md`

## Card
**Meaning:** A modular knowledge container attached to exactly one Node.
Answers "what do we know about it," where the Node answers "what exists."
**Historical aliases:** none found.
**Do not confuse with:** the Node itself — a Card holds content, a Node
only holds identity.
**Evidence:** `history/08_...md` #436; `final-requirements/05-card-system.md`

## Relationship
**Meaning:** A first-class, identity-bearing connection between two
Nodes, with its own properties — not just a foreign-key pair.
**Historical aliases:** **"Link"** — this was the original term used in
"LC-NODE-001"/"LAR-001"-era specifications.
**Do not confuse with:** a simple boolean "these are connected" flag —
Relationships carry meaning (type, description, status) of their own.
**Evidence:** `history/08_...md` #462; `final-requirements/07-relationship-system.md`

## Timeline
**Meaning:** Confirmed in-scope for Version 1; detailed historical spec
not independently located in this reorganization pass.
**Historical aliases:** none found.
**Evidence:** `history/08_...md` #524

## Workspace
**Meaning:** Referenced repeatedly as a distinct architectural concept
("Workspace Specification," "Workspace/session architecture") — exact
final definition not independently traced in this pass. In this
repository's actual current implementation, `WorkspaceContext` is the
per-project dependency container shared across screens.
**Evidence:** `history/08_...md` #470 (Chapter 10, "Workspace
Specification," under the earlier LAR-001 numbering)

## Repository (architectural term, not "git repository")
**Meaning:** The central authority for creating, reading, updating,
deleting, validating, and tracking every modification to project data.
No component outside the Repository layer may directly modify project
data.
**Do not confuse with:** a git/GitHub repository — same word, unrelated
meaning, both used throughout this project's own materials.
**Evidence:** `history/09_...md` #612

## Command / CommandHistory / CompoundCommand
**Meaning:** Not independently confirmed with a dedicated historical
specification chapter in this pass. These terms describe this
repository's *actual current implementation* of an undo/redo system
(verified separately, by direct source-code inspection, elsewhere in
this project's history — not from this ChatGPT archive). Whether or in
what form this was specified in the original design conversation is
**UNCERTAIN** per this pass; see `final-requirements/22-command-undo-redo.md`.

## Transaction
**Meaning:** A guarantee that a set of operations either all succeed or
all roll back — related to, but historically distinct from, the
Command/Undo-Redo concept.
**Evidence:** `history/09_...md` #614

## LCD-XXX / LAR-001
**Meaning:** Document numbering schemes used at different points in the
historical planning conversation to organize formal specification
chapters. **Not a stable identifier on its own** — see `LCD-INDEX.md`
for the full renumbering history before treating any specific LCD number
as authoritative.

## Simple Mode / Complex Mode
**Meaning:** Two UI complexity levels — Simple Mode keeps the app
approachable; Complex Mode exposes deeper worldbuilding functionality
without forcing it on users who don't need it.
**Evidence:** `history/09_...md` #836; cross-confirmed in this
repository's own `Instructions_for_LoreCanvas` file.

## MapMaker
**Meaning:** A planned (not confirmed implemented) offline map-building
feature — drawing/editing maps, placing locations, regions, cities,
terrain features, layers, markers, connections between locations.
**Status:** Confirmed as *designed*; current implementation status is
**REQUIRES CODE AUDIT** (see `IMPLEMENTATION-AUDIT-MATRIX.md`).
**Evidence:** `history/09_...md` #820, #836
