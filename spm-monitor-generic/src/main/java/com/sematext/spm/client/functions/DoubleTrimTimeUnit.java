package com.sematext.spm.client.functions;

import java.util.concurrent.TimeUnit;

/**
 * - DoubleTrimTimeUnit - Trims the time unit strings from the end of value of the metricName
 * and returns the value converted to specified time unit as double.
 * E.g. DoubleTrimTimeUnit(discoveryTime, ms) - trims the time unit from the end of value of metric `discoveryTime` and
 * convert & return the result in milliseconds.
 */

public class DoubleTrimTimeUnit extends TrimTimeUnit {
    protected Number convert(String trimmedValue, TimeUnit to, TimeUnit from) {
        double time = Double.parseDouble(trimmedValue);
        if (from.ordinal() < to.ordinal()) {
            return time / from.convert(1, to);
        } else {
            return time * to.convert(1, from);
        }
    }
}
