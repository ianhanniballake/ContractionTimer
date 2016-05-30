package com.ianhanniballake.contractiontimer;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.strictmode.StrictModeController;

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
public class ContractionTimerApplication extends Application {
    /**
     * Sets strict mode if we are in debug mode.
     *
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            StrictModeController.createInstance().setStrictMode();
        }
        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseAnalytics.getInstance(this).setUserProperty("debug", Boolean.toString(BuildConfig.DEBUG));
        }
        super.onCreate();
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
        NotificationUpdateService.updateNotification(this);
    }
}