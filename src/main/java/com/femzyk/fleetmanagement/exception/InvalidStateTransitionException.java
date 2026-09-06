package com.femzyk.fleetmanagement.exception;

/** A status change is not permitted by the state machine of the entity (vehicle, trip, assignment). */
public class InvalidStateTransitionException extends ValidationException {
    public InvalidStateTransitionException(String message) { super(message); }
}
