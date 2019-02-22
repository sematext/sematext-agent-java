package com.sematext.spm.client.functions;

import com.sematext.spm.client.observation.CalculationFunction;

import java.util.Arrays;
import java.util.Comparator;

public abstract class TrimUnit implements CalculationFunction {
    protected String[] getUnits(Object[] params) {
        String[] unitsToTrim = Arrays.copyOfRange(params, 1, params.length, String[].class);
        // sort the unit by length
        Arrays.sort(unitsToTrim, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        return unitsToTrim;
    }
}
