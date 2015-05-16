package com.ianhanniballake.contractiontimer.ui;

import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.example.android.supportv7.app.AppCompatPreferenceActivity;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {
    /**
     * Analytics preference name
     */
    public static final String ANALYTICS_PREFERENCE_KEY = "analytics";
    /**
     * Appwidget Background preference name
     */
    public static final String APPWIDGET_BACKGROUND_PREFERENCE_KEY = "appwidget_background";
    /**
     * Average Time Frame recently changed for ContractionAverageFragment preference name
     */
    public static final String AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY = "average_time_frame_changed_fragment";
    /**
     * Average Time Frame recently changed for MainActivity preference name
     */
    public static final String AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY = "average_time_frame_changed_main";
    /**
     * Average Time Frame preference name
     */
    public static final String AVERAGE_TIME_FRAME_PREFERENCE_KEY = "average_time_frame";
    /**
     * Keep Screen On preference name
     */
    public static final String KEEP_SCREEN_ON_PREFERENCE_KEY = "keepScreenOn";
    /**
     * Lock Portrait preference name
     */
    public static final String LOCK_PORTRAIT_PREFERENCE_KEY = "lock_portrait";
    /**
     * Notification Enabled preference name
     */
    public static final String NOTIFICATION_ENABLE_PREFERENCE_KEY = "notification_enable";
    private final static String TAG = Preferences.class.getSimpleName();
    /**
     * Reference to the ListPreference corresponding with the Appwidget background
     */
    private ListPreference appwidgetBackgroundListPreference;
    /**
     * Reference to the ListPreference corresponding with the average time frame
     */
    private ListPreference averageTimeFrameListPreference;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_settings);
        appwidgetBackgroundListPreference = (ListPreference) getPreferenceScreen().findPreference(
                Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY);
        appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
        averageTimeFrameListPreference = (ListPreference) getPreferenceScreen().findPreference(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY);
        averageTimeFrameListPreference.setSummary(getString(R.string.pref_average_time_frame_summary) + "\n"
                + averageTimeFrameListPreference.getEntry());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Preferences selected home");
            GtmManager.getInstance(this).pushEvent("Home");
            Intent parentIntent = NavUtils.getParentActivityIntent(this);
            if (NavUtils.shouldUpRecreateTask(this, parentIntent)) {
                NavUtils.navigateUpTo(this, parentIntent);
            } else {
                NavUtils.navigateUpFromSameTask(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_lock_portrait_default));
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
        if (TextUtils.equals(preference.getKey(), "export")) {
            GtmManager.getInstance(this).pushEvent("Export");
        } else if (TextUtils.equals(preference.getKey(), "import")) {
            GtmManager.getInstance(this).pushEvent("Import");
        } else if (TextUtils.equals(preference.getKey(), "license")) {
            GtmManager.getInstance(this).pushEvent("License");
        } else if (TextUtils.equals(preference.getKey(), "about")) {
            GtmManager.getInstance(this).pushEvent("About");
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        GtmManager gtmManager = GtmManager.getInstance(this);
        if (key.equals(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY)) {
            final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_keep_screen_on_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Keep Screen On: " + newIsKeepScreenOn);
            gtmManager.pushPreferenceChanged("Keep Screen On", newIsKeepScreenOn);
        } else if (key.equals(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY)) {
            final boolean newIsLockPortrait = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_lock_portrait_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Lock Portrait: " + newIsLockPortrait);
            gtmManager.pushPreferenceChanged("Lock Portrait", newIsLockPortrait);
            if (newIsLockPortrait)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (key.equals(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY)) {
            final String newAppwidgetBackground = appwidgetBackgroundListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Appwidget Background: " + newAppwidgetBackground);
            gtmManager.pushPreferenceChanged("Appwidget Background", newAppwidgetBackground);
            appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
        } else if (key.equals(Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY)) {
            final boolean newNotifcationEnabled = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_notification_enable_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Notification Enabled: " + newNotifcationEnabled);
            gtmManager.pushPreferenceChanged("Notification Enabled", newNotifcationEnabled);
            NotificationUpdateService.updateNotification(this);
        } else if (key.equals(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY)) {
            final String newAverageTimeFrame = averageTimeFrameListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Average Time Frame: " + newAverageTimeFrame);
            gtmManager.pushPreferenceChanged("Average Time Frame", newAverageTimeFrame);
            final Editor editor = sharedPreferences.edit();
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true);
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true);
            editor.commit();
            averageTimeFrameListPreference.setSummary(getString(R.string.pref_average_time_frame_summary)
                    + "\n" + averageTimeFrameListPreference.getEntry());
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
            NotificationUpdateService.updateNotification(this);
        } else if (key.equals(Preferences.ANALYTICS_PREFERENCE_KEY)) {
            final boolean newCollectAnalytics = sharedPreferences.getBoolean(Preferences.ANALYTICS_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_analytics_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Analytics: " + newCollectAnalytics);
            gtmManager.pushPreferenceChanged("Analytics", newCollectAnalytics);
            gtmManager.push("optOut", newCollectAnalytics);
        }
        new BackupManager(this).dataChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GtmManager.getInstance(this).pushOpenScreen("Preferences");
    }
}
