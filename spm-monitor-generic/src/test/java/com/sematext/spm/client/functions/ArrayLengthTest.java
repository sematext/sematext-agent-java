package com.sematext.spm.client.functions;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;

public class ArrayLengthTest {

    @Test
    public void testArrayLength() {
        ArrayLength arrayLength = new ArrayLength();
        Map<String,Object> metrics = new HashMap<String, Object>();
        assertEquals(0,arrayLength.calculateAttribute(metrics,"partition"));
        metrics.put("partition",new String[]{});
        assertEquals(0,arrayLength.calculateAttribute(metrics,"partition"));
        metrics.put("partition",new String[]{"1","2"});
        assertEquals(2,arrayLength.calculateAttribute(metrics,"partition"));
        metrics.put("partition",new Integer[]{4,5,6});
        assertEquals(3,arrayLength.calculateAttribute(metrics,"partition"));
    }
}
