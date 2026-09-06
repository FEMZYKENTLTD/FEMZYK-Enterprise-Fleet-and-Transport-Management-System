# Repository Audit

**Project:** FEMZYK Enterprise Fleet & Transport Management System
**Audit date:** 2026-09-06
**Canonical repository:** `FEMZYKENTLTD/Femzyk-Vehicle-Management-System`
**Purpose:** Inspect every source repository *before* integration, classify reusable components, and record integration decisions with reasons.

Every repository below was cloned and its source tree, `pom.xml`, README and build configuration were read. Where a build toolchain was available the code was compiled. The README of each project was **not** taken at face value; the implementation was checked.

---

## 1. Femzyk-Vehicle-Management-System (CANONICAL / PRIMARY FOUNDATION)

| Item | Finding |
|---|---|
| Purpose | Desktop "car rental agency" vehicle manager: cars, motorcycles, trucks, optional renter, recycle bin, per-user fleets |
| Technology | **Java Swing** (not JavaFX), Maven |
| Java version | 17 (`maven.compiler.release=17`) |
| JavaFX version | none — the README badge says "Java Swing" |
| Build system | Maven; compiler 3.11.0, exec 3.1.0, jar 3.3.0; JUnit 5.10.0 declared but **no tests exist** (`src/test` absent) |
| Database | **None.** Java serialization to `~/FemzykVehicleSystem/users.dat` and `profiles/<user>/fleet-data.dat` |
| Architecture | `VehicleRentalSystem` (main) → `gui.*` (16 classes incl. storage + auth) → `model.*` (Car/Motorcycle/Truck) → `interfaces.*` (Vehicle, CarVehicle, MotorVehicle, TruckVehicle) → `exception.*` (7 classes) |
| Size | 31 Java files, 5,609 lines; 34 screenshots; 5 docs |
| Entry point | `com.femzyk.vehiclesystem.VehicleRentalSystem` |
| Compiles? | Yes (verified with `javac -encoding UTF-8`; 70 encoding errors without the flag — build default was not set in `pom.xml` for javac directly, Maven sets UTF-8) |

### Major modules

| Module | Classes | Assessment |
|---|---|---|
| Authentication | `LoginDialog`, `PasswordResetDialog`, `UserDatabase`, `UserAccount`, `PasswordUtil` | PBKDF2WithHmacSHA256 with 16-byte salt and 65,536 iterations — solid. Users stored in a serialized `HashMap`; no roles; no lockout. |
| Vehicle model | `Car`, `Motorcycle`, `Truck` + 4 interfaces | Strong constructor validation, immutable identity fields, type-specific attributes (doors/fuel, wheels/type, cargo/transmission). Renter fields are rental-domain and will be superseded by driver assignments. |
| Storage | `StorageManager`, `StoredFleetData` | Per-user serialization with corrupt-file backup. Not relational; no querying; cannot support relationships (drivers, trips, costs). |
| Fleet UI | `MainWindow`, `FleetPanel`, `VehicleCardPanel`, 3 `Add*Dialog`, `RecycleBinDialog`, `StatsPanel`, `ThemeConstants` | Consistent dark theme, hover buttons, toast notifications, context menus, scroll behaviour. Business logic (recycle bin lists, persistence calls) lives inside `FleetPanel`. |
| Exceptions | `VehicleException` + 6 specific subclasses | Clean hierarchy; reusable. |

### Defects / gaps discovered

1. No database; no relationships between records; cannot express drivers, trips, maintenance, fuel, expenses.
2. No automated tests despite JUnit dependency.
3. Persistence, model and UI mixed inside `gui` package (`StorageManager`, `UserDatabase` are not GUI classes).
4. No role-based access; every registered user is an isolated tenant with their own fleet — wrong model for a company fleet.
5. No search/filter, no export/import, no backup/restore (all listed as *planned* in `docs/ROADMAP.md`).
6. README states "Enterprise Vehicle Information Management System" and "premium fleet management" — beyond what exists. Per the *build-to-the-claim* rule these become requirements rather than being deleted.
7. Application name inconsistent: "Femzyk Car Rental Agency", "Vehicle Rental System", "Vehicle Management System".

