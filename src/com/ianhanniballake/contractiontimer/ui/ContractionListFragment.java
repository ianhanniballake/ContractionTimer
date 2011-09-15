package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>
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
			final TextView startTimeView = (TextView) view
					.findViewById(R.id.start_time);
			final TextView endTimeView = (TextView) view
					.findViewById(R.id.end_time);
			final TextView durationView = (TextView) view
					.findViewById(R.id.duration);
			final TextView frequencyView = (TextView) view
					.findViewById(R.id.frequency);
			final TextView noteView = (TextView) view.findViewById(R.id.note);
			String timeFormat = "hh:mm:ssaa";
			if (DateFormat.is24HourFormat(context))
				timeFormat = "kk:mm:ss";
			final int startTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = cursor.getLong(startTimeColumnIndex);
			startTimeView.setText(DateFormat.format(timeFormat, startTime));
			final int endTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			if (cursor.isNull(endTimeColumnIndex))
			{
				endTimeView.setText(" ");
				durationView.setText("");
				liveDurationUpdate.setStartTime(startTime);
				liveDurationUpdate.setDurationView(durationView);
				liveDurationHandler.post(liveDurationUpdate);
			}
			else
			{
				final long endTime = cursor.getLong(endTimeColumnIndex);
				endTimeView.setText(DateFormat.format(timeFormat, endTime));
				final long durationInSeconds = (endTime - startTime) / 1000;
				durationView.setText(DateUtils
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
				frequencyView.setText(DateUtils
						.formatElapsedTime(frequencyInSeconds));
				// Go back to the previous spot
				cursor.moveToPrevious();
			}
			else
				frequencyView.setText("");
			final int noteColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
			final String note = cursor.getString(noteColumnIndex);
			noteView.setText(note);
			if (note.equals(""))
				noteView.setVisibility(View.GONE);
			else
				noteView.setVisibility(View.VISIBLE);
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent)
		{
			return inflater.inflate(R.layout.list_item_contraction, parent,
					false);
		}
	}

	/**
	 * Updates the current contraction's duration every second, providing a real
	 * time view of the duration
	 */
	private class LiveDurationUpdate implements Runnable
	{
		/**
		 * Duration view to update
		 */
		private TextView durationView = null;
		/**
		 * Start time of the current contraction
		 */
		private long startTime = 0;

		/**
		 * Updates the appropriate duration view to the current elapsed time and
		 * schedules this to rerun in 1 second
		 */
		@Override
		public void run()
		{
			final long durationInSeconds = (System.currentTimeMillis() - startTime) / 1000;
			durationView
					.setText(DateUtils.formatElapsedTime(durationInSeconds));
			liveDurationHandler.postDelayed(this, 1000);
		}

		/**
		 * Setter for the current contraction's duration view
		 * 
		 * @param durationView
		 *            Duration view to update
		 */
		public void setDurationView(final TextView durationView)
		{
			this.durationView = durationView;
		}

		/**
		 * Setter for the current contraction's start time, used to calculate
		 * the elapsed duration
		 * 
		 * @param startTime
		 *            Start time of the current contraction
		 */
		public void setStartTime(final long startTime)
		{
			this.startTime = startTime;
		}
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
	 * Handler of live duration updates
	 */
	private final Handler liveDurationHandler = new Handler();
	/**
	 * Reference to the Runnable live duration updater
	 */
	private final LiveDurationUpdate liveDurationUpdate = new LiveDurationUpdate();

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.list_loading));
		adapter = new ContractionListCursorAdapter(getActivity(), null, 0);
		setListAdapter(adapter);
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		registerForContextMenu(getListView());
		getLoaderManager().initLoader(0, null, this);
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
				final NoteDialogFragment noteDialogFragment = new NoteDialogFragment(
						info.id, existingNote);
				noteDialogFragment.show(getFragmentManager(), "reset");
				return true;
			case R.id.menu_context_delete:
				final Uri deleteUri = ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						info.id);
				contractionQueryHandler
						.startDelete(0, 0, deleteUri, null, null);
				if (info.position == 0)
				{
					final ContractionControlsFragment controls = (ContractionControlsFragment) getFragmentManager()
							.findFragmentById(R.id.controls);
					controls.reload();
				}
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
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(getActivity(),
				ContractionContract.Contractions.CONTENT_ID_URI_BASE, null,
				null, null, null);
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
	public void setEmptyText(final CharSequence text)
	{
		final TextView emptyText = (TextView) getListView().getEmptyView();
		emptyText.setText(text);
	}
}
