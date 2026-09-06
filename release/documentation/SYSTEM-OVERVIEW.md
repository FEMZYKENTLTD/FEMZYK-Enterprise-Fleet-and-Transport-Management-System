# System Overview — FEMZYK Enterprise Fleet & Transport Management System

**Version:** 2.0.0 · **Course:** LASU CSC 392 (SIWES) · **Organisation:** FEMZYK ENT LTD  
**Stack:** Java 17 · Swing · SQLite (sqlite-jdbc) · Maven · JUnit 5

## 1. Purpose

A single desktop application that lets a small/medium Nigerian transport operation manage its **people** (employees, drivers, system users), its **fleet** (vehicles, assignments, trips) and its **operations** (maintenance, fuel, expenses), and get **insight** (dashboard, reports, audit trail) from one relational database.

## 2. Project evolution (honest history)

This system was **not** written from zero. It is the consolidation of several FEMZYKENTLTD student projects into one product:

| Origin repository | What it contributed | How it was used |
|---|---|---|
| **Femzyk-Vehicle-Management-System v4.x** (this repo's history) | Swing app, Car/Motorcycle/Truck models, PBKDF2 login, recycle bin, dark theme, `.dat` serialization storage | Product identity kept; PBKDF2 parameters kept so old passwords still work; recycle bin kept; models refactored into an abstract `Vehicle` hierarchy over SQLite; `.dat` storage **replaced** by SQLite with a migration path |
| **EMPLOYEE-MANAGEMENT-SYSTEM** | SQLite DatabaseManager pattern, CRUD + stream statistics, filter bar | Adapted into `database/`, `EmployeeService.statistics()`, `CrudTablePanel` filters |
| **CourseManagementSystem** | SystemLogger, JTable screen pattern | Became `audit_logs` table + `AuditService`; JTable pattern became the generic `CrudTablePanel` |
| **Contact_Book_Pro** | Auto-codes, validators | `CodeSequenceRepository` (EMP-0001, DRV-0001, VEH-0001, TRP-0001), `Validators` |
| **stock-analysis-javafx** | Statistics helpers | `util/Statistics` |

The full KEEP/ADAPT/REFACTOR/REPLACE/DO-NOT-INTEGRATE analysis is in [REPOSITORY-AUDIT.md](REPOSITORY-AUDIT.md).

## 3. Modules

| Module | Key capabilities |
|---|---|
| Authentication & RBAC | PBKDF2 hashed passwords, lockout after 5 failures (15 min), forced password change, 4 roles with fine-grained permissions enforced in the **service layer** |
| Employees | CRUD, departments, statuses, salary statistics, recycle bin, CSV import/export |
| Drivers | Linked 1:1 to employees, licence class (A–E), expiry alerts, recycle bin |
| Vehicles | Car / Motorcycle / Truck / Van / Bus (single-table inheritance), status state machine, mileage never decreases, history view, recycle bin |
| Assignments | Conflict-free vehicle↔driver assignment (DB unique indexes + service rules), return, reassign, cancel, history |
| Trips | PLANNED → ACTIVE → COMPLETED / CANCELLED with odometer validation; completing updates the vehicle mileage |
| Maintenance | Types, statuses, due-by-date and due-by-mileage reminders, auto-posted expense |
| Fuel | Purchases, price/litre, auto-posted expense, km/L efficiency from odometer readings |
| Expenses | Manual + system-generated, totals by category / vehicle / month |
| Dashboard | 8 KPI tiles + 6 charts + alert list, all from live SQL aggregates |
| Reports | 10 reports, CSV / HTML / PDF export (PDF writer is built in, no library) |
| Audit log | Every login, create, update, delete, status change, export, backup |
| Data & System | Verified backup/restore, CSV import/export with templates, demo data, legacy `.dat` migration |

## 4. Roles

| Role | Can |
|---|---|
| Administrator | Everything, including users, audit log, backups, demo data |
| Fleet Manager | All fleet/people/operations records, reports, import/export |
| Operations Officer | Trips, maintenance, fuel, expenses; view everything else |
| Viewer | Read-only access + report export |

## 5. Default credentials (development / evaluation)

On first run, when the `users` table is empty, the system creates **`admin` / `Admin@2024`** with *must change password* set. This is documented deliberately; it is not a hidden secret, and it must be changed at first sign-in.

## 6. Where data lives

`~/.femzyk-fleet/fleet.db` (SQLite, WAL mode) with `backups/`, `exports/`, `logs/` beside it. Override with `--data DIR`.

## 7. Documentation map

ARCHITECTURE · DATABASE-DESIGN · USER-GUIDE · INSTALLATION-GUIDE · TEST-PLAN · TEST-RESULTS · SDLC-DOCUMENTATION · CSC392-EVIDENCE-MATRIX · REPOSITORY-AUDIT · CHANGELOG · RELEASE-NOTES · ../ACCEPTANCE-STATUS.md
