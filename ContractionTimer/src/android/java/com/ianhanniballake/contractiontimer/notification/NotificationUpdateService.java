package com.ianhanniballake.contractiontimer.notification;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.preview.support.wearable.notifications.WearableNotifications;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetToggleService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * IntentService which updates the ongoing notification
 */
public class NotificationUpdateService extends IntentService {
    private static final int NOTIFICATION_ID = 0;

    public NotificationUpdateService() {
        super(NotificationUpdateService.class.getSimpleName());
    }

    public static void updateNotification(Context context) {
        context.startService(new Intent(context, NotificationUpdateService.class));
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean notificationsEnabled = preferences.getBoolean(Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY,
                getResources().getBoolean(R.bool.pref_notification_enable_default));
        if (!notificationsEnabled) {
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
                getString(R.string.pref_settings_average_time_frame_default)));
        final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
        final String[] selectionArgs = {Long.toString(timeCutoff)};
        final Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                selection, selectionArgs, null);
        if (data == null || !data.moveToFirst()) {
            notificationManager.cancel(NOTIFICATION_ID);
            if (data != null) {
                data.close();
            }
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification);
        Intent contentIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        // Determine whether a contraction is currently ongoing
        final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
        final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
        final boolean contractionOngoing = data.isNull(endTimeColumnIndex);
        builder.setOngoing(contractionOngoing);
        Intent startStopIntent = new Intent(this, AppWidgetToggleService.class);
        PendingIntent startStopPendingIntent = PendingIntent.getService(this, 0, startStopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (contractionOngoing) {
            builder.setContentTitle(getString(R.string.notification_timing));
            builder.addAction(R.drawable.ic_action_stop, getString(R.string.contraction_end),
                    startStopPendingIntent);
        } else {
            builder.setContentTitle(getString(R.string.app_name));
            builder.addAction(R.drawable.ic_action_start, getString(R.string.contraction_start),
                    startStopPendingIntent);
        }
        // See if there is a note and build a page if it exists
        final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
        final String note = data.getString(noteColumnIndex);
        Notification notePage;
        if (TextUtils.isEmpty(note)) {
            notePage = null;
        } else {
            notePage = new NotificationCompat.Builder(this)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .setBigContentTitle(getString(R.string.detail_note_label))
                            .bigText(note))
                    .build();
        }
        // Fill in the 'when', which will be used to show live progress via the chronometer feature
        final long when = contractionOngoing ? data.getLong(startTimeColumnIndex) : data.getLong(endTimeColumnIndex);
        builder.setWhen(when);
        builder.setUsesChronometer(true);
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
        if (TextUtils.isEmpty(note)) {
            bigText = getString(R.string.notification_big_text, formattedAverageDuration,
                    formattedAverageFrequency);
        } else {
            bigText = getString(R.string.notification_big_text_with_note, formattedAverageDuration,
                    formattedAverageFrequency, note);
        }
        builder.setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
        // Close the cursor
        data.close();
        // Create a separate page for the averages as the big text is not shown on Android Wear in chronometer mode
        Notification averagePage = new NotificationCompat.Builder(this)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle(getString(R.string.notification_second_page_title))
                        .addLine(getString(R.string.notification_second_page_duration, formattedAverageDuration))
                        .addLine(getString(R.string.notification_second_page_frequency, formattedAverageFrequency)))
                .build();
        WearableNotifications.Builder wearableBuilder = new WearableNotifications.Builder(builder);
        wearableBuilder.addPage(averagePage);
        if (notePage != null) {
            wearableBuilder.addPage(notePage);
        }
        notificationManager.notify(NOTIFICATION_ID, wearableBuilder.build());
    }
}
