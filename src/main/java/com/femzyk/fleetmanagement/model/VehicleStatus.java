package com.femzyk.fleetmanagement.model;

import java.util.EnumSet;
import java.util.Set;

/** Vehicle life-cycle states with the allowed transitions (a small state machine). */
public enum VehicleStatus {
    AVAILABLE, ASSIGNED, IN_SERVICE, MAINTENANCE, OUT_OF_SERVICE, RETIRED;

    public Set<VehicleStatus> allowedTransitions() {
        switch (this) {
            case AVAILABLE:      return EnumSet.of(ASSIGNED, IN_SERVICE, MAINTENANCE, OUT_OF_SERVICE, RETIRED);
            case ASSIGNED:       return EnumSet.of(AVAILABLE, IN_SERVICE, MAINTENANCE, OUT_OF_SERVICE);
            case IN_SERVICE:     return EnumSet.of(ASSIGNED, AVAILABLE, MAINTENANCE, OUT_OF_SERVICE);
            case MAINTENANCE:    return EnumSet.of(AVAILABLE, ASSIGNED, OUT_OF_SERVICE, RETIRED);
            case OUT_OF_SERVICE: return EnumSet.of(AVAILABLE, MAINTENANCE, RETIRED);
            case RETIRED:
            default:             return EnumSet.noneOf(VehicleStatus.class);
        }
    }

    public boolean canTransitionTo(VehicleStatus target) {
        return target == this || allowedTransitions().contains(target);
    }

    /** Human-readable label, e.g. "Out of service". */
    public String getLabel() {
        String n = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    public boolean isOperational() {
        return this == AVAILABLE || this == ASSIGNED || this == IN_SERVICE;
    }
}
