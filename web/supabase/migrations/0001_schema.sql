-- ============================================================================
-- FEMZYK Enterprise Fleet & Transport Management System — cloud schema
-- PostgreSQL port of src/main/resources/schema.sql (SQLite, v1)
--
-- Same tables, same CHECK constraints, same partial unique indexes and the
-- same human-readable code sequences (EMP-0001, VEH-0001 ...).  Dates that are
-- calendar dates stay DATE; audit/created/updated columns become TIMESTAMPTZ.
--
-- Access model: the browser never touches this database directly.  The
-- Next.js server (service-role key) is the only client, so RLS is enabled with
-- no policies — anon/publishable keys are denied everything.
-- ============================================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------- users -----
create table if not exists users (
    id                     bigint generated always as identity primary key,
    username               text not null unique,
    email                  text not null unique,
    full_name              text not null,
    password_salt          text not null,
    password_hash          text not null,
    role                   text not null check (role in ('ADMINISTRATOR','FLEET_MANAGER','OPERATIONS_OFFICER','VIEWER')),
    active                 boolean not null default true,
    failed_login_attempts  integer not null default 0,
    locked_until           timestamptz,
    last_login_at          timestamptz,
    last_password_reset_at timestamptz,
    must_change_password   boolean not null default false,
    created_at             timestamptz not null default now(),
    updated_at             timestamptz not null default now(),
    deleted_at             timestamptz
);
create unique index if not exists uq_users_username on users (lower(username));
create unique index if not exists uq_users_email    on users (lower(email));

-- ------------------------------------------------------------ employees -----
create table if not exists employees (
    id                      bigint generated always as identity primary key,
    employee_code           text not null unique,
    first_name              text not null,
    last_name               text not null,
    phone                   text,
    email                   text unique,
    address                 text,
    department              text not null,
    position                text,
    date_of_birth           date,
    hire_date               date not null,
    salary                  numeric(14,2) check (salary is null or salary >= 0),
    status                  text not null default 'ACTIVE' check (status in ('ACTIVE','ON_LEAVE','SUSPENDED','TERMINATED')),
    emergency_contact_name  text,
    emergency_contact_phone text,
    notes                   text,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now(),
    deleted_at              timestamptz
);
create index if not exists idx_employees_department on employees (department);
create index if not exists idx_employees_status     on employees (status);
create index if not exists idx_employees_name       on employees (last_name, first_name);
create unique index if not exists uq_employees_email on employees (lower(email)) where email is not null;

-- -------------------------------------------------------------- drivers -----
create table if not exists drivers (
    id                  bigint generated always as identity primary key,
    driver_code         text not null unique,
    employee_id         bigint not null unique references employees (id) on delete restrict,
    licence_number      text not null unique,
    licence_category    text not null check (licence_category in ('A','B','C','D','E')),
    licence_issue_date  date,
    licence_expiry_date date not null,
    status              text not null default 'ACTIVE' check (status in ('ACTIVE','ON_LEAVE','SUSPENDED','INACTIVE')),
    notes               text,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    deleted_at          timestamptz
);
create unique index if not exists uq_drivers_licence on drivers (lower(licence_number));
create index if not exists idx_drivers_status on drivers (status);
create index if not exists idx_drivers_expiry on drivers (licence_expiry_date);

-- ------------------------------------------------------------- vehicles -----
create table if not exists vehicles (
    id                  bigint generated always as identity primary key,
    vehicle_code        text not null unique,
    registration_number text not null unique,
    vehicle_type        text not null check (vehicle_type in ('CAR','MOTORCYCLE','TRUCK','VAN','BUS')),
    make                text not null,
    model               text not null,
    year                integer not null check (year between 1900 and 2100),
    color               text,
    vin                 text unique,
    engine_number       text,
    acquisition_date    date,
    acquisition_cost    numeric(14,2) not null default 0 check (acquisition_cost >= 0),
    current_mileage     bigint not null default 0 check (current_mileage >= 0),
    fuel_type           text not null check (fuel_type in ('PETROL','DIESEL','ELECTRIC','HYBRID','CNG')),
    capacity            integer not null default 0 check (capacity >= 0),
    status              text not null default 'AVAILABLE'
                        check (status in ('AVAILABLE','ASSIGNED','IN_SERVICE','MAINTENANCE','OUT_OF_SERVICE','RETIRED')),
    attribute_number    double precision,
    attribute_text      text,
    notes               text,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    deleted_at          timestamptz
);
create unique index if not exists uq_vehicles_registration on vehicles (upper(registration_number));
create index if not exists idx_vehicles_status on vehicles (status);
create index if not exists idx_vehicles_type   on vehicles (vehicle_type);
create index if not exists idx_vehicles_make   on vehicles (make, model);

