package com.sematext.spm.client.postgresql;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PosgreSQLVersionCheckTest {
    @Test
    public void testGetVersion() throws IOException {
        PostgreSQLVersionCheck check = new PostgreSQLVersionCheck();

        String rawVersion = "10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)";
        Assert.assertEquals("10.6", check.getVersion(rawVersion));

        rawVersion = "11beta3";
        Assert.assertEquals("11", check.getVersion(rawVersion));
    }
}
