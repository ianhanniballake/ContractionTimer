package com.ianhanniballake.contractiontimer

import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.support.annotation.RequiresApi
import android.text.format.DateUtils
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(api = Build.VERSION_CODES.N)
class QuickTileService : TileService() {
    companion object {
        private const val TAG = "QuickTileService"
    }

    private val observer by lazy {
        object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean, uri: Uri) {
                updateTile()
            }
        }
    }
    private var contractionOngoing: Boolean = false
    private var latestContractionId: Long = 0

    override fun onStartListening() {
        contentResolver.registerContentObserver(
                ContractionContract.Contractions.CONTENT_URI, true, observer)
        updateTile()
    }

    private fun updateTile() {
        val projection = arrayOf(BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME, ContractionContract.Contractions.COLUMN_NAME_END_TIME)
        val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val averagesTimeFrame = preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default))?.toLong() ?: return
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        val selectionArgs = arrayOf(timeCutoff.toString())
        // Get the average duration and frequency
        val averages: String? = contentResolver.query(
                ContractionContract.Contractions.CONTENT_URI, projection,
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
                val formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds)
                val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
                val formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds)
                getString(R.string.tile_label, formattedAverageDuration, formattedAverageFrequency)
            } else {
                null
            }
        }

        // Set the status of the contraction toggle button
        // Need to use a separate cursor as there could be running contractions
        // outside of the average time frame
        contentResolver.query(ContractionContract.Contractions.CONTENT_URI,
                projection, null, null, null)?.use { allData ->
            contractionOngoing = allData.moveToFirst() && allData.isNull(allData.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME))
            latestContractionId = if (contractionOngoing) allData.getLong(allData.getColumnIndex(BaseColumns._ID)) else 0
        } ?: run {
            contractionOngoing = false
            latestContractionId = 0
        }
        val context = this
        qsTile?.apply {
            val backupLabel: String
            if (contractionOngoing) {
                state = Tile.STATE_ACTIVE
                icon = Icon.createWithResource(context, R.drawable.ic_tile_stop)
                contentDescription = getString(R.string.appwidget_contraction_stop)
                backupLabel = getString(R.string.notification_timing)
            } else {
                state = Tile.STATE_INACTIVE
                icon = Icon.createWithResource(context, R.drawable.ic_tile_start)
                contentDescription = getString(R.string.appwidget_contraction_start)
                backupLabel = getString(R.string.app_name)
            }
            label = averages ?: backupLabel
            updateTile()
        }
    }

    override fun onClick() {
        val analytics = FirebaseAnalytics.getInstance(this)
        if (!contractionOngoing) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting contraction")
            analytics.logEvent("quick_tile_start", null)
            // Start a new contraction
            GlobalScope.launch {
                contentResolver.insert(ContractionContract.Contractions.CONTENT_URI,
                        ContentValues())
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(this@QuickTileService)
                NotificationUpdateService.updateNotification(this@QuickTileService)
            }
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Stopping contraction")
            analytics.logEvent("quick_tile_stop", null)
            val newEndTime = ContentValues()
            newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis())
            val updateUri = ContentUris.withAppendedId(
                    ContractionContract.Contractions.CONTENT_ID_URI_BASE, latestContractionId)
            // Add the new end time to the last contraction
            GlobalScope.launch {
                contentResolver.update(updateUri, newEndTime, null, null)
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(this@QuickTileService)
                NotificationUpdateService.updateNotification(this@QuickTileService)
            }
        }
    }

    override fun onStopListening() {
        contentResolver.unregisterContentObserver(observer)
    }
}
