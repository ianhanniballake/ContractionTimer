package com.ianhanniballake.contractiontimer.ui;

import android.database.Cursor;
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

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragmentBase extends ContractionListFragment
{
	@Override
	protected void bindView(final View view, final Cursor cursor)
	{
		// Nothing to do
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.menu_context_view:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Context Menu selected view");
				EasyTracker.getTracker().trackEvent("ContextMenu", "View", "", 0L);
				viewContraction(info.id);
				return true;
			case R.id.menu_context_note:
				final TextView noteView = (TextView) info.targetView.findViewById(R.id.note);
				final String existingNote = noteView.getText().toString();
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Context Menu selected "
							+ (existingNote.equals("") ? "Add Note" : "Edit Note"));
				EasyTracker.getTracker().trackEvent("ContextMenu", "Note",
						existingNote.equals("") ? "Add Note" : "Edit Note", (long) info.position);
				showNoteDialog(info.id, existingNote);
				return true;
			case R.id.menu_context_delete:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Context Menu selected delete");
				EasyTracker.getTracker().trackEvent("ContextMenu", "Delete", "", (long) info.position);
				deleteContraction(info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		final MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.list_context, menu);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final TextView noteView = (TextView) info.targetView.findViewById(R.id.note);
		final CharSequence note = noteView.getText();
		final MenuItem noteItem = menu.findItem(R.id.menu_context_note);
		if (note.equals(""))
			noteItem.setTitle(R.string.note_dialog_title_add);
		else
			noteItem.setTitle(R.string.note_dialog_title_edit);
		final MenuItem deleteItem = menu.findItem(R.id.menu_context_delete);
		deleteItem.setTitle(getResources().getQuantityText(R.plurals.menu_context_delete, 1));
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Context Menu Opened");
		EasyTracker.getTracker().trackEvent("ContextMenu", "Open", note.equals("") ? "Add Note" : "Edit Note", 0L);
	}

	/**
	 * Sets up the ListView with no selection and a context menu
	 */
	@Override
	protected void setupListView()
	{
		final ListView listView = getListView();
		listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listView);
	}

	@Override
	protected void setupNewView(final View view)
	{
		// Nothing to do
	}
}
