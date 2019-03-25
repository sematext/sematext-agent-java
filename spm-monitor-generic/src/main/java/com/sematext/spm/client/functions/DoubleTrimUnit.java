package com.sematext.spm.client.functions;

import com.sematext.spm.client.observation.CalculationFunction;

import java.util.Map;

/**
 * Trims the specified unit string from the end of value of the metricName and returns Double.
 * e.g.DoubleTrimUnit(Value,ms) - trims `ms` from  the end of value of metric name `Value`
 */
public class DoubleTrimUnit implements CalculationFunction {
    @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
        if (params != null && params.length == 2) {
            String metricName = params[0].toString();
            String unit = params[1].toString();
            String value = (String) metrics.get(metricName);
            return value == null ? null : Double.parseDouble(value.substring(0, value.indexOf(unit)).trim());
        } else {
            throw new IllegalArgumentException("Missing metric name and unit in params");
        }
    }

    @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
        throw new UnsupportedOperationException("Can't be used in tag context");
    }
}
