package com.ianhanniballake.contractiontimer.strictmode

import android.annotation.TargetApi
import android.os.Build
import android.os.StrictMode

/**
 * Sets up the Strict Mode based on Gingerbread policies
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
class StrictModeControllerV10 : StrictModeController() {
    override fun setStrictMode() {
        val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll()
                .penaltyLog()
        StrictMode.setThreadPolicy(threadPolicy.build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }
}
