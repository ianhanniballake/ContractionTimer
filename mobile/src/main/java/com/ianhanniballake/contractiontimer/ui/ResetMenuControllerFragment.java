package com.ianhanniballake.contractiontimer.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Headless fragment which controls the Reset action in the MainActivity ActionBar, enabling/disabling it based on
 * whether there are contractions to reset
 */
public class ResetMenuControllerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = ResetMenuControllerFragment.class.getSimpleName();
    /**
     * Cursor Adapter which holds the current contractions
     */
    private CursorAdapter adapter;

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
        setHasOptionsMenu(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final String[] projection = {BaseColumns._ID};
        return new CursorLoader(getActivity(), ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        adapter.swapCursor(null);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        adapter.swapCursor(data);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_reset_menu_controller, menu);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final int contractionCount = adapter == null ? 0 : adapter.getCount();
        final boolean hasContractions = contractionCount > 0;
        final MenuItem reset = menu.findItem(R.id.menu_reset);
        reset.setEnabled(hasContractions);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reset:
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Menu selected Reset");
                final ResetDialogFragment resetDialogFragment = new ResetDialogFragment();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Showing Dialog");
                FirebaseAnalytics.getInstance(getContext()).logEvent("reset_open", null);
                resetDialogFragment.show(getFragmentManager(), "reset");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
