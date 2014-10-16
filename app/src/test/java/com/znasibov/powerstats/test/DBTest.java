package com.znasibov.powerstats.test;

import android.util.Log;

import com.znasibov.powerstats.PowerRecord;
import com.znasibov.powerstats.PowerStatsDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(RobolectricGradleTestRunner.class)
public class DBTest {
    private PowerStatsDatabase db;
    Integer dbCounter = 0;

    @Before
    public void setup() {
        dbCounter++;

        ShadowLog.stream = System.out;
        ShadowActivity activity = new ShadowActivity();
        String path = activity.getDatabasePath(PowerStatsDatabase.DB_NAME).toString();
        (new File(path)).delete();
        db = new PowerStatsDatabase(path, dbCounter.toString());
    }

    PowerRecord makeTestRecord() {
        PowerRecord record = new PowerRecord();
        record.setBatteryLevel(50);
        record.setBatteryVoltage(1000);
        record.setBatteryHealth(PowerRecord.BATTERY_HEALTH_GOOD);
        record.setBatteryPowerSource(PowerRecord.BATTERY_PLUGGED_USB);
        record.setBatteryStatus(PowerRecord.BATTERY_STATUS_DISCHARGING);
        record.setBatteryTemperature(30);
        record.setBatteryScale(200);
        record.setPhoneServiceState(PowerRecord.PHONE_SERVICE_POWER_ON);
        record.setWifiState(PowerRecord.WIFI_STATE_ENABLED);
        record.setScreenState(PowerRecord.SCREEN_OFF);
        record.setGpsState(PowerRecord.GPS_STATE_OFF);
        return record;
    }


    @Test
    public void testSmoke() {

    }

    @Test
    public void testExists() {
        assertTrue(db.exists());
    }

    @Test
    public void testCreate() {
        assertTrue(db.exists());
    }

    @Test
    public void testRemove() {
        assertTrue(db.exists());
        db.remove();
        assertFalse(db.exists());
    }

    @Test
    public void testInsertSmoke() {
        PowerRecord info = new PowerRecord();
        db.insert(info);
    }

    @Test
    public void testGetAllSmoke() {
        ArrayList<PowerRecord> records = db.getAll();
        assertEquals(0, records.size());
    }

    @Test
    public void testInsertNoneGetAllVerifyZero() {
        ArrayList<PowerRecord> records = db.getAll();
        assertEquals(0, records.size());
    }

    @Test
    public void testInsertOneGetAllVerifyOne() {
        PowerRecord recordIn = makeTestRecord();
        db.insert(recordIn);

        ArrayList<PowerRecord> records = db.getAll();
        assertEquals(1, records.size());

        PowerRecord record = records.get(0);
        assertEquals(50, record.getBatteryLevel());
        assertEquals(1000, record.getBatteryVoltage());
        assertEquals(PowerRecord.BATTERY_HEALTH_GOOD, record.getBatteryHealth());
        assertEquals(PowerRecord.BATTERY_PLUGGED_USB, record.getBatteryPowerSource());
        assertEquals(PowerRecord.BATTERY_STATUS_DISCHARGING, record.getBatteryStatus());
        assertEquals(30, record.getBatteryTemperature());
        assertEquals(200, record.getBatteryScale());
    }

    @Test
    public void testInsertNoneGetInPeriodVerifyZero() {
        ArrayList<PowerRecord> records = db.getInPeriod(0, System.currentTimeMillis(), false);
        assertEquals(0, records.size());
    }

    @Test
    public void testInsertOneGetInPeriodVerifyOne() {
        PowerRecord recordIn = makeTestRecord();
        db.insert(recordIn);

        ArrayList<PowerRecord> records = db.getInPeriod(
                recordIn.getTimestamp() - 1, recordIn.getTimestamp(), false);
        assertEquals(1, records.size());

        PowerRecord record = records.get(0);
        assertEquals(50, record.getBatteryLevel());
    }

    @Test
    public void testInsertTwoGetInPeriodVerifyTwo() throws Exception {
        PowerRecord record1 = makeTestRecord();
        Thread.sleep(10);
        PowerRecord record2 = makeTestRecord();
        db.insert(record1);
        db.insert(record2);

        ArrayList<PowerRecord> records1 = db.getInPeriod(
                record1.getTimestamp() - 1, record1.getTimestamp(), false);
        assertEquals(1, records1.size());

        ArrayList<PowerRecord> records2 = db.getInPeriod(
                record2.getTimestamp(), record2.getTimestamp() + 1, false);
        assertEquals(1, records2.size());

        ArrayList<PowerRecord> records12 = db.getInPeriod(
                record1.getTimestamp(), record2.getTimestamp(), false);
    }

    @Test
    public void testGetCorrectVersion() throws Exception {
        assertEquals(4, db.getVersion());
    }

    @Test
    public void testGetSetProperty() {
        db.setProperty("Time", "Money");
        assertEquals("Money", db.getProperty("Time"));

        db.setProperty("Time", "Value");
        assertEquals("Value", db.getProperty("Time"));

        db.setProperty("Other", "10");
        assertEquals("10", db.getProperty("Other"));
    }

    @Test
    public void testDebugConstants() {
        assertFalse(db.DEBUG_FORCE_RESET_DATABSE);
    }
}
