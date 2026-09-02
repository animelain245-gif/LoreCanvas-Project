# LoreCanvas — Feature Inventory

Compact index. See the linked `final-requirements/` document for detail
and evidence on each row. "Expected Code Area" is a plausible guess based
on this repository's known module structure, not a confirmed audit
finding — actual implementation status requires a real code audit (see
`IMPLEMENTATION-AUDIT-MATRIX.md`).

| Feature | Final Requirement (short) | Status | Evidence | Expected Code Area |
|---|---|---|---|---|
| Product Vision | Offline-first worldbuilding app for writers | FINAL | `final-requirements/01-product-vision.md` | N/A (product-level) |
| Generic Node/Entity Model | One Node type represents all worldbuilding categories via a `type` field | FINAL | `final-requirements/02-core-application-model.md` | `core-domain` |
| Node | Identity/existence layer | FINAL (concept), UNCERTAIN (exact fields) | `final-requirements/04-node-system.md` | `core-domain`, `core-repository` |
| Card | Modular knowledge container attached to a Node | FINAL (concept), UNCERTAIN (exact fields) | `final-requirements/05-card-system.md` | `core-domain`, `core-repository` |
| Relationship (formerly "Link") | First-class, identity-bearing connection between two Nodes | FINAL | `final-requirements/07-relationship-system.md` | `core-domain`, `core-repository` |
| Timeline | Confirmed in-scope for V1; detailed spec not located in this pass | UNCERTAIN | `final-requirements/08-timeline-system.md` | `core-domain`, `core-repository` |
| Command / Undo-Redo | Not independently located as a dedicated historical spec | UNCERTAIN | `final-requirements/22-command-undo-redo.md` | `core-commands`, `core-repository` |
| Storage / Persistence | Local, deterministic, versioned, recoverable, human-inspectable files | FINAL (philosophy), UNCERTAIN (exact spec) | `final-requirements/23-storage-persistence.md` | `core-storage` |
| UI / Navigation | Simple Mode + Complex Mode | FINAL (mode split), UNCERTAIN (screens/nav detail) | `final-requirements/24-ui-navigation.md` | `app` |
| Offline-First | Every V1 feature works with zero internet dependency | FINAL | `final-requirements/25-offline-first-requirements.md` | all modules |
| MapMaker | Offline map-building: draw/edit maps, place locations, layers, markers, connections | Historically FINAL as a design; implementation status REQUIRES CODE AUDIT | `LORECANVAS-GLOSSARY.md` (MapMaker entry) | Not yet identified — no obvious current module |
| Writing Workflow (Entity Highlighting) | Select text while writing, link it to a Node, surface it in a contextual sidebar | FINAL (concept), UNCERTAIN (data model/UI detail) | `final-requirements/16-writing-workflow.md` | `app` |
| Search | Referenced repeatedly ("Search Experience" chapter, LCD-008) | UNCERTAIN detail | Not yet written up as its own `final-requirements/` doc — REQUIRES FURTHER HISTORICAL RESEARCH | `core-search` |
| Graph | Not independently traced in this pass | UNCERTAIN | REQUIRES FURTHER HISTORICAL RESEARCH | `core-graph` |
| Template | Referenced (Template Engine, offline guarantee at message #296) | UNCERTAIN detail | Not yet written up as its own `final-requirements/` doc | `core-repository` (TemplateRepository) |
| Plugin System | Designed but explicitly deferred (ADR-007) | FUTURE | `DESIGN-DECISION-LEDGER.md` D-010 | `core-plugin` |
| Cloud Sync | Explicitly deferred, optional-only if ever added | FUTURE | `DESIGN-DECISION-LEDGER.md` D-007 | Not applicable to current offline-only implementation |
| AI Features | Explicitly excluded from Version 1 | FUTURE | `DESIGN-DECISION-LEDGER.md` D-008 | Not applicable |
| Real-time Collaboration | Explicitly excluded from Version 1 | FUTURE | `DESIGN-DECISION-LEDGER.md` D-009 | Not applicable |
| Import security (path traversal prevention) | Explicit historical requirement to prevent path traversal during import | FINAL | `DESIGN-DECISION-LEDGER.md` D-014 | `core-repository` (`ImportExportRepository`) — **note: this exact vulnerability was found and fixed in this repository's actual commit history, independent of this documentation pass** |

## Not yet covered by a `final-requirements/` document

The full 26-topic list suggested by the reorganization task includes
several categories not written up here due to time constraints on this
pass, not because evidence doesn't exist: Species, Magic System,
Worldbuilding/Chronicle, PlotThread, Locations/Maps (partially covered
via the MapMaker glossary entry), Search (detail), Graph (detail),
Templates (detail), Import/Export (detail beyond the security
requirement), Monetization/Platform. **Per `02-core-application-model.md`,
most of the worldbuilding-category items in this list (Species, Magic
System, Chronicle, PlotThread, Locations) are very likely represented as
Node `type` values rather than needing their own separate requirement
documents — but this has not been independently confirmed for each one
individually in this pass.**
