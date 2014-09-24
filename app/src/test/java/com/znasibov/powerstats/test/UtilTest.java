package com.znasibov.powerstats.test;


import com.znasibov.powerstats.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
public class UtilTest {
    @Test
    public void testTimeToMs() {
        assertEquals(3000, Util.secondsToMs(3));
        assertEquals(120000, Util.minutesToMs(2));
        assertEquals(123000, Util.minutesToMs(2, 3));
        assertEquals(14400000, Util.hoursToMs(4));
        assertEquals(14520000, Util.hoursToMs(4, 2));
        assertEquals(14523000, Util.hoursToMs(4, 2, 3));
    }
}

