package com.znasibov.powerstats.test;


import android.util.Log;

import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;

import java.lang.Exception;
import java.lang.RuntimeException;

public class RobolectricGradleTestRunner extends RobolectricTestRunner {
    public RobolectricGradleTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        String myAppPath = RobolectricGradleTestRunner.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();

        String relativePath = "./../../";
        String manifestPath = myAppPath + relativePath + "src/main/AndroidManifest.xml";
        String resPath = myAppPath + relativePath + "src/main/res";
        String assetPath = myAppPath + relativePath + "src/main/assets";

//        return createAppManifest(Fs.fileFromPath(manifestPath),
//                                 Fs.fileFromPath(resPath),
//                                 Fs.fileFromPath(assetPath));

        return new AndroidManifest(Fs.fileFromPath(manifestPath), Fs.fileFromPath(resPath)) {
            @Override
            public int getTargetSdkVersion() {
                return 18;
            }
        };
    }
}