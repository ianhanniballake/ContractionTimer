package com.ianhanniballake.contractiontimer;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Activity showing the options menu.
 */
public class MenuActivity extends Activity {
    private final Handler mHandler = new Handler();
    private AsyncQueryHandler mAsyncQueryHandler;
    private long mOngoingContractionId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
                super.onQueryComplete(token, cookie, cursor);
            }
        };
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timer, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final ContentResolver contentResolver = getContentResolver();
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_END_TIME};
        final Cursor data = contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection, null, null,
                null);
        final boolean hasContractions = data != null && data.moveToFirst();
        final boolean contractionOngoing = hasContractions
                && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        MenuItem startContraction = menu.findItem(R.id.menu_start_contraction);
        startContraction.setVisible(!contractionOngoing);
        MenuItem stopContraction = menu.findItem(R.id.menu_stop_contraction);
        stopContraction.setVisible(contractionOngoing);
        MenuItem reset = menu.findItem(R.id.menu_reset);
        reset.setVisible(hasContractions);
        if (contractionOngoing) {
            mOngoingContractionId = data.getLong(data.getColumnIndex(BaseColumns._ID));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_start_contraction:
                Log.d(MenuActivity.class.getSimpleName(), "Starting contraction");
                mAsyncQueryHandler.startInsert(0, null, ContractionContract.Contractions.CONTENT_URI,
                        new ContentValues());
                return true;
            case R.id.menu_stop_contraction:
                Log.d(MenuActivity.class.getSimpleName(), "Stopping contraction");
                final ContentValues newEndTime = new ContentValues();
                newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis());
                final Uri updateUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE,
                        mOngoingContractionId);
                mAsyncQueryHandler.startUpdate(0, null, updateUri, newEndTime, null, null);
                return true;
            case R.id.menu_reset:
                Log.d(MenuActivity.class.getSimpleName(), "Resetting contractions");
                // TODO Add confirm activity
                mAsyncQueryHandler.startDelete(0, 0, ContractionContract.Contractions.CONTENT_URI, null, null);
                return true;
            case R.id.menu_stop:
                // Stop the service at the end of the message queue for proper options menu
                // animation. This is only needed when starting a new Activity or stopping a Service
                // that published a LiveCard.
                post(new Runnable() {
                    @Override
                    public void run() {
                        stopService(new Intent(MenuActivity.this, TimerService.class));
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        finish();
    }

    /**
     * Posts a {@link Runnable} at the end of the message loop, overridable for testing.
     */
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}
