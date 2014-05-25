package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Fragment which controls starting and stopping the contraction timer
 */
public class ContractionControlsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = ContractionControlsFragment.class.getSimpleName();
    /**
     * Cursor Adapter which holds the latest contraction
     */
    CursorAdapter adapter;
    /**
     * Handler for asynchronous inserts/updates of contractions
     */
    AsyncQueryHandler contractionQueryHandler;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new CursorAdapter(getActivity(), null, 0) {
            @Override
            public void bindView(final View view, final Context context, final Cursor cursor) {
                // Nothing to do
            }

            @Override
            public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
                return null;
            }
        };
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contractionQueryHandler = new AsyncQueryHandler(getActivity().getContentResolver()) {
            // No call backs needed
        };
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME};
        return new CursorLoader(getActivity(), getActivity().getIntent().getData(), projection, null, null, null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_contraction_controls, container, false);
        final ToggleButton toggleContraction = (ToggleButton) view.findViewById(R.id.toggleContraction);
        toggleContraction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Disable the button to ensure we give the database a chance to
                // complete the insert/update
                toggleContraction.setEnabled(false);
                GtmManager gtmManager = GtmManager.getInstance(ContractionControlsFragment.this);
                gtmManager.push("control", "Controls");
                if (toggleContraction.isChecked()) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Starting contraction");
                    gtmManager.pushEvent("Start");
                    // Start a new contraction
                    contractionQueryHandler.startInsert(0, null, ContractionContract.Contractions.CONTENT_URI,
                            new ContentValues());
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Stopping contraction");
                    gtmManager.pushEvent("Stop");
                    final ContentValues newEndTime = new ContentValues();
                    newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis());
                    final long latestContractionId = adapter.getItemId(0);
                    final Uri updateUri = ContentUris.withAppendedId(
                            ContractionContract.Contractions.CONTENT_ID_URI_BASE, latestContractionId);
                    // Add the new end time to the last contraction
                    contractionQueryHandler.startUpdate(0, 0, updateUri, newEndTime, null, null);
                }
            }
        });
        return view;
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        adapter.swapCursor(null);
        final View view = getView();
        if (view == null)
            return;
        final ToggleButton toggleContraction = (ToggleButton) view.findViewById(R.id.toggleContraction);
        toggleContraction.setChecked(false);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        adapter.swapCursor(data);
        final View view = getView();
        if (view == null)
            return;
        final ToggleButton toggleContraction = (ToggleButton) view.findViewById(R.id.toggleContraction);
        toggleContraction.setEnabled(true);
        final boolean contractionOngoing = data != null && data.moveToFirst()
                && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        toggleContraction.setChecked(contractionOngoing);
        AppWidgetUpdateHandler.createInstance().updateAllWidgets(getActivity());
        NotificationUpdateService.updateNotification(getActivity());
    }
}
