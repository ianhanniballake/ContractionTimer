package com.ianhanniballake.contractiontimer.appwidget;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.provider.ContractionContract.Contractions;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * Service which creates the RemoteViews used in the ListView collection in the Detail App Widgets
 */
@TargetApi(11)
public class DetailAppWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsService.RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public long getItemId(final int position) {
                final int idColumnIndex = data.getColumnIndex(BaseColumns._ID);
                if (data.moveToPosition(position))
                    return data.getLong(idColumnIndex);
                return position;
            }

            @Override
            public RemoteViews getLoadingView() {
                final SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(DetailAppWidgetRemoteViewsService.this);
                RemoteViews views;
                final String appwidgetBackground = preferences.getString(
                        Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                        getString(R.string.pref_appwidget_background_default));
                if (appwidgetBackground.equals("light"))
                    views = new RemoteViews(getPackageName(), R.layout.list_item_detail_appwidget_loading_light);
                else
                    views = new RemoteViews(getPackageName(), R.layout.list_item_detail_appwidget_loading_dark);
                return views;
            }

            @Override
            public RemoteViews getViewAt(final int position) {
                final SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(DetailAppWidgetRemoteViewsService.this);
                RemoteViews views;
                final String appwidgetBackground = preferences.getString(
                        Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                        getString(R.string.pref_appwidget_background_default));
                if (appwidgetBackground.equals("light"))
                    views = new RemoteViews(getPackageName(), R.layout.list_item_detail_appwidget_light);
                else
                    views = new RemoteViews(getPackageName(), R.layout.list_item_detail_appwidget_dark);
                if (!data.moveToPosition(position))
                    return views;
                String timeFormat = "hh:mm:ssaa";
                if (DateFormat.is24HourFormat(DetailAppWidgetRemoteViewsService.this))
                    timeFormat = "kk:mm:ss";
                final int startTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long startTime = data.getLong(startTimeColumnIndex);
                views.setTextViewText(R.id.start_time, DateFormat.format(timeFormat, startTime));
                final int endTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
                final boolean isContractionOngoing = data.isNull(endTimeColumnIndex);
                if (isContractionOngoing) {
                    views.setTextViewText(R.id.end_time, " ");
                    views.setTextViewText(R.id.duration, getString(R.string.duration_ongoing));
                } else {
                    final long endTime = data.getLong(endTimeColumnIndex);
                    views.setTextViewText(R.id.end_time, DateFormat.format(timeFormat, endTime));
                    final long durationInSeconds = (endTime - startTime) / 1000;
                    views.setTextViewText(R.id.duration, DateUtils.formatElapsedTime(durationInSeconds));
                }
                // If we aren't the last entry, move to the next (previous in
                // time)
                // contraction to get its start time to compute the frequency
                if (!data.isLast() && data.moveToNext()) {
                    final int prevContractionStartTimeColumnIndex = data
                            .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                    final long prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex);
                    final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
                    views.setTextViewText(R.id.frequency, DateUtils.formatElapsedTime(frequencyInSeconds));
                    // Go back to the previous spot
                    data.moveToPrevious();
                }
                final Intent fillInIntent = new Intent();
                final int idColumnIndex = data.getColumnIndex(BaseColumns._ID);
                final long id = data.getLong(idColumnIndex);
                fillInIntent.setData(ContentUris.withAppendedId(Contractions.CONTENT_ID_URI_BASE, id));
                views.setOnClickFillInIntent(R.id.list_item_detail_appwidget, fillInIntent);
                return views;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null)
                    data.close();
                data = getContentResolver().query(Contractions.CONTENT_URI, null, null, null, null);
            }

            @Override
            public void onDestroy() {
                if (data != null)
                    data.close();
            }
        };
    }
}
