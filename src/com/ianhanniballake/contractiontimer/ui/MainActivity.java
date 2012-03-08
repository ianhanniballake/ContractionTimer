package com.ianhanniballake.contractiontimer.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Main Activity for managing contractions
 */
public class MainActivity extends ActionBarFragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	/**
	 * Intent extra used to signify that this activity was launched from a
	 * widget
	 */
	public final static String LAUNCHED_FROM_WIDGET_EXTRA = "com.ianhanniballake.contractiontimer.LaunchedFromWidget";
	/**
	 * Adapter to store and manage the current cursor
	 */
	private CursorAdapter adapter;

	/**
	 * Builds a string representing a user friendly formatting of the average
	 * duration / frequency information
	 * 
	 * @return The formatted average data
	 */
	private String getAverageData()
	{
		final TextView averageDurationView = (TextView) findViewById(R.id.average_duration);
		final TextView averageFrequencyView = (TextView) findViewById(R.id.average_frequency);
		final Cursor data = adapter.getCursor();
		data.moveToLast();
		final int startTimeColumnIndex = data
				.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
		final long lastStartTime = data.getLong(startTimeColumnIndex);
		String averageDataFormat;
		if (adapter.getCount() == 1)
			averageDataFormat = getString(R.string.share_average_single);
		else
			averageDataFormat = getString(R.string.share_average_multiple);
		final CharSequence relativeTimeSpan = DateUtils
				.getRelativeTimeSpanString(lastStartTime,
						System.currentTimeMillis(), 0);
		return String.format(averageDataFormat, relativeTimeSpan,
				adapter.getCount(), averageDurationView.getText(),
				averageFrequencyView.getText());
	}

	@Override
	public void onAnalyticsServiceConnected()
	{
		if (getIntent().hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA))
		{
			final String widgetIdentifier = getIntent().getExtras().getString(
					MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
			Log.d(getClass().getSimpleName(), "Launched from "
					+ widgetIdentifier);
			AnalyticsManagerService
					.trackEvent(this, widgetIdentifier, "Launch");
		}
		Log.d(getClass().getSimpleName(), "Showing activity");
		AnalyticsManagerService.trackPageView(this);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		adapter = new CursorAdapter(this, null, 0)
		{
			@Override
			public void bindView(final View view, final Context context,
					final Cursor cursor)
			{
				// Nothing to do
			}

			@Override
			public View newView(final Context context, final Cursor cursor,
					final ViewGroup parent)
			{
				return null;
			}
		};
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		final String[] projection = { BaseColumns._ID,
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME,
				ContractionContract.Contractions.COLUMN_NAME_NOTE };
		final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME
				+ ">?";
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final long averagesTimeFrame = Long.parseLong(preferences.getString(
				Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
				getString(R.string.pref_settings_average_time_frame_default)));
		final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
		final String[] selectionArgs = { Long.toString(timeCutoff) };
		return new CursorLoader(this,
				ContractionContract.Contractions.CONTENT_URI, projection,
				selection, selectionArgs, null);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_reset:
				Log.d(getClass().getSimpleName(), "Menu selected Reset");
				AnalyticsManagerService.trackEvent(this, "Menu", "Reset", "",
						adapter.getCount());
				final ResetDialogFragment resetDialogFragment = new ResetDialogFragment();
				Log.d(resetDialogFragment.getClass().getSimpleName(),
						"Showing Dialog");
				AnalyticsManagerService
						.trackPageView(this, resetDialogFragment);
				resetDialogFragment.show(getSupportFragmentManager(), "reset");
				return true;
			case R.id.menu_share_averages:
				Log.d(getClass().getSimpleName(),
						"Menu selected Share Averages");
				AnalyticsManagerService.trackEvent(this, "Menu", "Share",
						"Averages", adapter.getCount());
				shareAverages();
				return true;
			case R.id.menu_share_all:
				Log.d(getClass().getSimpleName(), "Menu selected Share All");
				AnalyticsManagerService.trackEvent(this, "Menu", "Share",
						"All", adapter.getCount());
				shareAll();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(this, Preferences.class));
				return true;
			case R.id.menu_about:
				Log.d(getClass().getSimpleName(), "Menu selected About");
				AnalyticsManagerService
						.trackEvent(this, "Menu", "About", "", 0);
				final AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
				Log.d(aboutDialogFragment.getClass().getSimpleName(),
						"Showing Dialog");
				AnalyticsManagerService
						.trackPageView(this, aboutDialogFragment);
				aboutDialogFragment.show(getSupportFragmentManager(), "about");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		// Set sharing buttons status
		final int contractionCount = adapter == null ? 0 : adapter.getCount();
		final boolean enableShare = contractionCount > 0;
		final MenuItem shareAverages = menu.findItem(R.id.menu_share_averages);
		shareAverages.setEnabled(enableShare);
		final MenuItem shareAll = menu.findItem(R.id.menu_share_all);
		shareAll.setEnabled(enableShare);
		return true;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean isKeepScreenOn = preferences.getBoolean(
				Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY, false);
		Log.d(getClass().getSimpleName(), "Keep Screen On: " + isKeepScreenOn);
		if (isKeepScreenOn)
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		else
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final boolean averageTimeFrameChanged = preferences.getBoolean(
				Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY,
				false);
		if (averageTimeFrameChanged)
		{
			final Editor editor = preferences.edit();
			editor.remove(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
				editor.apply();
			else
				editor.commit();
			getSupportLoaderManager().restartLoader(0, null, this);
		}
	}

	/**
	 * Builds the data to share and opens the Intent chooser
	 */
	private void shareAll()
	{
		final Cursor data = adapter.getCursor();
		if (data.getCount() == 0)
			return;
		final StringBuffer formattedData = new StringBuffer(getAverageData());
		formattedData.append("<br /><br />");
		formattedData.append(getText(R.string.share_details_header));
		formattedData.append(":<br /><br />");
		data.moveToPosition(-1);
		while (data.moveToNext())
		{
			String timeFormat = "hh:mm:ssaa";
			if (DateFormat.is24HourFormat(this))
				timeFormat = "kk:mm:ss";
			final int startTimeColumnIndex = data
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = data.getLong(startTimeColumnIndex);
			final CharSequence formattedStartTime = DateFormat.format(
					timeFormat, startTime);
			final int endTimeColumnIndex = data
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			if (data.isNull(endTimeColumnIndex))
			{
				final String detailTimeOngoingFormat = getString(R.string.share_detail_time_ongoing);
				formattedData.append(String.format(detailTimeOngoingFormat,
						formattedStartTime));
			}
			else
			{
				final String detailTimeFormat = getString(R.string.share_detail_time_finished);
				final long endTime = data.getLong(endTimeColumnIndex);
				final CharSequence formattedEndTime = DateFormat.format(
						timeFormat, endTime);
				final long durationInSeconds = (endTime - startTime) / 1000;
				final CharSequence formattedDuration = DateUtils
						.formatElapsedTime(durationInSeconds);
				formattedData.append(String
						.format(detailTimeFormat, formattedStartTime,
								formattedEndTime, formattedDuration));
			}
			// If we aren't the last entry, move to the next (previous in time)
			// contraction to get its start time to compute the frequency
			if (!data.isLast() && data.moveToNext())
			{
				final String detailFrequencyFormat = getString(R.string.share_detail_frequency);
				final int prevContractionStartTimeColumnIndex = data
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
				final long prevContractionStartTime = data
						.getLong(prevContractionStartTimeColumnIndex);
				final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
				final CharSequence formattedFrequency = DateUtils
						.formatElapsedTime(frequencyInSeconds);
				formattedData.append(" ");
				formattedData.append(String.format(detailFrequencyFormat,
						formattedFrequency));
				// Go back to the previous spot
				data.moveToPrevious();
			}
			final int noteColumnIndex = data
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
			final String note = data.getString(noteColumnIndex);
			if (!note.equals(""))
			{
				formattedData.append(": ");
				formattedData.append(note);
			}
			formattedData.append("<br />");
		}
		final Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		shareIntent.setType("text/html");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT,
				getText(R.string.share_subject));
		shareIntent.putExtra(Intent.EXTRA_TEXT,
				Html.fromHtml(formattedData.toString()));
		startActivity(Intent.createChooser(shareIntent,
				getText(R.string.share_pick_application)));
	}

	/**
	 * Builds the averages data to share and opens the Intent chooser
	 */
	private void shareAverages()
	{
		final Cursor data = adapter.getCursor();
		if (data.getCount() == 0)
			return;
		final String formattedData = getAverageData();
		final Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_SUBJECT,
				getText(R.string.share_subject));
		shareIntent.putExtra(Intent.EXTRA_TEXT, formattedData);
		startActivity(Intent.createChooser(shareIntent,
				getText(R.string.share_pick_application)));
	}
}