package com.znasibov.powerstats.test;


import android.util.Log;

import com.znasibov.powerstats.PowerRecord;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

interface DirtyAction {
    void performDirtyAction(PowerRecord p);
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
        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryStatus(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryHealth(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryLevel(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryPowerSource(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryPresent(!p.getBatteryPresent());
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryScale(PowerRecord.DEFAULT_SCALE + 1);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryTechnology(PowerRecord.DEFAULT_TECHNOLOGY + "_DIRTY");
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryTemperature(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setBatteryVoltage(0);
            }
        });

        isDirty(new DirtyAction() {
            @Override
            public void performDirtyAction(PowerRecord p) {
                p.setWifiState(PowerRecord.WIFI_STATE_ENABLED);
            }
        });
    }

    void isDirty(DirtyAction action) throws Exception {
        isDirty(action, null);
    }

    void isDirty(DirtyAction action, PowerRecord p) throws Exception{
        if (p == null) {
            p = new PowerRecord();
        }

        long oldTimestamp = p.getTimestamp();
        Thread.sleep(10);

        action.performDirtyAction(p);

        long newTimestamp = p.getTimestamp();

        assertTrue(p.isDirty());
        assertTrue(oldTimestamp != newTimestamp);
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
        p.setBatteryTemperature(100);
        p.setBatteryVoltage(1000);
        p.setPhoneServiceState(PowerRecord.PHONE_SERVICE_POWER_ON);
        p.setScreenState(PowerRecord.SCREEN_OFF);
        assertFalse(p.isReadyForRecording());

        p.setWifiState(PowerRecord.WIFI_STATE_ENABLED);
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
