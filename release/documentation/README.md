# FEMZYK Enterprise Fleet & Transport Management System

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Java%20Swing-2563EB?style=for-the-badge)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Tests](https://img.shields.io/badge/JUnit%205-49%20passing-16A34A?style=for-the-badge)

A single Java 17 desktop application for managing a company fleet: **employees, drivers, vehicles, assignments, trips, maintenance, fuel, expenses, reports and audit** — on one SQLite database, behind role-based login.

Developed by **Olufemi Keripe (FEMZYK ENTERPRISES LTD)** and submitted for **LASU CSC 392 (SIWES)**. Version **2.0.0**.

---

## Contents
- [Project evolution](#project-evolution)
- [Features](#features)
- [Screenshots](#screenshots)
- [Quick start](#quick-start)
- [Build from source](#build-from-source)
- [Architecture](#architecture)
- [Testing](#testing)
- [Documentation](#documentation)
- [Repository layout](#repository-layout)
- [Author](#author)

---

## Project evolution

This repository did not start from zero. It is the consolidation of several earlier FEMZYKENTLTD projects into one system:

| Earlier project | What was carried forward |
|---|---|
| **Femzyk Vehicle Management System v1–v4** (this repo's history, `docs/legacy-v4/`) | PBKDF2 password hashing, login/lockout/password policy, recycle bin, vehicle model (car / motorcycle / truck, now extended), dark theme, and the ability to read v4 `.dat` files for migration |
| **EMPLOYEE-MANAGEMENT-SYSTEM** | Employee CRUD, department/status model, stream-based statistics, filtering patterns |
| **CourseManagementSystem** | System logger → audit log design, JTable/CRUD panel patterns |
| **Contact_Book_Pro** | Formatted record codes (EMP-0001, VEH-0001 …) and input validators |
| **stock-analysis-javafx** | Statistics helper and chart concepts (JavaFX itself was not adopted) |

The file-by-file KEEP / ADAPT / REFACTOR / REPLACE / DO NOT INTEGRATE decisions are in [docs/REPOSITORY-AUDIT.md](docs/REPOSITORY-AUDIT.md). The serialised `.dat` storage of v4.x was **replaced** by SQLite; existing v4 data is migrated, not discarded.

## Features

| Area | Highlights |
|---|---|
| Security | PBKDF2-HMAC-SHA256 hashing, account lockout, forced first-login password change, 4 roles (Administrator, Fleet Manager, Data/Operations Officer, Viewer) with 20 permissions enforced in the service layer, last-administrator protection |
| Employees & drivers | CRUD, departments, employment status, licence class & expiry alerts, statistics, recycle bin |
| Vehicles | Car / Motorcycle / Truck / Van / Bus, status state machine (AVAILABLE → ASSIGNED → IN_SERVICE → MAINTENANCE → OUT_OF_SERVICE → RETIRED with legal transitions only), odometer never decreases |
| Assignments | One active assignment per vehicle and per driver (enforced by partial unique indexes), licence-class check, return / reassign / cancel, full history |
| Trips | PLANNED → ACTIVE → COMPLETED / CANCELLED, mileage validation, fuel & cost capture |
| Maintenance, fuel, expenses | Due reminders, vehicle goes to MAINTENANCE while work is in progress, fuel efficiency, costs automatically mirrored to read-only expense records |
| Dashboard | KPIs and four charts computed live from the database |
| Reports | Fleet, employee, driver, assignment, trip, maintenance, fuel, expense, monthly summary, audit — CSV / HTML / PDF |
| Data & system | Audit log, verified backup & restore, CSV import/export with templates, clearly-marked demo data, one-click migration of v4 `.dat` files, headless self-check |

## Screenshots

Rendered from the running application (version 2.0.0, demo data). More in [`screenshots/v2/`](screenshots/v2/); the original v4 screenshots are kept in [`screenshots/legacy-v4/`](screenshots/legacy-v4/).

| Dashboard | Vehicles |
|---|---|
| ![Dashboard](screenshots/v2/dashboard.png) | ![Vehicles](screenshots/v2/vehicles.png) |

| Assignments | Trips |
|---|---|
| ![Assignments](screenshots/v2/assignments.png) | ![Trips](screenshots/v2/trips.png) |

| Reports | Audit log |
|---|---|
| ![Reports](screenshots/v2/reports.png) | ![Audit](screenshots/v2/audit.png) |

## Quick start

Requires Java 17+.

```bash
java -jar release/FEMZYK-Fleet-Management.jar
```

* First login: **`admin` / `Admin@2024`** — you are required to set a new password immediately.
* Data is stored in `~/FemzykFleetManagement/fleet.db` (override with `--data <dir>`).
* Sample records: *Data & System ▸ Load demo data* (tagged `[DEMO DATA]`), or start with `--demo`.
* Upgrading from v4.x: *Data & System ▸ Scan & migrate legacy data* reads `~/FemzykVehicleSystem/` without modifying it.
* Scripted health check: `java -jar release/FEMZYK-Fleet-Management.jar --headless-check`.

## Build from source

```bash
git clone https://github.com/FEMZYKENTLTD/Femzyk-Vehicle-Management-System.git
cd Femzyk-Vehicle-Management-System
mvn clean package          # runs 49 tests, produces target/FEMZYK-Fleet-Management.jar
java -jar target/FEMZYK-Fleet-Management.jar
```

Only runtime dependency: `sqlite-jdbc` (shaded into the jar). Details in [docs/INSTALLATION-GUIDE.md](docs/INSTALLATION-GUIDE.md).

## Architecture

```
Swing UI  →  Services (business rules, permissions, transactions)  →  Repositories (prepared SQL)  →  SQLite
```

Package `com.femzyk.fleetmanagement`: `config`, `database`, `model`, `repository`, `service`, `security`, `audit`, `validation`, `exception`, `reporting`, `io`, `legacy`, `ui`, `util`. The UI never touches JDBC; repositories never contain business rules. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/DATABASE-DESIGN.md](docs/DATABASE-DESIGN.md) (13 tables, 23 indexes, FK/UNIQUE/CHECK constraints, WAL, schema versioning).

## Testing

```
mvn clean test
...
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
```

11 JUnit 5 classes: services (state machines, conflicts, licence rules, mileage rules, derived expenses, RBAC), security (hashing, policy, lockout), IO (backup/restore, CSV round-trip), legacy migration against **real v4.2 `.dat` files**, and an offscreen render of all 13 screens. Plan and results: [docs/TEST-PLAN.md](docs/TEST-PLAN.md), [docs/TEST-RESULTS.md](docs/TEST-RESULTS.md). The interactive manual checklist has not yet been run in the (display-less) build environment — this is stated plainly in the results.

## Documentation

| Document | Purpose |
|---|---|
| [SYSTEM-OVERVIEW](docs/SYSTEM-OVERVIEW.md) | What the system is, roles, modules |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | Layers, packages, key design decisions |
| [REPOSITORY-AUDIT](docs/REPOSITORY-AUDIT.md) | What was reused from which repo, and why |
| [DATABASE-DESIGN](docs/DATABASE-DESIGN.md) | Schema, constraints, indexes, transactions |
| [USER-GUIDE](docs/USER-GUIDE.md) | Screen-by-screen usage |
| [INSTALLATION-GUIDE](docs/INSTALLATION-GUIDE.md) | Install, run, upgrade, troubleshoot |
| [TEST-PLAN](docs/TEST-PLAN.md) / [TEST-RESULTS](docs/TEST-RESULTS.md) | Test cases and actual outcomes |
| [SDLC-DOCUMENTATION](docs/SDLC-DOCUMENTATION.md) | Requirements, phases, risks |
| [CSC392-EVIDENCE-MATRIX](docs/CSC392-EVIDENCE-MATRIX.md) | Criterion → evidence mapping |
| [CHANGELOG](docs/CHANGELOG.md) / [RELEASE-NOTES](docs/RELEASE-NOTES.md) | Version history |
| [ACCEPTANCE-STATUS](ACCEPTANCE-STATUS.md) | Per-feature status |
| [docs/legacy-v4/](docs/legacy-v4/) | Original v4 README, plan, roadmap, design, user guide |

## Repository layout

```
src/main/java/com/femzyk/fleetmanagement/   application source
src/main/resources/schema.sql               database schema
src/test/java/                              JUnit 5 tests
src/test/resources/legacy-v4/               real v4 .dat fixtures for migration tests
docs/                                       documentation set (+ docs/legacy-v4/)
screenshots/v2/, screenshots/legacy-v4/     current and historical screenshots
release/                                    jar, demo database, sample CSVs, docs copy
```

## Author

**Olufemi Keripe** — FEMZYK ENTERPRISES LTD, Nigeria  
GitHub: https://github.com/FEMZYKENTLTD · Email: femzykenterprises@gmail.com

Acknowledgments: Lagos State University (CSC 392 SIWES), University of the People (where v1 of the vehicle system began as a CS 1102 assignment), the Java, Swing, Maven and SQLite communities.
