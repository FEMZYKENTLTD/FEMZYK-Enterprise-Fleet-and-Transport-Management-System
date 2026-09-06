# SDLC Documentation

Model followed: **iterative / incremental** with a short audit phase up front, then build-test-fix cycles per layer. Each phase is a Git commit on the integration branch so the progression is auditable.

## Phase 1 — Requirements

**Sources:** the SIWES brief, the five existing FEMZYKENTLTD repositories and their READMEs (the v4.x README claimed features that were then implemented rather than deleted — e.g. dashboard statistics, password reset, recycle bin).

**Functional requirements (FR):**

| ID | Requirement | Realised in |
|---|---|---|
| FR-01 | Secure login with hashed passwords, lockout and 4 roles | AuthService, PasswordUtil, Permission |
| FR-02 | Employee CRUD with departments, statuses, statistics, recycle bin | EmployeeService |
| FR-03 | Driver profiles with licence class and expiry alerts | DriverService |
| FR-04 | Vehicle register with types, statuses and legal transitions | VehicleService, VehicleStatus |
| FR-05 | Assignments with conflict prevention and history | AssignmentService + unique indexes |
| FR-06 | Trips with lifecycle and odometer validation | TripService |
| FR-07 | Maintenance with due reminders | MaintenanceService |
| FR-08 | Fuel tracking with efficiency | FuelService |
| FR-09 | Expense tracking incl. derived costs | ExpenseService |
| FR-10 | Dashboard with live values and charts | DashboardService, DashboardPanel |
| FR-11 | Reports with CSV/PDF export | ReportService + exporters |
| FR-12 | Audit log | AuditService |
| FR-13 | Backup/restore, CSV import/export | BackupService, CsvImportExportService |
| FR-14 | Demo data and first-run seeding | DemoDataService, AuthService.ensureDefaultAdministrator |
| FR-15 | Safe migration of v4.x `.dat` data | LegacyDataMigrator |

**Non-functional:** Java 17, single jar, single SQLite file, offline, prepared statements, transactions, < 2 s start-up, 1024×680 minimum window.

## Phase 2 — Analysis & repository audit

Every source repository was cloned and classified file-by-file (KEEP / ADAPT / REFACTOR / REPLACE / DO NOT INTEGRATE) — see REPOSITORY-AUDIT.md (commit `88d0832`). Outcome: canonical repo stays, `.dat` storage replaced, Swing retained, JavaFX declined (see ARCHITECTURE §2). A Git tag `checkpoint-pre-integration` and a file-system backup were taken before any destructive change.

## Phase 3 — Design

* Layered architecture (ARCHITECTURE.md), package `com.femzyk.fleetmanagement`.
* Relational schema with constraints (DATABASE-DESIGN.md).
* Domain rules captured as enum methods and service preconditions; UI mirrors them.
* Generic UI building blocks designed once (`CrudTablePanel`, `FormBuilder`, `FormDialog`) and reused by 13 modules.

## Phase 4 — Implementation (commits)

| Commit | Content |
|---|---|
| `88d0832` | Repository audit |
| `f4b6578` | Core: models, schema, DatabaseManager, 12 repositories, security, 11 services, reporting, IO, legacy migrator, demo data, smoke test |
| `7466a3b` | Swing UI: login, main frame, dashboard, all module panels, reports, audit, users, system tools |
| `0f85e71` | Headless render test, dashboard scroll fix |
| `fa441c2` | Full test suite (49 tests) with real v4 `.dat` fixtures |
| `2c409fa` | .gitignore fix (keep fixtures) |
| (final) | Documentation set, release artifacts, README |

Old `com.femzyk.vehiclesystem` sources were removed only after the replacement compiled and the migration test proved old data could still be read.

## Phase 5 — Testing

TEST-PLAN.md / TEST-RESULTS.md. Build-until-green: each failing test was diagnosed and either the code or (in one case) the demo data was corrected; no test was weakened to pass.

## Phase 6 — Deployment / packaging

`mvn clean package` → shaded jar `FEMZYK-Fleet-Management.jar` (contains sqlite-jdbc). `release/` holds the jar, a demo database, sample CSVs and the docs. INSTALLATION-GUIDE.md describes installation and upgrade from v4.x.

## Phase 7 — Maintenance

CHANGELOG.md and ACCEPTANCE-STATUS.md track state; the audit log and rotating application log support production diagnosis; `--headless-check` supports scripted health checks.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| Losing legacy data | Non-destructive migration, files never modified, tested with real v4 output |
| Password compatibility | Same PBKDF2 parameters, covered by T-41 |
| Regressions in shared UI components | All 13 panels rendered in T-49 after every change |
| Corrupt backup | Every backup is opened and integrity-checked before being reported as successful |
