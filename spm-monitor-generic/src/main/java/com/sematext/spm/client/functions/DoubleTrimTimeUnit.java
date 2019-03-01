package com.sematext.spm.client.functions;

import java.util.concurrent.TimeUnit;

public class DoubleTrimTimeUnit extends LongTrimTimeUnit {
    protected Object convert(String trimmedValue, TimeUnit to, TimeUnit from) {
        double time = Double.parseDouble(trimmedValue);
        if (from.ordinal() < to.ordinal()) {
            return time / from.convert(1, to);
        } else {
            return time * to.convert(1, from);
        }
    }
}
