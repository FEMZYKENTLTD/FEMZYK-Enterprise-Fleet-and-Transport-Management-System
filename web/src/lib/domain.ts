/**
 * Domain constants — mirrored 1:1 from the Java desktop application
 * (com.femzyk.fleetmanagement.model), so both editions enforce the same rules.
 */

export const ROLES = [
  { value: 'ADMINISTRATOR', label: 'Administrator' },
  { value: 'FLEET_MANAGER', label: 'Fleet Manager' },
  { value: 'OPERATIONS_OFFICER', label: 'Operations Officer' },
  { value: 'VIEWER', label: 'Viewer' },
] as const;
export type Role = (typeof ROLES)[number]['value'];

export const PERMISSIONS = [
  'VIEW_DASHBOARD',
  'VIEW_RECORDS',
  'MANAGE_EMPLOYEES',
  'MANAGE_DRIVERS',
  'MANAGE_VEHICLES',
  'MANAGE_ASSIGNMENTS',
  'MANAGE_TRIPS',
  'MANAGE_MAINTENANCE',
  'MANAGE_FUEL',
  'MANAGE_EXPENSES',
  'VIEW_REPORTS',
  'EXPORT_DATA',
  'IMPORT_DATA',
  'MANAGE_USERS',
  'VIEW_AUDIT_LOG',
  'MANAGE_BACKUPS',
  'LOAD_DEMO_DATA',
] as const;
export type Permission = (typeof PERMISSIONS)[number];

/** Same matrix as com.femzyk.fleetmanagement.model.Permission.forRole(). */
export const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  ADMINISTRATOR: [...PERMISSIONS],
  FLEET_MANAGER: [
    'VIEW_DASHBOARD', 'VIEW_RECORDS', 'MANAGE_EMPLOYEES', 'MANAGE_DRIVERS', 'MANAGE_VEHICLES',
    'MANAGE_ASSIGNMENTS', 'MANAGE_TRIPS', 'MANAGE_MAINTENANCE', 'MANAGE_FUEL', 'MANAGE_EXPENSES',
    'VIEW_REPORTS', 'EXPORT_DATA', 'IMPORT_DATA',
  ],
  OPERATIONS_OFFICER: [
    'VIEW_DASHBOARD', 'VIEW_RECORDS', 'MANAGE_TRIPS', 'MANAGE_MAINTENANCE', 'MANAGE_FUEL',
    'MANAGE_EXPENSES', 'VIEW_REPORTS', 'EXPORT_DATA',
  ],
  VIEWER: ['VIEW_DASHBOARD', 'VIEW_RECORDS', 'VIEW_REPORTS', 'EXPORT_DATA'],
};

export function roleHas(role: Role, permission: Permission): boolean {
  return ROLE_PERMISSIONS[role]?.includes(permission) ?? false;
}

export const VEHICLE_TYPES = ['CAR', 'MOTORCYCLE', 'TRUCK', 'VAN', 'BUS'] as const;
export type VehicleType = (typeof VEHICLE_TYPES)[number];

export const VEHICLE_STATUSES = ['AVAILABLE', 'ASSIGNED', 'IN_SERVICE', 'MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED'] as const;
export type VehicleStatus = (typeof VEHICLE_STATUSES)[number];

/** State machine copied from VehicleStatus.allowedTransitions(). */
export const VEHICLE_TRANSITIONS: Record<VehicleStatus, VehicleStatus[]> = {
  AVAILABLE: ['ASSIGNED', 'IN_SERVICE', 'MAINTENANCE', 'OUT_OF_SERVICE', 'RETIRED'],
  ASSIGNED: ['AVAILABLE', 'IN_SERVICE', 'MAINTENANCE', 'OUT_OF_SERVICE'],
  IN_SERVICE: ['ASSIGNED', 'AVAILABLE', 'MAINTENANCE', 'OUT_OF_SERVICE'],
  MAINTENANCE: ['AVAILABLE', 'ASSIGNED', 'OUT_OF_SERVICE', 'RETIRED'],
  OUT_OF_SERVICE: ['AVAILABLE', 'MAINTENANCE', 'RETIRED'],
  RETIRED: [],
};

export const FUEL_TYPES = ['PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG'] as const;
export type FuelType = (typeof FUEL_TYPES)[number];

