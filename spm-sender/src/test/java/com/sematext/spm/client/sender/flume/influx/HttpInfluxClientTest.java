/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.spm.client.sender.flume.influx;

import org.junit.Assert;
import org.junit.Test;

public class HttpInfluxClientTest {
  @Test
  public void testGetBulkUrl() {
    Assert.assertEquals("https://metric.sematext.com/write?db=test&v=1.2&host=myhost",
        HttpInfluxClient.getBulkUrl("https://metric.sematext.com", "/write?db=test", "v=1.2&host=myhost"));
    Assert.assertEquals("https://metric.sematext.com/write?db=test&v=1.2&host=myhost",
        HttpInfluxClient.getBulkUrl("https://metric.sematext.com", "write?db=test", "&v=1.2&host=myhost"));
    Assert.assertEquals("https://metric.sematext.com/write?v=1.2&host=myhost",
        HttpInfluxClient.getBulkUrl("https://metric.sematext.com/write", "", "v=1.2&host=myhost"));
  }
}
