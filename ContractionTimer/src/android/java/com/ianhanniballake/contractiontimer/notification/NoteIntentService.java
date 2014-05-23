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

import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Service which can automatically add/replace the note on the current contraction from a voice input source or start
 * the appropriate UI if no voice input is given
 */
public class NoteIntentService extends IntentService {
    public NoteIntentService() {
        super(NoteIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d(NoteIntentService.class.getSimpleName(), "Received text: " + text);
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {BaseColumns._ID};
        Cursor data = contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection, null,
                null, null);
        if (data == null || !data.moveToFirst()) {
            // This shouldn't happen as checkServiceState ensures at least one contraction exists
            Log.w(NoteIntentService.class.getSimpleName(), "Could not find contraction");
            if (data != null) {
                data.close();
            }
            return;
        }
        long id = data.getInt(data.getColumnIndex(BaseColumns._ID));
        data.close();
        Uri contractionUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
        if (TextUtils.isEmpty(text)) {
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, contractionUri);
            taskStackBuilder.addNextIntentWithParentStack(viewIntent);
            Intent editIntent = new Intent(Intent.ACTION_EDIT, contractionUri);
            taskStackBuilder.addNextIntent(editIntent);
            taskStackBuilder.startActivities();
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, text);
        int count = contentResolver.update(contractionUri, values, null, null);
        if (count == 1) {
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
            NotificationUpdateService.updateNotification(this);
        } else {
            Log.e(NoteIntentService.class.getSimpleName(), "Error updating contraction's note");
        }
    }
}
