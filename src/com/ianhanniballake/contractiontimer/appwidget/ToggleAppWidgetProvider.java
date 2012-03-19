package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Handles updates of the 'Toggle' style App Widgets
 */
public class ToggleAppWidgetProvider extends AppWidgetProvider
{
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
	{
		Log.d(getClass().getSimpleName(), "Updating Toggle App Widgets");
		final Intent service = new Intent(context, ToggleAppWidgetService.class);
		service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		context.startService(service);
	}
}