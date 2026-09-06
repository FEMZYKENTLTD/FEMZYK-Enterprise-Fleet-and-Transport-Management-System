# Test Results

**Run date:** 2026-09-06 (UTC) · **Environment:** Linux sandbox, OpenJDK 17.0.2, Apache Maven 3.9.9, no display (headless)  
**Command:** `mvn clean package` (tests run as part of the build)

## 1. Automated results

```
[INFO] Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  12.5 s
```

| Test class | Tests | Failures | Errors |
|---|---|---|---|
| SmokeTest | 1 | 0 | 0 |
| service.AuthServiceTest | 8 | 0 | 0 |
| service.EmployeeAndDriverServiceTest | 8 | 0 | 0 |
| service.VehicleAndAssignmentServiceTest | 7 | 0 | 0 |
| service.TripServiceTest | 6 | 0 | 0 |
| service.MaintenanceFuelExpenseServiceTest | 6 | 0 | 0 |
| service.DashboardAndReportTest | 3 | 0 | 0 |
| security.PasswordUtilTest | 3 | 0 | 0 |
| io.BackupAndCsvTest | 4 | 0 | 0 |
| legacy.LegacyDataMigratorTest | 2 | 0 | 0 |
| ui.HeadlessRenderTest | 1 | 0 | 0 |
| **Total** | **49** | **0** | **0** |

Surefire reports are regenerated under `target/surefire-reports/` on every build.

## 2. Build verification

```
$ java -jar target/FEMZYK-Fleet-Management.jar --data /tmp/fleetdata --headless-check --demo
FEMZYK Enterprise Fleet & Transport Management System v2.0.0
Database: /tmp/fleetdata/fleet.db
Integrity check: ok
Demo data loaded: Summary[loaded=true, employees=10, drivers=6, vehicles=10, assignments=5, trips=12, maintenance=7, fuel=10, expenses=6]
Users: 1, vehicles: 10, employees: 10, audit rows: 94

$ java -jar target/FEMZYK-Fleet-Management.jar --data /tmp/fleetdata --headless-check --demo   # second run
Integrity check: ok
Users: 1, vehicles: 10, employees: 10, audit rows: 94          # demo not re-loaded, counts unchanged
```

## 3. Defects found and fixed during testing

| # | Found by | Defect | Fix |
|---|---|---|---|
| 1 | T-01 (first run) | Demo data gave the staff-shuttle driver a class-B licence; `AssignmentService` correctly refused the bus | Demo data corrected to class D — the rule was right, the data was wrong |
| 2 | T-48 | Legacy shadow classes declared `final` fields initialised to null; Java serialization could not populate them, migrated vehicles had year 0 and hit the `year BETWEEN 1900 AND 2100` CHECK | Fields made non-final |
| 3 | T-49 visual check | Dashboard showed a horizontal scrollbar at 1140 px | Body panel tracks viewport width |
| 4 | Build on fresh clone (review) | `.gitignore` excluded `*.dat`, which would have dropped the legacy test fixtures and broken T-47/T-48 for anyone cloning | Exception added for `src/test/resources/legacy-v4/**` |

## 4. Manual checklist status

The build sandbox has no display server, so the M-01…M-10 cases in TEST-PLAN.md could **not be executed interactively** in this environment. What was verified instead: every panel constructs, loads real data and paints offscreen (T-49, screenshots in `screenshots/v2/`), and every action behind the buttons is exercised through the service tests. The manual checklist remains **open** and should be run on a desktop before final submission; record the outcome here.

| ID | Status |
|---|---|
| M-01 … M-10 | NOT RUN (no display in build environment) |

## 5. Known limitations (not defects)

* PDF export uses a built-in minimal writer: Helvetica only, long cell text is truncated with "…"; use HTML export for a richer print layout.
* Single-user desktop app: SQLite WAL allows a second reader, but concurrent writers from two machines are not a supported scenario.
* Code coverage is not measured (JaCoCo is not available in the offline build cache); the table above lists what is covered by name.