-- -------------------------------------------------- vehicle_assignments -----
create table if not exists vehicle_assignments (
    id           bigint generated always as identity primary key,
    vehicle_id   bigint not null references vehicles (id) on delete restrict,
    driver_id    bigint not null references drivers (id)  on delete restrict,
    assigned_at  date not null,
    returned_at  date,
    status       text not null default 'ACTIVE' check (status in ('ACTIVE','RETURNED','CANCELLED')),
    purpose      text,
    notes        text,
    assigned_by  text,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    check (returned_at is null or returned_at >= assigned_at)
);
-- At most one ACTIVE assignment per vehicle and per driver.
create unique index if not exists uq_assignment_active_vehicle on vehicle_assignments (vehicle_id) where status = 'ACTIVE';
create unique index if not exists uq_assignment_active_driver  on vehicle_assignments (driver_id)  where status = 'ACTIVE';
create index if not exists idx_assignments_vehicle on vehicle_assignments (vehicle_id, assigned_at);

-- ---------------------------------------------------------------- trips -----
create table if not exists trips (
    id               bigint generated always as identity primary key,
    trip_code        text not null unique,
    vehicle_id       bigint not null references vehicles (id) on delete restrict,
    driver_id        bigint not null references drivers (id)  on delete restrict,
    origin           text,
    destination      text not null,
    purpose          text,
    departure_time   timestamptz not null,
    return_time      timestamptz,
    start_mileage    bigint check (start_mileage is null or start_mileage >= 0),
    end_mileage      bigint check (end_mileage is null or end_mileage >= 0),
    fuel_used_litres numeric(10,2) check (fuel_used_litres is null or fuel_used_litres >= 0),
    status           text not null default 'PLANNED' check (status in ('PLANNED','ACTIVE','COMPLETED','CANCELLED')),
    notes            text,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    check (end_mileage is null or start_mileage is null or end_mileage >= start_mileage),
    check (return_time is null or return_time >= departure_time)
);
create index if not exists idx_trips_vehicle on trips (vehicle_id, departure_time);
create index if not exists idx_trips_driver  on trips (driver_id, departure_time);
create index if not exists idx_trips_status  on trips (status);

