package com.ianhanniballake.contractiontimer.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Handles updating all App Widgets
 */
public class AppWidgetUpdateHandlerBase extends AppWidgetUpdateHandler
{
	/**
	 * Updates all instances of the Control App Widgets
	 * 
	 * @param context
	 *            Context used to trigger updates
	 * @param appWidgetManager
	 *            AppWidgetManager instance
	 */
	private static void updateControlWidgets(final Context context, final AppWidgetManager appWidgetManager)
	{
		final boolean controlWidgetsExist = appWidgetManager.getAppWidgetIds(new ComponentName(context,
				ControlAppWidgetProvider.class)).length > 0;
		if (controlWidgetsExist)
			context.startService(new Intent(context, ControlAppWidgetService.class));
	}

	/**
	 * Updates all instances of the Toggle App Widgets
	 * 
	 * @param context
	 *            Context used to trigger updates
	 * @param appWidgetManager
	 *            AppWidgetManager instance
	 */
	private static void updateToggleWidgets(final Context context, final AppWidgetManager appWidgetManager)
	{
		final boolean toggleWidgetsExist = appWidgetManager.getAppWidgetIds(new ComponentName(context,
				ToggleAppWidgetProvider.class)).length > 0;
		if (toggleWidgetsExist)
			context.startService(new Intent(context, ToggleAppWidgetService.class));
	}

	/**
	 * Updates all App Widgets with the latest information. This should be called whenever a contraction is updated
	 * 
	 * @param context
	 *            Context used to trigger updates
	 */
	@Override
	public void updateAllWidgets(final Context context)
	{
		final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		AppWidgetUpdateHandlerBase.updateToggleWidgets(context, appWidgetManager);
		AppWidgetUpdateHandlerBase.updateControlWidgets(context, appWidgetManager);
	}
}
