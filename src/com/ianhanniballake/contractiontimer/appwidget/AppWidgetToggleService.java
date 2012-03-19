package com.ianhanniballake.contractiontimer.appwidget;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Starts a new contraction or stops the current contraction, updating all
 * widgets upon completion
 */
public class AppWidgetToggleService extends IntentService
{
	/**
	 * Intent extra used to determine which widget called this service
	 */
	public final static String WIDGET_NAME_EXTRA = "com.ianhanniballake.contractiontimer.WidgetName";

	/**
	 * Creates a new AppWidgetToggleService
	 */
	public AppWidgetToggleService()
	{
		super(AppWidgetToggleService.class.getSimpleName());
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(getClass().getSimpleName(), "Creating service");
		AnalyticsManagerService.startSession(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.d(getClass().getSimpleName(), "Destroying service");
		AnalyticsManagerService.stopSession(this);
	}

	@Override
	protected void onHandleIntent(final Intent intent)
	{
		final String widgetName = intent
				.getStringExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA);
		final ContentResolver contentResolver = getContentResolver();
		final String[] projection = { BaseColumns._ID,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME };
		final Cursor data = contentResolver.query(
				ContractionContract.Contractions.CONTENT_URI, projection, null,
				null, null);
		final boolean contractionOngoing = data.moveToFirst()
				&& data.isNull(data
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
		if (contractionOngoing)
		{
			Log.d(AppWidgetToggleService.this.getClass().getSimpleName(),
					"Stopping contraction");
			AnalyticsManagerService.trackEvent(this, widgetName, "Stop");
			final ContentValues newEndTime = new ContentValues();
			newEndTime.put(
					ContractionContract.Contractions.COLUMN_NAME_END_TIME,
					System.currentTimeMillis());
			final long latestContractionId = data.getInt(data
					.getColumnIndex(BaseColumns._ID));
			final Uri updateUri = ContentUris.withAppendedId(
					ContractionContract.Contractions.CONTENT_ID_URI_BASE,
					latestContractionId);
			// Add the new end time to the last contraction
			contentResolver.update(updateUri, newEndTime, null, null);
		}
		else
		{
			Log.d(AppWidgetToggleService.this.getClass().getSimpleName(),
					"Starting contraction");
			AnalyticsManagerService.trackEvent(this, widgetName, "Start");
			// Start a new contraction
			contentResolver.insert(
					ContractionContract.Contractions.CONTENT_URI,
					new ContentValues());
		}
		// Close the cursor
		data.close();
		AppWidgetUpdateHandler.updateAllWidgets(this);
	}
}
