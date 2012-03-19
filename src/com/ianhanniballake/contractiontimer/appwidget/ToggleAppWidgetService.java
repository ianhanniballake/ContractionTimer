package com.ianhanniballake.contractiontimer.appwidget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * Handles updates of the 'Toggle' style App Widgets
 */
public class ToggleAppWidgetService extends IntentService
{
	/**
	 * Identifier for this widget to be used in Google Analytics
	 */
	private final static String WIDGET_IDENTIFIER = "ToggleWidget";

	/**
	 * Creates a new ToggleAppWidgetService
	 */
	public ToggleAppWidgetService()
	{
		super(ToggleAppWidgetService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(final Intent intent)
	{
		Log.d(getClass().getSimpleName(), "Updating Toggle App Widgets");
		final String[] projection = { BaseColumns._ID,
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME };
		final Cursor data = getContentResolver().query(
				ContractionContract.Contractions.CONTENT_URI, projection, null,
				null, null);
		final boolean contractionOngoing = data.moveToFirst()
				&& data.isNull(data
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
		RemoteViews views;
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final String appwidgetBackground = preferences.getString(
				Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
				getString(R.string.pref_appwidget_background_default));
		if (appwidgetBackground.equals("light"))
			views = new RemoteViews(getPackageName(),
					R.layout.toggle_appwidget_light);
		else
			views = new RemoteViews(getPackageName(),
					R.layout.toggle_appwidget_dark);
		// Set the status of the contraction toggle button
		final Intent toggleContractionIntent = new Intent(this,
				AppWidgetToggleService.class);
		toggleContractionIntent.putExtra(
				AppWidgetToggleService.WIDGET_NAME_EXTRA,
				ToggleAppWidgetService.WIDGET_IDENTIFIER);
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
		if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
			appWidgetManager.updateAppWidget(intent
					.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
					views);
		else
			appWidgetManager.updateAppWidget(new ComponentName(this,
					ToggleAppWidgetProvider.class), views);
	}
}
