package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tagmanager.DataLayer;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

import java.util.Calendar;

/**
 * Fragment to list contractions entered by the user
 */
public abstract class ContractionListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        OnClickListener {
    private final static String TAG = ContractionListFragment.class.getSimpleName();
    /**
     * Handler of live duration updates
     */
    final Handler liveDurationHandler = new Handler();
    /**
     * Handler of time since last contraction updates
     */
    final Handler timeSinceLastHandler = new Handler();
    /**
     * Start time of the current contraction
     */
    long currentContractionStartTime = 0;
    /**
     * Reference to the Runnable live duration updater
     */
    final Runnable liveDurationUpdate = new Runnable() {
        /**
         * Updates the appropriate duration view to the current elapsed time and schedules this to rerun in 1 second
         */
        @Override
        public void run() {
            final View rootView = getView();
            if (rootView != null) {
                final TextView currentContractionDurationView = (TextView) rootView.findViewWithTag("durationView");
                if (currentContractionDurationView != null) {
                    final long durationInSeconds = (System.currentTimeMillis() - currentContractionStartTime) / 1000;
                    currentContractionDurationView.setText(DateUtils.formatElapsedTime(durationInSeconds));
                }
            }
            liveDurationHandler.postDelayed(this, 1000);
        }
    };
    /**
     * View for the header row
     */
    View headerView = null;
    /**
     * Reference to the Runnable time since last contraction updater
     */
    private final Runnable timeSinceLastUpdate = new Runnable() {
        /**
         * Updates the time since last contraction and schedules this to rerun in 1 second
         */
        @Override
        public void run() {
            if (headerView != null) {
                final TextView timeSinceLastView = (TextView) headerView.findViewById(R.id.list_header_time_since_last);
                if (timeSinceLastView != null && currentContractionStartTime != 0) {
                    final long timeSinceLastInSeconds = (System.currentTimeMillis() - currentContractionStartTime) / 1000;
                    timeSinceLastView.setText(DateUtils.formatElapsedTime(timeSinceLastInSeconds));
                }
            }
            timeSinceLastHandler.postDelayed(this, 1000);
        }
    };
    /**
     * Adapter to display the list's data
     */
    private CursorAdapter adapter;
    /**
     * Handler for asynchronous deletes of contractions
     */
    private AsyncQueryHandler contractionQueryHandler = null;

    /**
     * Do any version specific view binding
     *
     * @param view   Current view
     * @param cursor Cursor pointing to the current data
     */
    protected abstract void bindView(final View view, final Cursor cursor);

