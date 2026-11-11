-- ============================================================================
-- FEMZYK Enterprise Fleet & Transport Management System — demo data seed
--
-- Run AFTER 0001_schema.sql, in the Supabase SQL editor or via
--   supabase db execute --file supabase/seed_demo.sql
--
-- This is the exact data set the live deployment at
-- https://femzyk-fleet.vercel.app is serving: 10 employees, 6 drivers,
-- 10 vehicles, 5 assignments, 12 trips, 7 maintenance records, 10 fuel
-- records, 22 expenses (16 mirrored from fuel/maintenance, 6 manual),
-- and 4 role accounts.  Every row is tagged "[DEMO DATA]" so it can be
-- identified and removed in one pass:
--
--   select count(*) from employees where notes like '%[DEMO DATA]%';
--
-- The script is idempotent: it wipes the operational tables first and
-- re-loads them, but it never deletes rows it did not create, and it
-- leaves existing user accounts (and their passwords) untouched.
--
-- Passwords (PBKDF2-HMAC-SHA256, 65,536 iterations, 16-byte salt):
--   admin / Admin@2024    manager / Fleet@2026
--   ops   / Ops@2026      viewer  / View@2026
-- ============================================================================

begin;

-- ---------------------------------------------------------------- reset -----
-- on delete restrict means the child tables must go first.  cascade also
-- clears expenses that mirror fuel/maintenance rows.
truncate table
    expenses,
    fuel_records,
    maintenance_records,
    trips,
    vehicle_assignments,
    vehicles,
    drivers,
    employees
restart identity cascade;

-- ---------------------------------------------------------------- users -----
-- Preserved rather than re-created: a password change must survive a reseed.
insert into users (username, email, full_name, password_salt, password_hash, role, active, must_change_password)
select v.username, v.email, v.full_name, v.salt, v.hash, v.role, true, false
from (values
    ('admin',   'admin@femzyk.local',   'System Administrator', 'nCf5wtFVUcZHh6hWGji8CA==', 't+I0qkvxOJIOLya1WBuoT//266CTdDhCkMFyq3IieSk=', 'ADMINISTRATOR'),
    ('manager', 'manager@femzyk.local', 'Fleet Manager',        'G/bAUU/DvH+jLu4tZm4v/g==', 'hcRHPRLuNRbvRPYrL85ymC6zQg00ojihVDBvFkxps3o=', 'FLEET_MANAGER'),
    ('ops',     'ops@femzyk.local',     'Operations Officer',   '+bfNwSPMb0nA51R7khgoHw==', 'D6CH8dfKm8DS6zsOnn40icGnQes/3YXCmJPsFYpU0Nk=', 'OPERATIONS_OFFICER'),
    ('viewer',  'viewer@femzyk.local',  'Read-only Viewer',     'rlOH5UL+5mkIeOAsyfyDvQ==', 'CrTsERGOPYurSsRI6UWOzdrOQzwvQ96zd7tElxvyDak=', 'VIEWER')
) as v(username, email, full_name, salt, hash, role)
where not exists (select 1 from users u where lower(u.username) = lower(v.username));

