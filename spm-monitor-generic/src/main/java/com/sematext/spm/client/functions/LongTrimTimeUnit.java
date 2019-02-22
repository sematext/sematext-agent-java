package com.sematext.spm.client.functions;

import com.sematext.spm.client.observation.CalculationFunction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * - LongTrimTimeUnit - Trims the time unit strings (ns, us, ms, s, m, h) from the end of value of the metricName
     and returns the value converted to specified time unit.
     E.g. LongTrimUnit(discoveryTime, ms) - trims the time unit from the end of value of metric `discoveryTime` and
     convert & return the result in milliseconds.
 */
public class LongTrimTimeUnit implements CalculationFunction {
    private static final Map<String, TimeUnit> TIME_UNITS = new LinkedHashMap<String, TimeUnit>();

    static {
        TIME_UNITS.put("ns", TimeUnit.NANOSECONDS);
        TIME_UNITS.put("us", TimeUnit.MICROSECONDS);
        TIME_UNITS.put("ms", TimeUnit.MILLISECONDS);
        TIME_UNITS.put("s", TimeUnit.SECONDS);
        TIME_UNITS.put("m", TimeUnit.MINUTES);
        TIME_UNITS.put("h", TimeUnit.HOURS);
    }

    @Override
    public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
        if (params != null && params.length == 2) {
            String metricName = params[0].toString();
            String value = (String) metrics.get(metricName);
            TimeUnit unitToConvert = TIME_UNITS.get(params[1].toString());
            if (unitToConvert == null) {
                throw new IllegalArgumentException(String.format("Cannot find units in value %s for %s", value, metricName));
            }
            return convert(value, metricName, unitToConvert);
        } else {
            throw new IllegalArgumentException("Missing metric name and unit to convert in params");
        }
    }

    private long convert(String value, String metricName, TimeUnit toUnit) {
        for (Map.Entry<String, TimeUnit> fromUnit : TIME_UNITS.entrySet()) {
            if (value.contains(fromUnit.getKey())) {
                long time = Long.parseLong(value.substring(0, value.indexOf(fromUnit.getKey())).trim());
                return toUnit.convert(time, fromUnit.getValue());
            }
        }
        throw new IllegalArgumentException(String.format("Cannot find units in value %s for %s", value, metricName));
    }

    @Override
    public String calculateTag(Map<String, String> objectNameTags, Object... params) {
        throw new UnsupportedOperationException("Can't be used in tag context");
    }
}
