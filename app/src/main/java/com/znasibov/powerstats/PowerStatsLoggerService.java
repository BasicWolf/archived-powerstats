package com.znasibov.powerstats;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;


public class PowerStatsLoggerService extends Service {
    PowerRecord pr;
    IBinder binder;
    Set<PowerStatsReceiver> powerStatsReceivers;
    PowerStatsDatabase database;
    TelephonyManager telephony;

    public class ServiceBinder extends Binder {
        PowerStatsLoggerService getService() {
            return PowerStatsLoggerService.this;
        }
    };

    public PowerStatsLoggerService() {
        pr = new PowerRecord();
        binder = new ServiceBinder();
    }

    @Override
    public void onCreate() {
        String dbPath = getAppDatabasePath();
        Log.d("PowerStatsLoggerService", "Database path: " + dbPath);
        database = new PowerStatsDatabase(dbPath);
        powerStatsReceivers = new LinkedHashSet<PowerStatsReceiver>();
        registerReceivers();
        retrieveLatestData();
    }

    private String getAppDatabasePath() {
        // TODO: Will not work if external storage is not available
        return (new File(Environment.getExternalStorageDirectory().getPath(),
                         getDatabasePath(PowerStatsDatabase.DB_NAME).getPath())).getAbsolutePath();
    }

    private void registerReceivers() {
        registerReceiver(batteryStateReceiver,
                         new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(wifiStateChangedReceiver,
                         new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(screenStateReceiver,
                         new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(screenStateReceiver,
                         new IntentFilter(Intent.ACTION_SCREEN_OFF));
        telephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private void retrieveLatestData() {
        Intent intent;

        intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        updateBatteryState(intent);

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        intent = new Intent();
        intent.putExtra(WifiManager.EXTRA_WIFI_STATE, wifiManager.getWifiState());
        updateWifiState(intent);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        intent = new Intent();
        intent.setAction(powerManager.isScreenOn() ? Intent.ACTION_SCREEN_ON : Intent.ACTION_SCREEN_OFF);
        updateScreenState(intent);
    }

    BroadcastReceiver batteryStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryState(intent);
        }
    };

    BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScreenState(intent);
        }
    };

    private BroadcastReceiver wifiStateChangedReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiState(intent);
        }
    };

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onServiceStateChanged (ServiceState serviceState) {
            updatePhoneServiceState(serviceState);

        }
    }

    private void updateBatteryState(Intent intent) {
        boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,
                                             PowerRecord.UNKNOWN);
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
        int powerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                                               BatteryManager.BATTERY_STATUS_UNKNOWN);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,
                                       PowerRecord.DEFAULT_SCALE);
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
                                         PowerRecord.UNKNOWN);
        String technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

        pr.setBatteryPresent(present);
        pr.setBatteryLevel(level);
        pr.setBatteryTemperature(temperature);
        pr.setBatteryVoltage(voltage);
        pr.setBatteryHealth(health);
        pr.setBatteryPowerSource(powerSource);
        pr.setBatteryStatus(status);
        pr.setBatteryScale(scale);
        pr.setBatteryTechnology(technology);
        recordChanged();
    }

    private void updateWifiState(Intent intent) {
        int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE ,
                                                PowerRecord.UNKNOWN);
        switch(extraWifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_UNKNOWN:
            case PowerRecord.UNKNOWN:
                pr.setWifiState(extraWifiState);
        }
        recordChanged();
    }

    private void updateScreenState(Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
            pr.setScreenState(PowerRecord.SCREEN_ON);
        }
        else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            pr.setScreenState(PowerRecord.SCREEN_OFF);
        }
        recordChanged();
    }

    private void updatePhoneServiceState(ServiceState serviceState) {
        switch (serviceState.getState()) {
            case ServiceState.STATE_IN_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
            case ServiceState.STATE_OUT_OF_SERVICE:
                pr.setPhoneServiceState(PowerRecord.PHONE_SERVICE_POWER_ON);
                break;
            case ServiceState.STATE_POWER_OFF:
                pr.setPhoneServiceState(PowerRecord.PHONE_SERVICE_POWER_OFF);
                break;
        }
        recordChanged();
    }

    private void recordChanged() {
        if (pr.isDirty() && pr.isReadyForRecording()) {
            storeRecordToDatabase();
            notifyPowerStatsReceivers();
        }
    }

    private void storeRecordToDatabase () {
        // Log.i("LoggerService", pr.toString(this));
        database.insert(pr);
        pr.clean();
    }

    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BatteryLogger", "Received start id " + startId + ": " + intent);
        /*
        // We want this pslService to continue running until it is explicitly
        // stopped, so return sticky.
        */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        telephony.listen(null, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void register(PowerStatsReceiver obj) {
        powerStatsReceivers.add(obj);

        // Immediately notify new receiver if power record is ready
        if (pr.isReadyForRecording()) {
            obj.onReceive(pr.copy());
        }
    }

    public void unregister(PowerStatsReceiver obj) {
        powerStatsReceivers.remove(obj);
    }

    public void notifyPowerStatsReceivers() {
        PowerRecord prCopy = pr.copy();
        for (PowerStatsReceiver r : powerStatsReceivers) {
            r.onReceive(prCopy);
        }
    }

    public ArrayList<PowerRecord> getRecords(long periodMs) {
        long now = System.currentTimeMillis();
        ArrayList<PowerRecord> records = database.getInPeriod(now - periodMs, now, false);

        boolean noRecordsInRequestedPeriod = records.size() == 0;
        if (noRecordsInRequestedPeriod) {
            records = database.getInPeriod(now - periodMs, now, true);
        }

        if (records.size() > 0) {
            PowerRecord lastRecord = records.get(records.size() - 1).copy();
            lastRecord.setTimestamp(now);
            records.add(lastRecord);
        }

        return records;
    }

    public ArrayList<PowerRecord> getRecords() {
        return database.getAll();
    }
}


//
//    private Object mPowerProfile_;
//
//    private static final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
//
//    mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
//            .getConstructor(Context.class).newInstance(this);
//
//    double batteryCapacity=(Double)Class                                                          .forName(POWER_PROFILE_CLASS).getMethod("getAveragePower", java.lang.String.class).invoke(mPowerProfile_, "battery.capacity");
//
