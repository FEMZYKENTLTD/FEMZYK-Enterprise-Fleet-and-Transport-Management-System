# Acceptance Status

Status scale: NOT STARTED → IN PROGRESS → IMPLEMENTED → TESTED → DOCUMENTED → RELEASED.  
"TESTED" means covered by automated tests in `src/test`; "RELEASED" means also documented and included in `release/`. Updated 2026-09-06.

| Feature | Status | Evidence |
|---|---|---|
| Authentication (PBKDF2, lockout, forced password change) | RELEASED | AuthServiceTest, PasswordUtilTest; USER-GUIDE §1 |
| Role-based access control (4 roles / 20 permissions, service-layer enforcement) | RELEASED | AuthServiceTest, EmployeeAndDriverServiceTest (viewer denied) |
| User management, last-administrator protection | RELEASED | AuthServiceTest |
| Employee management (CRUD, departments, statistics, recycle bin) | RELEASED | EmployeeAndDriverServiceTest |
| Driver management (licence class/expiry, employee cascade) | RELEASED | EmployeeAndDriverServiceTest |
| Vehicle management (types, status state machine, odometer rule, recycle bin) | RELEASED | VehicleAndAssignmentServiceTest |
| Assignments (conflict prevention, licence check, history, return/reassign/cancel) | RELEASED | VehicleAndAssignmentServiceTest |
| Trips (PLANNED/ACTIVE/COMPLETED/CANCELLED, mileage validation) | RELEASED | TripServiceTest |
| Maintenance (due tracking, IN_PROGRESS → vehicle MAINTENANCE, derived expense) | RELEASED | MaintenanceFuelExpenseServiceTest |
| Fuel (efficiency, odometer bump, derived expense) | RELEASED | MaintenanceFuelExpenseServiceTest |
| Expenses (manual + system-generated read-only) | RELEASED | MaintenanceFuelExpenseServiceTest |
| Dashboard with real DB values and charts | RELEASED | DashboardAndReportTest, HeadlessRenderTest, screenshots/v2 |
| Search / filter on every list | IMPLEMENTED · DOCUMENTED | Repository `search()` methods used by panels; verified by rendering only — interactive filtering not covered by an automated test |
| Reports (10 kinds) with CSV / HTML / PDF export | RELEASED | DashboardAndReportTest (all kind×format combinations) |
| Audit log | RELEASED | AuthServiceTest (login events), LegacyDataMigratorTest (MIGRATE) |
| Backup / restore | RELEASED | BackupAndCsvTest |
| CSV import / export | RELEASED | BackupAndCsvTest |
| Demo data (marked, idempotent) | RELEASED | SmokeTest (`--headless-check --demo` twice) |
| First-run seeding (default admin only when users table empty) | RELEASED | AuthServiceTest |
| Legacy `.dat` migration (users + fleet data, idempotent, non-destructive) | RELEASED | LegacyDataMigratorTest with real v4 fixtures |
| SQLite relational schema (PK/FK/UNIQUE/CHECK/indexes/transactions) | RELEASED | schema.sql, DATABASE-DESIGN.md |
| Swing UI — all 13 screens | IMPLEMENTED · TESTED (offscreen) · DOCUMENTED | HeadlessRenderTest; interactive manual checklist M-01…M-10 NOT RUN (no display in build environment) |
| Single shaded jar + release bundle | RELEASED | release/ |
| Documentation set (13 docs + README) | RELEASED | docs/ |

Open items before final hand-in: run the manual checklist in TEST-PLAN.md on a desktop and record the result in TEST-RESULTS.md §4.
