package com.ianhanniballake.contractiontimer.appwidget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.ianhanniballake.contractiontimer.R;

/**
 * Handles updating all App Widgets
 */
public class AppWidgetUpdateHandlerV11 extends AppWidgetUpdateHandlerBase
{
	/**
	 * Updates all instances of the Detail App Widgets
	 * 
	 * @param context
	 *            Context used to trigger updates
	 * @param appWidgetManager
	 *            AppWidgetManager instance
	 */
	@TargetApi(11)
	private static void updateDetailWidgets(final Context context, final AppWidgetManager appWidgetManager)
	{
		final int[] detailAppWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
				DetailAppWidgetProvider.class));
		final boolean detailWidgetsExist = detailAppWidgetIds.length > 0;
		if (detailWidgetsExist)
		{
			context.startService(new Intent(context, DetailAppWidgetService.class));
			appWidgetManager.notifyAppWidgetViewDataChanged(detailAppWidgetIds, R.id.list_view);
		}
	}

	@Override
	public void updateAllWidgets(final Context context)
	{
		super.updateAllWidgets(context);
		final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		AppWidgetUpdateHandlerV11.updateDetailWidgets(context, appWidgetManager);
	}
}
