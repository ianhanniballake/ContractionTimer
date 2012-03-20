package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>, OnClickListener
{
	/**
	 * Cursor Adapter for creating and binding contraction list view items
	 */
	private class ContractionListCursorAdapter extends CursorAdapter
	{
		/**
		 * Local reference to the layout inflater service
		 */
		private final LayoutInflater inflater;

		/**
		 * @param context
		 *            The context where the ListView associated with this
		 *            SimpleListItemFactory is running
		 * @param c
		 *            The database cursor. Can be null if the cursor is not
		 *            available yet.
		 * @param flags
		 *            Flags used to determine the behavior of the adapter, as
		 *            per
		 *            {@link CursorAdapter#CursorAdapter(Context, Cursor, int)}.
		 */
		public ContractionListCursorAdapter(final Context context,
				final Cursor c, final int flags)
		{
			super(context, c, flags);
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor)
		{
			final Object viewTag = view.getTag();
			ViewHolder holder;
			if (viewTag == null)
			{
				holder = new ViewHolder();
				holder.startTime = (TextView) view
						.findViewById(R.id.start_time);
				holder.endTime = (TextView) view.findViewById(R.id.end_time);
				holder.duration = (TextView) view.findViewById(R.id.duration);
				holder.frequency = (TextView) view.findViewById(R.id.frequency);
				holder.note = (TextView) view.findViewById(R.id.note);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					holder.showPopup = (Button) view
							.findViewById(R.id.show_popup);
				view.setTag(holder);
			}
			else
				holder = (ViewHolder) viewTag;
			String timeFormat = "hh:mm:ssaa";
			if (DateFormat.is24HourFormat(context))
				timeFormat = "kk:mm:ss";
			final int startTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = cursor.getLong(startTimeColumnIndex);
			holder.startTime.setText(DateFormat.format(timeFormat, startTime));
			final int endTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			final boolean isContractionOngoing = cursor
					.isNull(endTimeColumnIndex);
			if (isContractionOngoing)
			{
				holder.endTime.setText(" ");
				holder.duration.setText("");
				currentContractionStartTime = startTime;
				holder.duration.setTag("durationView");
				liveDurationHandler.removeCallbacks(liveDurationUpdate);
				liveDurationHandler.post(liveDurationUpdate);
			}
			else
			{
				final long endTime = cursor.getLong(endTimeColumnIndex);
				holder.endTime.setText(DateFormat.format(timeFormat, endTime));
				holder.duration.setTag("");
				final long durationInSeconds = (endTime - startTime) / 1000;
				holder.duration.setText(DateUtils
						.formatElapsedTime(durationInSeconds));
			}
			// If we aren't the last entry, move to the next (previous in time)
			// contraction to get its start time to compute the frequency
			if (!cursor.isLast() && cursor.moveToNext())
			{
				final int prevContractionStartTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
				final long prevContractionStartTime = cursor
						.getLong(prevContractionStartTimeColumnIndex);
				final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
				holder.frequency.setText(DateUtils
						.formatElapsedTime(frequencyInSeconds));
				// Go back to the previous spot
				cursor.moveToPrevious();
			}
			else
				holder.frequency.setText("");
			final int noteColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
			final String note = cursor.getString(noteColumnIndex);
			holder.note.setText(note);
			if (note.equals(""))
				holder.note.setVisibility(View.GONE);
			else
				holder.note.setVisibility(View.VISIBLE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			{
				final Object showPopupTag = holder.showPopup.getTag();
				PopupHolder popupHolder;
				if (showPopupTag == null)
				{
					popupHolder = new PopupHolder();
					holder.showPopup.setTag(popupHolder);
				}
				else
					popupHolder = (PopupHolder) showPopupTag;
				final int idColumnIndex = cursor
						.getColumnIndex(BaseColumns._ID);
				popupHolder.id = cursor.getLong(idColumnIndex);
				popupHolder.existingNote = note;
				// Don't allow popup menu while the Contextual Action Bar is
				// present
				holder.showPopup
						.setEnabled(getListView().getCheckedItemCount() == 0);
			}
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent)
		{
			final View view = inflater.inflate(R.layout.list_item_contraction,
					parent, false);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			{
				final Button showPopup = (Button) view
						.findViewById(R.id.show_popup);
				showPopup.setOnClickListener(ContractionListFragment.this);
			}
			return view;
		}
	}

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
	 * Helper class used to store temporary references to list item views
	 */
	static class ViewHolder
	{
		/**
		 * TextView representing the duration of the contraction
		 */
		TextView duration;
		/**
		 * TextView representing the formatted end time of the contraction
		 */
		TextView endTime;
		/**
		 * TextView representing the frequency of the contraction in relation to
		 * the previous contraction
		 */
		TextView frequency;
		/**
		 * TextView representing the note attached to the contraction
		 */
		TextView note;
		/**
		 * Button to trigger the PopupMenu on v11+ devices
		 */
		Button showPopup;
		/**
		 * TextView representing the formatted start time of the contraction
		 */
		TextView startTime;
	}

	/**
	 * Adapter to display the list's data
	 */
	private CursorAdapter adapter;
	/**
	 * Handler for asynchronous deletes of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;
	/**
	 * Start time of the current contraction
	 */
	private long currentContractionStartTime = 0;
	/**
	 * Handler of live duration updates
	 */
	private final Handler liveDurationHandler = new Handler();
	/**
	 * Reference to the Runnable live duration updater
	 */
	private final Runnable liveDurationUpdate = new Runnable()
	{
		/**
		 * Updates the appropriate duration view to the current elapsed time and
		 * schedules this to rerun in 1 second
		 */
		@Override
		public void run()
		{
			final View rootView = getView();
			if (rootView != null)
			{
				final TextView currentContractionDurationView = (TextView) rootView
						.findViewWithTag("durationView");
				if (currentContractionDurationView != null)
				{
					final long durationInSeconds = (System.currentTimeMillis() - currentContractionStartTime) / 1000;
					currentContractionDurationView.setText(DateUtils
							.formatElapsedTime(durationInSeconds));
				}
			}
			liveDurationHandler.postDelayed(this, 1000);
		}
	};

	/**
	 * Deletes a given contraction
	 * 
	 * @param id
	 *            contraction id to delete
	 */
	private void deleteContraction(final long id)
	{
		final Uri deleteUri = ContentUris.withAppendedId(
				ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
		contractionQueryHandler.startDelete(0, 0, deleteUri, null, null);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.list_loading));
		adapter = new ContractionListCursorAdapter(getActivity(), null, 0);
		setListAdapter(adapter);
		final ListView listView = getListView();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			listView.setDrawSelectorOnTop(true);
			listView.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(final AdapterView<?> parent,
						final View view, final int position, final long id)
				{
					// We need to launch a new activity to display the details
					final Intent intent = new Intent(getActivity(),
							ViewActivity.class);
					intent.putExtra(BaseColumns._ID, id);
					startActivity(intent);
				}
			});
			listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
			listView.setMultiChoiceModeListener(new MultiChoiceModeListener()
			{
				@Override
				public boolean onActionItemClicked(final ActionMode mode,
						final MenuItem item)
				{
					switch (item.getItemId())
					{
						case R.id.menu_context_note:
							final long contractionId = listView
									.getCheckedItemIds()[0];
							final int position = listView
									.getCheckedItemPositions().keyAt(0);
							final Cursor cursor = (Cursor) listView
									.getItemAtPosition(position);
							final int noteColumnIndex = cursor
									.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
							final String existingNote = cursor
									.getString(noteColumnIndex);
							Log.d(getClass().getSimpleName(),
									"Context Action Mode selected "
											+ (existingNote.equals("") ? "Add Note"
													: "Edit Note"));
							AnalyticsManagerService.trackEvent(getActivity(),
									"ContextActionBar", "Note", existingNote
											.equals("") ? "Add Note"
											: "Edit Note", position);
							showNoteDialog(contractionId, existingNote);
							mode.finish();
							return true;
						case R.id.menu_context_delete:
							final long[] selectedIds = getListView()
									.getCheckedItemIds();
							Log.d(getClass().getSimpleName(),
									"Context Action Mode selected delete");
							AnalyticsManagerService.trackEvent(getActivity(),
									"ContextActionBar", "Delete", "",
									selectedIds.length);
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
					mode.invalidate();
				}

				@Override
				public boolean onPrepareActionMode(final ActionMode mode,
						final Menu menu)
				{
					final int selectedItemsSize = listView
							.getCheckedItemCount();
					// Set whether to display the note menu item
					final MenuItem noteItem = menu
							.findItem(R.id.menu_context_note);
					final boolean showNoteItem = selectedItemsSize == 1;
					// Set the title of the note menu item
					if (showNoteItem)
					{
						final int position = listView.getCheckedItemPositions()
								.keyAt(0);
						final Cursor cursor = (Cursor) listView
								.getItemAtPosition(position);
						final int noteColumnIndex = cursor
								.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
						final String note = cursor.getString(noteColumnIndex);
						if (note.equals(""))
							noteItem.setTitle(R.string.note_dialog_title_add);
						else
							noteItem.setTitle(R.string.note_dialog_title_edit);
					}
					noteItem.setVisible(showNoteItem);
					// Set the title of the delete menu item
					final MenuItem deleteItem = menu
							.findItem(R.id.menu_context_delete);
					final CharSequence currentTitle = deleteItem.getTitle();
					CharSequence newTitle;
					if (selectedItemsSize == 1)
						newTitle = getString(R.string.menu_context_delete_single);
					else
						newTitle = getString(R.string.menu_context_delete_multiple);
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
		else
		{
			listView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
			registerForContextMenu(listView);
		}
		getLoaderManager().initLoader(0, null, this);
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
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(final MenuItem item)
			{
				switch (item.getItemId())
				{
					case R.id.menu_context_note:
						Log.d(getClass().getSimpleName(),
								"Popup Menu selected "
										+ (popupHolder.existingNote.equals("") ? "Add Note"
												: "Edit Note"));
						AnalyticsManagerService.trackEvent(getActivity(),
								"PopupMenu", "Note", popupHolder.existingNote
										.equals("") ? "Add Note" : "Edit Note");
						showNoteDialog(popupHolder.id, popupHolder.existingNote);
						return true;
					case R.id.menu_context_delete:
						Log.d(getClass().getSimpleName(),
								"Popup Menu selected delete");
						AnalyticsManagerService.trackEvent(getActivity(),
								"PopupMenu", "Delete");
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
	public boolean onContextItemSelected(final MenuItem item)
	{
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.menu_context_note:
				final TextView noteView = (TextView) info.targetView
						.findViewById(R.id.note);
				final String existingNote = noteView.getText().toString();
				Log.d(getClass().getSimpleName(), "Context Menu selected "
						+ (existingNote.equals("") ? "Add Note" : "Edit Note"));
				AnalyticsManagerService.trackEvent(getActivity(),
						"ContextMenu", "Note",
						existingNote.equals("") ? "Add Note" : "Edit Note",
						info.position);
				showNoteDialog(info.id, existingNote);
				return true;
			case R.id.menu_context_delete:
				Log.d(getClass().getSimpleName(),
						"Context Menu selected delete");
				AnalyticsManagerService.trackEvent(getActivity(),
						"ContextMenu", "Delete", "", info.position);
				deleteContraction(info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		contractionQueryHandler = new AsyncQueryHandler(getActivity()
				.getContentResolver())
		{
			// No call backs needed
		};
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		final MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.list_context, menu);
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final TextView noteView = (TextView) info.targetView
				.findViewById(R.id.note);
		final CharSequence note = noteView.getText();
		final MenuItem noteItem = menu.findItem(R.id.menu_context_note);
		if (note.equals(""))
			noteItem.setTitle(R.string.note_dialog_title_add);
		else
			noteItem.setTitle(R.string.note_dialog_title_edit);
		Log.d(getClass().getSimpleName(), "Context Menu Opened");
		AnalyticsManagerService.trackEvent(getActivity(), "ContextMenu",
				"Open", note.equals("") ? "Add Note" : "Edit Note");
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(getActivity(),
				ContractionContract.Contractions.CONTENT_URI, null, null, null,
				null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_contraction_list, container,
				false);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		liveDurationHandler.removeCallbacks(liveDurationUpdate);
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		liveDurationHandler.removeCallbacks(liveDurationUpdate);
		adapter.swapCursor(data);
		if (data.getCount() == 0)
			setEmptyText(getText(R.string.list_empty));
		else
			getListView().setSelection(0);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		liveDurationHandler.removeCallbacks(liveDurationUpdate);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		final View rootView = getView();
		if (rootView != null)
		{
			final TextView currentContractionDurationView = (TextView) rootView
					.findViewWithTag("durationView");
			if (currentContractionDurationView != null)
			{
				// Ensures the live duration update is running
				liveDurationHandler.removeCallbacks(liveDurationUpdate);
				liveDurationHandler.post(liveDurationUpdate);
			}
		}
	}

	@Override
	public void setEmptyText(final CharSequence text)
	{
		final TextView emptyText = (TextView) getListView().getEmptyView();
		emptyText.setText(text);
	}

	/**
	 * Shows the 'note' dialog
	 * 
	 * @param id
	 *            contraction id
	 * @param existingNote
	 *            existing note attached to this contraction if it exists
	 */
	private void showNoteDialog(final long id, final String existingNote)
	{
		final NoteDialogFragment noteDialogFragment = new NoteDialogFragment();
		final Bundle args = new Bundle();
		args.putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, id);
		args.putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, existingNote);
		noteDialogFragment.setArguments(args);
		Log.d(noteDialogFragment.getClass().getSimpleName(), "Showing Dialog");
		AnalyticsManagerService
				.trackPageView(getActivity(), noteDialogFragment);
		noteDialogFragment.show(getFragmentManager(), "note");
	}
}
