package com.ianhanniballake.contractiontimer.appwidget

import android.content.Context
import android.os.Build

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
        @JvmStatic
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
    abstract fun updateAllWidgets(context: Context)
}
