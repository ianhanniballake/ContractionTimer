package com.ianhanniballake.contractiontimer.appwidget

import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.os.Binder
import android.os.Build
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.AdapterView
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract.Contractions
import com.ianhanniballake.contractiontimer.ui.Preferences

/**
 * Service which creates the RemoteViews used in the ListView collection in the Detail App Widgets
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class DetailAppWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return object : RemoteViewsFactory {
            private var data: Cursor? = null

            override fun getCount() = data?.count ?: 0

            override fun getItemId(position: Int) = data?.run {
                val idColumnIndex = getColumnIndex(BaseColumns._ID)
                if (moveToPosition(position)) getLong(idColumnIndex) else position.toLong()
            } ?: position.toLong()

            override fun getLoadingView(): RemoteViews {
                val preferences = PreferenceManager
                        .getDefaultSharedPreferences(this@DetailAppWidgetRemoteViewsService)
                val appwidgetBackground = preferences.getString(
                        Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                        getString(R.string.pref_appwidget_background_default))
                return if (appwidgetBackground == "light")
                    RemoteViews(packageName, R.layout.list_item_detail_appwidget_loading_light)
                else
                    RemoteViews(packageName, R.layout.list_item_detail_appwidget_loading_dark)
            }

            override fun getViewAt(position: Int): RemoteViews {
                val context = this@DetailAppWidgetRemoteViewsService
                val preferences = PreferenceManager
                        .getDefaultSharedPreferences(context)
                val appwidgetBackground = preferences.getString(
                        Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                        getString(R.string.pref_appwidget_background_default))
                val views = if (appwidgetBackground == "light")
                    RemoteViews(packageName, R.layout.list_item_detail_appwidget_light)
                else
                    RemoteViews(packageName, R.layout.list_item_detail_appwidget_dark)
                val data = data
                if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position))
                    return views
                val timeFormat =  if (DateFormat.is24HourFormat(context))
                    "kk:mm:ss"
                else
                    "hh:mm:ssa"
                val startTimeColumnIndex = data
                    .getColumnIndex(Contractions.COLUMN_NAME_START_TIME)
                val startTime = data.getLong(startTimeColumnIndex)
                views.setTextViewText(R.id.start_time, DateFormat.format(timeFormat, startTime))
                val endTimeColumnIndex = data
                    .getColumnIndex(Contractions.COLUMN_NAME_END_TIME)
                val isContractionOngoing = data.isNull(endTimeColumnIndex)
                if (isContractionOngoing) {
                    views.setTextViewText(R.id.end_time, " ")
                    views.setTextViewText(R.id.duration, getString(R.string.duration_ongoing))
                } else {
                    val endTime = data.getLong(endTimeColumnIndex)
                    views.setTextViewText(R.id.end_time, DateFormat.format(timeFormat, endTime))
                    val durationInSeconds = (endTime - startTime) / 1000
                    views.setTextViewText(R.id.duration, DateUtils.formatElapsedTime(durationInSeconds))
                }
                // If we aren't the last entry, move to the next (previous in time)
                // contraction to get its start time to compute the frequency
                if (!data.isLast && data.moveToNext()) {
                    val prevContractionStartTimeColumnIndex = data
                        .getColumnIndex(Contractions.COLUMN_NAME_START_TIME)
                    val prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex)
                    val frequencyInSeconds = (startTime - prevContractionStartTime) / 1000
                    views.setTextViewText(R.id.frequency, DateUtils.formatElapsedTime(frequencyInSeconds))
                    // Go back to the previous spot
                    data.moveToPrevious()
                }
                val fillInIntent = Intent()
                val idColumnIndex = data.getColumnIndex(BaseColumns._ID)
                val id = data.getLong(idColumnIndex)
                fillInIntent.data = ContentUris.withAppendedId(Contractions.CONTENT_ID_URI_BASE, id)
                views.setOnClickFillInIntent(R.id.list_item_detail_appwidget, fillInIntent)
                return views
            }

            override fun getViewTypeCount() = 1

            override fun hasStableIds() = true

            override fun onCreate() {
                // Nothing to do
            }

            override fun onDataSetChanged() {
                val token = Binder.clearCallingIdentity()
                data?.close()
                data = contentResolver.query(Contractions.CONTENT_URI, null, null, null, null)
                Binder.restoreCallingIdentity(token)
            }

            override fun onDestroy() {
                data?.close()?.also {
                    data = null
                }
            }
        }
    }
}
