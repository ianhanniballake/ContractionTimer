package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ianhanniballake.contractiontimer.BuildConfig;

/**
 * Handles updates of the 'Detail' style App Widgets
 */
public class DetailAppWidgetProvider extends AppWidgetProvider
{
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Updating Detail App Widgets");
		final Intent service = new Intent(context, DetailAppWidgetService.class);
		service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(service);
	}
}