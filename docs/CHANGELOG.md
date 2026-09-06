# Changelog

All notable changes to this project. Versions 1.x–4.x refer to the original *Femzyk Vehicle Management System*; 2.0.0 is the first release under the *FEMZYK Enterprise Fleet & Transport Management System* name (versioning restarted with the new product name and Maven artifact `fleet-management`).

## [2.0.0] — 2026-09-06

### Added
* SQLite relational database (13 tables, constraints, indexes, WAL, schema versioning) replacing Java-serialised `.dat` files.
* Employee, Driver, Assignment, Trip, Maintenance, Fuel and Expense modules.
* Role-based access control with four roles and 20 permissions enforced in the service layer.
* Dashboard with live KPIs and four charts computed from the database.
* Ten report kinds with CSV, HTML and PDF export.
* Audit log of every data change and security event.
* Backup (`VACUUM INTO` + integrity check) and restore, CSV import/export.
* Clearly marked demo data set and idempotent first-run seeding of the default administrator.
* One-click, non-destructive migration of v4.x `users.dat` and per-user `fleet-data.dat`.
* Headless self-check mode (`--headless-check`).
* 49 automated JUnit 5 tests including a headless UI render test.

### Changed
* Product renamed to **FEMZYK Enterprise Fleet & Transport Management System**; package `com.femzyk.fleetmanagement`.
* Vehicle model refactored to abstract `Vehicle` with single-table inheritance; added VAN and BUS types.
* Recycle bin, password policy and PBKDF2 hashing retained and moved to the database.
* Build now Maven-based, producing one shaded jar.

### Removed
* `StorageManager`, `UserDatabase` and per-user `.dat` persistence (a read-only reader is kept for migration).
* Old ad-hoc `run.sh`/manual javac build.

## [4.2] and earlier — legacy
See `docs/legacy-v4/` for the original project plan, roadmap, technical design and user guide of the serialised-file versions.
