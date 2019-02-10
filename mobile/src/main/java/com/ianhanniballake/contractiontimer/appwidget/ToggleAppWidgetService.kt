package com.ianhanniballake.contractiontimer.appwidget

import android.app.IntentService
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.closeable
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.Preferences

/**
 * Handles updates of the 'Toggle' style App Widgets
 */
class ToggleAppWidgetService : IntentService(TAG) {
    companion object {
        private const val TAG = "ToggleAppWidgetService"
        /**
         * Identifier for this widget to be used in Analytics
         */
        private const val WIDGET_IDENTIFIER = "widget_toggle"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Toggle App Widgets")
        val projection = arrayOf(BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME)
        val contractionOngoing = contentResolver.query(
                ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null)?.closeable()?.use { data ->
            data.moveToFirst() && data.isNull(data.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME))
        } ?: false
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appwidgetBackground = preferences.getString(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                getString(R.string.pref_appwidget_background_default))
        val views = if (appwidgetBackground == "light")
            RemoteViews(packageName, R.layout.toggle_appwidget_light)
        else
            RemoteViews(packageName, R.layout.toggle_appwidget_dark)
        // Set the status of the contraction toggle button
        val toggleContractionIntent = Intent(this, AppWidgetToggleService::class.java).apply {
            putExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA,
                    ToggleAppWidgetService.WIDGET_IDENTIFIER)
        }
        val toggleContractionPendingIntent = PendingIntent.getService(this, 0, toggleContractionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        if (contractionOngoing) {
            views.setViewVisibility(R.id.contraction_toggle_on, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.contraction_toggle_on, toggleContractionPendingIntent)
            views.setViewVisibility(R.id.contraction_toggle_off, View.GONE)
        } else {
            views.setViewVisibility(R.id.contraction_toggle_off, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.contraction_toggle_off, toggleContractionPendingIntent)
            views.setViewVisibility(R.id.contraction_toggle_on, View.GONE)
        }
        // Update the widgets
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (intent?.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) == true)
            appWidgetManager.updateAppWidget(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS), views)
        else
            appWidgetManager.updateAppWidget(ComponentName(this, ToggleAppWidgetProvider::class.java), views)
    }
}