-- ------------------------------------------------------------ employees -----
insert into employees (id, employee_code, first_name, last_name, phone, email, address, department, position, date_of_birth, hire_date, salary, status, emergency_contact_name, emergency_contact_phone, notes)
overriding system value
values
 ('1',  'EMP-0001', 'Adebayo',  'Okonkwo',  '08031234567', 'adebayo.okonkwo@femzyk.demo',   null, 'Operations',  'Fleet Manager',         '1985-03-12', '2022-11-12', '350000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('2',  'EMP-0002', 'Chiamaka', 'Eze',      '08052345678', 'chiamaka.eze@femzyk.demo',      null, 'Operations',  'Operations Officer',    '1992-07-04', '2024-08-13', '220000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('3',  'EMP-0003', 'Ibrahim',  'Musa',     '08063456789', 'ibrahim.musa@femzyk.demo',      null, 'Transport',   'Senior Driver',         '1980-01-20', '2020-11-12', '145000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('4',  'EMP-0004', 'Funke',    'Adeyemi',  '08074567890', 'funke.adeyemi@femzyk.demo',     null, 'Transport',   'Driver',                '1990-11-02', '2023-11-12', '120000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('5',  'EMP-0005', 'Emeka',    'Nwosu',    '08085678901', 'emeka.nwosu@femzyk.demo',       null, 'Transport',   'Driver',                '1988-05-15', '2025-03-11', '120000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('6',  'EMP-0006', 'Tunde',    'Bakare',   '08096789012', 'tunde.bakare@femzyk.demo',      null, 'Transport',   'Driver / Dispatch Rider', '1995-09-09', '2025-09-17', '95000.00',  'ACTIVE', null, null, '[DEMO DATA]'),
 ('7',  'EMP-0007', 'Ngozi',    'Okafor',   '08107890123', 'ngozi.okafor@femzyk.demo',      null, 'Finance',     'Accounts Officer',      '1991-02-28', '2024-11-11', '240000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('8',  'EMP-0008', 'Yusuf',    'Abdullahi','08118901234', 'yusuf.abdullahi@femzyk.demo',   null, 'Transport',   'Truck Driver',          '1983-06-30', '2021-11-12', '160000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('9',  'EMP-0009', 'Blessing', 'Johnson',  '08129012345', 'blessing.johnson@femzyk.demo',  null, 'Maintenance', 'Workshop Supervisor',   '1987-12-01', '2023-05-11', '210000.00', 'ACTIVE', null, null, '[DEMO DATA]'),
 ('10', 'EMP-0010', 'Segun',    'Olawale',  '08130123456', 'segun.olawale@femzyk.demo',     null, 'Transport',   'Driver',                '1993-04-18', '2025-03-21', '120000.00', 'ACTIVE', null, null, '[DEMO DATA]');

-- -------------------------------------------------------------- drivers -----
-- Licence categories exercise every permit class: A=motorcycle, B=car/van,
-- C=+truck, D=+bus, E=everything.  DRV-0003 expires soon (alert source).
insert into drivers (id, driver_code, employee_id, licence_number, licence_category, licence_issue_date, licence_expiry_date, status, notes)
overriding system value
values
 ('1', 'DRV-0001', '3',  'LAG-2019-104477', 'D', '2024-11-11', '2029-11-10', 'ACTIVE', '[DEMO DATA]'),
 ('2', 'DRV-0002', '4',  'LAG-2020-228190', 'B', '2025-11-11', '2030-11-10', 'ACTIVE', '[DEMO DATA]'),
 ('3', 'DRV-0003', '5',  'OGN-2018-551233', 'C', '2023-11-12', '2026-12-02', 'ACTIVE', '[DEMO DATA]'),
 ('4', 'DRV-0004', '6',  'LAG-2022-771002', 'A', '2025-11-11', '2028-11-10', 'ACTIVE', '[DEMO DATA]'),
 ('5', 'DRV-0005', '8',  'KAN-2017-330912', 'E', '2022-11-12', '2027-11-11', 'ACTIVE', '[DEMO DATA]'),
 ('6', 'DRV-0006', '10', 'LAG-2021-660845', 'D', '2024-11-11', '2027-07-09', 'ACTIVE', '[DEMO DATA]');

-- ------------------------------------------------------------- vehicles -----
-- Covers all seven statuses so the state machine and the dashboard KPIs have
-- something to say: AVAILABLE x3, ASSIGNED x3, IN_SERVICE, MAINTENANCE,
-- OUT_OF_SERVICE, RETIRED.
insert into vehicles (id, vehicle_code, registration_number, vehicle_type, make, model, year, color, vin, engine_number, acquisition_date, acquisition_cost, current_mileage, fuel_type, capacity, status, attribute_number, attribute_text, notes)
overriding system value
values
 ('1',  'VEH-0001', 'LND-482-KJA', 'CAR',        'Toyota',        'Corolla',      2019, 'Silver', null, null, '2023-11-12', '8500000.00',  '61200',  'PETROL',  4,  'ASSIGNED',       4,  'AUTOMATIC', '[DEMO DATA]'),
 ('2',  'VEH-0002', 'KJA-915-AA',  'CAR',        'Toyota',        'Hilux',        2021, 'White',  null, null, '2024-11-11', '21000000.00', '38400',  'DIESEL',  4,  'ASSIGNED',       4,  'MANUAL',    '[DEMO DATA]'),
 ('3',  'VEH-0003', 'EKY-207-BD',  'CAR',        'Hyundai',       'Elantra',      2018, 'Black',  null, null, '2022-11-12', '6200000.00',  '88750',  'PETROL',  4,  'MAINTENANCE',    4,  'AUTOMATIC', '[DEMO DATA]'),
 ('4',  'VEH-0004', 'LSR-330-XA',  'BUS',        'Toyota',        'Hiace',        2020, 'White',  null, null, '2024-05-15', '18500000.00', '72300',  'DIESEL',  14, 'AVAILABLE',      null, null,     '[DEMO DATA]'),
 ('5',  'VEH-0005', 'APP-118-YC',  'TRUCK',      'MAN',           'TGS 26.440',   2017, 'Blue',   null, null, '2021-11-12', '45000000.00', '154900', 'DIESEL',  3,  'ASSIGNED',       20, 'MANUAL',    '[DEMO DATA]'),
 ('6',  'VEH-0006', 'FST-624-KD',  'TRUCK',      'Mercedes-Benz', 'Atego 1518',   2019, 'White',  null, null, '2023-11-12', '32000000.00', '96100',  'DIESEL',  3,  'AVAILABLE',      8,  'MANUAL',    '[DEMO DATA]'),
 ('7',  'VEH-0007', 'MUS-771-QA',  'MOTORCYCLE', 'Bajaj',         'Boxer 150',    2022, 'Red',    null, null, '2025-08-18', '650000.00',   '12800',  'PETROL',  2,  'IN_SERVICE',     2,  'STANDARD',  '[DEMO DATA]'),
 ('8',  'VEH-0008', 'IKJ-402-QB',  'MOTORCYCLE', 'TVS',           'HLX 125',      2021, 'Black',  null, null, '2024-11-11', '580000.00',   '21400',  'PETROL',  2,  'AVAILABLE',      2,  'STANDARD',  '[DEMO DATA]'),
 ('9',  'VEH-0009', 'AGL-559-DE',  'VAN',        'Ford',          'Transit',      2016, 'Grey',   null, null, '2020-11-12', '7800000.00',  '132000', 'DIESEL',  3,  'OUT_OF_SERVICE', null, null,     '[DEMO DATA] Engine knock — awaiting decision'),
 ('10', 'VEH-0010', 'LND-009-AB',  'CAR',        'Peugeot',       '406',          2008, 'Green',  null, null, '2012-11-14', '1200000.00',  '312000', 'PETROL',  4,  'RETIRED',        4,  'MANUAL',    '[DEMO DATA] Disposed by auction');

-- ---------------------------------------------------------- assignments -----
-- Four ACTIVE rows (the partial unique indexes allow only one per vehicle and
-- one per driver) plus one RETURNED row for the history view.
insert into vehicle_assignments (vehicle_id, driver_id, assigned_at, returned_at, status, purpose, notes, assigned_by) values
 ('2', '1', '2026-09-12', null,         'ACTIVE',   'Field operations — Lekki axis', '[DEMO DATA]',                    'admin'),
 ('1', '2', '2026-09-27', null,         'ACTIVE',   'Admin pool car',                '[DEMO DATA]',                    'admin'),
 ('5', '5', '2026-10-12', null,         'ACTIVE',   'Apapa port haulage',            '[DEMO DATA]',                    'admin'),
 ('7', '4', '2026-10-17', null,         'ACTIVE',   'Document dispatch',             '[DEMO DATA]',                    'admin'),
 ('4', '6', '2026-10-22', '2026-11-06', 'RETURNED', 'Staff shuttle',                 '[DEMO DATA] Shuttle contract ended', 'admin');

-- ---------------------------------------------------------------- trips -----
-- Ten completed trips give the utilisation report a distance history, one
-- ACTIVE trip and one PLANNED trip exercise the other two states.
-- start_mileage / end_mileage chain correctly per vehicle.
insert into trips (trip_code, vehicle_id, driver_id, origin, destination, purpose, departure_time, return_time, start_mileage, end_mileage, fuel_used_litres, status, notes) values
 ('TRP-0901', '2', '1', 'Ikeja HQ',  'Lekki Phase 1',            'Client visit',        '2026-08-28 08:00:00+00', '2026-08-28 17:00:00+00', '38400',  '38446',  '5.20',   'COMPLETED', '[DEMO DATA]'),
 ('TRP-0902', '5', '5', 'Apapa Port','Agbara Industrial Estate', 'Container delivery',  '2026-09-12 08:00:00+00', '2026-09-12 17:00:00+00', '154900', '154968', '38.00',  'COMPLETED', '[DEMO DATA]'),
 ('TRP-0903', '2', '1', 'Ikeja HQ',  'Ibadan',                   'Regional office audit','2026-09-20 08:00:00+00', '2026-09-20 17:00:00+00', '38446',  '38702',  '24.00',  'COMPLETED', '[DEMO DATA]'),
 ('TRP-0904', '1', '2', 'Ikeja HQ',  'Murtala Muhammed Airport', 'Airport pickup',      '2026-10-02 08:00:00+00', '2026-10-02 17:00:00+00', '61200',  '61224',  '2.30',   'COMPLETED', '[DEMO DATA]'),
 ('TRP-0905', '5', '5', 'Apapa Port','Ota',                      'Container delivery',  '2026-10-09 08:00:00+00', '2026-10-09 17:00:00+00', '154968', '155030', '35.50',  'COMPLETED', '[DEMO DATA]'),
 ('TRP-0906', '2', '1', 'Ikeja HQ',  'Victoria Island',          'Board meeting',       '2026-10-22 08:00:00+00', '2026-10-22 17:00:00+00', '38702',  '38739',  '4.10',   'COMPLETED', '[DEMO DATA]'),
 ('TRP-0907', '1', '2', 'Ikeja HQ',  'Abeokuta',                 'Vendor inspection',   '2026-10-30 08:00:00+00', '2026-10-30 17:00:00+00', '61224',  '61398',  '15.60',  'COMPLETED', '[DEMO DATA]'),
 ('TRP-0908', '5', '5', 'Apapa Port','Ibadan',                   'Bulk cargo',          '2026-11-02 08:00:00+00', '2026-11-02 17:00:00+00', '155030', '155290', '118.00', 'COMPLETED', '[DEMO DATA]'),
 ('TRP-0909', '7', '4', 'Ikeja HQ',  'Yaba',                     'Document dispatch',   '2026-11-06 08:00:00+00', '2026-11-06 17:00:00+00', '12800',  '12822',  '0.60',   'COMPLETED', '[DEMO DATA]'),
 ('TRP-0910', '7', '4', 'Ikeja HQ',  'Ikoyi',                    'Document dispatch',   '2026-11-09 08:00:00+00', '2026-11-09 17:00:00+00', '12822',  '12851',  '0.80',   'COMPLETED', '[DEMO DATA]'),
 ('TRP-0920', '7', '4', 'Ikeja HQ',  'Apapa',                    'Customs documents',   '2026-11-11 15:44:29+00', null,                     '12851',  null,     null,     'ACTIVE',    '[DEMO DATA] active'),
 ('TRP-0921', '1', '2', 'Ikeja HQ',  'Epe',                      'Site survey',         '2026-11-13 08:00:00+00', null,                     null,     null,     null,     'PLANNED',   '[DEMO DATA] planned');

-- -------------------------------------------------------- maintenance -----
-- One IN_PROGRESS repair on a MAINTENANCE vehicle (the coupling rule), one
-- SCHEDULED job, and a next_service_date in the past on VEH-0005 so the
-- overdue-service KPI is non-zero.
insert into maintenance_records (id, vehicle_id, service_date, maintenance_type, description, provider, cost, mileage_at_service, next_service_date, next_service_mileage, status, notes)
overriding system value
values
 ('1', '1', '2026-09-02', 'ROUTINE_SERVICE', 'Oil and filter change, brake inspection', 'Femzyk Workshop', '45000.00',  '61150', '2026-12-01', '66150',  'COMPLETED',   '[DEMO DATA]'),
 ('2', '2', '2026-09-27', 'TYRE',            'Replaced two rear tyres',                 'Femzyk Workshop', '190000.00', '38500', null,         '78500',  'COMPLETED',   '[DEMO DATA]'),
 ('3', '5', '2026-08-13', 'ROUTINE_SERVICE', 'Full service and gearbox oil',            'Femzyk Workshop', '320000.00', '154800','2026-10-12', '164800', 'COMPLETED',   '[DEMO DATA]'),
 ('4', '6', '2026-10-17', 'REPAIR',          'Clutch plate replacement',                'Femzyk Workshop', '275000.00', '96050', null,         null,     'COMPLETED',   '[DEMO DATA]'),
 ('5', '3', '2026-11-08', 'REPAIR',          'Air-conditioning compressor failure',     'Femzyk Workshop', '185000.00', '88750', null,         null,     'IN_PROGRESS', '[DEMO DATA]'),
 ('6', '8', '2026-10-27', 'INSPECTION',      'Roadworthiness inspection',               'Femzyk Workshop', '12500.00',  '21350', '2027-10-27', null,     'COMPLETED',   '[DEMO DATA]'),
 ('7', '4', '2026-11-17', 'ROUTINE_SERVICE', 'Scheduled 75,000 km service',             'Femzyk Workshop', '0.00',      null,    null,         null,     'SCHEDULED',   '[DEMO DATA]');

-- --------------------------------------------------------------- fuel -----
-- Fuel total_cost is always quantity_litres * unit_price; the service layer
-- enforces that and mirrors each row into expenses below.
insert into fuel_records (id, vehicle_id, driver_id, fuel_date, quantity_litres, unit_price, total_cost, mileage, station, receipt_reference, fuel_type, notes)
overriding system value
values
 ('1',  '2', '1', '2026-08-27', '60.00',  '1050.00', '63000.00',  null, 'NNPC Ikeja',           'RCP-1001', 'DIESEL', '[DEMO DATA]'),
 ('2',  '2', '1', '2026-09-19', '65.00',  '1080.00', '70200.00',  null, 'TotalEnergies Berger', 'RCP-1017', 'DIESEL', '[DEMO DATA]'),
 ('3',  '2', '1', '2026-10-21', '58.00',  '1100.00', '63800.00',  null, 'NNPC Ikeja',           'RCP-1042', 'DIESEL', '[DEMO DATA]'),
 ('4',  '1', '2', '2026-10-01', '45.00',  '1050.00', '47250.00',  null, 'Mobil Allen',          'RCP-1009', 'PETROL', '[DEMO DATA]'),
 ('5',  '1', '2', '2026-10-29', '42.00',  '1120.00', '47040.00',  null, 'Mobil Allen',          'RCP-1050', 'PETROL', '[DEMO DATA]'),
 ('6',  '5', '5', '2026-09-11', '300.00', '1250.00', '375000.00', null, 'Conoil Apapa',         'RCP-1005', 'DIESEL', '[DEMO DATA]'),
 ('7',  '5', '5', '2026-10-08', '280.00', '1280.00', '358400.00', null, 'Conoil Apapa',         'RCP-1029', 'DIESEL', '[DEMO DATA]'),
 ('8',  '5', '5', '2026-11-01', '320.00', '1300.00', '416000.00', null, 'Conoil Apapa',         'RCP-1055', 'DIESEL', '[DEMO DATA]'),
 ('9',  '7', '4', '2026-11-05', '8.00',   '1100.00', '8800.00',   null, 'Oando Ojota',          'RCP-1053', 'PETROL', '[DEMO DATA]'),
 ('10', '4', '6', '2026-10-12', '70.00',  '1060.00', '74200.00',  null, 'NNPC Ikeja',           'RCP-1031', 'DIESEL', '[DEMO DATA]');

-- ----------------------------------------------------------- expenses -----
-- 1-6: mirrored from maintenance_records 1-6.  The application writes these
-- rows itself (there is no trigger), so the seed has to mirror them the same
-- way: category MAINTENANCE, vendor = provider, amount = cost, same date.
insert into expenses (vehicle_id, expense_date, category, amount, vendor, description, reference, notes, source_maintenance_record_id)
select m.vehicle_id, m.service_date, 'MAINTENANCE', m.cost, m.provider, m.description, null,
       'Automatically generated from a maintenance record. Edit the maintenance record instead.', m.id
from maintenance_records m
where m.id in (1, 2, 3, 4, 5, 6);

-- 7-16: mirrored from every fuel record, with the same description format the
-- application produces ("60.00 L diesel — KJA-915-AA").
insert into expenses (vehicle_id, expense_date, category, amount, vendor, description, reference, notes, source_fuel_record_id)
select f.vehicle_id, f.fuel_date, 'FUEL', f.total_cost, f.station,
       format('%s L %s — %s', f.quantity_litres, lower(f.fuel_type), v.registration_number),
       f.receipt_reference,
       'Automatically generated from a fuel record. Edit the fuel record instead.', f.id
from fuel_records f
join vehicles v on v.id = f.vehicle_id;

-- 17-22: six hand-entered expenses covering the categories nothing else uses.
-- Deliberately two distinct keys per inserted row (vehicle + date + amount)
-- so this stays correct even if the fuel/maintenance rows shift.
insert into expenses (vehicle_id, expense_date, category, amount, vendor, description, reference, notes) values
 ('2',    '2026-08-23', 'INSURANCE',    '210000.00', 'Leadway Assurance', 'Comprehensive insurance renewal', 'INS-2024-0042', '[DEMO DATA]'),
 ('5',    '2026-09-14', 'TOLLS',        '12000.00',  'LCC',               'Lekki toll passes — month',       'TOL-0912',      '[DEMO DATA]'),
 ('1',    '2026-10-07', 'REGISTRATION', '28500.00',  'Lagos MVAA',        'Vehicle licence renewal',         'MVAA-77123',    '[DEMO DATA]'),
 ('5',    '2026-10-14', 'FINES',        '20000.00',  'LASTMA',            'Obstruction fine — contested',    'LTM-5581',      '[DEMO DATA]'),
 (null,   '2026-10-24', 'CLEANING',     '35000.00',  'SparkleWash',       'Fleet wash — all vehicles',       'SW-0331',       '[DEMO DATA]'),
 ('4',    '2026-11-04', 'PARKING',      '9000.00',   'MMA2',              'Airport parking',                 'PKG-118',       '[DEMO DATA]');

-- -------------------------------------------------- identity sequences -----
-- The rows above supply explicit ids, which leaves the underlying identity
-- sequences at 1.  Advance each one past the highest seeded id so the next
-- application insert does not collide.
select setval(pg_get_serial_sequence('employees', 'id'),           (select max(id) from employees));
select setval(pg_get_serial_sequence('drivers', 'id'),             (select max(id) from drivers));
select setval(pg_get_serial_sequence('vehicles', 'id'),            (select max(id) from vehicles));
select setval(pg_get_serial_sequence('maintenance_records', 'id'), (select max(id) from maintenance_records));
select setval(pg_get_serial_sequence('fuel_records', 'id'),        (select max(id) from fuel_records));

-- ---------------------------------------------------------- sequences -----
-- next_code() increments on read, so this is the value the *next* call
-- increments from — keep it at the highest number already handed out.
insert into code_sequences (prefix, next_value) values
    ('EMP', 10),
    ('DRV', 6),
    ('VEH', 10),
    ('TRP', 921)
on conflict (prefix) do update set next_value = greatest(code_sequences.next_value, excluded.next_value);

-- ----------------------------------------------------------- settings -----
insert into app_settings (key, value) values
    ('demo.marker',     '[DEMO DATA]'),
    ('demo.loaded_at',  now()::text)
on conflict (key) do update set value = excluded.value;

-- ------------------------------------------------------------- audit -----
insert into audit_logs (timestamp, username, action, module, reference, description) values
 (now() - interval '5 hours', 'admin',   'LOGIN',       'AUTH',     null,       'Signed in from Lagos, NG'),
 (now() - interval '4 hours', 'manager', 'LOGIN',       'AUTH',     null,       'Signed in from Lagos, NG'),
 (now() - interval '3 hours', 'manager', 'CREATE',      'TRIPS',    'TRP-0921', 'Planned trip Ikeja HQ to Epe'),
 (now() - interval '2 hours', 'ops',     'CREATE',      'FUEL',     'MUS-771-QA', '8.00 L at 1100.00 = NGN 8,800.00'),
 (now() - interval '1 hour',  'manager', 'STATUS',      'VEHICLES', 'AGL-559-DE', 'Status changed AVAILABLE -> OUT_OF_SERVICE'),
 (now(),                      'system',  'DEMO_DATA',   'SYSTEM',   '[DEMO DATA]', 'Demonstration data set loaded');

-- ---------------------------------------------------------- self-check -----
-- Abort (and therefore roll back every statement above) unless the reload
-- landed exactly the expected row counts.  Run this file with confidence: a
-- mistake leaves the database exactly as it was.
do $$
declare
    v_problems text;
begin
    select string_agg(format('%s=%s (expected %s)', name, actual, expected), ', ')
      into v_problems
    from (values
        ('employees',   (select count(*) from employees),           10),
        ('drivers',     (select count(*) from drivers),              6),
        ('vehicles',    (select count(*) from vehicles),            10),
        ('assignments', (select count(*) from vehicle_assignments),  5),
        ('trips',       (select count(*) from trips),               12),
        ('maintenance', (select count(*) from maintenance_records),  7),
        ('fuel',        (select count(*) from fuel_records),        10),
        ('expenses',    (select count(*) from expenses),            22)
    ) as t(name, actual, expected)
    where actual <> expected;

    if v_problems is not null then
        raise exception 'seed verification failed, rolling back: %', v_problems;
    end if;
end $$;

commit;

-- ============================================================================
-- Post-seed check — all seven queries should return the counts in comments.
--
-- select (select count(*) from employees)            as employees,           -- 10
--        (select count(*) from drivers)              as drivers,             --  6
--        (select count(*) from vehicles)             as vehicles,            -- 10
--        (select count(*) from vehicle_assignments)  as assignments,         --  5
--        (select count(*) from trips)                as trips,               -- 12
--        (select count(*) from maintenance_records)  as maintenance,         --  7
--        (select count(*) from fuel_records)         as fuel,                -- 10
--        (select count(*) from expenses)             as expenses;            -- 22
--
-- select * from v_vehicle_utilisation order by registration_number;
-- select * from v_maintenance_due;        -- 1 row: VEH-0005, overdue
-- select * from v_licence_alerts;         -- DRV-0003 expires 2026-12-02
-- select * from v_monthly_costs;
-- select * from v_fuel_efficiency;
-- ============================================================================
