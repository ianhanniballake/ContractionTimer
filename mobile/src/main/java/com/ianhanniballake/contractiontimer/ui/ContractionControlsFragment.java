package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
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

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

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
    private boolean contractionOngoing = false;

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
        final Context context = getActivity();
        contractionQueryHandler = new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onInsertComplete(final int token, final Object cookie, final Uri uri) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context);
                NotificationUpdateService.updateNotification(context);
            }

            @Override
            protected void onUpdateComplete(final int token, final Object cookie, final int result) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context);
                NotificationUpdateService.updateNotification(context);
            }
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
        final FloatingActionButton view = (FloatingActionButton)
                inflater.inflate(R.layout.fragment_contraction_controls, container, false);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // Disable the button to ensure we give the database a chance to
                // complete the insert/update
                view.setEnabled(false);
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
                if (!contractionOngoing) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Starting contraction");
                    analytics.logEvent("control_start", null);
                    // Start a new contraction
                    contractionQueryHandler.startInsert(0, null, ContractionContract.Contractions.CONTENT_URI,
                            new ContentValues());
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Stopping contraction");
                    analytics.logEvent("control_stop", null);
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
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        adapter.swapCursor(data);
        final FloatingActionButton view = (FloatingActionButton) getView();
        if (view == null)
            return;
        view.setEnabled(true);
        contractionOngoing = data != null && data.moveToFirst()
                && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        if (contractionOngoing) {
            view.setImageResource(R.drawable.ic_notif_action_stop);
        } else {
            view.setImageResource(R.drawable.ic_notif_action_start);
        }
    }
}
