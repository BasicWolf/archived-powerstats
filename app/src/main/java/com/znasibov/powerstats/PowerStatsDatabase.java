package com.znasibov.powerstats;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;

public class PowerStatsDatabase {
    /** Database configuration **/
    public static final int DB_VERSION = 5;
    public static final String DB_NAME = "records.db";

    /** Control flags **/
    public static final boolean DEBUG_FORCE_RESET_DATABSE = false;


    /** Constants **/
    static final String TABLE_RECORDS = "records";
    static final String TABLE_PROPERTIES = "properties";

    static final String PROPERTY_VERSION = "version";

    static final String COL_KEY = "key";
    static final String COL_VALUE = "value";

    static final String COL_ID = "id";
    static final String COL_TIMESTAMP = "timestamp";

    static final String COL_BATTERY_STATUS = "battery_status";
    static final String COL_BATTERY_HEALTH = "battery_health";
    static final String COL_BATTERY_LEVEL = "battery_level";
    static final String COL_BATTERY_POWER_SOURCE = "battery_power_source";
    static final String COL_BATTERY_SCALE = "battery_scale";
    static final String COL_BATTERY_TEMPERATURE = "batery_temperature";
    static final String COL_BATTERY_VOLTAGE = "battery_voltage";

    static final String COL_PHONE_SERVICE_STATE = "phone_service_state";
    static final String COL_WIFI_STATE = "wifi_state";
    static final String COL_SCREEN_STATE = "screen_state";
    static final String COL_GPS_STATE = "gps_state";
    static final String COL_MOBILE_DATA_STATE = "mobile_data_state";

    /** Locals **/
    SQLiteDatabase mDb;
    String mPath;
    HashSet<DatabaseUpgrader> dbUpgraders = new HashSet<DatabaseUpgrader>();

    public PowerStatsDatabase(String path) {
        createOrOpen(path);
        upgradeIfNecessary();
    }

    public PowerStatsDatabase(String path, String suffix) {
        mPath = (new File(path + "_" + suffix)).getAbsolutePath().toString();
        createOrOpen(path);
    }

    public void createOrOpen(String path) {
        File dbFile = new File(path);
        dbFile.getParentFile().mkdirs();
        mPath = dbFile.getAbsolutePath().toString();

        if (exists() && !DEBUG_FORCE_RESET_DATABSE) {
            open();
        } else {
            create();
        }
    }

