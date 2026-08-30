# AGENTS.md

LoreCanvas is an offline-first Android writer/story-planning application
(Kotlin + Jetpack Compose).

**Current source code is the implementation source of truth.**
Historical design documents are evidence of *intended* behavior, not
implementation fact.

Do not infer implementation status from historical documents. Always
verify against actual source.

## Design history

@./docs/design-history/chatgpt-lorecanvas-design-history/LORECANVAS-DESIGN-INDEX.md

Read that file first for anything involving original design intent,
feature requirements, or "was this planned" questions. Do not read the
full historical transcript (`history/` inside that directory) unless the
task genuinely requires it — the index and topic documents cover most
needs in a fraction of the size.

## When auditing design vs. implementation

1. Read `LORECANVAS-DESIGN-INDEX.md`.
2. Read the relevant `final-requirements/*.md` document for the specific
   topic.
3. Check `DESIGN-DECISION-LEDGER.md` for why a decision was made.
4. Inspect the actual current source code.
5. Compare design intent against implementation.
6. Report discrepancies. Do not modify code unless explicitly requested.

## Resolving conflicting historical statements

Prefer the later, explicitly finalized decision over an earlier proposal.
Never convert PROPOSED → FINAL, FUTURE → REQUIRED, or UNCERTAIN →
REQUIRED without direct evidence — several items in this archive are
honestly marked UNCERTAIN rather than guessed at; leave them that way
unless new evidence is found.

## Citations

- For historical/design claims: cite the specific file and message
  reference in `docs/design-history/.../history/`.
- For implementation claims: cite the actual source file/class/function.

## Context discipline

Use the smallest relevant context necessary. The design-history archive
is organized specifically so most questions can be answered from
`LORECANVAS-DESIGN-INDEX.md` plus one or two linked documents (a few KB
to a few tens of KB), not the full ~2.3MB historical transcript.
