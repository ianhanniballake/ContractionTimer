package com.ianhanniballake.contractiontimer.ui;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment to list contractions entered by the user
 */
@TargetApi(11)
public class ContractionListFragmentV11 extends ContractionListFragment
		implements OnClickListener
{
	/**
	 * Helper class used to store temporary information to aid in handling
	 * PopupMenu item selection
	 */
	static class PopupHolder
	{
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
	 * Key used to store the selected item note in the bundle
	 */
	private final static String SELECTED_ITEM_NOTE_KEY = "com.ianhanniballake.contractiontimer.SELECTED_ITEM_NOTE_KEY";
	/**
	 * Note associated with the currently selected item
	 */
	private String selectedItemNote = null;

	@Override
	protected void bindView(final View view, final Cursor cursor)
	{
		final View showPopupView = view.findViewById(R.id.show_popup);
		final Object showPopupTag = showPopupView.getTag();
		PopupHolder popupHolder;
		if (showPopupTag == null)
		{
			popupHolder = new PopupHolder();
			showPopupView.setTag(popupHolder);
		}
		else
			popupHolder = (PopupHolder) showPopupTag;
		final int idColumnIndex = cursor.getColumnIndex(BaseColumns._ID);
		popupHolder.id = cursor.getLong(idColumnIndex);
		final int noteColumnIndex = cursor
				.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
		final String note = cursor.getString(noteColumnIndex);
		popupHolder.existingNote = note;
		// Don't allow popup menu while the Contextual Action Bar is
		// present
		showPopupView.setEnabled(getListView().getCheckedItemCount() == 0);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
			selectedItemNote = savedInstanceState
					.getString(ContractionListFragmentV11.SELECTED_ITEM_NOTE_KEY);
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onClick(final View v)
	{
		final PopupMenu popup = new PopupMenu(getActivity(), v);
		final MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.list_context, popup.getMenu());
		final PopupHolder popupHolder = (PopupHolder) v.getTag();
		final MenuItem noteItem = popup.getMenu().findItem(
				R.id.menu_context_note);
		if (popupHolder.existingNote.equals(""))
			noteItem.setTitle(R.string.note_dialog_title_add);
		else
			noteItem.setTitle(R.string.note_dialog_title_edit);
		final MenuItem deleteItem = popup.getMenu().findItem(
				R.id.menu_context_delete);
		deleteItem.setTitle(getResources().getQuantityText(
				R.plurals.menu_context_delete, 1));
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(final MenuItem item)
			{
				switch (item.getItemId())
				{
					case R.id.menu_context_view:
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Popup Menu selected view");
						EasyTracker.getTracker().trackEvent("PopupMenu",
								"View", "", 0L);
						viewContraction(popupHolder.id);
						return true;
					case R.id.menu_context_note:
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Popup Menu selected "
											+ (popupHolder.existingNote
													.equals("") ? "Add Note"
													: "Edit Note"));
						EasyTracker
								.getTracker()
								.trackEvent(
										"PopupMenu",
										"Note",
										popupHolder.existingNote.equals("") ? "Add Note"
												: "Edit Note", 0L);
						showNoteDialog(popupHolder.id, popupHolder.existingNote);
						return true;
					case R.id.menu_context_delete:
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Popup Menu selected delete");
						EasyTracker.getTracker().trackEvent("PopupMenu",
								"Delete", "", 0L);
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
	public void onSaveInstanceState(final Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(ContractionListFragmentV11.SELECTED_ITEM_NOTE_KEY,
				selectedItemNote);
	}

	/**
	 * Sets up the ListView for multiple item selection with the Contextual
	 * Action Bar
	 */
	@Override
	protected void setupListView()
	{
		final ListView listView = getListView();
		listView.setDrawSelectorOnTop(true);
		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener()
		{
			@Override
			public boolean onActionItemClicked(final ActionMode mode,
					final MenuItem item)
			{
				final long contractionId = listView.getCheckedItemIds()[0];
				switch (item.getItemId())
				{
					case R.id.menu_context_view:
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Context Action Mode selected view");
						EasyTracker.getTracker().trackEvent("ContextActionBar",
								"View", "", 0L);
						viewContraction(contractionId);
						return true;
					case R.id.menu_context_note:
						final int position = listView.getCheckedItemPositions()
								.keyAt(0);
						final Cursor cursor = (Cursor) listView.getAdapter()
								.getItem(position);
						final int noteColumnIndex = cursor
								.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
						final String existingNote = cursor
								.getString(noteColumnIndex);
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Context Action Mode selected "
											+ (existingNote.equals("") ? "Add Note"
													: "Edit Note"));
						EasyTracker.getTracker().trackEvent(
								"ContextActionBar",
								"Note",
								existingNote.equals("") ? "Add Note"
										: "Edit Note", (long) position);
						showNoteDialog(contractionId, existingNote);
						mode.finish();
						return true;
					case R.id.menu_context_delete:
						final long[] selectedIds = getListView()
								.getCheckedItemIds();
						if (BuildConfig.DEBUG)
							Log.d(getClass().getSimpleName(),
									"Context Action Mode selected delete");
						EasyTracker.getTracker().trackEvent("ContextActionBar",
								"Delete", "", (long) selectedIds.length);
						for (final long id : selectedIds)
							deleteContraction(id);
						mode.finish();
						return true;
					default:
						return false;
				}
			}

			@Override
			public boolean onCreateActionMode(final ActionMode mode,
					final Menu menu)
			{
				final MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.list_context, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(final ActionMode mode)
			{
				// Nothing to do
			}

			@Override
			public void onItemCheckedStateChanged(final ActionMode mode,
					final int position, final long id, final boolean checked)
			{
				final int selectedItemsSize = listView.getCheckedItemCount();
				if (selectedItemsSize == 0)
					return;
				// This is called in the middle of the ListView's selected items
				// being refreshed (in a state where the getCheckedItemCount
				// call returns the new number of items, but the
				// getCheckedItemPositions() call returns the old items.
				// Therefore to give the ListView some time to stabilize, we
				// post this call to invalidate
				getView().post(new Runnable()
				{
					@Override
					public void run()
					{
						mode.invalidate();
					}
				});
			}

			@Override
			public boolean onPrepareActionMode(final ActionMode mode,
					final Menu menu)
			{
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
				{
					final int position = listView.getCheckedItemPositions()
							.keyAt(0);
					final ListAdapter adapter = listView.getAdapter();
					@SuppressWarnings("resource")
					final Cursor cursor = position < adapter.getCount() ? (Cursor) adapter
							.getItem(position) : null;
					// The cursor will be null when first resuming the Fragment
					// so we'll used the selectedItemNote loaded from the Bundle
					if (cursor != null)
					{
						final int noteColumnIndex = cursor
								.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
						selectedItemNote = cursor.getString(noteColumnIndex);
					}
					if ("".equals(selectedItemNote))
						noteItem.setTitle(R.string.note_dialog_title_add);
					else
						noteItem.setTitle(R.string.note_dialog_title_edit);
				}
				noteItem.setVisible(showNoteItem);
				// Set the title of the delete menu item
				final MenuItem deleteItem = menu
						.findItem(R.id.menu_context_delete);
				final CharSequence currentTitle = deleteItem.getTitle();
				final CharSequence newTitle = getResources().getQuantityText(
						R.plurals.menu_context_delete, selectedItemsSize);
				deleteItem.setTitle(newTitle);
				// Set the Contextual Action Bar title with the new item
				// size
				final CharSequence modeTitle = mode.getTitle();
				final CharSequence newModeTitle = String.format(
						getString(R.string.menu_context_action_mode_title),
						selectedItemsSize);
				mode.setTitle(newModeTitle);
				return !newModeTitle.equals(modeTitle)
						|| !newTitle.equals(currentTitle);
			}
		});
	}

	@Override
	protected void setupNewView(final View view)
	{
		final Button showPopup = (Button) view.findViewById(R.id.show_popup);
		showPopup.setOnClickListener(this);
	}
}
