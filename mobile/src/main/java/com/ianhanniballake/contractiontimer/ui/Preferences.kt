package com.ianhanniballake.contractiontimer.ui

import android.app.backup.BackupManager
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService

/**
 * Activity managing the various application preferences
 */
class Preferences : AppCompatActivity() {
    companion object {
        private const val TAG = "Preferences"
        /**
         * Appwidget Background preference name
         */
        const val APPWIDGET_BACKGROUND_PREFERENCE_KEY = "appwidget_background"
        /**
         * Average Time Frame recently changed for ContractionAverageFragment preference name
         */
        const val AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY = "average_time_frame_changed_fragment"
        /**
         * Average Time Frame recently changed for MainActivity preference name
         */
        const val AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY = "average_time_frame_changed_main"
        /**
         * Average Time Frame preference name
         */
        const val AVERAGE_TIME_FRAME_PREFERENCE_KEY = "average_time_frame"
        /**
         * Keep Screen On preference name
         */
        const val KEEP_SCREEN_ON_PREFERENCE_KEY = "keepScreenOn"
        /**
         * Lock Portrait preference name
         */
        const val LOCK_PORTRAIT_PREFERENCE_KEY = "lock_portrait"
        /**
         * Notification Enabled preference name
         */
        const val NOTIFICATION_ENABLE_PREFERENCE_KEY = "notification_enable"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setHomeButtonEnabled(true)
        setContentView(R.layout.activity_preferences)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Preferences selected home")
            val parentIntent = NavUtils.getParentActivityIntent(this)
            if (NavUtils.shouldUpRecreateTask(this, parentIntent)) {
                TaskStackBuilder.create(this)
                        .addNextIntentWithParentStack(parentIntent)
                        .startActivities()
            } else {
                NavUtils.navigateUpFromSameTask(this)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class PreferencesFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        /**
         * Reference to the ListPreference corresponding with the Appwidget background
         */
        private lateinit var appwidgetBackgroundListPreference: ListPreference
        /**
         * Reference to the ListPreference corresponding with the average time frame
         */
        private lateinit var averageTimeFrameListPreference: ListPreference

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences_settings)
            appwidgetBackgroundListPreference = preferenceScreen.findPreference(
                    Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY) as ListPreference
            appwidgetBackgroundListPreference.summary = appwidgetBackgroundListPreference.entry
            averageTimeFrameListPreference = preferenceScreen.findPreference(
                    Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY) as ListPreference
            averageTimeFrameListPreference.summary = (getString(R.string.pref_average_time_frame_summary) + "\n"
                    + averageTimeFrameListPreference.entry)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    resources.getBoolean(R.bool.pref_lock_portrait_default))
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Lock Portrait: $isLockPortrait")
            activity.requestedOrientation = if (isLockPortrait)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "export" -> FirebaseAnalytics.getInstance(context).logEvent("export_open", null)
                "import" -> FirebaseAnalytics.getInstance(context).logEvent("import_open", null)
            }
            return super.onPreferenceTreeClick(preference)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle()
            when (key) {
                Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                    val newIsKeepScreenOn = sharedPreferences.getBoolean(
                            Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                            resources.getBoolean(R.bool.pref_keep_screen_on_default))
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Keep Screen On: $newIsKeepScreenOn")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newIsKeepScreenOn.toString())
                    analytics.logEvent("preference_keep_screen_on", bundle)
                }
                Preferences.LOCK_PORTRAIT_PREFERENCE_KEY -> {
                    val newIsLockPortrait = sharedPreferences.getBoolean(
                            Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                            resources.getBoolean(R.bool.pref_lock_portrait_default))
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Lock Portrait: $newIsLockPortrait")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newIsLockPortrait.toString())
                    analytics.logEvent("preference_lock_portrait", bundle)
                    activity.requestedOrientation = if (newIsLockPortrait)
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY -> {
                    val newAppwidgetBackground = appwidgetBackgroundListPreference.value
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Appwidget Background: $newAppwidgetBackground")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAppwidgetBackground)
                    analytics.logEvent("preference_widget_background", bundle)
                    appwidgetBackgroundListPreference.summary = appwidgetBackgroundListPreference.entry
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                }
                Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY -> {
                    val newNotifcationEnabled = sharedPreferences.getBoolean(
                            Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                            resources.getBoolean(R.bool.pref_notification_enable_default))
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Notification Enabled: $newNotifcationEnabled")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newNotifcationEnabled.toString())
                    analytics.logEvent("preference_notification_enabled", bundle)
                    NotificationUpdateService.updateNotification(context)
                }
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY -> {
                    val newAverageTimeFrame = averageTimeFrameListPreference.value
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Average Time Frame: $newAverageTimeFrame")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAverageTimeFrame)
                    analytics.logEvent("preference_average_time_frame", bundle)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true)
                    editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true)
                    editor.apply()
                    averageTimeFrameListPreference.summary = (getString(R.string.pref_average_time_frame_summary)
                            + "\n" + averageTimeFrameListPreference.entry)
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                    NotificationUpdateService.updateNotification(context)
                }
            }
            BackupManager(context).dataChanged()
        }

        override fun onStart() {
            super.onStart()
            FirebaseAnalytics.getInstance(context).logEvent("preferences_open", null)
        }
    }
}
