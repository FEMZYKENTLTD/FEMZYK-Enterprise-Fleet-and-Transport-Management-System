package com.femzyk.fleetmanagement.model;

import java.util.EnumSet;
import java.util.Set;

/** Fine-grained permissions granted to roles. Checked in the service layer, mirrored in the UI. */
public enum Permission {
    VIEW_DASHBOARD,
    VIEW_RECORDS,
    MANAGE_EMPLOYEES,
    MANAGE_DRIVERS,
    MANAGE_VEHICLES,
    MANAGE_ASSIGNMENTS,
    MANAGE_TRIPS,
    MANAGE_MAINTENANCE,
    MANAGE_FUEL,
    MANAGE_EXPENSES,
    VIEW_REPORTS,
    EXPORT_DATA,
    IMPORT_DATA,
    MANAGE_USERS,
    VIEW_AUDIT_LOG,
    MANAGE_BACKUPS,
    LOAD_DEMO_DATA;

    public static Set<Permission> forRole(Role role) {
        switch (role) {
            case ADMINISTRATOR:
                return EnumSet.allOf(Permission.class);
            case FLEET_MANAGER:
                return EnumSet.of(VIEW_DASHBOARD, VIEW_RECORDS, MANAGE_EMPLOYEES, MANAGE_DRIVERS, MANAGE_VEHICLES,
                        MANAGE_ASSIGNMENTS, MANAGE_TRIPS, MANAGE_MAINTENANCE, MANAGE_FUEL, MANAGE_EXPENSES,
                        VIEW_REPORTS, EXPORT_DATA, IMPORT_DATA);
            case OPERATIONS_OFFICER:
                return EnumSet.of(VIEW_DASHBOARD, VIEW_RECORDS, MANAGE_TRIPS, MANAGE_MAINTENANCE, MANAGE_FUEL,
                        MANAGE_EXPENSES, VIEW_REPORTS, EXPORT_DATA);
            case VIEWER:
            default:
                return EnumSet.of(VIEW_DASHBOARD, VIEW_RECORDS, VIEW_REPORTS, EXPORT_DATA);
        }
    }
}