-- ---------------------------------------------------- maintenance_records ---
create table if not exists maintenance_records (
    id                   bigint generated always as identity primary key,
    vehicle_id           bigint not null references vehicles (id) on delete restrict,
    service_date         date not null,
    maintenance_type     text not null check (maintenance_type in ('ROUTINE_SERVICE','REPAIR','INSPECTION','TYRE','BODYWORK','ELECTRICAL','OTHER')),
    description          text not null,
    provider             text,
    cost                 numeric(14,2) not null default 0 check (cost >= 0),
    mileage_at_service   bigint check (mileage_at_service is null or mileage_at_service >= 0),
    next_service_date    date,
    next_service_mileage bigint check (next_service_mileage is null or next_service_mileage >= 0),
    status               text not null default 'COMPLETED' check (status in ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    notes                text,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now()
);
create index if not exists idx_maintenance_vehicle on maintenance_records (vehicle_id, service_date);
create index if not exists idx_maintenance_next    on maintenance_records (next_service_date);

-- --------------------------------------------------------- fuel_records -----
create table if not exists fuel_records (
    id                bigint generated always as identity primary key,
    vehicle_id        bigint not null references vehicles (id) on delete restrict,
    driver_id         bigint references drivers (id) on delete set null,
    fuel_date         date not null,
    quantity_litres   numeric(10,2) not null check (quantity_litres > 0),
    unit_price        numeric(12,2) not null check (unit_price >= 0),
    total_cost        numeric(14,2) not null check (total_cost >= 0),
    mileage           bigint check (mileage is null or mileage >= 0),
    station           text,
    receipt_reference text,
    fuel_type         text check (fuel_type is null or fuel_type in ('PETROL','DIESEL','ELECTRIC','HYBRID','CNG')),
    notes             text,
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now()
);
create index if not exists idx_fuel_vehicle on fuel_records (vehicle_id, fuel_date);
create index if not exists idx_fuel_date    on fuel_records (fuel_date);

-- ------------------------------------------------------------- expenses -----
create table if not exists expenses (
    id                           bigint generated always as identity primary key,
    vehicle_id                   bigint references vehicles (id) on delete restrict,
    expense_date                 date not null,
    category                     text not null check (category in ('FUEL','MAINTENANCE','REPAIRS','INSURANCE','REGISTRATION','TOLLS','PARKING','FINES','CLEANING','MISCELLANEOUS')),
    amount                       numeric(14,2) not null check (amount >= 0),
    vendor                       text,
    description                  text not null,
    reference                    text,
    notes                        text,
    source_fuel_record_id        bigint unique references fuel_records (id) on delete cascade,
    source_maintenance_record_id bigint unique references maintenance_records (id) on delete cascade,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz not null default now()
);
create index if not exists idx_expenses_date     on expenses (expense_date);
create index if not exists idx_expenses_vehicle  on expenses (vehicle_id, expense_date);
create index if not exists idx_expenses_category on expenses (category);

-- ----------------------------------------------------------- audit_logs -----
create table if not exists audit_logs (
    id          bigint generated always as identity primary key,
    timestamp   timestamptz not null default now(),
    username    text not null,
    action      text not null,
    module      text not null,
    reference   text,
    description text
);
create index if not exists idx_audit_timestamp on audit_logs (timestamp desc);
create index if not exists idx_audit_user      on audit_logs (username);

-- -------------------------------------------------------- code_sequences ----
create table if not exists code_sequences (
    prefix     text primary key,
    next_value integer not null
);

-- Atomically allocate the next human-readable code, e.g. next_code('EMP') -> EMP-0001.
create or replace function next_code(p_prefix text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    v_value integer;
begin
    insert into code_sequences (prefix, next_value)
    values (p_prefix, 1)
    on conflict (prefix) do update set next_value = code_sequences.next_value + 1
    returning next_value into v_value;
    return p_prefix || '-' || lpad(v_value::text, 4, '0');
end;
$$;

-- --------------------------------------------------------- app_settings -----
create table if not exists app_settings (
    key   text primary key,
    value text
);

-- ------------------------------------------------- audit helper function ----
create or replace function log_audit(p_username text, p_action text, p_module text, p_reference text, p_description text)
returns void
language sql
security definer
set search_path = public
as $$
    insert into audit_logs (username, action, module, reference, description)
    values (coalesce(p_username, 'system'), p_action, p_module, p_reference, p_description);
$$;

-- ------------------------------------------------------ reporting views -----
-- Fleet utilisation per vehicle (same aggregation as the Java ReportService).
create or replace view v_vehicle_utilisation as
select v.id,
       v.vehicle_code,
       v.registration_number,
       v.vehicle_type,
       v.make,
       v.model,
       v.year,
       v.status,
       v.current_mileage,
       coalesce(t.trip_count, 0)      as trip_count,
       coalesce(t.distance_km, 0)     as distance_km,
       coalesce(f.fuel_litres, 0)     as fuel_litres,
       coalesce(f.fuel_cost, 0)       as fuel_cost,
       coalesce(m.maintenance_cost,0) as maintenance_cost,
       coalesce(e.other_cost, 0)      as other_cost,
       coalesce(f.fuel_cost, 0) + coalesce(m.maintenance_cost, 0) + coalesce(e.other_cost, 0) as total_cost,
       case when coalesce(t.distance_km, 0) > 0
            then round((coalesce(f.fuel_cost, 0) + coalesce(m.maintenance_cost, 0) + coalesce(e.other_cost, 0)) / t.distance_km, 2)
            else null end as cost_per_km
from vehicles v
left join (
    select vehicle_id, count(*) as trip_count,
           sum(greatest(coalesce(end_mileage, 0) - coalesce(start_mileage, 0), 0)) as distance_km
    from trips where status = 'COMPLETED' group by vehicle_id
) t on t.vehicle_id = v.id
left join (
    select vehicle_id, sum(quantity_litres) as fuel_litres, sum(total_cost) as fuel_cost
    from fuel_records group by vehicle_id
) f on f.vehicle_id = v.id
left join (
    select vehicle_id, sum(cost) as maintenance_cost
    from maintenance_records where status <> 'CANCELLED' group by vehicle_id
) m on m.vehicle_id = v.id
left join (
    select vehicle_id, sum(amount) as other_cost
    from expenses
    where vehicle_id is not null and source_fuel_record_id is null and source_maintenance_record_id is null
    group by vehicle_id
) e on e.vehicle_id = v.id
where v.deleted_at is null;

-- Fuel efficiency: litres per 100 km between consecutive fill-ups of a vehicle.
create or replace view v_fuel_efficiency as
with ordered as (
    select f.vehicle_id,
           f.fuel_date,
           f.quantity_litres,
           f.total_cost,
           f.mileage,
           lag(f.mileage) over (partition by f.vehicle_id order by f.fuel_date, f.id) as prev_mileage
    from fuel_records f
)
select vehicle_id,
       count(*) as fill_ups,
       sum(quantity_litres) as litres,
       sum(total_cost) as cost,
       sum(case when mileage is not null and prev_mileage is not null and mileage > prev_mileage
                then mileage - prev_mileage else 0 end) as distance_km,
       case when sum(case when mileage is not null and prev_mileage is not null and mileage > prev_mileage
                           then mileage - prev_mileage else 0 end) > 0
            then round(100.0 * sum(quantity_litres) /
                 sum(case when mileage is not null and prev_mileage is not null and mileage > prev_mileage
                          then mileage - prev_mileage else 0 end), 2)
            else null end as litres_per_100km
from ordered
group by vehicle_id;

-- Maintenance that is due or overdue, evaluated against today's date / current odometer.
create or replace view v_maintenance_due as
with latest as (
    select distinct on (vehicle_id)
           vehicle_id, next_service_date, next_service_mileage, service_date
    from maintenance_records
    where next_service_date is not null or next_service_mileage is not null
    order by vehicle_id, service_date desc, id desc
)
select v.id as vehicle_id,
       v.registration_number,
       v.make || ' ' || v.model as vehicle,
       v.current_mileage,
       l.next_service_date,
       l.next_service_mileage,
       case
         when l.next_service_date is not null and l.next_service_date < current_date then 'OVERDUE'
         when l.next_service_mileage is not null and v.current_mileage >= l.next_service_mileage then 'OVERDUE'
         when l.next_service_date is not null and l.next_service_date <= current_date + 14 then 'DUE SOON'
         when l.next_service_mileage is not null and v.current_mileage >= l.next_service_mileage - 500 then 'DUE SOON'
         else 'OK'
       end as due_status
from vehicles v
join latest l on l.vehicle_id = v.id
where v.deleted_at is null and v.status not in ('RETIRED');

-- Licence expiry alerts.
create or replace view v_licence_alerts as
select d.id as driver_id,
       d.driver_code,
       d.licence_number,
       d.licence_category,
       d.licence_expiry_date,
       e.first_name || ' ' || e.last_name as driver_name,
       (d.licence_expiry_date - current_date) as days_remaining,
       case when d.licence_expiry_date < current_date then 'EXPIRED'
            when d.licence_expiry_date <= current_date + 30 then 'EXPIRING SOON'
            else 'VALID' end as licence_status
from drivers d
join employees e on e.id = d.employee_id
where d.deleted_at is null and d.status <> 'INACTIVE';

-- Monthly cost roll-up (fuel + maintenance + expenses) used by reports/charts.
create or replace view v_monthly_costs as
with months as (
    select to_char(date_trunc('month', d), 'YYYY-MM') as month
    from generate_series(current_date - interval '17 months', current_date, interval '1 month') d
)
select m.month,
       coalesce((select sum(total_cost) from fuel_records f where to_char(f.fuel_date, 'YYYY-MM') = m.month), 0) as fuel_cost,
       coalesce((select sum(cost) from maintenance_records r where to_char(r.service_date, 'YYYY-MM') = m.month and r.status <> 'CANCELLED'), 0) as maintenance_cost,
       coalesce((select sum(amount) from expenses e where to_char(e.expense_date, 'YYYY-MM') = m.month
                 and e.source_fuel_record_id is null and e.source_maintenance_record_id is null), 0) as other_cost,
       coalesce((select sum(amount) from expenses e where to_char(e.expense_date, 'YYYY-MM') = m.month), 0) as total_expenses
from months m
order by m.month;

-- Row-level security: no policies, therefore only the service role can read/write.
do $$
declare t text;
begin
    foreach t in array array['users','employees','drivers','vehicles','vehicle_assignments','trips',
                             'maintenance_records','fuel_records','expenses','audit_logs',
                             'code_sequences','app_settings']
    loop
        execute format('alter table %I enable row level security', t);
    end loop;
end;
$$;

-- Keep updated_at honest.
create or replace function touch_updated_at() returns trigger
language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

do $$
declare t text;
begin
    foreach t in array array['users','employees','drivers','vehicles','vehicle_assignments','trips',
                             'maintenance_records','fuel_records','expenses']
    loop
        execute format('drop trigger if exists trg_touch_%s on %I', t, t);
        execute format('create trigger trg_touch_%s before update on %I for each row execute function touch_updated_at()', t, t);
    end loop;
end;
$$;
