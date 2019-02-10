package com.ianhanniballake.contractiontimer.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

import com.ianhanniballake.contractiontimer.BuildConfig

/**
 * Handles updates of the 'Control' style App Widgets
 */
class ControlAppWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val TAG = "CtrlAppWidgetProvider"
    }

    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Control App Widgets")
        val service = Intent(context, ControlAppWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.startService(service)
    }
}
