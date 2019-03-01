package com.sematext.spm.client.functions;

import java.util.concurrent.TimeUnit;

/**
 * - LongTrimTimeUnit - Trims the time unit strings from the end of value of the metricName
 * and returns the value converted to specified time unit.
 * E.g. LongTrimUnit(discoveryTime, ms) - trims the time unit from the end of value of metric `discoveryTime` and
 * convert & return the result in milliseconds.
 */
public class LongTrimTimeUnit extends TrimTimeUnit {
    protected Number convert(String trimmedValue, TimeUnit toUnit, TimeUnit fromUnit) {
        long time = Long.parseLong(trimmedValue);
        return toUnit.convert(time, fromUnit);
    }
}
