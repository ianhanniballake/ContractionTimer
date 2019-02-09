package com.ianhanniballake.contractiontimer.appwidget

import android.annotation.TargetApi
import android.app.IntentService
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.support.v4.app.TaskStackBuilder
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.MainActivity
import com.ianhanniballake.contractiontimer.ui.Preferences
import com.ianhanniballake.contractiontimer.ui.ViewActivity

/**
 * Handles updates of the 'Detail' style App Widgets
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class DetailAppWidgetService : IntentService(TAG) {
    companion object {
        private const val TAG = "DetailAppWidgetProvider"
        /**
         * Identifier for this widget to be used in Analytics
         */
        private const val WIDGET_IDENTIFIER = "widget_detail"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Detail App Widgets")
        val projection = arrayOf(BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME, ContractionContract.Contractions.COLUMN_NAME_END_TIME)
        val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appwidgetBackground = preferences.getString(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                getString(R.string.pref_appwidget_background_default))
        val averagesTimeFrame = preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default))?.toLong() ?: return
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        val selectionArgs = arrayOf(java.lang.Long.toString(timeCutoff))
        // Set the average duration and frequency
        var formattedAverageDuration = ""
        var formattedAverageFrequency = ""
        contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection,
                selection, selectionArgs, null)?.use { data ->
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
                formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds)
                val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
                formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds)
            }
        }
        // Determine whether a contraction is currently ongoing
        // Need to use a separate cursor as there could be running contractions
        // outside of the average time frame
        val contractionOngoing = contentResolver.query(ContractionContract.Contractions.CONTENT_URI,
                projection, null, null, null)?.use { allData ->
            allData.moveToFirst() && allData.isNull(allData.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME))
        } ?: false
        // Get the list of app widgets to update
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val detailAppWidgetIds = if (intent?.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) == true)
            intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        else
            appWidgetManager.getAppWidgetIds(ComponentName(this, DetailAppWidgetProvider::class.java))
        // Build and update the views for each widget
        // We need to do it widget by widget to allow changes for keyguard vs
        // home screen widgets
        for (appWidgetId in detailAppWidgetIds) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Updating detail widget with id $appWidgetId")
            // Note that all widgets share the same theme
            val views = if (appwidgetBackground == "light")
                RemoteViews(packageName, R.layout.detail_appwidget_light)
            else
                RemoteViews(packageName, R.layout.detail_appwidget_dark)
            // Add the intent to the Application Launch button
            val applicationLaunchIntent = Intent(this, MainActivity::class.java)
            applicationLaunchIntent.putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, WIDGET_IDENTIFIER)
            applicationLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val applicationLaunchPendingIntent = PendingIntent.getActivity(this, 0,
                    applicationLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.application_launch, applicationLaunchPendingIntent)
            // Add in the averages
            views.setTextViewText(R.id.average_duration, formattedAverageDuration)
            views.setTextViewText(R.id.average_frequency, formattedAverageFrequency)
            // Add the intent for the toggle button
            val toggleContractionIntent = Intent(this, AppWidgetToggleService::class.java).apply {
                putExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA, WIDGET_IDENTIFIER)
            }
            val toggleContractionPendingIntent = PendingIntent.getService(this, 0,
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
            // Set up the collection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                setRemoteAdapter(views)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                setRemoteAdapterV11(views)
            val clickIntentTemplate = Intent(Intent.ACTION_VIEW)
                    .setComponent(ComponentName(this, ViewActivity::class.java)).apply {
                        putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, WIDGET_IDENTIFIER)
                    }
            val clickPendingIntentTemplate = TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setPendingIntentTemplate(R.id.list_view, clickPendingIntentTemplate)
            views.setEmptyView(R.id.list_view, R.id.empty_view)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private fun setRemoteAdapter(views: RemoteViews) {
        views.setRemoteAdapter(R.id.list_view, Intent(this,
                DetailAppWidgetRemoteViewsService::class.java))
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @Suppress("DEPRECATION")
    private fun setRemoteAdapterV11(views: RemoteViews) {
        views.setRemoteAdapter(0, R.id.list_view, Intent(this,
                DetailAppWidgetRemoteViewsService::class.java))
    }
}