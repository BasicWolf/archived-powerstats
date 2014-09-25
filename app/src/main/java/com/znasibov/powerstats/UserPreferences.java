package com.znasibov.powerstats;

import android.content.Context;
import android.content.SharedPreferences;


public class UserPreferences {
    private static final String fileName = "UserPreferences";

    private static volatile UserPreferences instance = null;

    private final SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    boolean editing = false;

    public UserPreferences(Context context) {
        prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        instance = new UserPreferences(context);
    }

    public static boolean getPowerStatsPlotSmoothScrollingEnabled() {
        return instance.prefs.getBoolean("PowerStatsPlotSmoothScrollingEnabled", false);
    }

    public static void setPowerStatsPlotSmoothScrollingEnabled(boolean value) {
        putBoolean("PowerStatsPlotSmoothScrollingEnabled", value);
    }

    public static long getPowerStatsPlotDefaultDomainSize() {
        return instance.prefs.getLong("PowerStatsPlotDefaultDomainSize", Util.hoursToMs(12));
    }

    public static void setPowerStatsPlotDefaultDomainSize(long value) {
        putLong("PowerStatsPlotDefaultDomainSize", value);
    }

    public static void beginEdit() {
        instance.editing = true;
    }

    public static void endEdit() {
        getEditor().commit();
    }

    public static void putLong(String key, long value) {
        getEditor().putLong(key, value);
        if (!instance.editing) {
            getEditor().apply();
        }
    }

    public static void putBoolean(String key, boolean value) {
        getEditor().putBoolean(key, value);
        if (!instance.editing) {
            getEditor().apply();
        }
    }

    private static SharedPreferences.Editor getEditor() {
        if (instance.editor == null) {
            instance.editor = instance.prefs.edit();
        }
        return instance.editor;
    }


}