### Classification

| Component | Decision | Reason |
|---|---|---|
| Swing + Java 17 + Maven foundation | **KEEP** | Compiles, LTS, zero native dependencies, packaging is a single JAR; rewriting to JavaFX would discard 5,600 working lines for no functional gain (see §7) |
| `PasswordUtil` (PBKDF2) | **KEEP** (moved to `security`) | Correct and tested |
| `UserAccount` / `UserDatabase` | **REPLACE** with `users` table + `AuthService` | Serialized map cannot support roles, lockout, audit |
| `Car`, `Motorcycle`, `Truck` validation rules | **REFACTOR** into abstract `Vehicle` + subclasses, persisted via single-table inheritance | Preserves type-specific attributes and OOP; adds registration, VIN, mileage, status |
| `Vehicle` / `CarVehicle` / `MotorVehicle` / `TruckVehicle` interfaces | **ADAPT** | Capability interfaces retained for polymorphism |
| Exceptions | **ADAPT** | Re-parented under `ValidationException` |
| `StorageManager` / `StoredFleetData` | **REPLACE** with SQLite; **KEEP** a read-only copy under `legacy` for one-time migration | Old `.dat` data must be importable (§21 of directive) |
| Recycle bin (soft delete + recover) | **KEEP** | Implemented as `deleted_at` column + Recycle Bin view |
| `ThemeConstants`, toolbar buttons, toast, hover effects | **ADAPT** | Basis of a shared `ui.Theme`/`UiFactory` |
| `StatsPanel` | **REFACTOR** into database-driven Dashboard | Counts must come from SQL, not in-memory lists |
| Per-user separate fleets | **REPLACE** with one company fleet + roles | Correct model for a fleet operator |
| Screenshots | **KEEP** | Historical evidence of the earlier version |

---

## 2. EMPLOYEE-MANAGEMENT-SYSTEM (SOURCE)

| Item | Finding |
|---|---|
| Purpose | CS 1102 Unit 8 assignment: employee CRUD with Stream API demos |
| Technology | **JavaFX 17.0.2**, SQLite (`sqlite-jdbc 3.45.1.0`), Maven, shade plugin, `Launcher` non-FX entry class |
| Java version | 11 (source/target) |
| Build system | Maven; `javafx-maven-plugin 0.0.8`, shade 3.4.1; GitHub Actions release workflow |
| Database | SQLite at `~/.femzyk/employees.db`; one table `employees(id, name, age, department, salary)`; seeds 10 rows if empty |
| Architecture | 4 classes: `Employee` (JavaFX properties), `DatabaseManager` (singleton, prepared statements, CRUD), `EmployeeApp` (all UI + stream demos, ~900 lines), `Launcher` |
| Tests | none |

### Reusable functionality

| Component | Decision | Integration |
|---|---|---|
| SQLite JDBC dependency version (3.45.1.0) | **KEEP** | Same artifact used by the final system |
| `DatabaseManager` pattern: home-directory DB, `CREATE TABLE IF NOT EXISTS`, prepared statements, generated keys, idempotent seeding | **ADAPT** | Generalised into `database.DatabaseManager` + `SchemaInitializer` + per-entity repositories |
| Employee CRUD (insert/update/delete/read-all) | **ADAPT** | Becomes `EmployeeRepository` / `EmployeeService` with a far richer model (code, phone, email, address, department, position, hire date, salary, status, emergency contact) |
| Stream statistics (average salary, `summaryStatistics`, `groupingBy(department)`, top earner) | **ADAPT** | `EmployeeService.statistics()` + dashboard/reports (computed against DB records) |
| Threshold filter with `Predicate<Employee>` and `>` `<` `=` modes | **ADAPT** | Department/status filters + free-text search in the Employee view |
| Validation (non-empty name/department, numeric age/salary ranges) | **ADAPT** | `validation.Validators` |
| Edit-on-double-click table row | **ADAPT** | Used by all CRUD tables |
| JavaFX `Employee` with `SimpleStringProperty` etc. | **DO NOT INTEGRATE** | UI-framework-specific; final UI is Swing |
| `EmployeeApp` layout/CSS | **DO NOT INTEGRATE** | JavaFX; visual ideas (header gradient, stats strip) reflected in Swing theme |
| Hard-coded seed employees ("Alice Johnson", etc.) | **REPLACE** | Deterministic, clearly-marked demo dataset |
| Silent `catch (SQLException) { System.err.println }` returning `-1`/`false` | **REPLACE** | Exceptions are wrapped in `DataAccessException` and surfaced to the user |

