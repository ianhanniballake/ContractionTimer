package com.ianhanniballake.contractiontimer.notification;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetToggleService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.MainActivity;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * IntentService which updates the ongoing notification
 */
public class NotificationUpdateService extends IntentService {
    private final static String TAG = NotificationUpdateService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0;

    public NotificationUpdateService() {
        super(TAG);
    }

    public static void updateNotification(Context context) {
        context.startService(new Intent(context, NotificationUpdateService.class));
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        NoteNoDisplayActivity.checkServiceState(this);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean notificationsEnabled = preferences.getBoolean(Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY,
                getResources().getBoolean(R.bool.pref_notification_enable_default));
        if (!notificationsEnabled) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Notifications disabled, cancelling notification");
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }
        final String[] projection = {BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                ContractionContract.Contractions.COLUMN_NAME_NOTE};
        final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?";
        final long averagesTimeFrame = Long.parseLong(preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default)));
        final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
        final String[] selectionArgs = {Long.toString(timeCutoff)};
        final Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                selection, selectionArgs, null);
        if (data == null || !data.moveToFirst()) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "No data found, cancelling notification");
            notificationManager.cancel(NOTIFICATION_ID);
            if (data != null) {
                data.close();
            }
            return;
        }
        // Set an alarm to update the notification after first start time + average time frame amount of time
        // This ensures that if no contraction has started since then (likely, otherwise we would have been called in
        // the mean time) we will fail the above check as there will be no contractions within the average time period
        // and the notification will be cancelled
        final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
        final long lastStartTime = data.getLong(startTimeColumnIndex);
        Intent autoCancelIntent = new Intent(this, NotificationUpdateService.class);
        PendingIntent autoCancelPendingIntent = PendingIntent.getService(this, 0, autoCancelIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(autoCancelPendingIntent);
        // We don't need to wake up the device as it doesn't matter if the notification is cancelled until the device
        // is woken up
        alarmManager.set(AlarmManager.RTC, lastStartTime + averagesTimeFrame, autoCancelPendingIntent);
        // Build the notification
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Building Notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        NotificationCompat.Builder publicBuilder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setCategory(NotificationCompat.CATEGORY_ALARM);
        publicBuilder.setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(this, R.color.primary))
                .setCategory(NotificationCompat.CATEGORY_ALARM);
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        contentIntent.putExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_EXTRA, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        publicBuilder.setContentIntent(pendingIntent);
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        // Determine whether a contraction is currently ongoing
        final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
        final boolean contractionOngoing = data.isNull(endTimeColumnIndex);
        Intent startStopIntent = new Intent(this, AppWidgetToggleService.class);
        startStopIntent.putExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA, "NotificationAction");
        PendingIntent startStopPendingIntent = PendingIntent.getService(this, 0, startStopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (contractionOngoing) {
            builder.setContentTitle(getString(R.string.notification_timing));
            publicBuilder.setContentTitle(getString(R.string.notification_timing));
            builder.addAction(R.drawable.ic_notif_action_stop, getString(R.string.appwidget_contraction_stop),
                    startStopPendingIntent);
            wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_wear_action_stop,
                    getString(R.string.appwidget_contraction_stop),
                    startStopPendingIntent));
        } else {
            builder.setContentTitle(getString(R.string.app_name));
            publicBuilder.setContentTitle(getString(R.string.app_name));
            builder.addAction(R.drawable.ic_notif_action_start, getString(R.string.appwidget_contraction_start),
                    startStopPendingIntent);
            wearableExtender.addAction(new NotificationCompat.Action(R.drawable.ic_wear_action_start,
                    getString(R.string.appwidget_contraction_start),
                    startStopPendingIntent));
        }
        // See if there is a note and build a page if it exists
        final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
        final String note = data.getString(noteColumnIndex);
        boolean hasNote = !TextUtils.isEmpty(note);
        // Fill in the 'when', which will be used to show live progress via the chronometer feature
        final long when = contractionOngoing ? data.getLong(startTimeColumnIndex) : data.getLong(endTimeColumnIndex);
        builder.setWhen(when);
        publicBuilder.setWhen(when);
        builder.setUsesChronometer(true);
        publicBuilder.setUsesChronometer(true);
        // Get the average duration and frequency
        double averageDuration = 0;
        double averageFrequency = 0;
        int numDurations = 0;
        int numFrequencies = 0;
        while (!data.isAfterLast()) {
            final long startTime = data.getLong(startTimeColumnIndex);
            if (!data.isNull(endTimeColumnIndex)) {
                final long endTime = data.getLong(endTimeColumnIndex);
                final long curDuration = endTime - startTime;
                averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1);
                numDurations++;
            }
            if (data.moveToNext()) {
                final long prevContractionStartTime = data.getLong(startTimeColumnIndex);
                final long curFrequency = startTime - prevContractionStartTime;
                averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1);
                numFrequencies++;
            }
        }
        final long averageDurationInSeconds = (long) (averageDuration / 1000);
        String formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds);
        final long averageFrequencyInSeconds = (long) (averageFrequency / 1000);
        String formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds);
        String contentText = getString(R.string.notification_content_text,
                formattedAverageDuration, formattedAverageFrequency);
        String bigText;
        String bigTextWithoutNote = getString(R.string.notification_big_text, formattedAverageDuration,
                formattedAverageFrequency);
        if (hasNote) {
            bigText = getString(R.string.notification_big_text_with_note, formattedAverageDuration,
                    formattedAverageFrequency, note);
        } else {
            bigText = bigTextWithoutNote;
        }
        builder.setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        publicBuilder.setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigTextWithoutNote));
        // Close the cursor
        data.close();
        // Create a separate page for the averages as the big text is not shown on Android Wear in chronometer mode
        Notification averagePage = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notification_second_page_title))
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle(getString(R.string.notification_second_page_title))
                        .addLine(getString(R.string.notification_second_page_duration, formattedAverageDuration))
                        .addLine(getString(R.string.notification_second_page_frequency, formattedAverageFrequency)))
                .build();
        wearableExtender.addPage(averagePage);
        if (hasNote) {
            Notification notePage = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.detail_note_label))
                    .setContentText(note)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(getString(R.string.detail_note_label))
                            .bigText(note))
                    .build();
            wearableExtender.addPage(notePage);
        }
        // Add 'Add Note'/'Edit Note' action
        int noteIconResId = hasNote ? R.drawable.ic_notif_action_edit :
                R.drawable.ic_notif_action_add;
        int wearIconResId = hasNote ? R.drawable.ic_wear_action_edit :
                R.drawable.ic_wear_action_add;
        String noteTitle = hasNote ? getString(R.string.note_dialog_title_edit) :
                getString(R.string.note_dialog_title_add);
        Intent noteIntent = new Intent(this, NoteNoDisplayActivity.class);
        PendingIntent notePendingIntent = PendingIntent.getActivity(this, 0, noteIntent, 0);
        RemoteInput remoteInput = new RemoteInput.Builder(Intent.EXTRA_TEXT).setLabel(noteTitle).build();
        builder.addAction(noteIconResId, noteTitle, notePendingIntent);
        wearableExtender.addAction(new NotificationCompat.Action.Builder(wearIconResId, noteTitle,
                notePendingIntent).addRemoteInput(remoteInput).build());
        builder.setPublicVersion(publicBuilder.build());
        notificationManager.notify(NOTIFICATION_ID, builder.extend(wearableExtender).build());
    }
}
