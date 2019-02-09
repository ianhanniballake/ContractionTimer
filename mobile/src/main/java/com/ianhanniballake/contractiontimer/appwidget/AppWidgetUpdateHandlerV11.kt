package com.ianhanniballake.contractiontimer.appwidget

import android.annotation.TargetApi
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

import com.ianhanniballake.contractiontimer.R

/**
 * Handles updating all App Widgets
 */
class AppWidgetUpdateHandlerV11 : AppWidgetUpdateHandlerBase() {
    /**
     * Updates all instances of the Detail App Widgets
     *
     * @param context          Context used to trigger updates
     * @param appWidgetManager AppWidgetManager instance
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun updateDetailWidgets(context: Context, appWidgetManager: AppWidgetManager) {
        val detailAppWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, DetailAppWidgetProvider::class.java))
        val detailWidgetsExist = detailAppWidgetIds.isNotEmpty()
        if (detailWidgetsExist) {
            context.startService(Intent(context, DetailAppWidgetService::class.java))
            appWidgetManager.notifyAppWidgetViewDataChanged(detailAppWidgetIds, R.id.list_view)
        }
    }

    override fun updateAllWidgets(context: Context) {
        super.updateAllWidgets(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateDetailWidgets(context, appWidgetManager)
    }
}
