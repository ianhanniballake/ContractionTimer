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
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment to list contractions entered by the user
 */
public abstract class ContractionListFragment extends ListFragment implements
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
				holder.showPopup = (Button) view.findViewById(R.id.show_popup);
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
			ContractionListFragment.this.bindView(holder, cursor);
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent)
		{
			final View view = inflater.inflate(R.layout.list_item_contraction,
					parent, false);
			setupNewView(view);
			return view;
		}
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
	private AsyncQueryHandler contractionQueryHandler = null;
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
	 * Do any version specific view binding
	 * 
	 * @param holder
	 *            ViewHolder associated with the current view
	 * @param cursor
	 *            Cursor pointing to the current data
	 */
	protected abstract void bindView(final ViewHolder holder,
			final Cursor cursor);

	/**
	 * Deletes a given contraction
	 * 
	 * @param id
	 *            contraction id to delete
	 */
	protected void deleteContraction(final long id)
	{
		final Uri deleteUri = ContentUris.withAppendedId(
				ContractionContract.Contractions.CONTENT_ID_URI_BASE, id);
		if (contractionQueryHandler == null)
			contractionQueryHandler = new AsyncQueryHandler(getActivity()
					.getContentResolver())
			{
				// No call backs needed
			};
		contractionQueryHandler.startDelete(0, 0, deleteUri, null, null);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.list_loading));
		adapter = new ContractionListCursorAdapter(getActivity(), null, 0);
		setListAdapter(adapter);
		setupListView();
		final ListView listView = getListView();
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
		getLoaderManager().initLoader(0, null, this);
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
	 * Sets up the ListView
	 */
	protected abstract void setupListView();

	/**
	 * Do any version specific new view setup
	 * 
	 * @param view
	 *            View being set up
	 */
	protected abstract void setupNewView(View view);

	/**
	 * Shows the 'note' dialog
	 * 
	 * @param id
	 *            contraction id
	 * @param existingNote
	 *            existing note attached to this contraction if it exists
	 */
	protected void showNoteDialog(final long id, final String existingNote)
	{
		final NoteDialogFragment noteDialogFragment = new NoteDialogFragment();
		final Bundle args = new Bundle();
		args.putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, id);
		args.putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, existingNote);
		noteDialogFragment.setArguments(args);
		if (BuildConfig.DEBUG)
			Log.d(noteDialogFragment.getClass().getSimpleName(),
					"Showing Dialog");
		AnalyticsManagerService
				.trackPageView(getActivity(), noteDialogFragment);
		noteDialogFragment.show(getFragmentManager(), "note");
	}
}
