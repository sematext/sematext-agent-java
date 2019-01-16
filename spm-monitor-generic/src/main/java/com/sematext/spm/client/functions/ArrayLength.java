package com.sematext.spm.client.functions;

import com.sematext.spm.client.observation.CalculationFunction;

import java.util.Map;

public class ArrayLength implements CalculationFunction {
    @Override
    public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
        if (params != null && params.length == 1) {
            String metricName = params[0].toString();
            if (metrics.containsKey(metricName)) {
                Object[] array = (Object[]) metrics.get(metricName);
                return array.length;
            }
            return 0;
        } else {
            throw new IllegalArgumentException("Missing metric name in params");
        }
    }

    @Override
    public String calculateTag(Map<String, String> objectNameTags, Object... params) {
        throw new UnsupportedOperationException("Can't be used in tag context");
    }
}
