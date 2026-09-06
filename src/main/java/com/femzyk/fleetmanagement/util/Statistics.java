package com.femzyk.fleetmanagement.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Small numeric helpers (average, maximum, occurrence count, cumulative sum).
 * Adapted from the aggregate routines of the stock-analysis exercise project.
 */
public final class Statistics {

    private Statistics() {}

    public static double average(Collection<? extends Number> values) {
        OptionalDouble avg = values.stream().mapToDouble(Number::doubleValue).average();
        return avg.isPresent() ? avg.getAsDouble() : 0.0;
    }

    public static double max(Collection<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
    }

    public static double sum(Collection<? extends Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).sum();
    }

    public static long countOccurrences(Collection<? extends Number> values, double target) {
        return values.stream().filter(v -> Double.compare(v.doubleValue(), target) == 0).count();
    }

    /** Running totals: [a, b, c] -> [a, a+b, a+b+c]. */
    public static List<Double> cumulativeSum(List<? extends Number> values) {
        List<Double> out = new ArrayList<>(values.size());
        double running = 0;
        for (Number n : values) {
            running += n.doubleValue();
            out.add(running);
        }
        return out;
    }
}
