package org.skywalking.apm.trace;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wusheng on 2017/2/18.
 */
public class LogDataTestCase {
    @Test
    public void testHoldValue() {
        Map<String, String> fields = new HashMap<String, String>();
        LogData logData = new LogData(123L, fields);

        Assert.assertEquals(123, logData.getTime());
        Assert.assertEquals(fields, logData.getFields());
    }
}
