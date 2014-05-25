package com.ianhanniballake.contractiontimer.ui;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tagmanager.DataLayer;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragmentBase extends ContractionListFragment {
    private final static String TAG = ContractionListFragmentBase.class.getSimpleName();

    @Override
    protected void bindView(final View view, final Cursor cursor) {
        // Nothing to do
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        GtmManager gtmManager = GtmManager.getInstance(this);
        gtmManager.push(DataLayer.mapOf("menu", "ContextMenu", "position", info.position));
        switch (item.getItemId()) {
            case R.id.menu_context_view:
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Context Menu selected view");
                gtmManager.pushEvent("View");
                viewContraction(info.id);
                return true;
            case R.id.menu_context_note:
                final TextView noteView = (TextView) info.targetView.findViewById(R.id.note);
                final String existingNote = noteView.getText().toString();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Context Menu selected " + (TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note"));
                gtmManager.pushEvent("Note", DataLayer.mapOf("type",
                        TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note"));
                showNoteDialog(info.id, existingNote);
                return true;
            case R.id.menu_context_delete:
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Context Menu selected delete");
                gtmManager.pushEvent("Delete", DataLayer.mapOf("count", 1));
                deleteContraction(info.id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.list_context, menu);
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final TextView noteView = (TextView) info.targetView.findViewById(R.id.note);
        final CharSequence note = noteView.getText();
        final MenuItem noteItem = menu.findItem(R.id.menu_context_note);
        if (TextUtils.isEmpty(note))
            noteItem.setTitle(R.string.note_dialog_title_add);
        else
            noteItem.setTitle(R.string.note_dialog_title_edit);
        final MenuItem deleteItem = menu.findItem(R.id.menu_context_delete);
        deleteItem.setTitle(getResources().getQuantityText(R.plurals.menu_context_delete, 1));
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Context Menu Opened");
        GtmManager.getInstance(this).pushEvent("Open", DataLayer.mapOf(
                "menu", "ContextMenu", "type", "count", DataLayer.OBJECT_NOT_PRESENT,
                TextUtils.isEmpty(note) ? "Add Note" : "Edit Note"));
    }

    /**
     * Sets up the ListView with no selection and a context menu
     */
    @Override
    protected void setupListView() {
        final ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(listView);
    }

    @Override
    protected void setupNewView(final View view) {
        // Nothing to do
    }
}
