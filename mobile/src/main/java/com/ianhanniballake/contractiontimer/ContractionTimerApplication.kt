package com.ianhanniballake.contractiontimer

import android.app.Application

import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.strictmode.StrictModeController

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
class ContractionTimerApplication : Application() {
    /**
     * Sets strict mode if we are in debug mode.
     *
     * @see android.app.Application.onCreate
     */
    override fun onCreate() {
        if (!FirebaseApp.getApps(this).isEmpty()) {
            if (BuildConfig.DEBUG) {
                StrictModeController.createInstance().setStrictMode()
            }
            FirebaseAnalytics.getInstance(this)
                    .setUserProperty("debug", BuildConfig.DEBUG.toString())
        }
        super.onCreate()
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(this)
        NotificationUpdateService.updateNotification(this)
    }
}
