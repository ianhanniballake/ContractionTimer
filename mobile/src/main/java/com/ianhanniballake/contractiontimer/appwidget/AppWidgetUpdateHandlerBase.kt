package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Handles updating all App Widgets
 */
open class AppWidgetUpdateHandlerBase : AppWidgetUpdateHandler() {
    /**
     * Updates all instances of the Control App Widgets
     *
     * @param context          Context used to trigger updates
     * @param appWidgetManager AppWidgetManager instance
     */
    private suspend fun updateControlWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = coroutineScope {
        async {
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ControlAppWidgetProvider::class.java))
            if (appWidgetIds.isNotEmpty()) {
                ControlAppWidgetProvider.updateControlAppWidget(context, appWidgetIds)
            }
        }
    }

    /**
     * Updates all instances of the Toggle App Widgets
     *
     * @param context          Context used to trigger updates
     * @param appWidgetManager AppWidgetManager instance
     */
    private suspend fun updateToggleWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = coroutineScope {
        async {
            val toggleWidgetsExist = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ToggleAppWidgetProvider::class.java)).isNotEmpty()
            if (toggleWidgetsExist)
                context.startService(Intent(context, ToggleAppWidgetService::class.java))
        }
    }

    override suspend fun collectWidgetUpdateAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = listOf(
            updateToggleWidgetsAsync(context, appWidgetManager),
            updateControlWidgetsAsync(context, appWidgetManager))
}
