package com.ianhanniballake.contractiontimer.appwidget;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Starts a new contraction or stops the current contraction, updating all widgets upon completion
 */
public class AppWidgetToggleService extends IntentService {
    /**
     * Intent extra used to determine which widget called this service
     */
    public final static String WIDGET_NAME_EXTRA = "com.ianhanniballake.contractiontimer.WidgetName";
    private final static String TAG = AppWidgetToggleService.class.getSimpleName();

    /**
     * Creates a new AppWidgetToggleService
     */
    public AppWidgetToggleService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final String widgetName = intent.getStringExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA);
        final ContentResolver contentResolver = getContentResolver();
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_END_TIME};
        final Cursor data = contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection, null, null,
                null);
        if (data == null) {
            return;
        }
        final boolean contractionOngoing = data.moveToFirst()
                && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
        if (contractionOngoing) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Stopping contraction");
            analytics.logEvent(widgetName + "_stop", null);
            final ContentValues newEndTime = new ContentValues();
            newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis());
            final long latestContractionId = data.getInt(data.getColumnIndex(BaseColumns._ID));
            final Uri updateUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE,
                    latestContractionId);
            // Add the new end time to the last contraction
            contentResolver.update(updateUri, newEndTime, null, null);
        } else {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting contraction");
            analytics.logEvent(widgetName + "_start", null);
            // Start a new contraction
            contentResolver.insert(ContractionContract.Contractions.CONTENT_URI, new ContentValues());
        }
        // Close the cursor
        data.close();
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
        NotificationUpdateService.updateNotification(this);
    }
}
