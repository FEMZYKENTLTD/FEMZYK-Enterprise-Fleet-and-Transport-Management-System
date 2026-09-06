package com.femzyk.fleetmanagement.exception;

import java.util.Collections;
import java.util.List;

/** Thrown when user-supplied data violates a business or format rule. Carries one or more messages. */
public class ValidationException extends FleetException {
    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<String> getErrors() { return errors; }
}
