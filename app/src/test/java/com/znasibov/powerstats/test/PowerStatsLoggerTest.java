package com.znasibov.powerstats.test;

// see
// https://github.com/appdroid/Appy/blob/f0d29e286d5622d325d490a1598eba4f34851fb9/Appy/test/com/appy/services/AppInfoTrackerTest.java

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.znasibov.powerstats.PowerStatsLoggerService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.*;


@RunWith(RobolectricGradleTestRunner.class)
public class PowerStatsLoggerTest {

    @Test
    public void testServiceStartedByApplication() {
        Application app = Robolectric.application;
        Context context = app.getApplicationContext();
        context.startService(new Intent(context, PowerStatsLoggerService.class));
        ShadowApplication shadowApp = (ShadowApplication)Robolectric.shadowOf(app);
        String serviceName = shadowApp.peekNextStartedService().getComponent().getClassName();

        assertEquals("com.znasibov.powerstats.PowerStatsLoggerService", serviceName);
    }
}
