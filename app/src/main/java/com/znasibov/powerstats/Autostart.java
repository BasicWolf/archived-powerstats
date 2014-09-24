package com.znasibov.powerstats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class Autostart extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent)
    {
        Intent blIntent = new Intent(context, PowerStatsLoggerService.class);
        context.startService(blIntent);
        Log.i("Autostart", "started");
    }
}
