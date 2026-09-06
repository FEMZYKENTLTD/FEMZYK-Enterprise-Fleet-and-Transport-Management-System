package com.femzyk.fleetmanagement.exception;

/** A domain rule was violated, e.g. assigning an inactive driver or a vehicle that is under maintenance. */
public class BusinessRuleException extends ValidationException {
    public BusinessRuleException(String message) { super(message); }
}