    /**
     * Deletes a given contraction
     *
     * @param id contraction id to delete
     */
    protected void deleteContraction(final long id) {
        // Ensure we don't attempt to delete contractions with invalid ids
        if (id < 0)
            return;
        final Uri deleteUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
        if (contractionQueryHandler == null)
            contractionQueryHandler = new AsyncQueryHandler(getActivity().getContentResolver()) {
                // No call backs needed
            };
        contractionQueryHandler.startDelete(0, 0, deleteUri, null, null);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getText(R.string.list_loading));
        final ListView listView = getListView();
        headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.list_header, listView, false);
        final FrameLayout headerFrame = new FrameLayout(getActivity());
        headerFrame.addView(headerView);
        listView.addHeaderView(headerFrame, null, false);
        adapter = new ContractionListCursorAdapter(getActivity());
        setListAdapter(adapter);
        setupListView();
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                viewContraction(id);
            }
        });
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onClick(final View v) {
        final PopupMenu popup = new PopupMenu(getActivity(), v);
        final MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.list_context, popup.getMenu());
        final PopupHolder popupHolder = (PopupHolder) v.getTag();
        final MenuItem noteItem = popup.getMenu().findItem(R.id.menu_context_note);
        if (popupHolder.existingNote.equals(""))
            noteItem.setTitle(R.string.note_dialog_title_add);
        else
            noteItem.setTitle(R.string.note_dialog_title_edit);
        final MenuItem deleteItem = popup.getMenu().findItem(R.id.menu_context_delete);
        deleteItem.setTitle(getResources().getQuantityText(R.plurals.menu_context_delete, 1));
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                GtmManager gtmManager = GtmManager.getInstance(ContractionListFragment.this);
                gtmManager.push("menu", "PopupMenu");
                switch (item.getItemId()) {
                    case R.id.menu_context_view:
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected view");
                        gtmManager.pushEvent("View");
                        viewContraction(popupHolder.id);
                        return true;
                    case R.id.menu_context_note:
                        String type = TextUtils.isEmpty(popupHolder.existingNote) ? "Add Note" : "Edit Note";
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected " + type);
                        gtmManager.pushEvent("Note", DataLayer.mapOf("type", type,
                                "position", DataLayer.OBJECT_NOT_PRESENT));
                        showNoteDialog(popupHolder.id, popupHolder.existingNote);
                        return true;
                    case R.id.menu_context_delete:
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected delete");
                        gtmManager.pushEvent("Delete", DataLayer.mapOf("count", 1));
                        deleteContraction(popupHolder.id);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(), getActivity().getIntent().getData(), null, null, null, null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contraction_list, container, false);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate);
        adapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate);
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate);
        adapter.swapCursor(data);
        if (data == null || data.getCount() == 0)
            setEmptyText(getText(R.string.list_empty));
        else {
            getListView().setSelection(0);
            data.moveToFirst();
            final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
            final boolean isContractionOngoing = data.isNull(endTimeColumnIndex);
            if (isContractionOngoing)
                headerView.setVisibility(View.GONE);
            else {
                final int startTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                currentContractionStartTime = data.getLong(startTimeColumnIndex);
                timeSinceLastHandler.post(timeSinceLastUpdate);
                headerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        liveDurationHandler.removeCallbacks(liveDurationUpdate);
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();
        final View rootView = getView();
        if (rootView != null) {
            final TextView currentContractionDurationView = (TextView) rootView.findViewWithTag("durationView");
            if (currentContractionDurationView != null) {
                // Ensures the live duration update is running
                liveDurationHandler.removeCallbacks(liveDurationUpdate);
                liveDurationHandler.post(liveDurationUpdate);
            }
        }
        if (headerView != null) {
            // Ensures the live time since last update is running
            timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate);
            timeSinceLastHandler.post(timeSinceLastUpdate);
        }
    }

    @Override
    public void setEmptyText(final CharSequence text) {
        final ListView listView = getListView();
        if (listView == null)
            return;
        final TextView emptyText = (TextView) listView.getEmptyView();
        if (emptyText == null)
            return;
        emptyText.setText(text);
    }

    /**
     * Sets up the ListView
     */
    protected abstract void setupListView();

    /**
     * Shows the 'note' dialog
     *
     * @param id           contraction id
     * @param existingNote existing note attached to this contraction if it exists
     */
    protected void showNoteDialog(final long id, final String existingNote) {
        // Ensure we don't attempt to change the note on contractions with
        // invalid ids
        if (id < 0)
            return;
        final NoteDialogFragment noteDialogFragment = new NoteDialogFragment();
        final Bundle args = new Bundle();
        args.putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, id);
        args.putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, existingNote);
        noteDialogFragment.setArguments(args);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Showing Dialog");
        GtmManager.getInstance(this).pushOpenScreen(TextUtils.isEmpty(existingNote) ? "NoteAdd" : "NoteEdit");
        noteDialogFragment.show(getFragmentManager(), "note");
    }

    /**
     * View the details of the given contraction
     *
     * @param id contraction id
     */
    protected void viewContraction(final long id) {
        // Ensure we don't attempt to view contractions with invalid ids
        if (id < 0)
            return;
        if (isDetached()) // Can't startActivity if we are detached
            return;
        final Uri contractionUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
        final Intent intent = new Intent(Intent.ACTION_VIEW, contractionUri).setPackage(getActivity().getPackageName());
        startActivity(intent);
    }

    /**
     * Helper class used to store temporary information to aid in handling PopupMenu item selection
     */
    static class PopupHolder {
        /**
         * A contraction's note, if any
         */
        String existingNote;
        /**
         * Cursor id for the contraction
         */
        long id;
    }

    /**
     * Cursor Adapter for creating and binding contraction list view items
     */
    private class ContractionListCursorAdapter extends CursorAdapter {
        /**
         * Local reference to the layout inflater service
         */
        private final LayoutInflater inflater;

        /**
         * @param context The context where the ListView associated with this Adapter
         */
        public ContractionListCursorAdapter(final Context context) {
            super(context, null, 0);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            String timeFormat = "hh:mm:ssaa";
            if (DateFormat.is24HourFormat(context))
                timeFormat = "kk:mm:ss";
            final char[] dateFormatOrder = DateFormat.getDateFormatOrder(mContext);
            final char[] dateFormatArray = {dateFormatOrder[0], dateFormatOrder[0], '/', dateFormatOrder[1],
                    dateFormatOrder[1]};
            final String dateFormat = new String(dateFormatArray);
            final int startTimeColumnIndex = cursor
                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
            final long startTime = cursor.getLong(startTimeColumnIndex);
            final Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(startTime);
            boolean showDateOnStartTime = false;
            final TextView startTimeView = (TextView) view.findViewById(R.id.start_time);
            final int endTimeColumnIndex = cursor.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
            final boolean isContractionOngoing = cursor.isNull(endTimeColumnIndex);
            final TextView endTimeView = (TextView) view.findViewById(R.id.end_time);
            final TextView durationView = (TextView) view.findViewById(R.id.duration);
            final Calendar endCal = Calendar.getInstance();
            boolean showDateOnEndTime = false;
            if (isContractionOngoing) {
                durationView.setText("");
                currentContractionStartTime = startTime;
                durationView.setTag("durationView");
                liveDurationHandler.removeCallbacks(liveDurationUpdate);
                liveDurationHandler.post(liveDurationUpdate);
            } else {
                final long endTime = cursor.getLong(endTimeColumnIndex);
                endCal.setTimeInMillis(endTime);
                durationView.setTag("");
                final long durationInSeconds = (endTime - startTime) / 1000;
                durationView.setText(DateUtils.formatElapsedTime(durationInSeconds));
            }
            if (startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)
                    || startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR))
                showDateOnEndTime = true;
            final TextView frequencyView = (TextView) view.findViewById(R.id.frequency);
            // If we aren't the last entry, move to the next (previous in time)
            // contraction to get its start time to compute the frequency
            if (!cursor.isLast() && cursor.moveToNext()) {
                final int prevContractionStartTimeColumnIndex = cursor
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long prevContractionStartTime = cursor.getLong(prevContractionStartTimeColumnIndex);
                final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
                frequencyView.setText(DateUtils.formatElapsedTime(frequencyInSeconds));
                // Check to see if the date changed between Contractions
                final int prevContractionEndTimeColumnIndex = cursor
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
                final long prevContractionEndTime = cursor.getLong(prevContractionEndTimeColumnIndex);
                final Calendar prevEndCal = Calendar.getInstance();
                prevEndCal.setTimeInMillis(prevContractionEndTime);
                if (startCal.get(Calendar.YEAR) != prevEndCal.get(Calendar.YEAR)
                        || startCal.get(Calendar.DAY_OF_YEAR) != prevEndCal.get(Calendar.DAY_OF_YEAR))
                    showDateOnStartTime = true;
                // Go back to the previous spot
                cursor.moveToPrevious();
            } else {
                frequencyView.setText("");
                // Always show the date on the very first start time
                showDateOnStartTime = true;
            }
            startTimeView.setText(DateFormat.format(timeFormat, startCal)
                    + (showDateOnStartTime ? " " + DateFormat.format(dateFormat, startCal) : ""));
            if (isContractionOngoing)
                endTimeView.setText(" ");
            else
                endTimeView.setText(DateFormat.format(timeFormat, endCal)
                        + (showDateOnEndTime ? " " + DateFormat.format(dateFormat, endCal) : ""));
            final int noteColumnIndex = cursor.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
            final String note = cursor.getString(noteColumnIndex);
            final TextView noteView = (TextView) view.findViewById(R.id.note);
            noteView.setText(note);
            if (TextUtils.isEmpty(note))
                noteView.setVisibility(View.GONE);
            else
                noteView.setVisibility(View.VISIBLE);
            final View showPopupView = view.findViewById(R.id.show_popup);
            final Object showPopupTag = showPopupView.getTag();
            PopupHolder popupHolder;
            if (showPopupTag == null) {
                popupHolder = new PopupHolder();
                showPopupView.setTag(popupHolder);
            } else
                popupHolder = (PopupHolder) showPopupTag;
            final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
            popupHolder.id = cursor.getLong(idColumnIndex);
            popupHolder.existingNote = note;
            ContractionListFragment.this.bindView(view, cursor);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            final View view = inflater.inflate(R.layout.list_item_contraction, parent, false);
            final ImageButton showPopup = (ImageButton) view.findViewById(R.id.show_popup);
            showPopup.setOnClickListener(ContractionListFragment.this);
            return view;
        }
    }
}
