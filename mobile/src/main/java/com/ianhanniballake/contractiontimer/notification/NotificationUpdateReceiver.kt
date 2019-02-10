package com.ianhanniballake.contractiontimer.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.RemoteInput
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.Log
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetToggleReceiver
import com.ianhanniballake.contractiontimer.extensions.goAsync
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.MainActivity
import com.ianhanniballake.contractiontimer.ui.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver which updates the ongoing notification
 */
class NotificationUpdateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationUpdate"
        private const val NOTIFICATION_ID = 0

        fun updateNotification(context: Context) {
            GlobalScope.launch {
                update(context)
            }
        }

        private fun update(context: Context) {
            NoteTranslucentActivity.checkServiceState(context)
            val notificationManager = NotificationManagerCompat.from(context)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val notificationsEnabled = preferences.getBoolean(
                    Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY,
                    context.resources.getBoolean(R.bool.pref_notification_enable_default))
            if (!notificationsEnabled) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Notifications disabled, cancelling notification")
                notificationManager.cancel(NOTIFICATION_ID)
                return
            }
            val projection = arrayOf(BaseColumns._ID,
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                    ContractionContract.Contractions.COLUMN_NAME_NOTE)
            val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
            val averagesTimeFrame = preferences.getString(
                    Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                    context.getString(R.string.pref_average_time_frame_default))!!.toLong()
            val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
            val selectionArgs = arrayOf(timeCutoff.toString())
            val data = context.contentResolver.query(
                    ContractionContract.Contractions.CONTENT_URI, projection,
                    selection, selectionArgs, null)
            if (data == null || !data.moveToFirst()) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "No data found, cancelling notification")
                notificationManager.cancel(NOTIFICATION_ID)
                data?.close()
                return
            }
            // Set an alarm to update the notification after first start time + average time frame amount of time
            // This ensures that if no contraction has started since then (likely, otherwise we would have been called in
            // the mean time) we will fail the above check as there will be no contractions within the average time period
            // and the notification will be cancelled
            val startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME)
            val lastStartTime = data.getLong(startTimeColumnIndex)
            val autoCancelIntent = Intent(context, NotificationUpdateReceiver::class.java)
            val autoCancelPendingIntent = PendingIntent.getBroadcast(context, 0, autoCancelIntent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(autoCancelPendingIntent)
            // We don't need to wake up the device as it doesn't matter if the notification is cancelled until the device
            // is woken up
            alarmManager.set(AlarmManager.RTC, lastStartTime + averagesTimeFrame, autoCancelPendingIntent)
            // Build the notification
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Building Notification")
            val builder = NotificationCompat.Builder(context)
            val publicBuilder = NotificationCompat.Builder(context)
            builder.setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
            publicBuilder.setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.primary))
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                putExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_EXTRA, true)
            }
            val pendingIntent = PendingIntent.getActivity(context, 0,
                    contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(pendingIntent)
            publicBuilder.setContentIntent(pendingIntent)
            val wearableExtender = NotificationCompat.WearableExtender()
            // Determine whether a contraction is currently ongoing
            val endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            val contractionOngoing = data.isNull(endTimeColumnIndex)
            val startStopIntent = Intent(context, AppWidgetToggleReceiver::class.java)
            startStopIntent.putExtra(AppWidgetToggleReceiver.WIDGET_NAME_EXTRA, "notification")
            val startStopPendingIntent = PendingIntent.getBroadcast(context, 0,
                    startStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            if (contractionOngoing) {
                builder.setContentTitle(context.getString(R.string.notification_timing))
                publicBuilder.setContentTitle(context.getString(R.string.notification_timing))
                builder.addAction(R.drawable.ic_notif_action_stop,
                        context.getString(R.string.appwidget_contraction_stop),
                        startStopPendingIntent)
                wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_wear_action_stop,
                        context.getString(R.string.appwidget_contraction_stop),
                        startStopPendingIntent))
            } else {
                builder.setContentTitle(context.getString(R.string.app_name))
                publicBuilder.setContentTitle(context.getString(R.string.app_name))
                builder.addAction(R.drawable.ic_notif_action_start,
                        context.getString(R.string.appwidget_contraction_start),
                        startStopPendingIntent)
                wearableExtender.addAction(NotificationCompat.Action(R.drawable.ic_wear_action_start,
                        context.getString(R.string.appwidget_contraction_start),
                        startStopPendingIntent))
            }
            // See if there is a note and build a page if it exists
            val noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE)
            val note = data.getString(noteColumnIndex)
            val hasNote = note?.isNotBlank() == true
            // Fill in the 'when', which will be used to show live progress via the chronometer feature
            val time = if (contractionOngoing) data.getLong(startTimeColumnIndex) else data.getLong(endTimeColumnIndex)
            builder.setWhen(time)
            publicBuilder.setWhen(time)
            builder.setUsesChronometer(true)
            publicBuilder.setUsesChronometer(true)
            // Get the average duration and frequency
            var averageDuration = 0.0
            var averageFrequency = 0.0
            var numDurations = 0
            var numFrequencies = 0
            while (!data.isAfterLast) {
                val startTime = data.getLong(startTimeColumnIndex)
                if (!data.isNull(endTimeColumnIndex)) {
                    val endTime = data.getLong(endTimeColumnIndex)
                    val curDuration = endTime - startTime
                    averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1)
                    numDurations++
                }
                if (data.moveToNext()) {
                    val prevContractionStartTime = data.getLong(startTimeColumnIndex)
                    val curFrequency = startTime - prevContractionStartTime
                    averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1)
                    numFrequencies++
                }
            }
            val averageDurationInSeconds = (averageDuration / 1000).toLong()
            val formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds)
            val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
            val formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds)
            val contentText = context.getString(R.string.notification_content_text,
                    formattedAverageDuration, formattedAverageFrequency)
            val bigTextWithoutNote = context.getString(R.string.notification_big_text,
                    formattedAverageDuration, formattedAverageFrequency)
            val bigText = if (hasNote) {
                context.getString(R.string.notification_big_text_with_note,
                        formattedAverageDuration, formattedAverageFrequency, note)
            } else {
                bigTextWithoutNote
            }
            builder.setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            publicBuilder.setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextWithoutNote))
            // Close the cursor
            data.close()
            // Create a separate page for the averages as the big text is not shown on Android Wear in chronometer mode
            val averagePage = NotificationCompat.Builder(context)
                    .setContentTitle(context.getString(R.string.notification_second_page_title))
                    .setStyle(NotificationCompat.InboxStyle()
                            .setBigContentTitle(context.getString(R.string.notification_second_page_title))
                            .addLine(context.getString(R.string.notification_second_page_duration,
                                    formattedAverageDuration))
                            .addLine(context.getString(R.string.notification_second_page_frequency,
                                    formattedAverageFrequency)))
                    .build()
            wearableExtender.addPage(averagePage)
            if (hasNote) {
                val notePage = NotificationCompat.Builder(context)
                        .setContentTitle(context.getString(R.string.detail_note_label))
                        .setContentText(note)
                        .setStyle(NotificationCompat.BigTextStyle()
                                .setBigContentTitle(context.getString(R.string.detail_note_label))
                                .bigText(note))
                        .build()
                wearableExtender.addPage(notePage)
            }
            // Add 'Add Note'/'Edit Note' action
            val noteIconResId = if (hasNote)
                R.drawable.ic_notif_action_edit
            else
                R.drawable.ic_notif_action_add
            val wearIconResId = if (hasNote)
                R.drawable.ic_wear_action_edit
            else
                R.drawable.ic_wear_action_add
            val noteTitle = if (hasNote)
                context.getString(R.string.note_dialog_title_edit)
            else
                context.getString(R.string.note_dialog_title_add)
            val noteIntent = Intent(context, NoteTranslucentActivity::class.java)
            val notePendingIntent = PendingIntent.getActivity(context, 0, noteIntent, 0)
            val remoteInput = RemoteInput.Builder(Intent.EXTRA_TEXT).setLabel(noteTitle).build()
            builder.addAction(noteIconResId, noteTitle, notePendingIntent)
            wearableExtender.addAction(NotificationCompat.Action.Builder(wearIconResId, noteTitle,
                    notePendingIntent).addRemoteInput(remoteInput).build())
            builder.setPublicVersion(publicBuilder.build())
            notificationManager.notify(NOTIFICATION_ID, builder.extend(wearableExtender).build())
        }
    }

    override fun onReceive(context: Context, intent: Intent?) = goAsync {
        update(context)
    }
}
