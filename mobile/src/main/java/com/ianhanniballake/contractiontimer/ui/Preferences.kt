package com.ianhanniballake.contractiontimer.ui

import android.app.backup.BackupManager
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver

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
            val parentIntent = NavUtils.getParentActivityIntent(this)!!
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
                APPWIDGET_BACKGROUND_PREFERENCE_KEY
            )!!
            appwidgetBackgroundListPreference.summary = appwidgetBackgroundListPreference.entry
            averageTimeFrameListPreference = preferenceScreen.findPreference(
                AVERAGE_TIME_FRAME_PREFERENCE_KEY
            )!!
            averageTimeFrameListPreference.summary = (getString(R.string.pref_average_time_frame_summary) + "\n"
                    + averageTimeFrameListPreference.entry)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val isLockPortrait = preferences.getBoolean(
                LOCK_PORTRAIT_PREFERENCE_KEY,
                resources.getBoolean(R.bool.pref_lock_portrait_default)
            )
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Lock Portrait: $isLockPortrait")
            requireActivity().requestedOrientation = if (isLockPortrait)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "export" -> FirebaseAnalytics.getInstance(requireContext())
                    .logEvent("export_open", null)
                "import" -> FirebaseAnalytics.getInstance(requireContext())
                    .logEvent("import_open", null)
            }
            return super.onPreferenceTreeClick(preference)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            val analytics = FirebaseAnalytics.getInstance(requireContext())
            val bundle = Bundle()
            when (key) {
                KEEP_SCREEN_ON_PREFERENCE_KEY -> {
                    val newIsKeepScreenOn = sharedPreferences.getBoolean(
                        KEEP_SCREEN_ON_PREFERENCE_KEY,
                        resources.getBoolean(R.bool.pref_keep_screen_on_default)
                    )
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Keep Screen On: $newIsKeepScreenOn")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newIsKeepScreenOn.toString())
                    analytics.logEvent("preference_keep_screen_on", bundle)
                }
                LOCK_PORTRAIT_PREFERENCE_KEY -> {
                    val newIsLockPortrait = sharedPreferences.getBoolean(
                        LOCK_PORTRAIT_PREFERENCE_KEY,
                        resources.getBoolean(R.bool.pref_lock_portrait_default)
                    )
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Lock Portrait: $newIsLockPortrait")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newIsLockPortrait.toString())
                    analytics.logEvent("preference_lock_portrait", bundle)
                    requireActivity().requestedOrientation = if (newIsLockPortrait)
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                APPWIDGET_BACKGROUND_PREFERENCE_KEY -> {
                    val newAppwidgetBackground = appwidgetBackgroundListPreference.value
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Appwidget Background: $newAppwidgetBackground")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAppwidgetBackground)
                    analytics.logEvent("preference_widget_background", bundle)
                    appwidgetBackgroundListPreference.summary =
                        appwidgetBackgroundListPreference.entry
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(requireContext())
                }
                NOTIFICATION_ENABLE_PREFERENCE_KEY -> {
                    val newNotifcationEnabled = sharedPreferences.getBoolean(
                        LOCK_PORTRAIT_PREFERENCE_KEY,
                        resources.getBoolean(R.bool.pref_notification_enable_default)
                    )
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Notification Enabled: $newNotifcationEnabled")
                    bundle.putString(
                        FirebaseAnalytics.Param.VALUE,
                        newNotifcationEnabled.toString()
                    )
                    analytics.logEvent("preference_notification_enabled", bundle)
                    NotificationUpdateReceiver.updateNotification(requireContext())
                }
                AVERAGE_TIME_FRAME_PREFERENCE_KEY -> {
                    val newAverageTimeFrame = averageTimeFrameListPreference.value
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Average Time Frame: $newAverageTimeFrame")
                    bundle.putString(FirebaseAnalytics.Param.VALUE, newAverageTimeFrame)
                    analytics.logEvent("preference_average_time_frame", bundle)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean(AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true)
                    editor.apply()
                    averageTimeFrameListPreference.summary =
                        (getString(R.string.pref_average_time_frame_summary)
                                + "\n" + averageTimeFrameListPreference.entry)
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(requireContext())
                    NotificationUpdateReceiver.updateNotification(requireContext())
                }
            }
            BackupManager(context).dataChanged()
        }

        override fun onStart() {
            super.onStart()
            FirebaseAnalytics.getInstance(requireContext()).logEvent("preferences_open", null)
        }
    }
}
