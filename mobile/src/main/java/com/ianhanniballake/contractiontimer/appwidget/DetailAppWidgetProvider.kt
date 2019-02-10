package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

import com.ianhanniballake.contractiontimer.BuildConfig

/**
 * Handles updates of the 'Detail' style App Widgets
 */
class DetailAppWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "DetailAppWidgetProvider"
    }

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Detail App Widgets")
        val service = Intent(context, DetailAppWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.startService(service)
    }
}
