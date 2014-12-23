package com.znasibov.powerstats;


import android.content.Context;
import android.location.GpsStatus;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.telephony.ServiceState;
import android.text.TextUtils;

public class PowerRecord {

    public static final int UNKNOWN = -1024;

    // -- Constants -- //

    // Battery constants
    public static final int DEFAULT_SCALE = 100;
    public static final String DEFAULT_TECHNOLOGY = "UNKNOWN";

    public static final int BATTERY_STATUS_UNKNOWN = BatteryManager.BATTERY_STATUS_UNKNOWN;
    public static final int BATTERY_STATUS_CHARGING = BatteryManager.BATTERY_STATUS_CHARGING;
    public static final int BATTERY_STATUS_DISCHARGING = BatteryManager.BATTERY_STATUS_DISCHARGING;
    public static final int BATTERY_STATUS_NOT_CHARGING = BatteryManager.BATTERY_STATUS_NOT_CHARGING;
    public static final int BATTERY_STATUS_FULL = BatteryManager.BATTERY_STATUS_FULL;
    public static final int BATTERY_HEALTH_UNKNOWN = BatteryManager.BATTERY_HEALTH_UNKNOWN;
    public static final int BATTERY_HEALTH_GOOD = BatteryManager.BATTERY_HEALTH_GOOD;
    public static final int BATTERY_HEALTH_OVERHEAT = BatteryManager.BATTERY_HEALTH_OVERHEAT;
    public static final int BATTERY_HEALTH_DEAD = BatteryManager.BATTERY_HEALTH_DEAD;
    public static final int BATTERY_HEALTH_OVER_VOLTAGE = BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE;
    public static final int BATTERY_HEALTH_UNSPECIFIED_FAILURE = BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE;
    public static final int BATTERY_HEALTH_COLD = BatteryManager.BATTERY_HEALTH_COLD;
    public static final int BATTERY_PLUGGED_AC = BatteryManager.BATTERY_PLUGGED_AC;
    public static final int BATTERY_PLUGGED_USB = BatteryManager.BATTERY_PLUGGED_USB;
    public static final int BATTERY_PLUGGED_WIRELESS = BatteryManager.BATTERY_PLUGGED_WIRELESS;

    // WIFI constants
    public static final int WIFI_STATE_DISABLED = WifiManager.WIFI_STATE_DISABLED;
    public static final int WIFI_STATE_ENABLED = WifiManager.WIFI_STATE_ENABLED;
    public static final int WIFI_STATE_UNKNOWN = WifiManager.WIFI_STATE_UNKNOWN;

    // Phone service
    public static final int PHONE_SERVICE_POWER_OFF = ServiceState.STATE_POWER_OFF;
    public static final int PHONE_SERVICE_POWER_ON = ServiceState.STATE_IN_SERVICE;

    // Screen
    public static final int SCREEN_ON = 1;
    public static final int SCREEN_OFF = 0;

    // GPS
    public static final int GPS_STATE_ON = GpsStatus.GPS_EVENT_STARTED;
    public static final int GPS_STATE_OFF = GpsStatus.GPS_EVENT_STOPPED;

    // Mobile data network
    public static final int MOBILE_DATA_ON = 1;
    public static final int MOBILE_DATA_OFF = 0;

    // -- Fields -- //
    private long timestamp;

    // Battery fields
    private int batteryStatus = UNKNOWN;
    private int batteryHealth = UNKNOWN;
    private int batteryLevel = UNKNOWN;
    private int batteryPowerSource = UNKNOWN;
    private boolean batteryPresent = true;
    private int batteryScale = DEFAULT_SCALE;
    private String batteryTechnology = DEFAULT_TECHNOLOGY;
    private int batteryTemperature = UNKNOWN;
    private int batteryVoltage = UNKNOWN;

    private int wifiState = UNKNOWN;
    private int mobileDataState = UNKNOWN;
    private int phoneServiceState = UNKNOWN;
    private int screenState = UNKNOWN;
    private int gpsState = UNKNOWN;

    private boolean dirty = false;
    private boolean readingFromDatabase = false;

