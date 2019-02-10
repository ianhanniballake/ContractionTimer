package com.ianhanniballake.contractiontimer.appwidget

import android.annotation.TargetApi
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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
    private suspend fun updateDetailWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = coroutineScope {
        async {
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DetailAppWidgetProvider::class.java))
            if (appWidgetIds.isNotEmpty()) {
                DetailAppWidgetProvider.updateDetailAppWidget(context, appWidgetIds)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view)
            }
        }
    }

    override suspend fun collectWidgetUpdateAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ) = listOf(
            *super.collectWidgetUpdateAsync(context, appWidgetManager).toTypedArray(),
            updateDetailWidgetsAsync(context, appWidgetManager))
}
