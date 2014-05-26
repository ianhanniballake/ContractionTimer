package com.ianhanniballake.contractiontimer.strictmode;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;

/**
 * Sets up the Strict Mode based on Gingerbread policies
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class StrictModeControllerV10 extends StrictModeController {
    @Override
    public void setStrictMode() {
        final StrictMode.ThreadPolicy.Builder threadPolicy = new StrictMode.ThreadPolicy.Builder().detectAll()
                .penaltyLog();
        StrictMode.setThreadPolicy(threadPolicy.build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
    }
}