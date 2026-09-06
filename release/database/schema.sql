-- FEMZYK Enterprise Fleet & Transport Management System
-- SQLite relational schema (version 1). Applied idempotently on start-up by SchemaInitializer.
-- Foreign keys are enforced with PRAGMA foreign_keys = ON (set per connection).

CREATE TABLE IF NOT EXISTS schema_version (
    version      INTEGER PRIMARY KEY,
    applied_at   TEXT    NOT NULL,
    description  TEXT
);

CREATE TABLE IF NOT EXISTS users (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    username               TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    email                  TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    full_name              TEXT    NOT NULL,
    password_salt          TEXT    NOT NULL,
    password_hash          TEXT    NOT NULL,
    role                   TEXT    NOT NULL CHECK (role IN ('ADMINISTRATOR','FLEET_MANAGER','OPERATIONS_OFFICER','VIEWER')),
    active                 INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
    failed_login_attempts  INTEGER NOT NULL DEFAULT 0,
    locked_until           TEXT,
    last_login_at          TEXT,
    last_password_reset_at TEXT,
    must_change_password   INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0,1)),
    created_at             TEXT    NOT NULL,
    updated_at             TEXT    NOT NULL,
    deleted_at             TEXT
);

CREATE TABLE IF NOT EXISTS employees (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_code           TEXT    NOT NULL UNIQUE,
    first_name              TEXT    NOT NULL,
    last_name               TEXT    NOT NULL,
    phone                   TEXT,
    email                   TEXT    UNIQUE COLLATE NOCASE,
    address                 TEXT,
    department              TEXT    NOT NULL,
    position                TEXT,
    date_of_birth           TEXT,
    hire_date               TEXT    NOT NULL,
    salary                  REAL    CHECK (salary IS NULL OR salary >= 0),
    status                  TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ON_LEAVE','SUSPENDED','TERMINATED')),
    emergency_contact_name  TEXT,
    emergency_contact_phone TEXT,
    notes                   TEXT,
    created_at              TEXT    NOT NULL,
    updated_at              TEXT    NOT NULL,
    deleted_at              TEXT
);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_employees_status     ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_name       ON employees(last_name, first_name);

CREATE TABLE IF NOT EXISTS drivers (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    driver_code         TEXT    NOT NULL UNIQUE,
    employee_id         INTEGER NOT NULL UNIQUE REFERENCES employees(id) ON DELETE RESTRICT,
    licence_number      TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    licence_category    TEXT    NOT NULL CHECK (licence_category IN ('A','B','C','D','E')),
    licence_issue_date  TEXT,
    licence_expiry_date TEXT    NOT NULL,
    status              TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ON_LEAVE','SUSPENDED','INACTIVE')),
    notes               TEXT,
    created_at          TEXT    NOT NULL,
    updated_at          TEXT    NOT NULL,
    deleted_at          TEXT
);
CREATE INDEX IF NOT EXISTS idx_drivers_status ON drivers(status);
CREATE INDEX IF NOT EXISTS idx_drivers_expiry ON drivers(licence_expiry_date);

CREATE TABLE IF NOT EXISTS vehicles (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_code        TEXT    NOT NULL UNIQUE,
    registration_number TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    vehicle_type        TEXT    NOT NULL CHECK (vehicle_type IN ('CAR','MOTORCYCLE','TRUCK','VAN','BUS')),
    make                TEXT    NOT NULL,
    model               TEXT    NOT NULL,
    year                INTEGER NOT NULL CHECK (year BETWEEN 1900 AND 2100),
    color               TEXT,
    vin                 TEXT    UNIQUE COLLATE NOCASE,
    engine_number       TEXT,
    acquisition_date    TEXT,
    acquisition_cost    REAL    NOT NULL DEFAULT 0 CHECK (acquisition_cost >= 0),
    current_mileage     INTEGER NOT NULL DEFAULT 0 CHECK (current_mileage >= 0),
    fuel_type           TEXT    NOT NULL CHECK (fuel_type IN ('PETROL','DIESEL','ELECTRIC','HYBRID','CNG')),
    capacity            INTEGER NOT NULL DEFAULT 0 CHECK (capacity >= 0),
    status              TEXT    NOT NULL DEFAULT 'AVAILABLE'
                        CHECK (status IN ('AVAILABLE','ASSIGNED','IN_SERVICE','MAINTENANCE','OUT_OF_SERVICE','RETIRED')),
    attribute_number    REAL,      -- doors / wheels / cargo tons (single-table inheritance)
    attribute_text      TEXT,      -- transmission / motorcycle style
    notes               TEXT,
    created_at          TEXT    NOT NULL,
    updated_at          TEXT    NOT NULL,
    deleted_at          TEXT
);
CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles(status);
CREATE INDEX IF NOT EXISTS idx_vehicles_type   ON vehicles(vehicle_type);
CREATE INDEX IF NOT EXISTS idx_vehicles_make   ON vehicles(make, model);