export const EMPLOYEE_STATUSES = ['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'] as const;
export const DRIVER_STATUSES = ['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'INACTIVE'] as const;
export const LICENCE_CATEGORIES = ['A', 'B', 'C', 'D', 'E'] as const;
export type LicenceCategory = (typeof LICENCE_CATEGORIES)[number];

export const LICENCE_DESCRIPTIONS: Record<LicenceCategory, string> = {
  A: 'Motorcycle',
  B: 'Car / light vehicle',
  C: 'Light truck / van',
  D: 'Heavy truck / bus',
  E: 'Articulated / heavy duty',
};

const LICENCE_ORDER: Record<LicenceCategory, number> = { A: 0, B: 1, C: 2, D: 3, E: 4 };

/** Same rule as LicenceCategory.permits(VehicleType) in Java. */
export function licencePermits(category: LicenceCategory, type: VehicleType): boolean {
  const rank = LICENCE_ORDER[category];
  switch (type) {
    case 'MOTORCYCLE': return category === 'A' || rank >= LICENCE_ORDER.C;
    case 'CAR': return rank >= LICENCE_ORDER.B;
    case 'TRUCK': return rank >= LICENCE_ORDER.C;
    case 'BUS': return rank >= LICENCE_ORDER.D;
    case 'VAN': return rank >= LICENCE_ORDER.B;
    default: return rank >= LICENCE_ORDER.B;
  }
}

export const TRIP_STATUSES = ['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as const;
export type TripStatus = (typeof TRIP_STATUSES)[number];

export const MAINTENANCE_TYPES = ['ROUTINE_SERVICE', 'REPAIR', 'INSPECTION', 'TYRE', 'BODYWORK', 'ELECTRICAL', 'OTHER'] as const;
export type MaintenanceType = (typeof MAINTENANCE_TYPES)[number];
export const MAINTENANCE_STATUSES = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as const;
export type MaintenanceStatus = (typeof MAINTENANCE_STATUSES)[number];

export const EXPENSE_CATEGORIES = [
  'FUEL', 'MAINTENANCE', 'REPAIRS', 'INSURANCE', 'REGISTRATION', 'TOLLS', 'PARKING', 'FINES', 'CLEANING', 'MISCELLANEOUS',
] as const;
export type ExpenseCategory = (typeof EXPENSE_CATEGORIES)[number];

export const ASSIGNMENT_STATUSES = ['ACTIVE', 'RETURNED', 'CANCELLED'] as const;

/** Permission required to create/update/delete each module's records. */
export const MODULE_PERMISSION: Record<string, Permission> = {
  employees: 'MANAGE_EMPLOYEES',
  drivers: 'MANAGE_DRIVERS',
  vehicles: 'MANAGE_VEHICLES',
  assignments: 'MANAGE_ASSIGNMENTS',
  trips: 'MANAGE_TRIPS',
  maintenance: 'MANAGE_MAINTENANCE',
  fuel: 'MANAGE_FUEL',
  expenses: 'MANAGE_EXPENSES',
  users: 'MANAGE_USERS',
  audit: 'VIEW_AUDIT_LOG',
  reports: 'VIEW_REPORTS',
};

/** Human labels. */
export const LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  ON_LEAVE: 'On leave',
  SUSPENDED: 'Suspended',
  TERMINATED: 'Terminated',
  INACTIVE: 'Inactive',
  AVAILABLE: 'Available',
  ASSIGNED: 'Assigned',
  IN_SERVICE: 'In service',
  MAINTENANCE: 'Maintenance',
  OUT_OF_SERVICE: 'Out of service',
  RETIRED: 'Retired',
  PLANNED: 'Planned',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  RETURNED: 'Returned',
  SCHEDULED: 'Scheduled',
  IN_PROGRESS: 'In progress',
  ROUTINE_SERVICE: 'Routine service',
  REPAIR: 'Repair',
  INSPECTION: 'Inspection',
  TYRE: 'Tyre',
  BODYWORK: 'Bodywork',
  ELECTRICAL: 'Electrical',
  OTHER: 'Other',
  CAR: 'Car',
  MOTORCYCLE: 'Motorcycle',
  TRUCK: 'Truck',
  VAN: 'Van',
  BUS: 'Bus',
  PETROL: 'Petrol',
  DIESEL: 'Diesel',
  ELECTRIC: 'Electric',
  HYBRID: 'Hybrid',
  CNG: 'CNG',
  FUEL: 'Fuel',
  REGISTRATION: 'Registration',
  TOLLS: 'Tolls',
  PARKING: 'Parking',
  FINES: 'Fines',
  CLEANING: 'Cleaning',
  MISCELLANEOUS: 'Miscellaneous',
  INSURANCE: 'Insurance',
  REPAIRS: 'Repairs',
};

export function label(value: string | null | undefined): string {
  if (!value) return '—';
  return LABELS[value] ?? value.replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
}

/** Status → badge colour classes (mirrors Theme.statusColor().) */
export function statusTone(status: string | null | undefined): string {
  switch ((status ?? '').toUpperCase()) {
    case 'AVAILABLE':
    case 'ACTIVE':
    case 'COMPLETED':
    case 'RETURNED':
    case 'VALID':
      return 'bg-emerald-50 text-emerald-700 ring-emerald-600/20';
    case 'ASSIGNED':
    case 'IN_SERVICE':
    case 'PLANNED':
    case 'IN_PROGRESS':
    case 'SCHEDULED':
      return 'bg-sky-50 text-sky-700 ring-sky-600/20';
    case 'MAINTENANCE':
    case 'ON_LEAVE':
    case 'DUE SOON':
    case 'EXPIRING SOON':
      return 'bg-amber-50 text-amber-700 ring-amber-600/20';
    case 'OUT_OF_SERVICE':
    case 'RETIRED':
    case 'SUSPENDED':
    case 'TERMINATED':
    case 'INACTIVE':
    case 'CANCELLED':
    case 'OVERDUE':
    case 'EXPIRED':
      return 'bg-rose-50 text-rose-700 ring-rose-600/20';
    default:
      return 'bg-slate-100 text-slate-600 ring-slate-500/20';
  }
}

export function vehicleTone(type: string | null | undefined): string {
  switch ((type ?? '').toUpperCase()) {
    case 'CAR': return 'bg-blue-50 text-blue-700 ring-blue-600/20';
    case 'MOTORCYCLE': return 'bg-orange-50 text-orange-700 ring-orange-600/20';
    case 'TRUCK': return 'bg-red-50 text-red-700 ring-red-600/20';
    case 'BUS': return 'bg-violet-50 text-violet-700 ring-violet-600/20';
    case 'VAN': return 'bg-teal-50 text-teal-700 ring-teal-600/20';
    default: return 'bg-slate-100 text-slate-600 ring-slate-500/20';
  }
}

export const CHART_COLORS = ['#1d4ed8', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#6b7280', '#84cc16', '#f97316'];
