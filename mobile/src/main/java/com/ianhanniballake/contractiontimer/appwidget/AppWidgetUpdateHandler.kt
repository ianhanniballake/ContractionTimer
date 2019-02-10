package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * Handles updating all App Widgets
 */
abstract class AppWidgetUpdateHandler {
    companion object {
        /**
         * Creates a version appropriate AppWidgetUpdateHandler instance
         *
         * @return an appropriate AppWidgetUpdateHandler
         */
        fun createInstance(): AppWidgetUpdateHandler {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                AppWidgetUpdateHandlerV11()
            else
                AppWidgetUpdateHandlerBase()
        }
    }

    /**
     * Updates all App Widgets with the latest information. This should be called whenever a
     * contraction is updated
     *
     * @param context Context used to trigger updates, must not be null
     */
    fun updateAllWidgets(context: Context) {
        GlobalScope.launch {
            collectWidgetUpdateAsync(context, AppWidgetManager.getInstance(context)).awaitAll()
        }
    }

    abstract suspend fun collectWidgetUpdateAsync(
            context: Context,
            appWidgetManager: AppWidgetManager
    ): List<Deferred<Unit>>
}
