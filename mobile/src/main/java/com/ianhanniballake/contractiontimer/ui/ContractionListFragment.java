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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ActionMenuView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

import java.lang.ref.WeakReference;
import java.util.Calendar;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = ContractionListFragment.class.getSimpleName();
    /**
     * Key used to store the selected item note in the bundle
     */
    private final static String SELECTED_ITEM_NOTE_KEY = "com.ianhanniballake.contractiontimer.SELECTED_ITEM_NOTE_KEY";
    /**
     * Handler of live duration updates
     */
    final Handler liveDurationHandler = new Handler();
    /**
     * Handler of time since last contraction updates
     */
    final Handler timeSinceLastHandler = new Handler();
    /**
     * Note associated with the currently selected item
     */
    String selectedItemNote = null;
    /**
     * Current ActionMode, if any
     */
    ActionMode mActionMode;
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
    ListView mListView;
    ViewAnimator mEmptyView;
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
     * Column headers view
     */
    private ViewGroup mColumnHeaders;
    /**
     * Adapter to display the list's data
     */
    private CursorAdapter adapter;
    /**
     * Handler for asynchronous deletes of contractions
     */
    private AsyncQueryHandler contractionQueryHandler = null;

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
        if (contractionQueryHandler == null) {
            contractionQueryHandler = new DeleteContractionQueryHandler(getActivity());
        }
        contractionQueryHandler.startDelete(0, 0, deleteUri, null, null);
    }

    private void itemSelected(ListView listView, int position) {
        final long[] checkedItems = listView.getCheckedItemIds();
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Item clicked: " + checkedItems.length);
        if (checkedItems.length == 0) {
            mActionMode.finish();
            return;
        } else if (checkedItems.length == 1) {
            SparseBooleanArray checked = listView.getCheckedItemPositions();
            int selectedPosition = checked.keyAt(0);
            // The checked item positions sometime contain both the old and new items. We need to make sure we
            // pick the remaining selected item, rather than the recently de-selected item.
            if (selectedPosition == position && listView.isItemChecked(position)) {
                selectedPosition = checked.keyAt(1);
            }
            final ListAdapter adapter = listView.getAdapter();
            if (adapter.isEmpty()) // onLoaderReset swapped in a null cursor
                return;
            final Cursor cursor = (Cursor) adapter.getItem(selectedPosition);
            if (cursor == null) // Ensure a valid cursor
                return;
            final int noteColumnIndex = cursor
                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
            selectedItemNote = cursor.getString(noteColumnIndex);
        }
        mActionMode.invalidate();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContractionListFragment.SELECTED_ITEM_NOTE_KEY, selectedItemNote);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null)
            selectedItemNote = savedInstanceState.getString(ContractionListFragment.SELECTED_ITEM_NOTE_KEY);
        headerView = getLayoutInflater(savedInstanceState).inflate(R.layout.list_header, mListView, false);
        final FrameLayout headerFrame = new FrameLayout(getActivity());
        headerFrame.addView(headerView);
        mListView.addHeaderView(headerFrame, null, false);
        adapter = new ContractionListCursorAdapter(getActivity());
        mListView.setAdapter(adapter);
        mListView.setDrawSelectorOnTop(true);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                if (mActionMode == null) {
                    viewContraction(id);
                } else {
                    itemSelected(mListView, position);
                }
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                if (mActionMode != null) {
                    mListView.setItemChecked(position, !mListView.isItemChecked(position));
                    itemSelected(mListView, position);
                    return true;
                }
                mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                mListView.setItemChecked(position, true);
                AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
                appCompatActivity.startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
                        final long contractionId = mListView.getCheckedItemIds()[0];
                        switch (menuItem.getItemId()) {
                            case R.id.menu_context_view:
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected view");
                                analytics.logEvent("view_cab", null);
                                viewContraction(contractionId);
                                return true;
                            case R.id.menu_context_note:
                                final int position = mListView.getCheckedItemPositions().keyAt(0);
                                final Cursor cursor = (Cursor) mListView.getAdapter().getItem(position);
                                final int noteColumnIndex = cursor
                                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
                                final String existingNote = cursor.getString(noteColumnIndex);
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected " + (TextUtils.isEmpty(existingNote) ?
                                            "Add Note" : "Edit Note"));
                                String noteEvent = TextUtils.isEmpty(existingNote) ? "note_add_cab" : "note_edit_cab";
                                analytics.logEvent(noteEvent, null);
                                showNoteDialog(contractionId, existingNote);
                                actionMode.finish();
                                return true;
                            case R.id.menu_context_delete:
                                final long[] selectedIds = ContractionListFragment.this.mListView.getCheckedItemIds();
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected delete");
                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.VALUE, Integer.toString(selectedIds.length));
                                analytics.logEvent("delete_cab", bundle);
                                for (final long id : selectedIds)
                                    deleteContraction(id);
                                actionMode.finish();
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public boolean onCreateActionMode(final ActionMode actionMode, final Menu menu) {
                        mActionMode = actionMode;
                        final MenuInflater inflater = actionMode.getMenuInflater();
                        inflater.inflate(R.menu.list_context, menu);
                        return true;
                    }

                    @Override
                    public void onDestroyActionMode(final ActionMode actionMode) {
                        SparseBooleanArray selectedItems = mListView.getCheckedItemPositions();
                        if (selectedItems != null) {
                            for (int i = 0; i < selectedItems.size(); i++) {
                                mListView.setItemChecked(selectedItems.keyAt(i), false);
                            }
                        }
                        mListView.post(new Runnable() {
                            @Override
                            public void run() {
                                mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                            }
                        });
                        mActionMode = null;
                    }

                    @Override
                    public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                        final int selectedItemsSize = mListView.getCheckedItemIds().length;
                        // Show or hide the view menu item
                        final MenuItem viewItem = menu.findItem(R.id.menu_context_view);
                        final boolean showViewItem = selectedItemsSize == 1;
                        viewItem.setVisible(showViewItem);
                        // Set whether to display the note menu item
                        final MenuItem noteItem = menu.findItem(R.id.menu_context_note);
                        final boolean showNoteItem = selectedItemsSize == 1;
                        // Set the title of the note menu item
                        if (showNoteItem)
                            if (TextUtils.isEmpty(selectedItemNote))
                                noteItem.setTitle(R.string.note_dialog_title_add);
                            else
                                noteItem.setTitle(R.string.note_dialog_title_edit);
                        noteItem.setVisible(showNoteItem);
                        // Set the title of the delete menu item
                        final MenuItem deleteItem = menu.findItem(R.id.menu_context_delete);
                        final CharSequence currentTitle = deleteItem.getTitle();
                        final CharSequence newTitle = getResources().getQuantityText(R.plurals.menu_context_delete,
                                selectedItemsSize);
                        deleteItem.setTitle(newTitle);
                        // Set the Contextual Action Bar title with the new item size
                        final CharSequence modeTitle = actionMode.getTitle();
                        final CharSequence newModeTitle = String.format(getString(R.string.menu_context_action_mode_title),
                                selectedItemsSize);
                        actionMode.setTitle(newModeTitle);
                        return !newModeTitle.equals(modeTitle) || !newTitle.equals(currentTitle);
                    }
                });
                return true;
            }
        });
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(getActivity(), getActivity().getIntent().getData(), null, null, null, null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contraction_list, container, false);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mEmptyView = (ViewAnimator) view.findViewById(android.R.id.empty);
        mListView.setEmptyView(mEmptyView);
        mColumnHeaders = (ViewGroup) view.findViewById(R.id.list_column_headers);
        return view;
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate);
        mColumnHeaders.setVisibility(View.GONE);
        adapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate);
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate);
        adapter.swapCursor(data);
        if (data == null || data.getCount() == 0) {
            mColumnHeaders.setVisibility(View.GONE);
            mEmptyView.setDisplayedChild(1);
        } else {
            mColumnHeaders.setVisibility(View.VISIBLE);
            mListView.setSelection(0);
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

    private static class DeleteContractionQueryHandler extends AsyncQueryHandler {
        private WeakReference<Context> mContext;

        public DeleteContractionQueryHandler(final Context context) {
            super(context.getContentResolver());
            mContext = new WeakReference<>(context.getApplicationContext());
        }

        @Override
        protected void onDeleteComplete(final int token, final Object cookie, final int result) {
            Context context = mContext.get();
            if (context != null) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context);
                NotificationUpdateService.updateNotification(context);
            }
        }
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
            inflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            String timeFormat = "hh:mm:ssa";
            if (DateFormat.is24HourFormat(context))
                timeFormat = "kk:mm:ss";
            String dateFormat;
            try {
                final char[] dateFormatOrder = DateFormat.getDateFormatOrder(mContext);
                final char[] dateFormatArray = {dateFormatOrder[0], dateFormatOrder[0], '/', dateFormatOrder[1],
                        dateFormatOrder[1]};
                dateFormat = new String(dateFormatArray);
            } catch (IllegalArgumentException e) {
                dateFormat = "MM/dd";
            }
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
            final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
            final long id = cursor.getLong(idColumnIndex);
            final ActionMenuView showPopupView = (ActionMenuView) view.findViewById(R.id.show_popup);
            final MenuItem noteItem = showPopupView.getMenu().findItem(R.id.menu_context_note);
            if (TextUtils.isEmpty(note))
                noteItem.setTitle(R.string.note_dialog_title_add);
            else
                noteItem.setTitle(R.string.note_dialog_title_edit);
            final MenuItem deleteItem = showPopupView.getMenu().findItem(R.id.menu_context_delete);
            deleteItem.setTitle(getResources().getQuantityText(R.plurals.menu_context_delete, 1));
            showPopupView.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getContext());
                    switch (item.getItemId()) {
                        case R.id.menu_context_view:
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Popup Menu selected view");
                            analytics.logEvent("view_popup", null);
                            viewContraction(id);
                            return true;
                        case R.id.menu_context_note:
                            String type = TextUtils.isEmpty(note) ? "Add Note" : "Edit Note";
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Popup Menu selected " + type);
                            String noteEvent = TextUtils.isEmpty(note) ? "note_add_popup" : "note_edit_popup";
                            analytics.logEvent(noteEvent, null);
                            showNoteDialog(id, note);
                            return true;
                        case R.id.menu_context_delete:
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Popup Menu selected delete");
                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.VALUE, Integer.toString(1));
                            analytics.logEvent("delete_popup", bundle);
                            deleteContraction(id);
                            return true;
                        default:
                            return false;
                    }
                }
            });
            // Don't allow popup menu while the Contextual Action Bar is present
            showPopupView.setEnabled(mActionMode == null);
        }

        @Override
        public View newView(final Context context, final Cursor cursor, final ViewGroup parent) {
            final View view = inflater.inflate(R.layout.list_item_contraction, parent, false);
            final ActionMenuView showPopup = (ActionMenuView) view.findViewById(R.id.show_popup);
            MenuInflater menuInflater = getActivity().getMenuInflater();
            menuInflater.inflate(R.menu.list_popup, showPopup.getMenu());
            return view;
        }
    }
}
