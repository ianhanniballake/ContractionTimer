package com.ianhanniballake.contractiontimer.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.extensions.closeable
import com.ianhanniballake.contractiontimer.extensions.goAsync
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles updates of the 'Toggle' style App Widgets
 */
class ToggleAppWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ToggleAppWidgetProvider"
        /**
         * Identifier for this widget to be used in Analytics
         */
        private const val WIDGET_IDENTIFIER = "widget_toggle"

        internal suspend fun updateToggleAppWidget(
                context: Context,
                appWidgetIds: IntArray? = null
        ) = withContext(Dispatchers.IO) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Updating Toggle App Widgets")
            val projection = arrayOf(BaseColumns._ID,
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            val contractionOngoing = context.contentResolver.query(
                    ContractionContract.Contractions.CONTENT_URI, projection,
                    null, null, null)?.closeable()?.use { data ->
                data.moveToFirst() && data.isNull(data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME))
            } ?: false
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val appwidgetBackground = preferences.getString(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                    context.getString(R.string.pref_appwidget_background_default))
            val views = if (appwidgetBackground == "light")
                RemoteViews(context.packageName, R.layout.toggle_appwidget_light)
            else
                RemoteViews(context.packageName, R.layout.toggle_appwidget_dark)
            // Set the status of the contraction toggle button
            val toggleContractionIntent = Intent(context, AppWidgetToggleReceiver::class.java).apply {
                putExtra(AppWidgetToggleReceiver.WIDGET_NAME_EXTRA, WIDGET_IDENTIFIER)
            }
            val toggleContractionPendingIntent = PendingIntent.getBroadcast(context, 0,
                    toggleContractionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetIds != null)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            else
                appWidgetManager.updateAppWidget(
                        ComponentName(context, ToggleAppWidgetProvider::class.java), views)
        }
    }

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) = goAsync {
        updateToggleAppWidget(context, appWidgetIds)
    }
}
