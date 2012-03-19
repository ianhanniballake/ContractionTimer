package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.ianhanniballake.contractiontimer.R;

/**
 * Handles updating all App Widgets
 */
public class AppWidgetUpdateHandler
{
	/**
	 * Updates all App Widgets with the latest information. This should be
	 * called whenever a contraction is updated
	 * 
	 * @param context
	 *            Context used to trigger updates
	 */
	public static void updateAllWidgets(final Context context)
	{
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		final boolean toggleWidgetsExist = appWidgetManager
				.getAppWidgetIds(new ComponentName(context,
						ToggleAppWidgetProvider.class)).length > 0;
		if (toggleWidgetsExist)
			context.startService(new Intent(context,
					ToggleAppWidgetService.class));
		final boolean controlWidgetsExist = appWidgetManager
				.getAppWidgetIds(new ComponentName(context,
						ControlAppWidgetProvider.class)).length > 0;
		if (controlWidgetsExist)
			context.startService(new Intent(context,
					ControlAppWidgetService.class));
		final int[] detailAppWidgetIds = AppWidgetManager.getInstance(context)
				.getAppWidgetIds(
						new ComponentName(context,
								DetailAppWidgetProvider.class));
		final boolean detailWidgetsExist = detailAppWidgetIds.length > 0;
		if (detailWidgetsExist)
		{
			context.startService(new Intent(context,
					DetailAppWidgetService.class));
			appWidgetManager.notifyAppWidgetViewDataChanged(detailAppWidgetIds,
					R.id.list_view);
		}
	}
}
