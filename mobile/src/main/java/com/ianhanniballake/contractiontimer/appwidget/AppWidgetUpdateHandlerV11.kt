package com.ianhanniballake.contractiontimer.appwidget

import android.annotation.TargetApi
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ianhanniballake.contractiontimer.R
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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
    private suspend fun updateDetailWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = coroutineScope {
        async {
            val detailAppWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DetailAppWidgetProvider::class.java))
            val detailWidgetsExist = detailAppWidgetIds.isNotEmpty()
            if (detailWidgetsExist) {
                context.startService(Intent(context, DetailAppWidgetService::class.java))
                appWidgetManager.notifyAppWidgetViewDataChanged(detailAppWidgetIds, R.id.list_view)
            }
        }
    }

    override suspend fun collectWidgetUpdateAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = listOf(
            *super.collectWidgetUpdateAsync(context, appWidgetManager).toTypedArray(),
            updateDetailWidgets(context, appWidgetManager))
}
