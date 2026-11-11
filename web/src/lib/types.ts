/** Row shapes for the API payloads (dates as ISO strings / DATE text). */

import type {
  ExpenseCategory, FuelType, LicenceCategory, MaintenanceStatus, MaintenanceType, Role, TripStatus, VehicleStatus, VehicleType,
} from './domain';

export type Employee = {
  id: number;
  employee_code: string;
  first_name: string;
  last_name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  department: string;
  position: string | null;
  date_of_birth: string | null;
  hire_date: string;
  salary: number | null;
  status: 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED';
  emergency_contact_name: string | null;
  emergency_contact_phone: string | null;
  notes: string | null;
  created_at: string;
  updated_at: string;
  deleted_at: string | null;
};

export type Driver = {
  id: number;
  driver_code: string;
  employee_id: number;
  licence_number: string;
  licence_category: LicenceCategory;
  licence_issue_date: string | null;
  licence_expiry_date: string;
  status: 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'INACTIVE';
  notes: string | null;
  deleted_at: string | null;
  employee_full_name: string;
  employee_email: string | null;
  employee_phone: string | null;
  employee_department: string;
};

export type Vehicle = {
  id: number;
  vehicle_code: string;
  registration_number: string;
  vehicle_type: VehicleType;
  make: string;
  model: string;
  year: number;
  color: string | null;
  vin: string | null;
  engine_number: string | null;
  acquisition_date: string | null;
  acquisition_cost: number;
  current_mileage: number;
  fuel_type: FuelType;
  capacity: number;
  status: VehicleStatus;
  attribute_number: number | null;
  attribute_text: string | null;
  notes: string | null;
  deleted_at: string | null;
  assigned_driver_name?: string | null;
  assigned_driver_id?: number | null;
};

export type Assignment = {
  id: number;
  vehicle_id: number;
  driver_id: number;
  assigned_at: string;
  returned_at: string | null;
  status: 'ACTIVE' | 'RETURNED' | 'CANCELLED';
  purpose: string | null;
  notes: string | null;
  assigned_by: string | null;
  registration_number: string;
  vehicle_display: string;
  driver_name: string;
  driver_code: string;
};

export type Trip = {
  id: number;
  trip_code: string;
  vehicle_id: number;
  driver_id: number;
  origin: string | null;
  destination: string;
  purpose: string | null;
  departure_time: string;
  return_time: string | null;
  start_mileage: number | null;
  end_mileage: number | null;
  fuel_used_litres: number | null;
  status: TripStatus;
  notes: string | null;
  registration_number: string;
  driver_name: string;
  distance_km: number | null;
};

export type MaintenanceRecord = {
  id: number;
  vehicle_id: number;
  service_date: string;
  maintenance_type: MaintenanceType;
  description: string;
  provider: string | null;
  cost: number;
  mileage_at_service: number | null;
  next_service_date: string | null;
  next_service_mileage: number | null;
  status: MaintenanceStatus;
  notes: string | null;
  registration_number: string;
  vehicle_display: string;
};

export type FuelRecord = {
  id: number;
  vehicle_id: number;
  driver_id: number | null;
  fuel_date: string;
  quantity_litres: number;
  unit_price: number;
  total_cost: number;
  mileage: number | null;
  station: string | null;
  receipt_reference: string | null;
  fuel_type: FuelType | null;
  notes: string | null;
  registration_number: string;
  driver_name: string | null;
};

export type Expense = {
  id: number;
  vehicle_id: number | null;
  expense_date: string;
  category: ExpenseCategory;
  amount: number;
  vendor: string | null;
  description: string;
  reference: string | null;
  notes: string | null;
  source_fuel_record_id: number | null;
  source_maintenance_record_id: number | null;
  registration_number: string | null;
  system_generated: boolean;
};

export type AuditLog = {
  id: number;
  timestamp: string;
  username: string;
  action: string;
  module: string;
  reference: string | null;
  description: string | null;
};

export type DashboardData = {
  kpis: {
    totalVehicles: number;
    availableVehicles: number;
    assignedVehicles: number;
    inServiceVehicles: number;
    maintenanceVehicles: number;
    outOfServiceVehicles: number;
    retiredVehicles: number;
    totalEmployees: number;
    activeEmployees: number;
    totalDrivers: number;
    activeDrivers: number;
    activeTrips: number;
    plannedTrips: number;
    completedTripsThisMonth: number;
    activeAssignments: number;
    overdueMaintenance: number;
    dueSoonMaintenance: number;
    expiringLicences: number;
    expiredLicences: number;
    monthFuelCost: number;
    monthMaintenanceCost: number;
    monthTotalExpenses: number;
    yearTotalExpenses: number;
    monthDistanceKm: number;
    fleetUtilisationPct: number;
  };
  vehiclesByStatus: Record<string, number>;
  vehiclesByType: Record<string, number>;
  expensesByMonth: { month: string; fuel: number; maintenance: number; other: number; total: number }[];
  expensesByCategory: { category: string; amount: number }[];
  employeesByDepartment: { department: string; count: number }[];
  maintenanceAlerts: {
    vehicle_id: number; registration_number: string; vehicle: string; current_mileage: number;
    next_service_date: string | null; next_service_mileage: number | null; due_status: 'OVERDUE' | 'DUE SOON' | 'OK';
  }[];
  licenceAlerts: {
    driver_id: number; driver_code: string; driver_name: string; licence_category: string;
    licence_expiry_date: string; days_remaining: number; licence_status: 'EXPIRED' | 'EXPIRING SOON' | 'VALID';
  }[];
  recentTrips: Trip[];
  recentAudit: AuditLog[];
  topCostVehicles: { registration_number: string; vehicle: string; total_cost: number; distance_km: number; cost_per_km: number | null }[];
};

export type UserSummary = {
  id: number;
  username: string;
  email: string;
  full_name: string;
  role: Role;
  active: boolean;
  must_change_password: boolean;
  last_login_at: string | null;
  created_at: string;
};
