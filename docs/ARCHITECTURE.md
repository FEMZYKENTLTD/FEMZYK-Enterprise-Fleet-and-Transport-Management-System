# Architecture

## 1. Layered design

```
┌──────────────────────────────────────────────────────────────┐
│  ui/            Swing: LoginDialog, MainFrame, modules/*      │  presentation
│                 components/ (CrudTablePanel, FormBuilder,     │
│                 charts, StatCard)                             │
├──────────────────────────────────────────────────────────────┤
│  service/       Business rules + transactions + RBAC guards   │  application
│  reporting/     Report model + CSV/HTML/PDF exporters         │
│  io/            Backup/restore, CSV import/export             │
│  legacy/        LegacyDataMigrator (.dat → SQLite)            │
├──────────────────────────────────────────────────────────────┤
│  repository/    One class per table, prepared statements,     │  data access
│                 row mappers, aggregate queries                │
├──────────────────────────────────────────────────────────────┤
│  database/      DatabaseManager (connection, transactions,    │  infrastructure
│                 WAL, FK enforcement), SchemaInitializer        │
│  model/         Entities + enums (state machines live here)   │
│  security/      PasswordUtil (PBKDF2), SessionContext         │
│  audit/         AuditService                                  │
│  validation/    Validators + fluent Errors collector          │
│  exception/     FleetException hierarchy                      │
│  config/util/   AppConfig, DateUtil, MoneyUtil, AppLogger ... │
└──────────────────────────────────────────────────────────────┘
```

**Rule:** UI → Service → Repository → DB. UI classes never touch `java.sql`; repositories never contain business rules; services never reference Swing.

`ServiceRegistry` is the composition root (manual dependency injection). Tests build a registry on an in-memory database in one line (`TestSupport.freshRegistry()`).

## 2. Key design decisions

| Decision | Reason |
|---|---|
| **Swing, not JavaFX** | The canonical repo was Swing; JavaFX 17 natives cannot be resolved/run in the offline build environment and would add ~30 MB of platform-specific jars. Swing keeps one jar that runs everywhere Java 17 runs. |
| **SQLite single file** | Zero-install relational DB with real PKs, FKs (`PRAGMA foreign_keys=ON`), unique indexes, CHECK constraints and transactions. One database replaces the previous `users.dat` + per-user `fleet-data.dat` and the EMS `employees.db`. |
| **Single-table inheritance for vehicles** | `vehicles.vehicle_type` discriminator + two generic attribute columns (`attribute_number`, `attribute_text`) carry the type-specific fields (doors/transmission, wheels/style, cargo tons). `VehicleRepository` instantiates `Car`/`Motorcycle`/`Truck`/`GenericVehicle` polymorphically. |
| **Permissions checked in services** | `SessionContext.require(Permission)` is called at the top of every mutating service method, so the UI hiding a button is convenience, not security. |
| **State machines in enums** | `VehicleStatus.canTransitionTo`, `LicenceCategory.permits`, `DriverStatus.canBeAssigned` are pure functions that are unit-testable without a DB. |
| **Derived expenses** | Fuel and maintenance costs create linked `expenses` rows (`source_fuel_record_id` / `source_maintenance_record_id`), so cost reports are complete without double entry; those rows are read-only in the Expenses UI. |
| **Hand-written PDF exporter** | No PDF library is reachable offline; a ~200-line PDF 1.4 writer (Helvetica, multi-page tables) produces valid files verified by `DashboardAndReportTest`. HTML export is offered for nicer print layout. |
| **Legacy shadow classes** | `com.femzyk.vehiclesystem.*` contains field-compatible, behaviour-free copies of the v4.x serializable classes purely so `ObjectInputStream` (with a whitelist filter) can read old files. |

## 3. Transactions

`DatabaseManager.inTransaction(work)` sets autocommit off, runs the lambda, commits, and rolls back on any exception; nested calls join the outer transaction. Multi-table operations (assign vehicle = insert assignment + update vehicle status + audit row; complete trip = update trip + vehicle mileage + status + audit) always run inside one.

## 4. Error handling

`FleetException` (runtime) → `ValidationException` (list of messages), `BusinessRuleException`, `InvalidStateTransitionException`, `DuplicateRecordException`, `RecordNotFoundException`, `AuthenticationException`, `AuthorizationException`, `DataAccessException`. `UiUtils.showError` maps each to a titled dialog; unexpected exceptions are logged to `~/.femzyk-fleet/logs/`.

## 5. Package map (main)

```
com.femzyk.fleetmanagement
├── FleetManagementApplication      entry point (--data, --demo, --headless-check)
├── audit        AuditService
├── config       AppConfig
├── database     DatabaseManager, SchemaInitializer, JdbcSupport
├── exception    9 exception types
├── io           BackupService, CsvImportExportService, CsvUtil
├── legacy       LegacyDataMigrator
├── model        25 entities/enums
├── reporting    Report, ReportService, Csv/Html/PdfReportExporter
├── repository   12 repositories
├── security     PasswordUtil, SessionContext
├── service      Auth, Employee, Driver, Vehicle, Assignment, Trip, Maintenance,
│                Fuel, Expense, Dashboard, DemoData, ServiceRegistry
├── ui           Theme, UiUtils, LoginDialog, ChangePasswordDialog, MainFrame, ModulePanel
│   ├── components  CrudTablePanel, EntityTableModel, FormBuilder, FormDialog,
│   │               BarChartPanel, PieChartPanel, StatCard, StatusCellRenderer
│   └── modules     13 module panels
├── util         AppLogger, DateUtil, MoneyUtil, Statistics, CodeGenerator
└── validation   Validators
com.femzyk.vehiclesystem            legacy deserialisation shadows only
```
