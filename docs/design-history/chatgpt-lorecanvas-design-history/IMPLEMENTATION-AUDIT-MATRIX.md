# LoreCanvas — Implementation Audit Matrix

Per this task's instructions, this document does **not** inspect current
source code — it exists to give a future code-audit task a compact
starting checklist, seeded from the historical requirements this
reorganization pass actually confirmed. Audit Status is `REQUIRES CODE
AUDIT` throughout unless the historical archive itself explicitly
established the answer (none did, for these rows — historical documents
describe intent, not later implementation).

| ID | Feature | Historical Requirement | Evidence | Expected Area | Audit Status |
|---|---|---|---|---|---|
| A-001 | Generic Node with `type` field | One Node class represents all worldbuilding categories | `final-requirements/02-core-application-model.md` | `core-domain` | REQUIRES CODE AUDIT |
| A-002 | Node identity rule | UUIDs immutable; objects never change identity | `history/09_...md` #504 | `core-domain` | REQUIRES CODE AUDIT |
| A-003 | Card attaches to exactly one Node | Modular knowledge containers | `final-requirements/05-card-system.md` | `core-domain`, `core-repository` | REQUIRES CODE AUDIT |
| A-004 | Relationship as first-class entity with identity + properties | Not a simple foreign-key pair | `final-requirements/07-relationship-system.md` | `core-domain`, `core-repository` | REQUIRES CODE AUDIT |
| A-005 | Timeline (detail unconfirmed) | Confirmed in-scope for V1 | `final-requirements/08-timeline-system.md` | `core-domain`, `core-repository` | REQUIRES CODE AUDIT |
| A-006 | Repository as sole authority for data mutation | No component outside Repository may directly modify project data | `history/09_...md` #612 | `core-repository` | REQUIRES CODE AUDIT |
| A-007 | Transactions atomic, all-or-nothing | Every modification occurs inside a transaction | `history/09_...md` #614 | `core-repository` | REQUIRES CODE AUDIT |
| A-008 | Command/Undo-Redo (historical spec uncertain) | Not independently located as dedicated spec | `final-requirements/22-command-undo-redo.md` | `core-commands` | REQUIRES CODE AUDIT |
| A-009 | Storage: deterministic, versioned, recoverable, human-inspectable | `final-requirements/23-storage-persistence.md` | `history/09_...md` #522 | `core-storage` | REQUIRES CODE AUDIT |
| A-010 | Autosave | Dedicated "Autosave Manager" concern | `history/09_...md` #618 | `core-storage` | REQUIRES CODE AUDIT |
| A-011 | Import: validate all imported data, prevent path traversal, sanitize external inputs | Explicit historical security requirement | `history/09_...md` #638, #648 | `core-repository` (`ImportExportRepository`) | **See note below — this specific item has independent evidence of having been audited and fixed, outside this documentation pass.** |
| A-012 | No `INTERNET` permission unless genuinely required | Offline-first | `final-requirements/25-offline-first-requirements.md` | `app/src/main/AndroidManifest.xml` | REQUIRES CODE AUDIT |
| A-013 | Simple Mode / Complex Mode UI split | `final-requirements/24-ui-navigation.md` | `history/09_...md` #836 | `app` | REQUIRES CODE AUDIT |
| A-014 | No login/authentication required | `history/01_...md` #56 | any | REQUIRES CODE AUDIT |
| A-015 | No AI features in Version 1 | `DESIGN-DECISION-LEDGER.md` D-008 | any | REQUIRES CODE AUDIT (confirm absence, not presence) |
| A-016 | No real-time collaboration in Version 1 | `DESIGN-DECISION-LEDGER.md` D-009 | any | REQUIRES CODE AUDIT (confirm absence, not presence) |
| A-017 | Plugin system: architecture only, deferred | `DESIGN-DECISION-LEDGER.md` D-010 | `core-plugin` | REQUIRES CODE AUDIT |
| A-018 | MapMaker: draw/edit maps, locations, layers, markers, connections | `LORECANVAS-GLOSSARY.md` (MapMaker) | `history/09_...md` #820 | Not yet identified | REQUIRES CODE AUDIT |
| A-019 | Search Experience | `history/09_...md` #622 (LCD-008) | `core-search` | REQUIRES CODE AUDIT |

## Note on A-011 (import security / path traversal)

This item is called out specially because, unlike every other row in
this matrix, its implementation status is **not actually unknown** — a
real path-traversal vulnerability matching this exact historical
requirement was found and fixed in this repository's own commit history,
during separate, earlier work on this project (not part of this
documentation-reorganization pass, and not re-verified by re-reading
source code in this pass either). This is flagged here as a concrete,
positive example of the "regression detection" the audit process cares
about: a documented Day-1 design requirement, an implementation gap that
existed for a period, and a later fix — worth using as a calibration
example for how seriously to take the other rows in this matrix.
