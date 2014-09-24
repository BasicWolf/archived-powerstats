package com.znasibov.powerstats;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import net.danlew.android.joda.JodaTimeAndroid;


public class PowerStatsApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerStatsApplication.context = getApplicationContext();

        UserPreferences.init(getAppContext());

        JodaTimeAndroid.init(this);
        startService(new Intent(getApplicationContext(), PowerStatsLoggerService.class));
    }

    public static Context getAppContext() {
        return PowerStatsApplication.context;
    }

}
