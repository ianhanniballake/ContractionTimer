package com.ianhanniballake.contractiontimer.appwidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.MainActivity;

/**
 * Handles updates of the 'Control' style App Widgets
 */
public class ControlAppWidgetService extends IntentService
{
	/**
	 * Identifier for this widget to be used in Google Analytics
	 */
	private final static String WIDGET_IDENTIFIER = "ControlWidget";

	/**
	 * Creates a new ControlAppWidgetService
	 */
	public ControlAppWidgetService()
	{
		super(ControlAppWidgetService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(final Intent intent)
	{
		Log.d(getClass().getSimpleName(), "Updating Control App Widgets");
		final String[] projection = { BaseColumns._ID,
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME };
		final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME
				+ ">?";
		// In the last hour
		final long timeCutoff = System.currentTimeMillis() - 1000 * 60 * 60;
		final String[] selectionArgs = { Long.toString(timeCutoff) };
		final Cursor data = getContentResolver().query(
				ContractionContract.Contractions.CONTENT_URI, projection,
				selection, selectionArgs, null);
		final boolean atLeastOneContraction = data.moveToFirst();
		final boolean contractionOngoing = atLeastOneContraction
				&& data.isNull(data
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
		final RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.control_appwidget);
		// Add the intent to the Application Launch button
		final Intent applicationLaunchIntent = new Intent(this,
				MainActivity.class);
		applicationLaunchIntent.putExtra(
				MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, WIDGET_IDENTIFIER);
		final PendingIntent applicationLaunchPendingIntent = PendingIntent
				.getActivity(this, 0, applicationLaunchIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.application_launch,
				applicationLaunchPendingIntent);
		// Set the average duration and frequency
		if (atLeastOneContraction)
		{
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
					averageDuration = (curDuration + numDurations
							* averageDuration)
							/ (numDurations + 1);
					numDurations++;
				}
				if (data.moveToNext())
				{
					final int prevContractionStartTimeColumnIndex = data
							.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
					final long prevContractionStartTime = data
							.getLong(prevContractionStartTimeColumnIndex);
					final long curFrequency = startTime
							- prevContractionStartTime;
					averageFrequency = (curFrequency + numFrequencies
							* averageFrequency)
							/ (numFrequencies + 1);
					numFrequencies++;
				}
			}
			final long averageDurationInSeconds = (long) (averageDuration / 1000);
			views.setTextViewText(R.id.average_duration,
					DateUtils.formatElapsedTime(averageDurationInSeconds));
			final long averageFrequencyInSeconds = (long) (averageFrequency / 1000);
			views.setTextViewText(R.id.average_frequency,
					DateUtils.formatElapsedTime(averageFrequencyInSeconds));
		}
		else
		{
			views.setTextViewText(R.id.average_duration, "");
			views.setTextViewText(R.id.average_frequency, "");
		}
		// Set the status of the contraction toggle button
		final Intent toggleContractionIntent = new Intent(this,
				AppWidgetToggleService.class);
		toggleContractionIntent.putExtra(
				AppWidgetToggleService.WIDGET_NAME_EXTRA, WIDGET_IDENTIFIER);
		final PendingIntent toggleContractionPendingIntent = PendingIntent
				.getService(this, 0, toggleContractionIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
		if (contractionOngoing)
		{
			views.setViewVisibility(R.id.contraction_toggle_on, View.VISIBLE);
			views.setOnClickPendingIntent(R.id.contraction_toggle_on,
					toggleContractionPendingIntent);
			views.setViewVisibility(R.id.contraction_toggle_off, View.GONE);
		}
		else
		{
			views.setViewVisibility(R.id.contraction_toggle_off, View.VISIBLE);
			views.setOnClickPendingIntent(R.id.contraction_toggle_off,
					toggleContractionPendingIntent);
			views.setViewVisibility(R.id.contraction_toggle_on, View.GONE);
		}
		// Close the cursor
		data.close();
		// Update the widgets
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(this);
		if (intent.hasExtra("appWidgetIds"))
			appWidgetManager.updateAppWidget(
					intent.getIntArrayExtra("appWidgetIds"), views);
		else
			appWidgetManager.updateAppWidget(new ComponentName(this,
					ControlAppWidgetProvider.class), views);
	}
}
