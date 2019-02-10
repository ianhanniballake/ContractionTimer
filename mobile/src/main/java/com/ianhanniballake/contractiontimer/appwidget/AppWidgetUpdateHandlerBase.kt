package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

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
    private fun updateControlWidgets(context: Context, appWidgetManager: AppWidgetManager) {
        val controlWidgetsExist = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ControlAppWidgetProvider::class.java)).isNotEmpty()
        if (controlWidgetsExist)
            context.startService(Intent(context, ControlAppWidgetService::class.java))
    }

    /**
     * Updates all instances of the Toggle App Widgets
     *
     * @param context          Context used to trigger updates
     * @param appWidgetManager AppWidgetManager instance
     */
    private fun updateToggleWidgets(context: Context, appWidgetManager: AppWidgetManager) {
        val toggleWidgetsExist = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ToggleAppWidgetProvider::class.java)).isNotEmpty()
        if (toggleWidgetsExist)
            context.startService(Intent(context, ToggleAppWidgetService::class.java))
    }

    override fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateToggleWidgets(context, appWidgetManager)
        updateControlWidgets(context, appWidgetManager)
    }
}
