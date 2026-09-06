package com.femzyk.fleetmanagement.util;

/**
 * Human-readable record codes such as EMP-0001, DRV-0001, VEH-0001, TRP-0001.
 * Pattern adapted from the Contact Book project's "cramable" database IDs.
 */
public final class CodeGenerator {

    private CodeGenerator() {}

    public static String format(String prefix, long sequence) {
        return String.format("%s-%04d", prefix, sequence);
    }
}
