import 'server-only';
import { db } from '../db';
import type { SessionUser } from '../session';
import { createEmployee } from './employees';
import { createDriver } from './drivers';
import { createVehicle } from './vehicles';
import { assignVehicle, returnVehicle } from './assignments';
import { createTrip, startTrip, completeTrip } from './trips';
import { createMaintenance, createFuel, createExpense } from './operations';
import { changeVehicleStatus } from './vehicles';
import { addDays, todayIso } from './common';

/**
 * Demo data set, mirroring DemoDataService from the Java edition: ten Lagos-area
 * employees, six drivers, nine vehicles (car, bus, truck, motorcycle, van),
 * assignments, three months of trips, fuel, maintenance and expenses.
 *
 * All records are created through the normal service functions so business
 * rules, code sequences and audit entries behave exactly as they would for a
 * real user. Figures are plausible demonstration values, not real company data.
 */

const MARKER = '[DEMO DATA]';

const demoAdmin: SessionUser = {
  id: 0,
  username: 'system',
  fullName: 'Demo Loader',
  email: 'system@femzyk.local',
  role: 'ADMINISTRATOR',
  mustChangePassword: false,
};

type Employee = Awaited<ReturnType<typeof createEmployee>>;
type Driver = Awaited<ReturnType<typeof createDriver>>;
type Vehicle = Awaited<ReturnType<typeof createVehicle>>;

