package com.ianhanniballake.contractiontimer;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.Preferences;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {
    private static final String TAG = QuickTileService.class.getSimpleName();
    private ContentObserver observer;
    private AsyncQueryHandler contractionQueryHandler;
    private boolean contractionOngoing;
    private long latestContractionId;

    @Override
    public void onCreate() {
        super.onCreate();
        observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange, final Uri uri) {
                updateTile();
            }
        };
        contractionQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onInsertComplete(final int token, final Object cookie, final Uri uri) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(QuickTileService.this);
                NotificationUpdateService.updateNotification(QuickTileService.this);
            }

            @Override
            protected void onUpdateComplete(final int token, final Object cookie, final int result) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(QuickTileService.this);
                NotificationUpdateService.updateNotification(QuickTileService.this);
            }
        };
    }

    @Override
    public void onStartListening() {
        getContentResolver().registerContentObserver(ContractionContract.Contractions.CONTENT_URI, true, observer);
        updateTile();
    }

    private void updateTile() {
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME};
        final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?";
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final long averagesTimeFrame = Long.parseLong(preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default)));
        final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
        final String[] selectionArgs = {Long.toString(timeCutoff)};
        final Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                selection, selectionArgs, null);
        // Get the average duration and frequency
        String averages = null;
        if (data != null && data.moveToFirst()) {
            double averageDuration = 0;
            double averageFrequency = 0;
            int numDurations = 0;
            int numFrequencies = 0;
            while (!data.isAfterLast()) {
                final int startTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long startTime = data.getLong(startTimeColumnIndex);
                final int endTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
                if (!data.isNull(endTimeColumnIndex)) {
                    final long endTime = data.getLong(endTimeColumnIndex);
                    final long curDuration = endTime - startTime;
                    averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1);
                    numDurations++;
                }
                if (data.moveToNext()) {
                    final int prevContractionStartTimeColumnIndex = data
                            .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                    final long prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex);
                    final long curFrequency = startTime - prevContractionStartTime;
                    averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1);
                    numFrequencies++;
                }
            }
            final long averageDurationInSeconds = (long) (averageDuration / 1000);
            String formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds);
            final long averageFrequencyInSeconds = (long) (averageFrequency / 1000);
            String formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds);
            averages = getString(R.string.tile_label,
                    formattedAverageDuration, formattedAverageFrequency);
        }
        // Set the status of the contraction toggle button
        // Need to use a separate cursor as there could be running contractions
        // outside of the average time frame
        final Cursor allData = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null);
        contractionOngoing = allData != null && allData.moveToFirst()
                && allData.isNull(allData.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        latestContractionId = contractionOngoing ? allData.getLong(allData.getColumnIndex(BaseColumns._ID)) : 0;
        // Close the cursors
        if (data != null)
            data.close();
        if (allData != null)
            allData.close();
        Tile tile = getQsTile();
        String backupLabel;
        if (contractionOngoing) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_stop));
            tile.setContentDescription(getString(R.string.appwidget_contraction_stop));
            backupLabel = getString(R.string.notification_timing);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_tile_start));
            tile.setContentDescription(getString(R.string.appwidget_contraction_start));
            backupLabel = getString(R.string.app_name);
        }
        tile.setLabel(averages != null ? averages : backupLabel);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        if (!contractionOngoing) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting contraction");
            analytics.logEvent("quick_tile_start", null);
            // Start a new contraction
            contractionQueryHandler.startInsert(0, null, ContractionContract.Contractions.CONTENT_URI,
                    new ContentValues());
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Stopping contraction");
            analytics.logEvent("quick_tile_stop", null);
            final ContentValues newEndTime = new ContentValues();
            newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis());
            final Uri updateUri = ContentUris.withAppendedId(
                    ContractionContract.Contractions.CONTENT_ID_URI_BASE, latestContractionId);
            // Add the new end time to the last contraction
            contractionQueryHandler.startUpdate(0, 0, updateUri, newEndTime, null, null);
        }
    }

    @Override
    public void onStopListening() {
        getContentResolver().unregisterContentObserver(observer);
    }
}
