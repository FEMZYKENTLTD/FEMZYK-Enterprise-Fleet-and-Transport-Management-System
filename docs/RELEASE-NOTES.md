# Release Notes — v2.0.0

**FEMZYK Enterprise Fleet & Transport Management System**  
Released 2026-09-06 · Java 17 · single jar · SQLite

## Highlights
* One application, one database, one build: employees, drivers, vehicles, assignments, trips, maintenance, fuel, expenses, reports, audit.
* Role-based access (Administrator, Fleet Manager, Data/Operations Officer, Viewer).
* Business rules enforced centrally: vehicle status transitions, assignment conflicts, licence class checks, odometer monotonicity, derived expenses.
* Dashboard and reports always reflect the live database.
* Existing v4.x data migrates in one click with no changes to the old files.

## Getting started
```
java -jar FEMZYK-Fleet-Management.jar
```
Default login `admin / Admin@2024` — you must set a new password on first login. Load demo data from *Data & System ▸ Load demo data* if you want sample records (they are tagged DEMO and can be removed later).

## Compatibility
* Requires Java 17 or later (tested with OpenJDK 17.0.2).
* Data directory defaults to `~/FemzykFleetManagement/`; override with `--data <dir>`.
* Upgrading from v4.x: run the app, choose *Data & System ▸ Scan & migrate legacy data*.

## Known limitations
* Minimal PDF writer (Helvetica, truncated long cells) — use HTML export for print-quality output.
* Single-workstation use; not intended for concurrent multi-user writes over a network share.
* Interactive UI checklist (TEST-RESULTS §4) has not yet been executed on a desktop with a display; automated offscreen rendering and service tests pass.

## Verification
49/49 automated tests pass; `mvn clean package` succeeds; `--headless-check --demo` reports integrity ok.
