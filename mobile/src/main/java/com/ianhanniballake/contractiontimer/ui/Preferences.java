package com.ianhanniballake.contractiontimer.ui;

import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_preferences);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Preferences selected home");
            Intent parentIntent = NavUtils.getParentActivityIntent(this);
            if (NavUtils.shouldUpRecreateTask(this, parentIntent)) {
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(parentIntent)
                        .startActivities();
            } else {
                NavUtils.navigateUpFromSameTask(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        /**
         * Reference to the ListPreference corresponding with the Appwidget background
         */
        private ListPreference appwidgetBackgroundListPreference;
        /**
         * Reference to the ListPreference corresponding with the average time frame
         */
        private ListPreference averageTimeFrameListPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
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
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                    .getBoolean(R.bool.pref_lock_portrait_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Lock Portrait: " + isLockPortrait);
            if (isLockPortrait)
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (TextUtils.equals(preference.getKey(), "export")) {
                FirebaseAnalytics.getInstance(getContext()).logEvent("export_open", null);
            } else if (TextUtils.equals(preference.getKey(), "import")) {
                FirebaseAnalytics.getInstance(getContext()).logEvent("import_open", null);
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
            Bundle bundle = new Bundle();
            switch (key) {
                case Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY:
                    final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                            getResources().getBoolean(R.bool.pref_keep_screen_on_default));
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Keep Screen On: " + newIsKeepScreenOn);
                    bundle.putString(FirebaseAnalytics.Param.VALUE, Boolean.toString(newIsKeepScreenOn));
                    analytics.logEvent("preference_keep_screen_on", bundle);
                    break;
                case Preferences.LOCK_PORTRAIT_PREFERENCE_KEY:
                    final boolean newIsLockPortrait = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                            getResources().getBoolean(R.bool.pref_lock_portrait_default));
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Lock Portrait: " + newIsLockPortrait);
                    bundle.putString(FirebaseAnalytics.Param.VALUE, Boolean.toString(newIsLockPortrait));
                    analytics.logEvent("preference_lock_portrait", bundle);
                    if (newIsLockPortrait)
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    else
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    break;
                case Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY:
                    final String newAppwidgetBackground = appwidgetBackgroundListPreference.getValue();
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Appwidget Background: " + newAppwidgetBackground);
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAppwidgetBackground);
                    analytics.logEvent("preference_widget_background", bundle);
                    appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(getContext());
                    break;
                case Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY:
                    final boolean newNotifcationEnabled = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                            getResources().getBoolean(R.bool.pref_notification_enable_default));
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Notification Enabled: " + newNotifcationEnabled);
                    bundle.putString(FirebaseAnalytics.Param.VALUE, Boolean.toString(newNotifcationEnabled));
                    analytics.logEvent("preference_notification_enabled", bundle);
                    NotificationUpdateService.updateNotification(getContext());
                    break;
                case Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY:
                    final String newAverageTimeFrame = averageTimeFrameListPreference.getValue();
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Average Time Frame: " + newAverageTimeFrame);
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAverageTimeFrame);
                    analytics.logEvent("preference_average_time_frame", bundle);
                    final SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true);
                    editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true);
                    editor.commit();
                    averageTimeFrameListPreference.setSummary(getString(R.string.pref_average_time_frame_summary)
                            + "\n" + averageTimeFrameListPreference.getEntry());
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(getContext());
                    NotificationUpdateService.updateNotification(getContext());
                    break;
            }
            new BackupManager(getContext()).dataChanged();
        }

        @Override
        public void onStart() {
            super.onStart();
            FirebaseAnalytics.getInstance(getContext()).logEvent("preferences_open", null);
        }
    }
}
