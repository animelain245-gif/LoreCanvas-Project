# LCD/LAR Document Numbering — Evolution Record

The historical design conversation used **two different document numbering
schemes at different times**, and the second scheme was itself revised
partway through. This file records that evolution honestly rather than
picking one version and presenting it as if it were the only one that
ever existed.

## Timeline of numbering schemes

| Scheme | Active roughly | Evidence (message index in `history/`) |
|---|---|---|
| **LAR-001** ("LoreCanvas Architecture Revision") | messages #406–#546 | First appears at #406: *"Instead, from this point forward, every response becomes a section of LAR-001 itself... Today we'll begin Revision B1: Core Engine Atlas."* |
| **LCD-XXX** ("LoreCanvas Design Chapter/Document"), first numbering | messages #542–~#638 | First appears at #542 as a table of contents for "Document 01" = LCD-001 = "Executive Summary" |
| **LCD-XXX, revised numbering** | ~#638 onward | The same LCD-00X labels are reused for **different** document titles than the first LCD pass — e.g. "LCD-002" meant one thing early and something else after this point. |

**LAR-001 and the first LCD pass overlap in the raw message range (#542
sits inside #406–#546) because the conversation was iterating on structure
in real time — LAR-001 chapters were being folded into the new LCD
document set, not run in parallel as two competing specs.**

## What this means for using this archive

- **Do not treat "LCD-002" (or any LCD number) as a stable, unambiguous
  identifier on its own.** Always check which numbering pass a reference
  came from before treating it as authoritative.
- The **content itself** (Node Engine, Card Engine, Command architecture,
  etc.) is stable and consistent across the renumbering — only the
  document *labels* changed, not the underlying design. This is why the
  `final-requirements/` documents in this archive are organized by
  **topic name** (e.g. `04-node-system.md`), not by LCD number — topic
  names survived the renumbering; LCD numbers didn't.
- When a `final-requirements/` document cites historical evidence, it
  cites the **message index in `history/`**, not an LCD number, for
  exactly this reason.

## Known LCD topic identity, where confidently established

Confirmed via direct inspection of the *later* (post-#638) numbering
pass, which is the one that persisted to the end of the planning-heavy
period:

| Later LCD-XXX | Topic (as of the later pass) |
|---|---|
| LCD-000 | Front matter / overview |
| LCD-001 | Executive Summary (consistent across both LCD passes) |
| LCD-002 | Core Application Framework *(early pass: "Product Vision")* |
| LCD-003 | System Architecture |
| LCD-004 | Package Architecture *(early pass: "Package Specifications")* |
| LCD-005 | Domain Model |
| LCD-004 | Package Architecture (confirmed, message #604) |
| LCD-005 | Domain Model (confirmed by reference at message #612, "since LCD-005 is now complete") |
| LCD-006 | Repository Architecture — includes a "Transaction Manager" chapter (message #614): every project-data modification occurs inside a transaction, either every operation succeeds or every operation rolls back |
| LCD-007 | Storage Engine (message #616) — includes an "Autosave Manager" chapter (message #618) |
| LCD-008 | User Experience / UX (message #620) — includes a "Search Experience" chapter (message #622) |
| LCD-009 | Functional Workflows (message #628) — includes a "Relationship Creation Workflow" chapter (message #630) |
| LCD-010, LCD-011 | Not directly located in this reorganization pass. **Status: UNCERTAIN.** |
| LCD-012 | Implementation Roadmap (message #642) — "converts the LoreCanvas design into an executable development plan" |
| LCD-013 | Testing & Quality Assurance (message #646) |
| LCD-014 | Plugin & Extension Architecture (message #652) |
| LCD-015, LCD-016 | First appear at message #640, exact topic not independently confirmed for this pass |
| LCD-017 | First appears at message #660, exact topic not independently confirmed for this pass |
| LCD-018 through LCD-022 | Appear later still (#688–#776), evidently added after the original 17-document set — these were not part of the "LCD-001 through LCD-017" set referenced elsewhere in this project's own documentation (e.g. `PROJECT_HISTORY.txt`), and their exact topics were not independently re-verified for this reorganization pass. **Status: UNCERTAIN** — flagged rather than guessed at.

**Do not resolve the LCD-006–013 exact-topic-per-number question by
guessing.** If it's needed for a future task, it requires actually
reading messages #542–#640 in `history/06_...md` and `history/07_...md`
in full, not inferring from adjacent context.
