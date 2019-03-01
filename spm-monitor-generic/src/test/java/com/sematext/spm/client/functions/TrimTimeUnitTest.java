package com.sematext.spm.client.functions;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TrimTimeUnitTest {

    @Test
    public void testLongTrimTimeUnit() {
        LongTrimTimeUnit func = new LongTrimTimeUnit();
        Map<String,Object> metrics = new HashMap<String, Object>();
        metrics.put("m1","10s");
        metrics.put("m2","10ms");
        metrics.put("m3","1000mseconds");
        metrics.put("m4","1hrs");
        long v = (Long) func.calculateAttribute(metrics, "m1", "ms");
        Assert.assertEquals(10000L,v);
        v = (Long) func.calculateAttribute(metrics, "m2", "ms");
        Assert.assertEquals(10,v);
        v = (Long) func.calculateAttribute(metrics, "m2", "s");
        Assert.assertEquals(0,v);
        v = (Long) func.calculateAttribute(metrics, "m3", "s");
        Assert.assertEquals(1,v);
        v = (Long) func.calculateAttribute(metrics, "m4", "mins");
        Assert.assertEquals(60,v);
    }

    @Test
    public void testDoubleTrimTimeUnit() {
        DoubleTrimTimeUnit func = new DoubleTrimTimeUnit();
        Map<String,Object> metrics = new HashMap<String, Object>();
        metrics.put("m1","81.777s");
        double v = (Double) func.calculateAttribute(metrics, "m1", "ms");
        Assert.assertEquals(81777.0,v,0.0);

        metrics.put("m2","0s");
        v = (Double) func.calculateAttribute(metrics, "m2", "ms");
        Assert.assertEquals(0.0,v,0.0);

        metrics.put("m3","10002ms");
        v = (Double) func.calculateAttribute(metrics, "m3", "s");
        Assert.assertEquals(10.002,v,0.0);
    }
}