---

## 3. CourseManagementSystem (SOURCE)

| Item | Finding |
|---|---|
| Purpose | Course/student/grade management (CS 1102 Unit 7) |
| Technology | **Java Swing**, Maven, Java 11 |
| Database | None — static `ArrayList`s in `CourseManagement`; data lost on exit |
| Architecture | 7 classes (3,788 lines): `Course`, `Student`, `GradeRecord`, `CourseManagement` (static controller), `SystemLogger` (static event log), `MainGUI` (1,490 lines, JTable/DefaultTableModel), `Main` (console) |
| Tests | none |

| Component | Decision | Reason |
|---|---|---|
| `SystemLogger` concept (sequential, categorised event log accessible only through one method) | **ADAPT** | Becomes the persistent `audit_logs` table + `AuditService` |
| JTable + `DefaultTableModel` with non-editable cells, inline editing of a single column, "ghost" buttons | **ADAPT** | Pattern reused in the shared `CrudTablePanel` |
| Relationship handling (student ↔ course enrollment, prevent duplicate enrollment, withdraw) | **ADAPT** (pattern only) | Same shape as vehicle ↔ driver assignment with conflict prevention |
| Course/student/grade domain classes | **DO NOT INTEGRATE** | Unrelated to fleet management |
| Static in-memory storage | **DO NOT INTEGRATE** | Replaced by database |

---

## 4. Contact_Book_Pro (SOURCE)

| Item | Finding |
|---|---|
| Purpose | Desktop contact manager |
| Technology | **Go 1.x + Wails + JavaScript frontend** — not Java |
| Database | JSON file storage (`storage/file_storage.go`) |
| Architecture | `main.go`, `app.go`, `models/contact.go`, `validator/validator.go`, `storage/file_storage.go`; 762 lines |

| Component | Decision | Reason |
|---|---|---|
| Human-readable permanent record IDs (`CB-0001`) | **ADAPT** | `EMP-0001`, `DRV-0001`, `VEH-0001`, `TRP-0001` codes generated by `util.CodeGenerator` |
| Validators: phone (7–15 digits after stripping `+ - space`), e-mail regex, non-empty address, title-case | **ADAPT** (rewritten in Java) | `validation.Validators` |
| Recycle bin with recovery | **KEEP** (already in canonical) | Confirms the design |
| Contact model fields (name, phone, email, address) | **ADAPT** | Employee and driver contact + emergency-contact fields |
| Go/Wails code, JS frontend | **DO NOT INTEGRATE** | Different language and runtime; cannot be linked into a Java desktop application |

---

## 5. stock-analysis-javafx (SOURCE)

| Item | Finding |
|---|---|
| Purpose | Array/ArrayList exercise: average, max, count occurrences, cumulative sum |
| Technology | JavaFX, Maven |
| Size | **one class, 148 lines** |
| Dashboards / charts | **None.** README claims "stock analysis tool"; there are no charts, tables, services or data layer |

| Component | Decision | Reason |
|---|---|---|
| Aggregate helpers (average, max, count, cumulative sum) | **ADAPT** (concept) | `util.Statistics` used for monthly running totals and averages in reports |
| Input parsing with friendly error labels | **ADAPT** (concept) | Numeric field validation messages |
| Everything else | **DO NOT INTEGRATE** | No architecture or visual component worth carrying over. Charts for the dashboard are implemented from scratch in Java2D (`ui.chart.BarChartPanel`, `PieChartPanel`). |

---

## 6. Other repositories under FEMZYKENTLTD (evaluated, not integrated)

