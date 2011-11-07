package com.ianhanniballake.contractiontimer.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.ControlAppWidgetService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment which displays the average duration and frequency
 */
public class ContractionAverageFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		final String[] projection = {
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME };
		final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME
				+ ">?";
		// In the last hour
		final long timeCutoff = System.currentTimeMillis() - 1000 * 60 * 60;
		final String[] selectionArgs = { Long.toString(timeCutoff) };
		return new CursorLoader(getActivity(),
				ContractionContract.Contractions.CONTENT_URI, projection,
				selection, selectionArgs, null);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_contraction_average,
				container, false);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		final TextView averageDurationView = (TextView) getActivity()
				.findViewById(R.id.average_duration);
		final TextView averageFrequencyView = (TextView) getActivity()
				.findViewById(R.id.average_frequency);
		averageDurationView.setText("");
		averageFrequencyView.setText("");
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		getActivity().startService(
				new Intent(getActivity(), ControlAppWidgetService.class));
		final TextView averageDurationView = (TextView) getActivity()
				.findViewById(R.id.average_duration);
		final TextView averageFrequencyView = (TextView) getActivity()
				.findViewById(R.id.average_frequency);
		if (!data.moveToNext())
		{
			averageDurationView.setText("");
			averageFrequencyView.setText("");
			return;
		}
		double averageDuration = 0;
		double averageFrequency = 0;
		int numDurations = 0;
		int numFrequencies = 0;
		while (!data.isAfterLast())
		{
			final int startTimeColumnIndex = data
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = data.getLong(startTimeColumnIndex);
			final int endTimeColumnIndex = data
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			if (!data.isNull(endTimeColumnIndex))
			{
				final long endTime = data.getLong(endTimeColumnIndex);
				final long curDuration = endTime - startTime;
				averageDuration = (curDuration + numDurations * averageDuration)
						/ (numDurations + 1);
				numDurations++;
			}
			if (data.moveToNext())
			{
				final int prevContractionStartTimeColumnIndex = data
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
				final long prevContractionStartTime = data
						.getLong(prevContractionStartTimeColumnIndex);
				final long curFrequency = startTime - prevContractionStartTime;
				averageFrequency = (curFrequency + numFrequencies
						* averageFrequency)
						/ (numFrequencies + 1);
				numFrequencies++;
			}
		}
		final long averageDurationInSeconds = (long) (averageDuration / 1000);
		averageDurationView.setText(DateUtils
				.formatElapsedTime(averageDurationInSeconds));
		final long averageFrequencyInSeconds = (long) (averageFrequency / 1000);
		averageFrequencyView.setText(DateUtils
				.formatElapsedTime(averageFrequencyInSeconds));
	}
}
