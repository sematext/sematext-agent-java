package com.sematext.spm.client.functions;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class LongTrimTimeUnitTest {

    @Test
    public void testLongTrimTimeUnit() {
        LongTrimTimeUnit func = new LongTrimTimeUnit();
        Map<String,Object> metrics = new HashMap<String, Object>();
        metrics.put("m1","10s");
        metrics.put("m2","10ms");
        long v = (Long) func.calculateAttribute(metrics, "m1", "ms");
        Assert.assertEquals(v,10000L);
        v = (Long) func.calculateAttribute(metrics, "m2", "ms");
        Assert.assertEquals(v,10);
        v = (Long) func.calculateAttribute(metrics, "m2", "s");
        Assert.assertEquals(v,0);
    }
}