    public String toString(Context context) {
        String[] labels = new String[] {
            "Battery: ",
            String.format("Present: %b", batteryPresent),
            String.format("Charging: %s", getBatteryStatusAsString()),
            String.format("Scale: %d", batteryScale),
            String.format("Level: %d", batteryLevel),
            String.format("Voltage: %d", batteryVoltage),
            String.format("Temperature: %d", batteryTemperature),
            String.format("Power source: %s", powerSourceToString()),
            String.format("Health: %s", healthToString()),
            String.format("Technology: %s", batteryTechnology),
            String.format("Wifi: %s", getWifiStateAsString()),
            String.format("Mobile data: %s", getMobileDataStateAsString()),
            String.format("Phone: %s", getPhoneServiceStateAsString()),
            String.format("Screen: %s", getScreenStateAsString()),
            String.format("GPS: %s", getGpsStateAsString()),
        };

        return "Power record (" +
               String.format("Timestamp: %d", timestamp) +
               ") \n" +
               TextUtils.join("\n", labels);
    }

    public String healthToString() {
        switch (batteryHealth) {
            case BATTERY_HEALTH_COLD:
                return "cold";
            case BATTERY_HEALTH_DEAD:
                return "dead";
            case BATTERY_HEALTH_GOOD:
                return "good";
            case BATTERY_HEALTH_OVER_VOLTAGE:
                return "over voltage";
            case BATTERY_HEALTH_OVERHEAT:
                return "overheat";
            case BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "unspecified failure";
            case BATTERY_HEALTH_UNKNOWN:
            case UNKNOWN:
            default:
                return "unknown";
        }
    }

    private String powerSourceToString() {
        switch (batteryPowerSource) {
            case 0:
                return "battery";
            case BATTERY_PLUGGED_AC:
                return "ac";
            case BATTERY_PLUGGED_USB:
                return "usb";
            case BATTERY_PLUGGED_WIRELESS:
                return "wireless";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }

    public String getBatteryStatusAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (batteryStatus) {
            case BATTERY_STATUS_CHARGING:
                return context.getString(R.string.battery_status_charging);
            case BATTERY_STATUS_DISCHARGING:
                return context.getString(R.string.battery_status_discharging);
            case BATTERY_STATUS_NOT_CHARGING:
                return context.getString(R.string.battery_status_not_charging);
            case BATTERY_STATUS_FULL:
                return context.getString(R.string.battery_status_full);
            case BATTERY_STATUS_UNKNOWN:
            case UNKNOWN:
            default:
                return context.getString(R.string.battery_status_unknown);
        }

    }

