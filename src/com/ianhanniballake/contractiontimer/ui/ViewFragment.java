package com.ianhanniballake.contractiontimer.ui;

import java.util.Date;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment showing the details of an individual contraction
 */
public class ViewFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
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
		 * TextView representing the formatted end date of the contraction
		 */
		TextView endDate;
		/**
		 * TextView representing the formatted end time of the contraction
		 */
		TextView endTime;
		/**
		 * TextView representing the note attached to the contraction
		 */
		TextView note;
		/**
		 * TextView representing the formatted start date of the contraction
		 */
		TextView startDate;
		/**
		 * TextView representing the formatted start time of the contraction
		 */
		TextView startTime;
	}

	/**
	 * Adapter to display the detailed data
	 */
	private CursorAdapter adapter;
	/**
	 * Id of the current contraction to show
	 */
	protected long contractionId = 0;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		adapter = new CursorAdapter(getActivity(), null, 0)
		{
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
					holder.startDate = (TextView) view
							.findViewById(R.id.start_date);
					holder.endTime = (TextView) view
							.findViewById(R.id.end_time);
					holder.endDate = (TextView) view
							.findViewById(R.id.end_date);
					holder.duration = (TextView) view
							.findViewById(R.id.duration);
					holder.note = (TextView) view.findViewById(R.id.note);
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
				holder.startTime.setText(DateFormat.format(timeFormat,
						startTime));
				final Date startDate = new Date(startTime);
				holder.startDate.setText(DateFormat
						.getDateFormat(getActivity()).format(startDate));
				final int endTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
				final boolean isContractionOngoing = cursor
						.isNull(endTimeColumnIndex);
				if (isContractionOngoing)
				{
					holder.endTime.setText(" ");
					holder.endDate.setText(" ");
					holder.duration
							.setText(getString(R.string.duration_ongoing));
				}
				else
				{
					final long endTime = cursor.getLong(endTimeColumnIndex);
					holder.endTime.setText(DateFormat.format(timeFormat,
							endTime));
					final Date endDate = new Date(endTime);
					holder.endDate.setText(DateFormat.getDateFormat(
							getActivity()).format(endDate));
					final long durationInSeconds = (endTime - startTime) / 1000;
					holder.duration.setText(DateUtils
							.formatElapsedTime(durationInSeconds));
				}
				final int noteColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
				final String note = cursor.getString(noteColumnIndex);
				holder.note.setText(note);
			}

			@Override
			public View newView(final Context context, final Cursor cursor,
					final ViewGroup parent)
			{
				// View is already inflated in onCreateView
				return null;
			}
		};
		if (getArguments() != null)
		{
			contractionId = getArguments().getLong(BaseColumns._ID, 0);
			if (contractionId != 0)
				getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		final Uri contractionUri = ContentUris.withAppendedId(
				ContractionContract.Contractions.CONTENT_ID_URI_PATTERN,
				contractionId);
		return new CursorLoader(getActivity(), contractionUri, null, null,
				null, null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_view, container, false);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> data)
	{
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
		if (data.moveToFirst())
			adapter.bindView(getView(), getActivity(), data);
	}
}
