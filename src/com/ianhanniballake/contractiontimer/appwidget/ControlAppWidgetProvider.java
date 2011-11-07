package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

/**
 * Handles updates of the 'Control' style App Widgets
 */
public class ControlAppWidgetProvider extends AppWidgetProvider
{
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
	{
		final Intent service = new Intent(context,
				ControlAppWidgetService.class);
		service.putExtra("appWidgetIds", appWidgetIds);
		context.startService(service);
	}
}