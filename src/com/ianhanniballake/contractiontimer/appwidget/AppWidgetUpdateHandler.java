package com.ianhanniballake.contractiontimer.appwidget;

import android.content.Context;
import android.os.Build;

/**
 * Handles updating all App Widgets
 */
public abstract class AppWidgetUpdateHandler
{
	/**
	 * Creates a version appropriate AppWidgetUpdateHandler instance
	 * 
	 * @return an appropriate AppWidgetUpdateHandler
	 */
	public static AppWidgetUpdateHandler createInstance()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			return new AppWidgetUpdateHandlerV11();
		return new AppWidgetUpdateHandlerBase();
	}

	/**
	 * Updates all App Widgets with the latest information. This should be
	 * called whenever a contraction is updated
	 * 
	 * @param context
	 *            Context used to trigger updates
	 */
	public abstract void updateAllWidgets(final Context context);
}