    public String getWifiStateAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (wifiState) {
            case WIFI_STATE_ENABLED:
                return context.getString(R.string.wifi_state_enabled);
            case WIFI_STATE_DISABLED:
                return context.getString(R.string.wifi_state_disabled);
            case WIFI_STATE_UNKNOWN:
            case UNKNOWN:
            default:
                return context.getString(R.string.wifi_state_unknown);
        }
    }

    public String getMobileDataStateAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (mobileDataState) {
            case MOBILE_DATA_ON:
                return context.getString(R.string.mobile_state_on);
            case MOBILE_DATA_OFF:
                return context.getString(R.string.mobile_state_off);
            case UNKNOWN:
            default:
                return context.getString(R.string.mobile_state_unknown);
        }
    }

    public String getPhoneServiceStateAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (phoneServiceState) {
            case PHONE_SERVICE_POWER_ON:
                return context.getString(R.string.phone_service_state_on);
            case PHONE_SERVICE_POWER_OFF:
                return context.getString(R.string.phone_service_state_off);
            case UNKNOWN:
            default:
                return context.getString(R.string.phone_service_state_unknown);
        }
    }

    public String getScreenStateAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (screenState) {
            case SCREEN_ON:
                return context.getString(R.string.screen_state_on);
            case SCREEN_OFF:
                return context.getString(R.string.screen_state_off);
            case UNKNOWN:
            default:
                return context.getString(R.string.screen_state_unknown);
        }
    }

    public String getGpsStateAsString() {
        Context context = PowerStatsApplication.getAppContext();
        switch (gpsState) {
            case GPS_STATE_ON:
                return context.getString(R.string.gps_state_on);
            case GPS_STATE_OFF:
                return context.getString(R.string.gps_state_off);
            case UNKNOWN:
            default:
                return context.getString(R.string.gps_state_unknown);
        }
    }


    public void clean() {
        dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    private void setDirty() {
        dirty = true;
        if (!readingFromDatabase) {
            updateTimestamp();
        }
    }

    public void setReadingFromDatabase(boolean readingFromDatabase) {
        this.readingFromDatabase = readingFromDatabase;
    }

    public PowerRecord() {
        updateTimestamp();
    }

    protected PowerRecord(PowerRecord p) {
        timestamp = p.timestamp;
        batteryStatus = p.batteryStatus;
        batteryHealth = p.batteryHealth;
        batteryLevel = p.batteryLevel;
        batteryPowerSource = p.batteryPowerSource;
        batteryPresent = p.batteryPresent;
        batteryScale = p.batteryScale;
        batteryTechnology = p.batteryTechnology;
        batteryTemperature = p.batteryTemperature;
        batteryVoltage = p.batteryVoltage;
        phoneServiceState = p.phoneServiceState;
        wifiState = p.wifiState;
        mobileDataState = p.mobileDataState;
        screenState = p.screenState;
        gpsState = p.gpsState;
        dirty = p.dirty;
    }

    public PowerRecord copy() {
        return new PowerRecord(this);
    }


    public boolean isReadyForRecording() {
        return  batteryLevel != UNKNOWN &&
                batteryVoltage != UNKNOWN &&
                batteryTemperature != UNKNOWN &&
                batteryHealth != UNKNOWN &&
                batteryPowerSource != UNKNOWN &&
                batteryStatus != UNKNOWN &&
                batteryScale != UNKNOWN &&
                phoneServiceState != UNKNOWN &&
                wifiState != UNKNOWN &&
                mobileDataState != UNKNOWN &&
                screenState != UNKNOWN &&
                gpsState != UNKNOWN;
    }

    private void updateTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Battery getters and setters //
    // ----------------------------//
    public int getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(int batteryStatus) {
        if (this.batteryStatus != batteryStatus) {
            setDirty();
        }
        this.batteryStatus = batteryStatus;
    }

    public int getBatteryHealth() {
        return batteryHealth;
    }

    public void setBatteryHealth(int batteryHealth) {
        if (this.batteryHealth != batteryHealth) {
            setDirty();
        }
        this.batteryHealth = batteryHealth;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        if (this.batteryLevel != batteryLevel) {
            setDirty();
        }
        this.batteryLevel = batteryLevel;
    }

    public int getBatteryPowerSource() {
        return batteryPowerSource;
    }

    public void setBatteryPowerSource(int batteryPowerSource) {
        if (this.batteryPowerSource != batteryPowerSource) {
            setDirty();
        }
        this.batteryPowerSource = batteryPowerSource;
    }

    public boolean getBatteryPresent() {
        return batteryPresent;
    }

    public void setBatteryPresent(boolean batteryPresent) {
        if (this.batteryPresent != batteryPresent) {
            setDirty();
        }
        this.batteryPresent = batteryPresent;
    }

    public int getBatteryScale() {
        return batteryScale;
    }

    public void setBatteryScale(int batteryScale) {
        if (this.batteryScale != batteryScale) {
            setDirty();
        }

        this.batteryScale = batteryScale;
    }

    public String getBatteryTechnology() {
        return batteryTechnology;
    }

    public void setBatteryTechnology(String batteryTechnology) {
        if (this.batteryTechnology != batteryTechnology) {
            setDirty();
        }
        if (batteryTechnology == null) {
            this.batteryTechnology = DEFAULT_TECHNOLOGY;
        } else {
            this.batteryTechnology = batteryTechnology;
        }
    }

    public int getBatteryTemperature() {
        return batteryTemperature;
    }

    public void setBatteryTemperature(int batteryTemperature) {
        this.batteryTemperature = batteryTemperature;
    }


    public int getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(int batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }


    public float getBatteryValue() {
        if (batteryScale > 0 && batteryLevel >= 0) {
            return 100 * batteryLevel / (float)batteryScale;
        } else {
            return 0;
        }
    }


    public int getWifiState() {
        return wifiState;
    }

    public void setWifiState(int wifiState) {
        if (this.wifiState != wifiState) {
            setDirty();
        }
        this.wifiState = wifiState;
    }

    public int getMobileDataState() {
        return mobileDataState;
    }

    public void setMobileDataState(int mobileDataState) {
        if (this.mobileDataState != mobileDataState) {
            setDirty();
        }
        this.mobileDataState = mobileDataState;
    }

    public int getPhoneServiceState() {
        return phoneServiceState;
    }

    public void setPhoneServiceState(int phoneServiceState) {
        if (this.phoneServiceState != phoneServiceState) {
            setDirty();
        }
        this.phoneServiceState = phoneServiceState;
    }

    public int getScreenState() {
        return screenState;
    }

    public void setScreenState(int screenState) {
        if (this.screenState != screenState) {
            setDirty();
        }
        this.screenState = screenState;
    }

    public int getGpsState() {
        return gpsState;
    }

    public void setGpsState(int gpsState) {
        if (this.gpsState != gpsState) {
            setDirty();
        }
        this.gpsState = gpsState;
    }

}
