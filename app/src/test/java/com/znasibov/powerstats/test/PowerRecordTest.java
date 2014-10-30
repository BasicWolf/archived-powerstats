package com.znasibov.powerstats.test;


import com.znasibov.powerstats.PowerRecord;


import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

interface PowerRecordModifier {
    void modify(PowerRecord p);
}


@RunWith(RobolectricGradleTestRunner.class)
public class PowerRecordTest {

    @Test
    public void testSmoke() {
        new PowerRecord();
    }

    @Test
    public void testDirtyAfterInitialization() {
        PowerRecord p = new PowerRecord();
        assertFalse(p.isDirty());
    }

    public void testTimestampNotNull() {
        PowerRecord p = new PowerRecord();
        assertTrue(p.getTimestamp() > 0);
    }

    @Test
    public void testNotDirtyAfterSetTimestamp() {
        PowerRecord p = new PowerRecord();
        p.setTimestamp(100);
        assertFalse(p.isDirty());
    }

    @Test
    public void testDirtyOnSetters() throws Exception {
        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryStatus(0);
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryHealth(0);
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryLevel(0);
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryPowerSource(0);
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryPresent(!p.getBatteryPresent());
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryScale(PowerRecord.DEFAULT_SCALE + 1);
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryTechnology(PowerRecord.DEFAULT_TECHNOLOGY + "_DIRTY");
            }
        });

        isDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setWifiState(PowerRecord.WIFI_STATE_ENABLED);
            }
        });
    }

    @Test
    public void testNotDirtyOnSetters() throws Exception {
        isNotDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryTemperature(0);
            }
        });

        isNotDirty(new PowerRecordModifier() {
            @Override
            public void modify(PowerRecord p) {
                p.setBatteryVoltage(0);
            }
        });
    }

    void isDirty(PowerRecordModifier action) throws Exception {
        isDirty(action, null, false);
    }

    void isNotDirty(PowerRecordModifier action) throws Exception {
        isDirty(action, null, true);
    }

    void isDirty(PowerRecordModifier action, PowerRecord p, boolean not) throws Exception {
        if (p == null) {
            p = new PowerRecord();
        }

        long oldTimestamp = p.getTimestamp();
        Thread.sleep(10);

        action.modify(p);

        long newTimestamp = p.getTimestamp();

        boolean shouldAssertTrue = !not;
        if (shouldAssertTrue) {
            assertTrue(p.isDirty());
            assertTrue(oldTimestamp != newTimestamp);
        } else {
            assertFalse(p.isDirty());
            assertFalse(oldTimestamp != newTimestamp);
        }

    }

    @Test
    public void testClean() {
        PowerRecord p = new PowerRecord();
        p.setBatteryLevel(100);
        assertTrue(p.isDirty());
        p.clean();
        assertFalse(p.isDirty());
    }

    @Test
    public void testReadyForRecording() {
        PowerRecord p = new PowerRecord();
        assertFalse(p.isReadyForRecording());

        p.setBatteryStatus(PowerRecord.BATTERY_STATUS_DISCHARGING);
        p.setBatteryHealth(PowerRecord.BATTERY_HEALTH_GOOD);
        p.setBatteryLevel(100);
        p.setBatteryPowerSource(PowerRecord.BATTERY_PLUGGED_AC);
        p.setBatteryPresent(true);
        p.setBatteryScale(100);
        p.setPhoneServiceState(PowerRecord.PHONE_SERVICE_POWER_ON);
        p.setScreenState(PowerRecord.SCREEN_OFF);
        p.setWifiState(PowerRecord.WIFI_STATE_ENABLED);
        assertFalse(p.isReadyForRecording());

        p.setGpsState(PowerRecord.GPS_STATE_OFF);
        assertTrue(p.isReadyForRecording());
    }

    @Test
    public void testCopyConstructor() {
        PowerRecord p = new PowerRecord();
        p.setBatteryStatus(PowerRecord.BATTERY_STATUS_DISCHARGING);
        p.setBatteryHealth(PowerRecord.BATTERY_HEALTH_GOOD);
        p.setBatteryLevel(100);
        p.setBatteryPowerSource(PowerRecord.BATTERY_PLUGGED_AC);
        p.setBatteryPresent(true);
        p.setBatteryScale(100);
        p.setBatteryTemperature(100);
        p.setBatteryVoltage(1000);
        p.setWifiState(PowerRecord.WIFI_STATE_ENABLED);

        PowerRecord p2 = p.copy();
        assertNotEquals(p, p2);
        assertEquals(p.getTimestamp(), p2.getTimestamp());
        assertEquals(p.getBatteryStatus(), p2.getBatteryStatus());
        assertEquals(p.getBatteryHealth(), p2.getBatteryHealth());
        assertEquals(p.isDirty(), p2.isDirty());
        assertEquals(p.getBatteryLevel(), p2.getBatteryLevel());
        assertEquals(p.getBatteryPowerSource(), p2.getBatteryPowerSource());
        assertEquals(p.getBatteryPresent(), p2.getBatteryPresent());
        assertEquals(p.getBatteryScale(), p2.getBatteryScale());
        assertEquals(p.getBatteryTechnology(), p2.getBatteryTechnology());
        assertEquals(p.getBatteryTemperature(), p2.getBatteryTemperature());
        assertEquals(p.getBatteryTechnology(), p2.getBatteryTechnology());
        assertEquals(p.getBatteryVoltage(), p2.getBatteryVoltage());
        assertEquals(p.getWifiState(), p2.getWifiState());
    }
}
