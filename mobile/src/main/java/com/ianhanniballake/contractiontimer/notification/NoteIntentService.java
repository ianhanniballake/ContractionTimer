package com.ianhanniballake.contractiontimer.notification;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.MainActivity;

/**
 * Service which can automatically add/replace the note on the current contraction from a voice input source or start
 * the appropriate UI if no voice input is given
 */
public class NoteIntentService extends IntentService {
    private final static String TAG = NoteIntentService.class.getSimpleName();
    /**
     * Action Google Now uses for 'Note to self' voice input
     */
    private final static String GOOGLE_NOW_INPUT = "com.google.android.gm.action.AUTO_SEND";

    public NoteIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received text: " + text);
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_NOTE};
        Cursor data = contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection, null,
                null, null);
        if (data == null || !data.moveToFirst()) {
            // This shouldn't happen as checkServiceState ensures at least one contraction exists
            Log.w(TAG, "Could not find contraction");
            if (data != null) {
                data.close();
            }
            return;
        }
        long id = data.getInt(data.getColumnIndex(BaseColumns._ID));
        String note = data.getString(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE));
        data.close();
        if (TextUtils.isEmpty(text)) {
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.putExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA, true);
            mainIntent.putExtra(BaseColumns._ID, id);
            mainIntent.putExtra(ContractionContract.Contractions.COLUMN_NAME_NOTE, note);
            taskStackBuilder.addNextIntent(mainIntent);
            taskStackBuilder.startActivities();
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, text);
        Uri contractionUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
        int count = contentResolver.update(contractionUri, values, null, null);
        if (count == 1) {
            String voiceInputSource = TextUtils.equals(intent.getAction(), GOOGLE_NOW_INPUT) ?
                    "google_now" : "remote_input";
            String noteEvent = TextUtils.isEmpty(note) ? "note_add" : "note_edit" + "_" + voiceInputSource;
            FirebaseAnalytics.getInstance(this).logEvent(noteEvent, null);
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
            NotificationUpdateService.updateNotification(this);
        } else {
            Log.e(TAG, "Error updating contraction's note");
        }
    }
}
