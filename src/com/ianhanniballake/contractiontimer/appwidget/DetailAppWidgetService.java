package com.ianhanniballake.contractiontimer.appwidget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.MainActivity;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * Handles updates of the 'Detail' style App Widgets
 */
@TargetApi(11)
public class DetailAppWidgetService extends IntentService
{
	/**
	 * Identifier for this widget to be used in Google Analytics
	 */
	private final static String WIDGET_IDENTIFIER = "DetailWidget";

	/**
	 * Creates a new DetailAppWidgetService
	 */
	public DetailAppWidgetService()
	{
		super(DetailAppWidgetService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(final Intent intent)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Updating Detail App Widgets");
		final String[] projection = { BaseColumns._ID,
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME };
		final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME
				+ ">?";
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final long averagesTimeFrame = Long.parseLong(preferences.getString(
				Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
				getString(R.string.pref_settings_average_time_frame_default)));
		final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
		final String[] selectionArgs = { Long.toString(timeCutoff) };
		final Cursor data = getContentResolver().query(
				ContractionContract.Contractions.CONTENT_URI, projection,
				selection, selectionArgs, null);
		final boolean atLeastOneContraction = data.moveToFirst();
		RemoteViews views;
		final String appwidgetBackground = preferences.getString(
				Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
				getString(R.string.pref_appwidget_background_default));
		if (appwidgetBackground.equals("light"))
			views = new RemoteViews(getPackageName(),
					R.layout.detail_appwidget_light);
		else
			views = new RemoteViews(getPackageName(),
					R.layout.detail_appwidget_dark);
		// Add the intent to the Application Launch button
		final Intent applicationLaunchIntent = new Intent(this,
				MainActivity.class);
		applicationLaunchIntent.putExtra(
				MainActivity.LAUNCHED_FROM_WIDGET_EXTRA,
				DetailAppWidgetService.WIDGET_IDENTIFIER);
		applicationLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
		// Need to use a separate cursor as there could be running contractions
		// outside of the average time frame
		final Cursor allData = getContentResolver().query(
				ContractionContract.Contractions.CONTENT_URI, projection, null,
				null, null);
		final boolean contractionOngoing = allData.moveToFirst()
				&& allData
						.isNull(allData
								.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
		final Intent toggleContractionIntent = new Intent(this,
				AppWidgetToggleService.class);
		toggleContractionIntent.putExtra(
				AppWidgetToggleService.WIDGET_NAME_EXTRA,
				DetailAppWidgetService.WIDGET_IDENTIFIER);
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			setRemoteAdapter(views);
		else
			setRemoteAdapterV11(views);
		final Intent clickIntentTemplate = new Intent(Intent.ACTION_VIEW);
		clickIntentTemplate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		clickIntentTemplate.putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA,
				DetailAppWidgetService.WIDGET_IDENTIFIER);
		final PendingIntent clickPendingIntentTemplate = PendingIntent
				.getActivity(this, 0, clickIntentTemplate,
						PendingIntent.FLAG_UPDATE_CURRENT);
		views.setPendingIntentTemplate(R.id.list_view,
				clickPendingIntentTemplate);
		views.setEmptyView(R.id.list_view, R.id.empty_view);
		// Close the cursors
		data.close();
		allData.close();
		// Update the widgets
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(this);
		if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
			appWidgetManager.updateAppWidget(intent
					.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
					views);
		else
			appWidgetManager.updateAppWidget(new ComponentName(this,
					DetailAppWidgetProvider.class), views);
	}

	/**
	 * Sets the remote adapter used to fill in the list items
	 * 
	 * @param views
	 *            RemoteViews to set the RemoteAdapter
	 */
	@TargetApi(14)
	private void setRemoteAdapter(final RemoteViews views)
	{
		views.setRemoteAdapter(R.id.list_view, new Intent(this,
				DetailAppWidgetRemoteViewsService.class));
	}

	/**
	 * Sets the remote adapter used to fill in the list items
	 * 
	 * @param views
	 *            RemoteViews to set the RemoteAdapter
	 */
	@SuppressWarnings("deprecation")
	private void setRemoteAdapterV11(final RemoteViews views)
	{
		views.setRemoteAdapter(0, R.id.list_view, new Intent(this,
				DetailAppWidgetRemoteViewsService.class));
	}
}
