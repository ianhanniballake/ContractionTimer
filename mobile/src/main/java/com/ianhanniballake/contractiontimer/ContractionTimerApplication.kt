package com.ianhanniballake.contractiontimer

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import com.ianhanniballake.contractiontimer.strictmode.StrictModeController

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
class ContractionTimerApplication : MultiDexApplication() {
    /**
     * Sets strict mode if we are in debug mode.
     *
     * @see android.app.Application.onCreate
     */
    override fun onCreate() {
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            if (BuildConfig.DEBUG) {
                StrictModeController.createInstance().setStrictMode()
            }
            FirebaseAnalytics.getInstance(this)
                .setUserProperty("debug", BuildConfig.DEBUG.toString())
        }
        super.onCreate()
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(this)
        NotificationUpdateReceiver.updateNotification(this)
    }
}
