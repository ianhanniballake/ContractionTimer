package com.ianhanniballake.contractiontimer

import android.graphics.drawable.Icon
import android.os.Build
import android.preference.PreferenceManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.database.Contraction
import com.ianhanniballake.contractiontimer.database.ContractionDatabase
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import com.ianhanniballake.contractiontimer.ui.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date

@RequiresApi(api = Build.VERSION_CODES.N)
class QuickTileService : TileService() {
    companion object {
        private const val TAG = "QuickTileService"
    }

    private val dao by lazy {
        ContractionDatabase.getInstance(this).contractionDao()
    }
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + supervisorJob)
    private var listeningJob: Job? = null
    private var latestContraction: Contraction? = null
    private val contractionOngoing: Boolean
        get() = latestContraction == null || latestContraction?.endTime != null

    override fun onStartListening() {
        listeningJob?.cancel()
        listeningJob = scope.launch {
            dao.latestContraction().collect {
                latestContraction = it
                updateTile()
            }
        }
    }

    private suspend fun updateTile() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val averagesTimeFrame = preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default))?.toLong() ?: return
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        val contractions = dao.contractionsBackTo(timeCutoff)
        val averages: String? = if (contractions.isNotEmpty()) {
            val averageDuration = contractions.mapNotNull { contraction ->
                if (contraction.endTime != null) {
                    contraction.endTime.time - contraction.startTime.time
                } else {
                    null
                }
            }.average()
            val averageFrequency = contractions.windowed(2) { window ->
                window[1].startTime.time - window[0].startTime.time
            }.average()
            val averageDurationInSeconds = (averageDuration / 1000).toLong()
            val formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds)
            val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
            val formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds)
            getString(R.string.tile_label, formattedAverageDuration, formattedAverageFrequency)
        } else {
            null
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
        val latestContraction = latestContraction
        if (latestContraction == null || latestContraction.endTime != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting contraction")
            analytics.logEvent("quick_tile_start", null)
            // Start a new contraction
            scope.launch {
                dao.insert(Contraction())
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(this@QuickTileService)
                NotificationUpdateReceiver.updateNotification(this@QuickTileService)
            }
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Stopping contraction")
            analytics.logEvent("quick_tile_stop", null)
            // Add the new end time to the last contraction
            scope.launch {
                dao.update(latestContraction.copy(endTime = Date()))
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(this@QuickTileService)
                NotificationUpdateReceiver.updateNotification(this@QuickTileService)
            }
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        supervisorJob.cancel()
    }
}
