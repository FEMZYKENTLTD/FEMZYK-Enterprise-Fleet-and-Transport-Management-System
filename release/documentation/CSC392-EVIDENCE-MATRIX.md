# CSC 392 Evidence Matrix

Maps each assessment criterion to concrete, checkable evidence in this repository. Self-assessment is honest: where evidence is partial it says so.

| # | Criterion (weight) | Evidence | Location | Self-assessment |
|---|---|---|---|---|
| 1 | Requirements & SDLC (10) | FR-01…FR-15 table, phases, risks, commit-per-phase history | docs/SDLC-DOCUMENTATION.md, `git log` | 9/10 — requirements were reverse-engineered from existing apps and the brief rather than elicited from a real client |
| 2 | Architecture (10) | Layered UI→Service→Repository→DB, single DB, dependency rule, diagram | docs/ARCHITECTURE.md, package layout under `src/main/java/com/femzyk/fleetmanagement` | 9/10 — no DI framework; `ServiceRegistry` is manual wiring |
| 3 | Java / OOP (10) | Abstract `Vehicle` + subclasses, enums with behaviour (`VehicleStatus.canTransitionTo`), generic `BaseRepository<T,ID>`, generic `CrudTablePanel<T>`, records for DTOs, streams for statistics, custom exception hierarchy | model/, repository/, ui/components/, exception/ | 9/10 |
| 4 | User interface (10) | Login, dashboard with 4 chart types, 13 module panels, search/filter, forms with validation, role-based menu hiding, theme | screenshots/v2/*.png, ui/ | 8/10 — verified by offscreen rendering + service tests only; interactive manual checklist not run in build environment (TEST-RESULTS §4) |
| 5 | Database (15) | 13 tables, PK/FK/UNIQUE/CHECK, 23 indexes, partial unique indexes for conflict prevention, WAL, transactions, prepared statements, schema versioning | src/main/resources/schema.sql, docs/DATABASE-DESIGN.md, database/DatabaseManager | 14/15 — no migration scripts beyond v1 yet |
| 6 | CRUD & business logic (15) | 11 services enforcing state machines, licence rules, odometer rules, derived expenses, soft delete/recycle bin, cascade rules | service/, tests T-05…T-40 | 14/15 |
| 7 | Testing (10) | 49 JUnit 5 tests, real legacy fixtures, headless UI render test, build-until-green history | src/test/java, docs/TEST-PLAN.md, docs/TEST-RESULTS.md | 8/10 — no coverage tool, no interactive UI tests |
| 8 | Security & validation (5) | PBKDF2-HMAC-SHA256, lockout, forced first password change, permission checks in service layer, last-admin protection, validators, prepared statements everywhere | security/, validation/, tests T-02…T-11, T-41…T-43 | 5/5 |
| 9 | Documentation (10) | 13 documents + README + acceptance status, screenshots, legacy docs retained | docs/, README.md, ACCEPTANCE-STATUS.md | 9/10 |
| 10 | Packaging & release (5) | Single shaded jar, `release/` bundle with jar, demo DB, sample CSVs, docs; installation guide incl. upgrade path | release/, docs/INSTALLATION-GUIDE.md | 5/5 |
| | **Total** | | | **90/100** |

## How to verify quickly (10 minutes)

1. `mvn clean package` → expect `Tests run: 49, Failures: 0`.
2. `java -jar release/FEMZYK-Fleet-Management.jar --headless-check --demo --data /tmp/x` → integrity ok, counts printed.
3. `java -jar release/FEMZYK-Fleet-Management.jar --data /tmp/x` → log in `admin / Admin@2024`, change password, open Dashboard.
4. Open `src/main/resources/schema.sql` and `docs/DATABASE-DESIGN.md` side by side.
5. Run `git log --oneline` to see the phase-by-phase history from the v4.x baseline `fc783e5`.
