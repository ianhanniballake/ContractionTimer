package com.ianhanniballake.contractiontimer.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.RemoteViews

import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.extensions.closeable
import com.ianhanniballake.contractiontimer.extensions.goAsync
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.MainActivity
import com.ianhanniballake.contractiontimer.ui.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles updates of the 'Control' style App Widgets
 */
class ControlAppWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "CtrlAppWidgetProvider"
        /**
         * Identifier for this widget to be used in Analytics
         */
        private const val WIDGET_IDENTIFIER = "widget_control"

        internal suspend fun updateControlAppWidget(
                context: Context,
                appWidgetIds: IntArray? = null
        ) = withContext(Dispatchers.IO) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Updating Control App Widgets")
            val projection = arrayOf(BaseColumns._ID,
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)

            val appwidgetBackground = preferences.getString(
                    Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                    context.getString(R.string.pref_appwidget_background_default))
            val views = if (appwidgetBackground == "light")
                RemoteViews(context.packageName, R.layout.control_appwidget_light)
            else
                RemoteViews(context.packageName, R.layout.control_appwidget_dark)
            // Add the intent to the Application Launch button
            val applicationLaunchIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, WIDGET_IDENTIFIER)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val applicationLaunchPendingIntent = PendingIntent.getActivity(context, 0,
                    applicationLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.application_launch, applicationLaunchPendingIntent)

            val averagesTimeFrame = preferences.getString(
                    Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                    context.getString(R.string.pref_average_time_frame_default))!!.toLong()
            val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
            val selectionArgs = arrayOf(timeCutoff.toString())
            context.contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection,
                    selection, selectionArgs, null)?.closeable()?.use { data ->
                // Set the average duration and frequency
                if (data.moveToFirst()) {
                    var averageDuration = 0.0
                    var averageFrequency = 0.0
                    var numDurations = 0
                    var numFrequencies = 0
                    while (!data.isAfterLast) {
                        val startTimeColumnIndex = data
                                .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                        val startTime = data.getLong(startTimeColumnIndex)
                        val endTimeColumnIndex = data
                                .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME)
                        if (!data.isNull(endTimeColumnIndex)) {
                            val endTime = data.getLong(endTimeColumnIndex)
                            val curDuration = endTime - startTime
                            averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1)
                            numDurations++
                        }
                        if (data.moveToNext()) {
                            val prevContractionStartTimeColumnIndex = data
                                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                            val prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex)
                            val curFrequency = startTime - prevContractionStartTime
                            averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1)
                            numFrequencies++
                        }
                    }
                    val averageDurationInSeconds = (averageDuration / 1000).toLong()
                    views.setTextViewText(R.id.average_duration, DateUtils.formatElapsedTime(averageDurationInSeconds))
                    val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
                    views.setTextViewText(R.id.average_frequency, DateUtils.formatElapsedTime(averageFrequencyInSeconds))
                } else {
                    views.setTextViewText(R.id.average_duration, "")
                    views.setTextViewText(R.id.average_frequency, "")
                }
            }

            // Set the status of the contraction toggle button
            // Need to use a separate cursor as there could be running contractions
            // outside of the average time frame
            context.contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection,
                    null, null, null)?.closeable()?.use { allData ->
                val contractionOngoing = (allData.moveToFirst() &&
                        allData.isNull(allData.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME)))
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
            }

            // Update the widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetIds != null)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            else
                appWidgetManager.updateAppWidget(ComponentName(context, ControlAppWidgetProvider::class.java), views)
        }
    }

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) = goAsync {
        updateControlAppWidget(context, appWidgetIds)
    }
}
