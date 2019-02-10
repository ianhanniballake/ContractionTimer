package com.ianhanniballake.contractiontimer.appwidget

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.BaseColumns
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.closeable
import com.ianhanniballake.contractiontimer.extensions.goAsync
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract

/**
 * Starts a new contraction or stops the current contraction, updating all widgets upon completion
 */
class AppWidgetToggleReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppWidgetToggleReceiver"
        /**
         * Intent extra used to determine which widget called this service
         */
        const val WIDGET_NAME_EXTRA = "com.ianhanniballake.contractiontimer.WidgetName"
    }
    override fun onReceive(context: Context, intent: Intent?) = goAsync {
        val widgetName = intent?.getStringExtra(WIDGET_NAME_EXTRA)
        val projection = arrayOf(BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME)
        context.contentResolver.query(ContractionContract.Contractions.CONTENT_URI,
                projection, null, null, null)?.closeable()?.use { data ->
            val contractionOngoing = data.moveToFirst() && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME))
            val analytics = FirebaseAnalytics.getInstance(context)
            if (contractionOngoing) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Stopping contraction")
                analytics.logEvent("${widgetName}_stop", null)
                val newEndTime = ContentValues().apply {
                    put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis())
                }
                val latestContractionId = data.getLong(data.getColumnIndex(BaseColumns._ID))
                val updateUri = ContentUris.withAppendedId(
                        ContractionContract.Contractions.CONTENT_ID_URI_BASE, latestContractionId)
                // Add the new end time to the last contraction
                context.contentResolver.update(updateUri, newEndTime, null, null)
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Starting contraction")
                analytics.logEvent(widgetName + "_start", null)
                // Start a new contraction
                context.contentResolver.insert(ContractionContract.Contractions.CONTENT_URI,
                        ContentValues())
            }
        }
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
        NotificationUpdateService.updateNotification(context)
    }
}
