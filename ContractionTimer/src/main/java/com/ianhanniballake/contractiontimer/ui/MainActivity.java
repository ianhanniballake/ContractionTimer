package com.ianhanniballake.contractiontimer.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.tagmanager.DataLayer;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Main Activity for managing contractions
 */
public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Intent extra used to signify that this activity was launched from a widget
     */
    public final static String LAUNCHED_FROM_WIDGET_EXTRA = "com.ianhanniballake.contractiontimer.LaunchedFromWidget";
    /**
     * BroadcastReceiver listening for ABOUT_CLOSE_ACTION, NOTE_CLOSE_ACTION, and RESET_CLOSE_ACTION actions
     */
    private final BroadcastReceiver dialogFragmentClosedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BuildConfig.DEBUG)
                Log.d(MainActivity.this.getClass().getSimpleName(),
                        "DialogFragmentClosedBR Received " + intent.getAction());
            GtmManager.getInstance(MainActivity.this).pushOpenScreen("Main");
        }
    };
    /**
     * Adapter to store and manage the current cursor
     */
    private CursorAdapter adapter;

    /**
     * Builds a string representing a user friendly formatting of the average duration / frequency information
     *
     * @return The formatted average data
     */
    private String getAverageData() {
        final TextView averageDurationView = (TextView) findViewById(R.id.average_duration);
        final TextView averageFrequencyView = (TextView) findViewById(R.id.average_frequency);
        final Cursor data = adapter.getCursor();
        data.moveToLast();
        final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
        final long lastStartTime = data.getLong(startTimeColumnIndex);
        final int count = adapter.getCount();
        final CharSequence relativeTimeSpan = DateUtils.getRelativeTimeSpanString(lastStartTime,
                System.currentTimeMillis(), 0);
        return getResources()
                .getQuantityString(
                        R.plurals.share_average,
                        count,
                        new Object[]{relativeTimeSpan, count, averageDurationView.getText(),
                                averageFrequencyView.getText()}
                );
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        // If there is no data associated with the Intent, sets the data to the
        // default URI, which accesses all contractions.
        if (intent.getData() == null)
            intent.setData(ContractionContract.Contractions.CONTENT_URI);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null)
            showFragments();
        adapter = new CursorAdapter(this, null, 0) {
            @Override
            public void bindView(final View view, final Context context, final Cursor cursor) {
                // Nothing to do
            }

            @Override
            public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
                return null;
            }
        };
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                ContractionContract.Contractions.COLUMN_NAME_NOTE};
        final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?";
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final long averagesTimeFrame = Long.parseLong(preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_settings_average_time_frame_default)));
        final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
        final String[] selectionArgs = {Long.toString(timeCutoff)};
        return new CursorLoader(this, getIntent().getData(), projection, selection, selectionArgs, null);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        GtmManager gtmManager = GtmManager.getInstance(this);
        gtmManager.push(DataLayer.mapOf("menu", "Menu", "count", adapter.getCount()));
        switch (item.getItemId()) {
            case R.id.menu_reset:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected Reset");
                gtmManager.pushEvent("Reset");
                final ResetDialogFragment resetDialogFragment = new ResetDialogFragment();
                if (BuildConfig.DEBUG)
                    Log.d(resetDialogFragment.getClass().getSimpleName(), "Showing Dialog");
                gtmManager.pushOpenScreen("Reset");
                resetDialogFragment.show(getSupportFragmentManager(), "reset");
                return true;
            case R.id.menu_add:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected Add");
                gtmManager.pushEvent("Add");
                final Intent addIntent = new Intent(Intent.ACTION_INSERT, getIntent().getData());
                startActivity(addIntent);
                return true;
            case R.id.menu_share_averages:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected Share Averages");
                gtmManager.pushEvent("Share", DataLayer.mapOf("type", "Averages"));
                shareAverages();
                return true;
            case R.id.menu_share_all:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected Share All");
                gtmManager.pushEvent("Share", DataLayer.mapOf("type", "All"));
                shareAll();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
            case R.id.menu_about:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected About");
                gtmManager.pushEvent("About");
                final AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
                if (BuildConfig.DEBUG)
                    Log.d(aboutDialogFragment.getClass().getSimpleName(), "Showing Dialog");
                gtmManager.pushOpenScreen("About");
                aboutDialogFragment.show(getSupportFragmentManager(), "about");
                return true;
            case R.id.menu_donate:
                if (BuildConfig.DEBUG)
                    Log.d(getClass().getSimpleName(), "Menu selected Donate");
                gtmManager.pushEvent("Donate");
                startActivity(new Intent(this, DonateActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Set sharing buttons status
        final int contractionCount = adapter == null ? 0 : adapter.getCount();
        final boolean enableShare = contractionCount > 0;
        final MenuItem shareAverages = menu.findItem(R.id.menu_share_averages);
        shareAverages.setEnabled(enableShare);
        final MenuItem shareAll = menu.findItem(R.id.menu_share_all);
        shareAll.setEnabled(enableShare);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isKeepScreenOn = preferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_settings_keep_screen_on_default));
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "Keep Screen On: " + isKeepScreenOn);
        if (isKeepScreenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_settings_lock_portrait_default));
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        final boolean averageTimeFrameChanged = preferences.getBoolean(
                Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, false);
        if (averageTimeFrameChanged) {
            final Editor editor = preferences.edit();
            editor.remove(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY);
            editor.commit();
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        GtmManager.getInstance(this).pushOpenScreen("Main");
        if (getIntent().hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)) {
            final String widgetIdentifier = getIntent().getStringExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Launched from " + widgetIdentifier);
            GtmManager.getInstance(this).pushEvent("Launch", DataLayer.mapOf("widget", widgetIdentifier));
            getIntent().removeExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
        }
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        final IntentFilter dialogCloseFilter = new IntentFilter();
        dialogCloseFilter.addAction(AboutDialogFragment.ABOUT_CLOSE_ACTION);
        dialogCloseFilter.addAction(NoteDialogFragment.NOTE_CLOSE_ACTION);
        dialogCloseFilter.addAction(ResetDialogFragment.RESET_CLOSE_ACTION);
        localBroadcastManager.registerReceiver(dialogFragmentClosedBroadcastReceiver, dialogCloseFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(dialogFragmentClosedBroadcastReceiver);
    }

    /**
     * Builds the data to share and opens the Intent chooser
     */
    private void shareAll() {
        final Cursor data = adapter.getCursor();
        if (data.getCount() == 0)
            return;
        final StringBuilder formattedData = new StringBuilder(getAverageData());
        formattedData.append("\n\n");
        formattedData.append(getText(R.string.share_details_header));
        formattedData.append(":\n\n");
        data.moveToPosition(-1);
        while (data.moveToNext()) {
            String timeFormat = "hh:mm:ssaa";
            if (DateFormat.is24HourFormat(this))
                timeFormat = "kk:mm:ss";
            final int startTimeColumnIndex = data
                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
            final long startTime = data.getLong(startTimeColumnIndex);
            final CharSequence formattedStartTime = DateFormat.format(timeFormat, startTime);
            final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
            if (data.isNull(endTimeColumnIndex)) {
                final String detailTimeOngoingFormat = getString(R.string.share_detail_time_ongoing);
                formattedData.append(String.format(detailTimeOngoingFormat, formattedStartTime));
            } else {
                final String detailTimeFormat = getString(R.string.share_detail_time_finished);
                final long endTime = data.getLong(endTimeColumnIndex);
                final CharSequence formattedEndTime = DateFormat.format(timeFormat, endTime);
                final long durationInSeconds = (endTime - startTime) / 1000;
                final CharSequence formattedDuration = DateUtils.formatElapsedTime(durationInSeconds);
                formattedData.append(String.format(detailTimeFormat, formattedStartTime, formattedEndTime,
                        formattedDuration));
            }
            // If we aren't the last entry, move to the next (previous in time)
            // contraction to get its start time to compute the frequency
            if (!data.isLast() && data.moveToNext()) {
                final String detailFrequencyFormat = getString(R.string.share_detail_frequency);
                final int prevContractionStartTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex);
                final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
                final CharSequence formattedFrequency = DateUtils.formatElapsedTime(frequencyInSeconds);
                formattedData.append(" ");
                formattedData.append(String.format(detailFrequencyFormat, formattedFrequency));
                // Go back to the previous spot
                data.moveToPrevious();
            }
            final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
            final String note = data.getString(noteColumnIndex);
            if (!TextUtils.isEmpty(note)) {
                formattedData.append(": ");
                formattedData.append(note);
            }
            formattedData.append("\n");
        }
        ShareCompat.IntentBuilder.from(this).setSubject(getText(R.string.share_subject).toString())
                .setType("text/plain").setText(formattedData.toString())
                .setChooserTitle(R.string.share_pick_application).startChooser();
    }

    /**
     * Builds the averages data to share and opens the Intent chooser
     */
    private void shareAverages() {
        final Cursor data = adapter.getCursor();
        if (data.getCount() == 0)
            return;
        final String formattedData = getAverageData();
        ShareCompat.IntentBuilder.from(this).setSubject(getText(R.string.share_subject).toString())
                .setType("text/plain").setText(formattedData).setChooserTitle(R.string.share_pick_application)
                .startChooser();
    }

    /**
     * Creates and shows the fragments for the MainActivity
     */
    private void showFragments() {
        final ContractionControlsFragment controlsFragment = new ContractionControlsFragment();
        ContractionListFragment listFragment;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            listFragment = new ContractionListFragmentV11();
        else
            listFragment = new ContractionListFragmentBase();
        final ContractionAverageFragment averageFragment = new ContractionAverageFragment();
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.controls, controlsFragment);
        ft.replace(R.id.list, listFragment);
        ft.replace(R.id.averages, averageFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
}