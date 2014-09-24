package com.znasibov.powerstats;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class PowerStatsDatabase {
    public static final boolean DEBUG_FORCE_RESET_DATABSE = false;

    public static final String DB_NAME = "records.db";
    public static final int DB_VERSION = 3;

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

    SQLiteDatabase mDb;
    String mPath;
    HashMap<Integer, DatabaseUpgrader> dbUpgraders = new HashMap<Integer, DatabaseUpgrader>();

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
        int version = getVersion();
        if (version != DB_VERSION) {
            initUpgraders();
            for (int i = version; i < DB_VERSION; i++) {
                dbUpgraders.get(i).upgrade(this);
            }
        }
    }

    private void initUpgraders() {
        dbUpgraders.put(2, new DatabaseUpgrader2to3());
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
        q.append(COL_SCREEN_STATE + " INTEGER");
        q.append(")");
        mDb.execSQL(q.toString());
    }

    public void insert(PowerRecord powerRecord) {
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, powerRecord.getTimestamp());
        values.put(COL_BATTERY_STATUS, powerRecord.getBatteryStatus());
        values.put(COL_BATTERY_HEALTH, powerRecord.getBatteryHealth());
        values.put(COL_BATTERY_LEVEL, powerRecord.getBatteryLevel());
        values.put(COL_BATTERY_POWER_SOURCE, powerRecord.getBatteryPowerSource());
        values.put(COL_BATTERY_SCALE, powerRecord.getBatteryScale());
        values.put(COL_BATTERY_TEMPERATURE, powerRecord.getBatteryTemperature());
        values.put(COL_BATTERY_VOLTAGE, powerRecord.getBatteryVoltage());
        values.put(COL_PHONE_SERVICE_STATE, powerRecord.getPhoneServiceState());
        values.put(COL_WIFI_STATE, powerRecord.getWifiState());
        values.put(COL_SCREEN_STATE, powerRecord.getScreenState());
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
        PowerRecord p = new PowerRecord();
        p.setReadingFromDatabase(true);
        p.setTimestamp(c.getLong(1));
        p.setBatteryStatus(c.getInt(2));
        p.setBatteryHealth(c.getInt(3));
        p.setBatteryLevel(c.getInt(4));
        p.setBatteryPowerSource(c.getInt(5));
        p.setBatteryScale(c.getInt(6));
        p.setBatteryTemperature(c.getInt(7));
        p.setBatteryVoltage(c.getInt(8));
        p.setPhoneServiceState(c.getInt(9));
        p.setWifiState(c.getInt(10));
        p.setScreenState(c.getInt(11));
        p.setReadingFromDatabase(false);
        return p;
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

interface DatabaseUpgrader {
    public int getNewVersion ();
    public void upgrade(PowerStatsDatabase db);
}


class DatabaseUpgrader2to3 implements DatabaseUpgrader {
    @Override
    public int getNewVersion () {
        return 3;
    }

    @Override
    public void upgrade(PowerStatsDatabase db) {
        StringBuilder q = new StringBuilder();
        q.append("ALTER TABLE " + db.TABLE_RECORDS + " ");
        q.append(" ADD COLUMN " + db.COL_SCREEN_STATE + " INTEGER");
        db.getDb().execSQL(q.toString());
        db.setProperty(db.PROPERTY_VERSION, "" + getNewVersion());
    }
}