export async function seedDemoData() {
  const today = todayIso();
  const d = (offset: number) => addDays(today, offset);
  const at = (offsetDays: number, hour = 8) => {
    const dt = new Date(`${d(offsetDays)}T00:00:00Z`);
    dt.setUTCHours(hour, 0, 0, 0);
    return dt.toISOString();
  };

  const created: Record<string, number> = { employees: 0, drivers: 0, vehicles: 0, assignments: 0, trips: 0, maintenance: 0, fuel: 0, expenses: 0 };

  /* ------------------------------------------------------------- employees */
  const employeeSpecs = [
    ['Adebayo', 'Okonkwo', '08031234567', 'adebayo.okonkwo@femzyk.demo', 'Operations', 'Fleet Manager', '1985-03-12', d(-1460), 350000],
    ['Chiamaka', 'Eze', '08052345678', 'chiamaka.eze@femzyk.demo', 'Operations', 'Operations Officer', '1992-07-04', d(-820), 220000],
    ['Ibrahim', 'Musa', '08063456789', 'ibrahim.musa@femzyk.demo', 'Transport', 'Senior Driver', '1980-01-20', d(-2190), 145000],
    ['Funke', 'Adeyemi', '08074567890', 'funke.adeyemi@femzyk.demo', 'Transport', 'Driver', '1990-11-02', d(-1095), 120000],
    ['Emeka', 'Nwosu', '08085678901', 'emeka.nwosu@femzyk.demo', 'Transport', 'Driver', '1988-05-15', d(-610), 120000],
    ['Tunde', 'Bakare', '08096789012', 'tunde.bakare@femzyk.demo', 'Transport', 'Driver / Dispatch Rider', '1995-09-09', d(-420), 95000],
    ['Ngozi', 'Okafor', '08107890123', 'ngozi.okafor@femzyk.demo', 'Finance', 'Accounts Officer', '1991-02-28', d(-730), 240000],
    ['Yusuf', 'Abdullahi', '08118901234', 'yusuf.abdullahi@femzyk.demo', 'Transport', 'Truck Driver', '1983-06-30', d(-1825), 160000],
    ['Blessing', 'Johnson', '08129012345', 'blessing.johnson@femzyk.demo', 'Maintenance', 'Workshop Supervisor', '1987-12-01', d(-1280), 210000],
    ['Segun', 'Olawale', '08130123456', 'segun.olawale@femzyk.demo', 'Transport', 'Driver', '1993-04-18', d(-600), 120000],
  ] as const;

  const employees: Employee[] = [];
  for (const [first, last, phone, email, department, position, dob, hire, salary] of employeeSpecs) {
    employees.push(
      await createEmployee(demoAdmin, {
        first_name: first, last_name: last, phone, email, department, position,
        date_of_birth: dob, hire_date: hire, salary, status: 'ACTIVE', notes: MARKER,
      }),
    );
    created.employees++;
  }

  /* --------------------------------------------------------------- drivers */
  const driverSpecs: { emp: number; licence: string; cat: 'A' | 'B' | 'C' | 'D' | 'E'; issued: string; expiry: string }[] = [
    { emp: 2, licence: 'LAG-2019-104477', cat: 'D', issued: d(-730), expiry: d(1095) },
    { emp: 3, licence: 'LAG-2020-228190', cat: 'B', issued: d(-365), expiry: d(1460) },
    { emp: 4, licence: 'OGN-2018-551233', cat: 'C', issued: d(-1095), expiry: d(21) },   // expiring soon
    { emp: 5, licence: 'LAG-2022-771002', cat: 'A', issued: d(-365), expiry: d(730) },
    { emp: 7, licence: 'KAN-2017-330912', cat: 'E', issued: d(-1460), expiry: d(365) },
    { emp: 9, licence: 'LAG-2021-660845', cat: 'D', issued: d(-730), expiry: d(240) },
  ];
  const drivers: Driver[] = [];
  for (const spec of driverSpecs) {
    drivers.push(
      await createDriver(demoAdmin, {
        employee_id: employees[spec.emp].id,
        licence_number: spec.licence,
        licence_category: spec.cat,
        licence_issue_date: spec.issued,
        licence_expiry_date: spec.expiry,
        status: 'ACTIVE',
        notes: MARKER,
      }),
    );
    created.drivers++;
  }

  /* -------------------------------------------------------------- vehicles */
  const vehicleSpecs: {
    reg: string; type: 'CAR' | 'MOTORCYCLE' | 'TRUCK' | 'VAN' | 'BUS'; make: string; model: string; year: number;
    color: string; capacity: number; mileage: number; cost: number; acquired: string; fuel: 'PETROL' | 'DIESEL';
    attributeNumber: number | null; attributeText: string | null;
  }[] = [
    { reg: 'LND-482-KJA', type: 'CAR', make: 'Toyota', model: 'Corolla', year: 2019, color: 'Silver', capacity: 4, mileage: 61200, cost: 8500000, acquired: d(-1095), fuel: 'PETROL', attributeNumber: 4, attributeText: 'AUTOMATIC' },
    { reg: 'KJA-915-AA', type: 'CAR', make: 'Toyota', model: 'Hilux', year: 2021, color: 'White', capacity: 4, mileage: 38400, cost: 21000000, acquired: d(-730), fuel: 'DIESEL', attributeNumber: 4, attributeText: 'MANUAL' },
    { reg: 'EKY-207-BD', type: 'CAR', make: 'Hyundai', model: 'Elantra', year: 2018, color: 'Black', capacity: 4, mileage: 88750, cost: 6200000, acquired: d(-1460), fuel: 'PETROL', attributeNumber: 4, attributeText: 'AUTOMATIC' },
    { reg: 'LSR-330-XA', type: 'BUS', make: 'Toyota', model: 'Hiace', year: 2020, color: 'White', capacity: 14, mileage: 72300, cost: 18500000, acquired: d(-910), fuel: 'DIESEL', attributeNumber: null, attributeText: null },
    { reg: 'APP-118-YC', type: 'TRUCK', make: 'MAN', model: 'TGS 26.440', year: 2017, color: 'Blue', capacity: 3, mileage: 154900, cost: 45000000, acquired: d(-1825), fuel: 'DIESEL', attributeNumber: 20, attributeText: 'MANUAL' },
    { reg: 'FST-624-KD', type: 'TRUCK', make: 'Mercedes-Benz', model: 'Atego 1518', year: 2019, color: 'White', capacity: 3, mileage: 96100, cost: 32000000, acquired: d(-1095), fuel: 'DIESEL', attributeNumber: 8, attributeText: 'MANUAL' },
    { reg: 'MUS-771-QA', type: 'MOTORCYCLE', make: 'Bajaj', model: 'Boxer 150', year: 2022, color: 'Red', capacity: 2, mileage: 12800, cost: 650000, acquired: d(-450), fuel: 'PETROL', attributeNumber: 2, attributeText: 'STANDARD' },
    { reg: 'IKJ-402-QB', type: 'MOTORCYCLE', make: 'TVS', model: 'HLX 125', year: 2021, color: 'Black', capacity: 2, mileage: 21400, cost: 580000, acquired: d(-730), fuel: 'PETROL', attributeNumber: 2, attributeText: 'STANDARD' },
    { reg: 'AGL-559-DE', type: 'VAN', make: 'Ford', model: 'Transit', year: 2016, color: 'Grey', capacity: 3, mileage: 132000, cost: 7800000, acquired: d(-2190), fuel: 'DIESEL', attributeNumber: null, attributeText: null },
  ];

  const vehicles: Vehicle[] = [];
  for (const v of vehicleSpecs) {
    vehicles.push(
      await createVehicle(demoAdmin, {
        registration_number: v.reg, vehicle_type: v.type, make: v.make, model: v.model, year: v.year,
        color: v.color, capacity: v.capacity, current_mileage: v.mileage, acquisition_cost: v.cost,
        acquisition_date: v.acquired, fuel_type: v.fuel, status: 'AVAILABLE',
        attribute_number: v.attributeNumber, attribute_text: v.attributeText, notes: MARKER,
      }),
    );
    created.vehicles++;
  }

  /* ----------------------------------------------------------- assignments */
  const assignments: [number, number, string][] = [
    [1, 0, 'Field operations — Lekki axis'],
    [0, 1, 'Admin pool car'],
    [4, 4, 'Apapa port haulage'],
    [6, 3, 'Document dispatch'],
    [3, 5, 'Staff shuttle'],
  ];
  for (const [vehicleIdx, driverIdx, purpose] of assignments) {
    const a = await assignVehicle(demoAdmin, { vehicle_id: vehicles[vehicleIdx].id, driver_id: drivers[driverIdx].id, purpose, notes: MARKER });
    created.assignments++;
    if (vehicleIdx === 3) await returnVehicle(demoAdmin, a.id, `${MARKER} Shuttle contract ended`);
  }

  /* ------------------------------------------------------------ trips */
  const completedTrips: [number, number, string, string, string, number, number, number, number][] = [
    [1, 0, 'Ikeja HQ', 'Lekki Phase 1', 'Client visit', -75, 38400, 38446, 5.2],
    [1, 0, 'Ikeja HQ', 'Ibadan', 'Regional office audit', -52, 38446, 38702, 24.0],
    [1, 0, 'Ikeja HQ', 'Victoria Island', 'Board meeting', -20, 38702, 38739, 4.1],
    [0, 1, 'Ikeja HQ', 'Murtala Muhammed Airport', 'Airport pickup', -40, 61200, 61224, 2.3],
    [0, 1, 'Ikeja HQ', 'Abeokuta', 'Vendor inspection', -12, 61224, 61398, 15.6],
    [4, 4, 'Apapa Port', 'Agbara Industrial Estate', 'Container delivery', -60, 154900, 154968, 38.0],
    [4, 4, 'Apapa Port', 'Ota', 'Container delivery', -33, 154968, 155030, 35.5],
    [4, 4, 'Apapa Port', 'Ibadan', 'Bulk cargo', -9, 155030, 155290, 118.0],
    [6, 3, 'Ikeja HQ', 'Yaba', 'Document dispatch', -5, 12800, 12822, 0.6],
    [6, 3, 'Ikeja HQ', 'Ikoyi', 'Document dispatch', -2, 12822, 12851, 0.8],
  ];
  for (const [vIdx, dIdx, origin, destination, purpose, offset, startKm, endKm, litres] of completedTrips) {
    const trip = await createTrip(demoAdmin, {
      vehicle_id: vehicles[vIdx].id, driver_id: drivers[dIdx].id, origin, destination, purpose,
      departure_time: at(offset), start_mileage: startKm, notes: MARKER,
    }, false);
    await startTrip(demoAdmin, trip.id);
    await completeTrip(demoAdmin, trip.id, { end_mileage: endKm, fuel_used_litres: litres, return_time: at(offset, 17), notes: MARKER });
    created.trips++;
  }
  // One active trip and one planned trip
  const activeTrip = await createTrip(demoAdmin, {
    vehicle_id: vehicles[6].id, driver_id: drivers[3].id, origin: 'Ikeja HQ', destination: 'Apapa',
    purpose: 'Customs documents', departure_time: new Date(Date.now() - 2 * 3600_000).toISOString(),
    start_mileage: 12851, notes: MARKER,
  }, false);
  await startTrip(demoAdmin, activeTrip.id);
  created.trips++;
  await createTrip(demoAdmin, {
    vehicle_id: vehicles[0].id, driver_id: drivers[1].id, origin: 'Ikeja HQ', destination: 'Epe',
    purpose: 'Site survey', departure_time: at(2, 8), notes: MARKER,
  }, false);
  created.trips++;

  /* --------------------------------------------------------- maintenance */
  const maintenanceSpecs: [number, number, string, string, number, number | null, string | null, number | null, 'COMPLETED' | 'IN_PROGRESS' | 'SCHEDULED'][] = [
    [0, -70, 'ROUTINE_SERVICE', 'Oil and filter change, brake inspection', 45000, 61150, d(20), 66150, 'COMPLETED'],
    [1, -45, 'TYRE', 'Replaced two rear tyres', 190000, 38500, null, 78500, 'COMPLETED'],
    [4, -90, 'ROUTINE_SERVICE', 'Full service and gearbox oil', 320000, 154800, d(-30), 164800, 'COMPLETED'],
    [5, -25, 'REPAIR', 'Clutch plate replacement', 275000, 96050, null, null, 'COMPLETED'],
    [2, -3, 'REPAIR', 'Air-conditioning compressor failure', 185000, 88750, null, null, 'IN_PROGRESS'],
    [7, -15, 'INSPECTION', 'Roadworthiness inspection', 12500, 21350, d(350), null, 'COMPLETED'],
    [3, 6, 'ROUTINE_SERVICE', 'Scheduled 75,000 km service', 0, null, null, null, 'SCHEDULED'],
  ];
  for (const [vIdx, offset, type, description, cost, odo, nextDate, nextOdo, status] of maintenanceSpecs) {
    await createMaintenance(demoAdmin, {
      vehicle_id: vehicles[vIdx].id, service_date: d(offset), maintenance_type: type, description,
      provider: 'Femzyk Workshop', cost, mileage_at_service: odo, next_service_date: nextDate,
      next_service_mileage: nextOdo, status, notes: MARKER,
    });
    created.maintenance++;
  }

  /* ---------------------------------------------------------------- fuel */
  const fuelSpecs: [number, number, number, number, number, string, string][] = [
    [1, 0, -76, 60, 1050, 'NNPC Ikeja', 'RCP-1001'],
    [1, 0, -53, 65, 1080, 'TotalEnergies Berger', 'RCP-1017'],
    [1, 0, -21, 58, 1100, 'NNPC Ikeja', 'RCP-1042'],
    [0, 1, -41, 45, 1050, 'Mobil Allen', 'RCP-1009'],
    [0, 1, -13, 42, 1120, 'Mobil Allen', 'RCP-1050'],
    [4, 4, -61, 300, 1250, 'Conoil Apapa', 'RCP-1005'],
    [4, 4, -34, 280, 1280, 'Conoil Apapa', 'RCP-1029'],
    [4, 4, -10, 320, 1300, 'Conoil Apapa', 'RCP-1055'],
    [6, 3, -6, 8, 1100, 'Oando Ojota', 'RCP-1053'],
    [3, 5, -30, 70, 1060, 'NNPC Ikeja', 'RCP-1031'],
  ];
  for (const [vIdx, dIdx, offset, litres, price, station, receipt] of fuelSpecs) {
    await createFuel(demoAdmin, {
      vehicle_id: vehicles[vIdx].id, driver_id: drivers[dIdx].id, fuel_date: d(offset),
      quantity_litres: litres, unit_price: price, station, receipt_reference: receipt, notes: MARKER,
    });
    created.fuel++;
  }

  /* ------------------------------------------------------------ expenses */
  const expenseSpecs: [number | null, number, string, number, string, string, string][] = [
    [1, -80, 'INSURANCE', 210000, 'Leadway Assurance', 'Comprehensive insurance renewal', 'INS-2024-0042'],
    [4, -58, 'TOLLS', 12000, 'LCC', 'Lekki toll passes — month', 'TOL-0912'],
    [0, -35, 'REGISTRATION', 28500, 'Lagos MVAA', 'Vehicle licence renewal', 'MVAA-77123'],
    [4, -28, 'FINES', 20000, 'LASTMA', 'Obstruction fine — contested', 'LTM-5581'],
    [null, -18, 'CLEANING', 35000, 'SparkleWash', 'Fleet wash — all vehicles', 'SW-0331'],
    [3, -7, 'PARKING', 9000, 'MMA2', 'Airport parking', 'PKG-118'],
  ];
  for (const [vIdx, offset, category, amount, vendor, description, reference] of expenseSpecs) {
    await createExpense(demoAdmin, {
      vehicle_id: vIdx === null ? null : vehicles[vIdx].id, expense_date: d(offset), category,
      amount, vendor, description, reference, notes: MARKER,
    });
    created.expenses++;
  }

  /* --------------------------------------------- retired / out-of-service */
  await changeVehicleStatus(demoAdmin, vehicles[8].id, 'OUT_OF_SERVICE', `${MARKER} Engine knock — awaiting decision`);
  const old = await createVehicle(demoAdmin, {
    registration_number: 'LND-009-AB', vehicle_type: 'CAR', make: 'Peugeot', model: '406', year: 2008,
    color: 'Green', capacity: 4, current_mileage: 312000, acquisition_cost: 1200000, acquisition_date: d(-5110),
    fuel_type: 'PETROL', status: 'AVAILABLE', attribute_number: 4, attribute_text: 'MANUAL', notes: MARKER,
  });
  created.vehicles++;
  await changeVehicleStatus(demoAdmin, old.id, 'RETIRED', `${MARKER} Disposed by auction`);

  // Small audit trail flourish
  await db().from('app_settings').upsert({ key: 'demo.marker', value: MARKER });

  return created;
}