CREATE TABLE IF NOT EXISTS vehicle_assignments (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id   INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    driver_id    INTEGER NOT NULL REFERENCES drivers(id)  ON DELETE RESTRICT,
    assigned_at  TEXT    NOT NULL,
    returned_at  TEXT,
    status       TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETURNED','CANCELLED')),
    purpose      TEXT,
    notes        TEXT,
    assigned_by  TEXT,
    created_at   TEXT    NOT NULL,
    updated_at   TEXT    NOT NULL,
    CHECK (returned_at IS NULL OR returned_at >= assigned_at)
);
-- At most one ACTIVE assignment per vehicle and per driver (partial unique indexes).
CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_active_vehicle ON vehicle_assignments(vehicle_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX IF NOT EXISTS uq_assignment_active_driver  ON vehicle_assignments(driver_id)  WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_assignments_vehicle ON vehicle_assignments(vehicle_id, assigned_at);

CREATE TABLE IF NOT EXISTS trips (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_code        TEXT    NOT NULL UNIQUE,
    vehicle_id       INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    driver_id        INTEGER NOT NULL REFERENCES drivers(id)  ON DELETE RESTRICT,
    origin           TEXT,
    destination      TEXT    NOT NULL,
    purpose          TEXT,
    departure_time   TEXT    NOT NULL,
    return_time      TEXT,
    start_mileage    INTEGER CHECK (start_mileage IS NULL OR start_mileage >= 0),
    end_mileage      INTEGER CHECK (end_mileage IS NULL OR end_mileage >= 0),
    fuel_used_litres REAL    CHECK (fuel_used_litres IS NULL OR fuel_used_litres >= 0),
    status           TEXT    NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','ACTIVE','COMPLETED','CANCELLED')),
    notes            TEXT,
    created_at       TEXT    NOT NULL,
    updated_at       TEXT    NOT NULL,
    CHECK (end_mileage IS NULL OR start_mileage IS NULL OR end_mileage >= start_mileage),
    CHECK (return_time IS NULL OR return_time >= departure_time)
);
CREATE INDEX IF NOT EXISTS idx_trips_vehicle   ON trips(vehicle_id, departure_time);
CREATE INDEX IF NOT EXISTS idx_trips_driver    ON trips(driver_id, departure_time);
CREATE INDEX IF NOT EXISTS idx_trips_status    ON trips(status);

CREATE TABLE IF NOT EXISTS maintenance_records (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id           INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    service_date         TEXT    NOT NULL,
    maintenance_type     TEXT    NOT NULL CHECK (maintenance_type IN ('ROUTINE_SERVICE','REPAIR','INSPECTION','TYRE','BODYWORK','ELECTRICAL','OTHER')),
    description          TEXT    NOT NULL,
    provider             TEXT,
    cost                 REAL    NOT NULL DEFAULT 0 CHECK (cost >= 0),
    mileage_at_service   INTEGER CHECK (mileage_at_service IS NULL OR mileage_at_service >= 0),
    next_service_date    TEXT,
    next_service_mileage INTEGER CHECK (next_service_mileage IS NULL OR next_service_mileage >= 0),
    status               TEXT    NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    notes                TEXT,
    created_at           TEXT    NOT NULL,
    updated_at           TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_maintenance_vehicle ON maintenance_records(vehicle_id, service_date);
CREATE INDEX IF NOT EXISTS idx_maintenance_next    ON maintenance_records(next_service_date);

CREATE TABLE IF NOT EXISTS fuel_records (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id        INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    driver_id         INTEGER REFERENCES drivers(id) ON DELETE SET NULL,
    fuel_date         TEXT    NOT NULL,
    quantity_litres   REAL    NOT NULL CHECK (quantity_litres > 0),
    unit_price        REAL    NOT NULL CHECK (unit_price >= 0),
    total_cost        REAL    NOT NULL CHECK (total_cost >= 0),
    mileage           INTEGER CHECK (mileage IS NULL OR mileage >= 0),
    station           TEXT,
    receipt_reference TEXT,
    fuel_type         TEXT    CHECK (fuel_type IS NULL OR fuel_type IN ('PETROL','DIESEL','ELECTRIC','HYBRID','CNG')),
    notes             TEXT,
    created_at        TEXT    NOT NULL,
    updated_at        TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fuel_vehicle ON fuel_records(vehicle_id, fuel_date);
CREATE INDEX IF NOT EXISTS idx_fuel_date    ON fuel_records(fuel_date);

CREATE TABLE IF NOT EXISTS expenses (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    vehicle_id                   INTEGER REFERENCES vehicles(id) ON DELETE RESTRICT,
    expense_date                 TEXT    NOT NULL,
    category                     TEXT    NOT NULL CHECK (category IN ('FUEL','MAINTENANCE','REPAIRS','INSURANCE','REGISTRATION','TOLLS','PARKING','FINES','CLEANING','MISCELLANEOUS')),
    amount                       REAL    NOT NULL CHECK (amount >= 0),
    vendor                       TEXT,
    description                  TEXT    NOT NULL,
    reference                    TEXT,
    notes                        TEXT,
    source_fuel_record_id        INTEGER UNIQUE REFERENCES fuel_records(id) ON DELETE CASCADE,
    source_maintenance_record_id INTEGER UNIQUE REFERENCES maintenance_records(id) ON DELETE CASCADE,
    created_at                   TEXT    NOT NULL,
    updated_at                   TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_expenses_date     ON expenses(expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_vehicle  ON expenses(vehicle_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp   TEXT NOT NULL,
    username    TEXT NOT NULL,
    action      TEXT NOT NULL,
    module      TEXT NOT NULL,
    reference   TEXT,
    description TEXT
);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_user      ON audit_logs(username);

-- Sequence table for human-readable codes (EMP-0001 ...). One row per prefix.
CREATE TABLE IF NOT EXISTS code_sequences (
    prefix     TEXT PRIMARY KEY,
    next_value INTEGER NOT NULL
);

-- Application settings (e.g. demo-data flag, legacy migration marker).
CREATE TABLE IF NOT EXISTS app_settings (
    key   TEXT PRIMARY KEY,
    value TEXT
);
