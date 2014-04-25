package com.ianhanniballake.contractiontimer.ui;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
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
@TargetApi(11)
public class ContractionListFragmentV11 extends ContractionListFragment {
    /**
     * Key used to store the selected item note in the bundle
     */
    private final static String SELECTED_ITEM_NOTE_KEY = "com.ianhanniballake.contractiontimer.SELECTED_ITEM_NOTE_KEY";
    /**
     * Note associated with the currently selected item
     */
    String selectedItemNote = null;

    @Override
    protected void bindView(final View view, final Cursor cursor) {
        final View showPopupView = view.findViewById(R.id.show_popup);
        // Don't allow popup menu while the Contextual Action Bar is
        // present
        showPopupView.setEnabled(getListView().getCheckedItemCount() == 0);
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

    /**
     * Sets up the ListView for multiple item selection with the Contextual Action Bar
     */
    @Override
    protected void setupListView() {
        final ListView listView = getListView();
        listView.setDrawSelectorOnTop(true);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            @Override
            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                GtmManager gtmManager = GtmManager.getInstance(getActivity());
                gtmManager.push("menu", "ContextActionBar");
                final long contractionId = listView.getCheckedItemIds()[0];
                switch (item.getItemId()) {
                    case R.id.menu_context_view:
                        if (BuildConfig.DEBUG)
                            Log.d(getClass().getSimpleName(), "Context Action Mode selected view");
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
                            Log.d(getClass().getSimpleName(),
                                    "Context Action Mode selected "
                                            + (TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note")
                            );
                        gtmManager.pushEvent("Note", DataLayer.mapOf("type",
                                TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note", "position", position));
                        showNoteDialog(contractionId, existingNote);
                        mode.finish();
                        return true;
                    case R.id.menu_context_delete:
                        final long[] selectedIds = getListView().getCheckedItemIds();
                        if (BuildConfig.DEBUG)
                            Log.d(getClass().getSimpleName(), "Context Action Mode selected delete");
                        gtmManager.pushEvent("Delete", DataLayer.mapOf("count", selectedIds.length));
                        for (final long id : selectedIds)
                            deleteContraction(id);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                final MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.list_context, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(final ActionMode mode) {
                // Nothing to do
            }

            @Override
            public void onItemCheckedStateChanged(final ActionMode mode, final int position, final long id,
                                                  final boolean checked) {
                final int selectedItemsSize = listView.getCheckedItemCount();
                if (selectedItemsSize == 0)
                    return;
                else if (selectedItemsSize == 1) {
                    final SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                    int selectedPosition = checkedItems.keyAt(0);
                    // The checked item positions sometime contain both the old and new items. We need to make sure we
                    // pick the remaining selected item, rather than the recently de-selected item.
                    if (selectedPosition == position && !checked)
                        selectedPosition = checkedItems.keyAt(1);
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
                mode.invalidate();
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
                final int selectedItemsSize = listView.getCheckedItemCount();
                // Show or hide the view menu item
                final MenuItem viewItem = menu.findItem(R.id.menu_context_view);
                final boolean showViewItem = selectedItemsSize == 1;
                viewItem.setVisible(showViewItem);
                // Set whether to display the note menu item
                final MenuItem noteItem = menu.findItem(R.id.menu_context_note);
                final boolean showNoteItem = selectedItemsSize == 1;
                // Set the title of the note menu item
                if (showNoteItem)
                    if ("".equals(selectedItemNote))
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
                final CharSequence modeTitle = mode.getTitle();
                final CharSequence newModeTitle = String.format(getString(R.string.menu_context_action_mode_title),
                        selectedItemsSize);
                mode.setTitle(newModeTitle);
                return !newModeTitle.equals(modeTitle) || !newTitle.equals(currentTitle);
            }
        });
    }

    @Override
    protected void setupNewView(final View view) {
    }
}
