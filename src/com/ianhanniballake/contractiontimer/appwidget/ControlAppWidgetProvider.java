package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Handles updates of the 'Control' style App Widgets
 */
public class ControlAppWidgetProvider extends AppWidgetProvider
{
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
	{
		Log.d(getClass().getSimpleName(), "Updating Control App Widgets");
		final Intent service = new Intent(context,
				ControlAppWidgetService.class);
		service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(service);
	}
}