package com.sematext.spm.client.functions;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TrimUnitTest {
    @Test
    public void testLongTrimUnit() {
        TrimUnit trimUnit = new LongTrimUnit();
        Map<String,Object> metrics = new HashMap<String, Object>();
        metrics.put("m1","10s");
        metrics.put("m2","20ms");
        Long value = (Long)trimUnit.calculateAttribute(metrics,"m1","s","ms");
        Assert.assertEquals(value.longValue(),10L);
        value = (Long)trimUnit.calculateAttribute(metrics,"m2","s","ms");
        Assert.assertEquals(value.longValue(),20L);
        value = (Long)trimUnit.calculateAttribute(metrics,"m1","s");
        Assert.assertEquals(value.longValue(),10L);
    }

    @Test
    public void testDoubleTrimUnit() {
        TrimUnit trimUnit = new DoubleTrimUnit();
        Map<String,Object> metrics = new HashMap<String, Object>();
        metrics.put("m1","10.30s");
        metrics.put("m2","20.35ms");
        Double value = (Double) trimUnit.calculateAttribute(metrics,"m1","s","ms");
        Assert.assertEquals(value,10.30,0.0);
        value = (Double)trimUnit.calculateAttribute(metrics,"m2","s","ms");
        Assert.assertEquals(value,20.35,0.0);
        value = (Double)trimUnit.calculateAttribute(metrics,"m1","s");
        Assert.assertEquals(value,10.30,0.0);
    }
}