    public void open() {
        mDb = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private void upgradeIfNecessary() {
        if (getVersion() == DB_VERSION) {
            return;
        }

        initUpgraders();
        for (DatabaseUpgrader du: dbUpgraders) {
            if (du.canUpgradeTo(DB_VERSION)) {
                du.upgrade(this);
            }
        }
    }

    private void initUpgraders() {
        // dbUpgraders.put(3, new DatabaseUpgrader3to4());
        dbUpgraders.add(new DatabaseUpgrader4to5());
    }

    public void create() {
        createDatabaseFile();
        createPropertiesTable();
        createRecordsTable();
    }

    private void createDatabaseFile() {
        remove();
        (new File(mPath)).getParentFile().mkdirs();
        mDb = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    private void createPropertiesTable() {
        StringBuilder q = new StringBuilder();
        q.append("CREATE TABLE " + TABLE_PROPERTIES + " (");
        q.append(COL_KEY + " TEXT PRIMARY KEY, ");
        q.append(COL_VALUE + " TEXT");
        q.append(")");
        mDb.execSQL(q.toString());

        setProperty(PROPERTY_VERSION, "" + DB_VERSION);
    }

    private void createRecordsTable() {
        StringBuilder q = new StringBuilder();
        q.append("CREATE TABLE " + TABLE_RECORDS + " (");
        q.append(COL_ID + " INTEGER PRIMARY KEY, ");
        q.append(COL_TIMESTAMP + " INTEGER, ");
        q.append(COL_BATTERY_STATUS + " INTEGER, ");
        q.append(COL_BATTERY_HEALTH + " INTEGER, ");
        q.append(COL_BATTERY_LEVEL + " INTEGER, ");
        q.append(COL_BATTERY_POWER_SOURCE + " INTEGER, ");
        q.append(COL_BATTERY_SCALE + " INTEGER, ");
        q.append(COL_BATTERY_TEMPERATURE + " INTEGER, ");
        q.append(COL_BATTERY_VOLTAGE + " INTEGER, ");
        q.append(COL_PHONE_SERVICE_STATE + " INTEGER, ");
        q.append(COL_WIFI_STATE + " INTEGER, ");
        q.append(COL_SCREEN_STATE + " INTEGER, ");
        q.append(COL_GPS_STATE + " INTEGER, ");
        q.append(COL_MOBILE_DATA_STATE + " INTEGER");
        q.append(")");
        mDb.execSQL(q.toString());
    }

    public void insert(PowerRecord r) {
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, r.getTimestamp());
        values.put(COL_BATTERY_STATUS, r.getBatteryStatus());
        values.put(COL_BATTERY_HEALTH, r.getBatteryHealth());
        values.put(COL_BATTERY_LEVEL, r.getBatteryLevel());
        values.put(COL_BATTERY_POWER_SOURCE, r.getBatteryPowerSource());
        values.put(COL_BATTERY_SCALE, r.getBatteryScale());
        values.put(COL_BATTERY_TEMPERATURE, r.getBatteryTemperature());
        values.put(COL_BATTERY_VOLTAGE, r.getBatteryVoltage());
        values.put(COL_PHONE_SERVICE_STATE, r.getPhoneServiceState());
        values.put(COL_WIFI_STATE, r.getWifiState());
        values.put(COL_SCREEN_STATE, r.getScreenState());
        values.put(COL_GPS_STATE, r.getGpsState());
        values.put(COL_MOBILE_DATA_STATE, r.getMobileDataState());
        mDb.insert(TABLE_RECORDS, null, values);
    }

    public void setProperty(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(COL_KEY, key);
        values.put(COL_VALUE, value);
        mDb.replace(TABLE_PROPERTIES, null, values);
    }

    public String getProperty (String key) {
        String query = MessageFormat.format(
                "SELECT {0} FROM {1} WHERE {2} = \"{3}\"",
                COL_VALUE, TABLE_PROPERTIES, COL_KEY, key);
        Cursor cursor = mDb.rawQuery(query, null);
        cursor.moveToFirst();
        return cursor.getString(0);
    }

    public ArrayList<PowerRecord> getAll() {
        Cursor cursor = mDb.query(TABLE_RECORDS, null, null, null, null, null, null);
        return fromCursor(cursor);
    }

    private ArrayList<PowerRecord> fromCursor(Cursor cursor) {
        ArrayList<PowerRecord> powerRecords = new ArrayList<PowerRecord>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                PowerRecord powerRecord = cursorToBatteryInfo(cursor);
                powerRecords.add(powerRecord);
            }
        }
        return powerRecords;
    }

    public ArrayList<PowerRecord> getInPeriod(long from, long to, boolean attachBoundaryRecord) {
        String query = "";

        if (attachBoundaryRecord) {
            query = MessageFormat.format(
                    "SELECT * FROM (SELECT * FROM {0} WHERE " +
                    "{1} < {2,number,#} ORDER BY {1} DESC LIMIT 1)",
                    TABLE_RECORDS, COL_TIMESTAMP, from);
            query += " UNION ";
        }

        query += MessageFormat.format(
                "SELECT * FROM {0} WHERE {1} >= {2,number,#} AND {1} <= {3,number,#}",
                TABLE_RECORDS, COL_TIMESTAMP, from, to);


        // query += " ORDER BY " + COL_TIMESTAMP;

        Cursor cursor = mDb.rawQuery(query, null);
        return fromCursor(cursor);
    }

    private PowerRecord cursorToBatteryInfo(Cursor c) {
        PowerRecord r = new PowerRecord();
        r.setReadingFromDatabase(true);
        r.setTimestamp(c.getLong(1));
        r.setBatteryStatus(c.getInt(2));
        r.setBatteryHealth(c.getInt(3));
        r.setBatteryLevel(c.getInt(4));
        r.setBatteryPowerSource(c.getInt(5));
        r.setBatteryScale(c.getInt(6));
        r.setBatteryTemperature(c.getInt(7));
        r.setBatteryVoltage(c.getInt(8));
        r.setPhoneServiceState(c.getInt(9));
        r.setWifiState(c.getInt(10));
        r.setScreenState(c.getInt(11));
        r.setGpsState(c.getInt(12));
        r.setMobileDataState(c.getInt(13));
        r.setReadingFromDatabase(false);
        return r;
    }

    public boolean exists() {
        return (new File(mPath)).exists();
    }

    public void close() {
        if (mDb != null) {
            mDb.close();
        }
    }

    public void remove() {
        close();
        (new File(mPath)).delete();
    }

    public SQLiteDatabase getDb() {
        return mDb;
    }

    public int getVersion() {
        return Integer.parseInt(getProperty(PROPERTY_VERSION));
    }

}

abstract class DatabaseUpgrader {
    abstract public int getVersion();
    abstract public void upgrade(PowerStatsDatabase db);

    public boolean canUpgradeTo(int version) {
        return getVersion() == version;
    }
}

class DatabaseUpgrader4to5 extends DatabaseUpgrader {
    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public void upgrade(PowerStatsDatabase db) {
        StringBuilder q = new StringBuilder();
        q.append("ALTER TABLE " + db.TABLE_RECORDS + " ");
        q.append(" ADD COLUMN " + db.COL_MOBILE_DATA_STATE + " INTEGER");
        db.getDb().execSQL(q.toString());
        db.setProperty(db.PROPERTY_VERSION, "" + getVersion());
    }
}

//class DatabaseUpgrader3to4 implements DatabaseUpgrader {
//
//    @Override
//    public int getVersion() {
//        return 4;
//    }
//
//    @Override
//    public void upgrade(PowerStatsDatabase db) {
//        StringBuilder q = new StringBuilder();
//        q.append("ALTER TABLE " + db.TABLE_RECORDS + " ");
//        q.append(" ADD COLUMN " + db.COL_GPS_STATE + " INTEGER");
//        db.getDb().execSQL(q.toString());
//        db.setProperty(db.PROPERTY_VERSION, "" + getVersion());
//    }
//}

