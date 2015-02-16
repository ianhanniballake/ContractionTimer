package com.ianhanniballake.contractiontimer.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.gms.tagmanager.DataLayer;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragmentV11 extends ContractionListFragment {
    private final static String TAG = ContractionListFragmentV11.class.getSimpleName();
    /**
     * Key used to store the selected item note in the bundle
     */
    private final static String SELECTED_ITEM_NOTE_KEY = "com.ianhanniballake.contractiontimer.SELECTED_ITEM_NOTE_KEY";
    /**
     * Note associated with the currently selected item
     */
    String selectedItemNote = null;
    ActionMode mActionMode;

    @Override
    protected void bindView(final View view, final Cursor cursor) {
        final View showPopupView = view.findViewById(R.id.show_popup);
        // Don't allow popup menu while the Contextual Action Bar is
        // present
        showPopupView.setEnabled(mActionMode == null);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        if (savedInstanceState != null)
            selectedItemNote = savedInstanceState.getString(ContractionListFragmentV11.SELECTED_ITEM_NOTE_KEY);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContractionListFragmentV11.SELECTED_ITEM_NOTE_KEY, selectedItemNote);
    }

    protected void itemSelected(ListView listView, int position) {
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

    /**
     * Sets up the ListView for multiple item selection with the Contextual Action Bar
     */
    @Override
    protected void setupListView() {
        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                if (mActionMode == null) {
                    viewContraction(id);
                } else {
                    itemSelected(listView, position);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                if (mActionMode != null) {
                    listView.setItemChecked(position, !listView.isItemChecked(position));
                    itemSelected(listView, position);
                    return true;
                }
                listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                listView.setItemChecked(position, true);
                ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
                actionBarActivity.startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                        GtmManager gtmManager = GtmManager.getInstance(getActivity());
                        gtmManager.push("menu", "ContextActionBar");
                        final long contractionId = listView.getCheckedItemIds()[0];
                        switch (menuItem.getItemId()) {
                            case R.id.menu_context_view:
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected view");
                                gtmManager.pushEvent("View");
                                viewContraction(contractionId);
                                return true;
                            case R.id.menu_context_note:
                                final int position = listView.getCheckedItemPositions().keyAt(0);
                                final Cursor cursor = (Cursor) listView.getAdapter().getItem(position);
                                final int noteColumnIndex = cursor
                                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
                                final String existingNote = cursor.getString(noteColumnIndex);
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected " + (TextUtils.isEmpty(existingNote) ?
                                            "Add Note" : "Edit Note"));
                                gtmManager.pushEvent("Note", DataLayer.mapOf("type",
                                        TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note", "position", position));
                                showNoteDialog(contractionId, existingNote);
                                actionMode.finish();
                                return true;
                            case R.id.menu_context_delete:
                                final long[] selectedIds = getListView().getCheckedItemIds();
                                if (BuildConfig.DEBUG)
                                    Log.d(TAG, "Context Action Mode selected delete");
                                gtmManager.pushEvent("Delete", DataLayer.mapOf("count", selectedIds.length));
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
                        SparseBooleanArray selectedItems = listView.getCheckedItemPositions();
                        if (selectedItems != null) {
                            for (int i = 0; i < selectedItems.size(); i++) {
                                listView.setItemChecked(selectedItems.keyAt(i), false);
                            }
                        }
                        listView.post(new Runnable() {
                            @Override
                            public void run() {
                                listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
                            }
                        });
                        mActionMode = null;
                    }

                    @Override
                    public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                        final int selectedItemsSize = listView.getCheckedItemIds().length;
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
    }
}
