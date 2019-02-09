package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

import com.ianhanniballake.contractiontimer.BuildConfig

/**
 * Handles updates of the 'Toggle' style App Widgets
 */
class ToggleAppWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "ToggleAppWidgetProvider"
    }

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Toggle App Widgets")
        val service = Intent(context, ToggleAppWidgetService::class.java)
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        context.startService(service)
    }
}
