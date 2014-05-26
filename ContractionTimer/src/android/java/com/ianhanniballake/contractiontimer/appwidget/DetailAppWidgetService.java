package com.ianhanniballake.contractiontimer.appwidget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.ui.MainActivity;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * Handles updates of the 'Detail' style App Widgets
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailAppWidgetService extends IntentService {
    private final static String TAG = DetailAppWidgetProvider.class.getSimpleName();
    /**
     * Identifier for the keyguard (lockscreen) version of this widget to be used in Google Analytics
     */
    private final static String KEYGUARD_WIDGET_IDENTIFIER = "DetailWidgetKeyguard";
    /**
     * Identifier for this widget to be used in Google Analytics
     */
    private final static String WIDGET_IDENTIFIER = "DetailWidget";

    /**
     * Creates a new DetailAppWidgetService
     */
    public DetailAppWidgetService() {
        super(TAG);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static String getWidgetIdentifier(final AppWidgetManager appWidgetManager, final int appWidgetId) {
        final Bundle myOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        final int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        if (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD)
            return KEYGUARD_WIDGET_IDENTIFIER;
        return WIDGET_IDENTIFIER;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating Detail App Widgets");
        final String[] projection = {BaseColumns._ID, ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME};
        final String selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?";
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final long averagesTimeFrame = Long.parseLong(preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_settings_average_time_frame_default)));
        final long timeCutoff = System.currentTimeMillis() - averagesTimeFrame;
        final String[] selectionArgs = {Long.toString(timeCutoff)};
        final Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                selection, selectionArgs, null);
        final String appwidgetBackground = preferences.getString(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY,
                getString(R.string.pref_appwidget_background_default));
        // Set the average duration and frequency
        String formattedAverageDuration = "";
        String formattedAverageFrequency = "";
        if (data != null && data.moveToFirst()) {
            double averageDuration = 0;
            double averageFrequency = 0;
            int numDurations = 0;
            int numFrequencies = 0;
            while (!data.isAfterLast()) {
                final int startTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long startTime = data.getLong(startTimeColumnIndex);
                final int endTimeColumnIndex = data
                        .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
                if (!data.isNull(endTimeColumnIndex)) {
                    final long endTime = data.getLong(endTimeColumnIndex);
                    final long curDuration = endTime - startTime;
                    averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1);
                    numDurations++;
                }
                if (data.moveToNext()) {
                    final int prevContractionStartTimeColumnIndex = data
                            .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                    final long prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex);
                    final long curFrequency = startTime - prevContractionStartTime;
                    averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1);
                    numFrequencies++;
                }
            }
            final long averageDurationInSeconds = (long) (averageDuration / 1000);
            formattedAverageDuration = DateUtils.formatElapsedTime(averageDurationInSeconds);
            final long averageFrequencyInSeconds = (long) (averageFrequency / 1000);
            formattedAverageFrequency = DateUtils.formatElapsedTime(averageFrequencyInSeconds);
        }
        // Determine whether a contraction is currently ongoing
        // Need to use a separate cursor as there could be running contractions
        // outside of the average time frame
        final Cursor allData = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null);
        final boolean contractionOngoing = allData != null && allData.moveToFirst()
                && allData.isNull(allData.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
        // Close the cursors
        if (data != null)
            data.close();
        if (allData != null)
            allData.close();
        // Get the list of app widgets to update
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        final int[] detailAppWidgetIds;
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS))
            detailAppWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        else
            detailAppWidgetIds = appWidgetManager
                    .getAppWidgetIds(new ComponentName(this, DetailAppWidgetProvider.class));
        // Build and update the views for each widget
        // We need to do it widget by widget to allow changes for keyguard vs
        // home screen widgets
        for (final int appWidgetId : detailAppWidgetIds) {
            // Need to determine if this widget is a keyguard or home screen
            // widget for Analytics purposes
            String widgetIdentifier;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                widgetIdentifier = getWidgetIdentifier(appWidgetManager, appWidgetId);
            else
                widgetIdentifier = WIDGET_IDENTIFIER;
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Updating " + widgetIdentifier + " with id " + appWidgetId);
            // Note that all widgets share the same theme
            RemoteViews views;
            if (appwidgetBackground.equals("light"))
                views = new RemoteViews(getPackageName(), R.layout.detail_appwidget_light);
            else
                views = new RemoteViews(getPackageName(), R.layout.detail_appwidget_dark);
            // Add the intent to the Application Launch button
            final Intent applicationLaunchIntent = new Intent(this, MainActivity.class);
            applicationLaunchIntent.putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, widgetIdentifier);
            applicationLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final PendingIntent applicationLaunchPendingIntent = PendingIntent.getActivity(this, 0,
                    applicationLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.application_launch, applicationLaunchPendingIntent);
            // Add in the averages
            views.setTextViewText(R.id.average_duration, formattedAverageDuration);
            views.setTextViewText(R.id.average_frequency, formattedAverageFrequency);
            // Add the intent for the toggle button
            final Intent toggleContractionIntent = new Intent(this, AppWidgetToggleService.class);
            toggleContractionIntent.putExtra(AppWidgetToggleService.WIDGET_NAME_EXTRA, widgetIdentifier);
            final PendingIntent toggleContractionPendingIntent = PendingIntent.getService(this, 0,
                    toggleContractionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (contractionOngoing) {
                views.setViewVisibility(R.id.contraction_toggle_on, View.VISIBLE);
                views.setOnClickPendingIntent(R.id.contraction_toggle_on, toggleContractionPendingIntent);
                views.setViewVisibility(R.id.contraction_toggle_off, View.GONE);
            } else {
                views.setViewVisibility(R.id.contraction_toggle_off, View.VISIBLE);
                views.setOnClickPendingIntent(R.id.contraction_toggle_off, toggleContractionPendingIntent);
                views.setViewVisibility(R.id.contraction_toggle_on, View.GONE);
            }
            // Set up the collection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                setRemoteAdapter(views);
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                setRemoteAdapterV11(views);
            final Intent clickIntentTemplate = new Intent(Intent.ACTION_VIEW).setPackage(getPackageName());
            clickIntentTemplate.putExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA, widgetIdentifier);
            final PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.list_view, clickPendingIntentTemplate);
            views.setEmptyView(R.id.list_view, R.id.empty_view);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setRemoteAdapter(final RemoteViews views) {
        views.setRemoteAdapter(R.id.list_view, new Intent(this, DetailAppWidgetRemoteViewsService.class));
    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    @SuppressWarnings("deprecation")
    private void setRemoteAdapterV11(final RemoteViews views) {
        views.setRemoteAdapter(0, R.id.list_view, new Intent(this, DetailAppWidgetRemoteViewsService.class));
    }
}