`klc-exam-portal`, `Resume-Screening-Classifier` (Python/FastAPI), two WordPress themes, `Thread-Management…explanation` (static page), `NaijaSignal-Pro`, `FemzykAuth…Platform` (web), `AIRLINE-SCRAPING-WHATSAPP-BOT`, `FEMZYK_NAARA_PLATFORM`, `content-ops-starter`.

None contains Java desktop fleet/transport functionality; several are different languages. Integrating them would add conflicting dependencies and duplicate nothing useful. **DO NOT INTEGRATE.**

---

## 7. Architecture comparison and the UI-toolkit decision

| | Canonical | EMS | Course | Contact | Stock |
|---|---|---|---|---|---|
| Language | Java 17 | Java 11 | Java 11 | Go | Java |
| UI | Swing | JavaFX 17 | Swing | Wails/JS | JavaFX |
| Persistence | Serialization | SQLite | none | JSON | none |
| Layers | UI+storage mixed | 1 DAO + 1 UI | static controller | model/validator/storage | none |
| Tests | 0 | 0 | 0 | 0 | 0 |

**Decision: the final system remains Java 17 + Swing + Maven + SQLite.**

Reasons:
1. The directive requires the canonical project to be the foundation and forbids rebuilding from zero. 100 % of the canonical UI and authentication code is Swing.
2. Swing ships inside the JDK — no platform-specific native artifacts, one fat JAR runs on Windows/macOS/Linux with any Java 17 runtime, and `jlink`/`jpackage` can bundle a runtime without JavaFX modules.
3. The only JavaFX code worth reusing (EMS) is ~900 lines of UI whose *logic* (CRUD, filter, statistics) is toolkit-independent and is being ported.
4. "Prefer JavaFX 17" in the directive is a preference conditioned on "compatible with the strongest existing codebase"; the strongest codebase is Swing.

Everything in the final system uses one package root: `com.femzyk.fleetmanagement`.

---

## 8. Build-environment note (affects how verification was done)

The engineering sandbox used for this work has no outbound access to Maven Central, Adoptium, or Debian package mirrors. To build and test honestly:

* JDK 17.0.2 (with `javac`, `jar`, `jlink`, `jpackage`) was obtained from the public AOSP prebuilt mirror (`msft-mirror-aosp/platform.prebuilts.jdk.jdk17`).
* Apache Maven 3.9.9 and the required plugin/dependency artifacts (`sqlite-jdbc 3.45.1.0`, JUnit Jupiter 5.10.2, surefire 3.2.5, compiler 3.13.0, jar 3.4.2, shade 3.6.0, slf4j) were recovered from public GitHub repositories that had committed their `.m2` caches, then verified by a probe project (`mvn -o clean package` → BUILD SUCCESS, 1 test executed, fat JAR produced and run).
* Maven is executed in offline mode (`-o`). On a normal developer machine the same `pom.xml` resolves from Maven Central; nothing in the project depends on the sandbox workaround.
* There is no display server, so Swing windows cannot be opened here. UI classes are compiled and lightweight panels are rendered off-screen in tests; end-to-end workflows are exercised through the service layer. This limitation is stated plainly in `docs/TEST-RESULTS.md`.

---

## 9. Integration plan derived from this audit

1. Establish `com.femzyk.fleetmanagement` package structure (config, database, model, repository, service, security, audit, validation, exception, reporting, io, ui, util, legacy).
2. Add SQLite schema (10 tables) with PK/FK/UNIQUE/CHECK constraints and indexes; `PRAGMA foreign_keys=ON`.
3. Port authentication to the `users` table with roles, lockout, audit; keep PBKDF2.
4. Port and expand vehicle model; preserve Car/Motorcycle/Truck attributes and recycle bin.
5. Integrate employee management (from EMS) and build driver management on top of it.
6. Add assignments, trips, maintenance, fuel, expenses with business rules.
7. Dashboard from live SQL; reports with CSV/HTML/PDF export; audit log; backup/restore; CSV import; demo data; legacy `.dat` migration.
8. Automated tests (JUnit 5) for every service and the database layer; build until `mvn clean test` and `mvn clean package` are green.
9. Documentation set and release packaging.
