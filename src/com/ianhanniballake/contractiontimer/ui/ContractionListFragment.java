package com.ianhanniballake.contractiontimer.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
			String timeFormat = "hh:mm:ssaa";
			if (DateFormat.is24HourFormat(context))
				timeFormat = "kk:mm:ss";
			final int startTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = cursor.getLong(startTimeColumnIndex);
			final TextView startTimeView = (TextView) view
					.findViewById(R.id.start_time);
			startTimeView.setText(DateFormat.format(timeFormat, startTime));
			final int endTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			final TextView endTimeView = (TextView) view
					.findViewById(R.id.end_time);
			if (cursor.isNull(endTimeColumnIndex))
				endTimeView.setText("");
			else
			{
				final long endTime = cursor.getLong(endTimeColumnIndex);
				endTimeView.setText(DateFormat.format(timeFormat, endTime));
			}
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
	 * Adapter to display the list's data
	 */
	private CursorAdapter adapter;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getText(R.string.list_loading));
		adapter = new ContractionListCursorAdapter(getActivity(), null, 0);
		setListAdapter(adapter);
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(getActivity(),
				ContractionContract.Contractions.CONTENT_ID_URI_BASE, null,
				null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
		setEmptyText(getText(R.string.list_empty));
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
		if (data.getCount() == 0)
			setEmptyText(getText(R.string.list_empty));
		else
			getListView().setSelection(0);
	}
}
