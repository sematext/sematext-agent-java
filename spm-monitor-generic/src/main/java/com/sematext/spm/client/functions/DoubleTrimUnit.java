package com.sematext.spm.client.functions;

import java.util.Map;

/**
 * Trims the specified unit string from the end of value of the metricName and returns Double.
 * e.g.DoubleTrimUnit(Value,ms) - trims `ms` from  the end of value of metric name `Value`
 */
public class DoubleTrimUnit extends TrimUnit {
    @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
        if (params != null && params.length > 1) {
            String metricName = params[0].toString();
            String value = (String) metrics.get(metricName);
            String[] unitsToTrim = getUnits(params);
            for (String unit : unitsToTrim) {
                if (value.contains(unit)) {
                    return Double.parseDouble(value.substring(0, value.indexOf(unit)).trim());
                }
            }
            throw new IllegalArgumentException(String.format("Cannot find units in value %s for %s", value, metricName));
        } else {
            throw new IllegalArgumentException("Missing metric name and unit in params");
        }
    }



    @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
        throw new UnsupportedOperationException("Can't be used in tag context");
    }
}
