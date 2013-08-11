package com.ianhanniballake.contractiontimer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarPreferenceActivity;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.backup.BackupController;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends ActionBarPreferenceActivity implements OnSharedPreferenceChangeListener
{
	/**
	 * Analytics preference name
	 */
	public static final String ANALYTICS_PREFERENCE_KEY = "analytics";
	/**
	 * Appwidget Background preference name
	 */
	public static final String APPWIDGET_BACKGROUND_PREFERENCE_KEY = "appwidget_background";
	/**
	 * Average Time Frame recently changed for ContractionAverageFragment preference name
	 */
	public static final String AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY = "average_time_frame_changed_fragment";
	/**
	 * Average Time Frame recently changed for MainActivity preference name
	 */
	public static final String AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY = "average_time_frame_changed_main";
	/**
	 * Average Time Frame preference name
	 */
	public static final String AVERAGE_TIME_FRAME_PREFERENCE_KEY = "average_time_frame";
	/**
	 * Keep Screen On preference name
	 */
	public static final String KEEP_SCREEN_ON_PREFERENCE_KEY = "keepScreenOn";
	/**
	 * Lock Portrait preference name
	 */
	public static final String LOCK_PORTRAIT_PREFERENCE_KEY = "lock_portrait";
	/**
	 * Reference to the ListPreference corresponding with the Appwidget background
	 */
	private ListPreference appwidgetBackgroundListPreference;
	/**
	 * Reference to the ListPreference corresponding with the average time frame
	 */
	private ListPreference averageTimeFrameListPreference;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_settings);
		appwidgetBackgroundListPreference = (ListPreference) getPreferenceScreen().findPreference(
				Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY);
		appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
		averageTimeFrameListPreference = (ListPreference) getPreferenceScreen().findPreference(
				Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY);
		averageTimeFrameListPreference.setSummary(getString(R.string.pref_settings_average_time_frame_summary) + "\n"
				+ averageTimeFrameListPreference.getEntry());
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				final Intent upIntent = NavUtils.getParentActivityIntent(this);
				if (NavUtils.shouldUpRecreateTask(this, upIntent))
				{
					TaskStackBuilder.create(this).addParentStack(this).startActivities();
					finish();
				}
				else
					NavUtils.navigateUpTo(this, upIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
				.getBoolean(R.bool.pref_settings_lock_portrait_default));
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Lock Portrait: " + isLockPortrait);
		if (isLockPortrait)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key)
	{
		if (key.equals(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY))
		{
			final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
					getResources().getBoolean(R.bool.pref_settings_keep_screen_on_default));
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Keep Screen On: " + newIsKeepScreenOn);
			EasyTracker.getTracker().sendEvent("Preferences", "Keep Screen On", Boolean.toString(newIsKeepScreenOn),
					0L);
		}
		else if (key.equals(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY))
		{
			final boolean newIsLockPortrait = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
					getResources().getBoolean(R.bool.pref_settings_lock_portrait_default));
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Lock Portrait: " + newIsLockPortrait);
			EasyTracker.getTracker()
					.sendEvent("Preferences", "Lock Portrait", Boolean.toString(newIsLockPortrait), 0L);
			if (newIsLockPortrait)
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			else
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		else if (key.equals(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY))
		{
			final String newAppwidgetBackground = appwidgetBackgroundListPreference.getValue();
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Appwidget Background: " + newAppwidgetBackground);
			EasyTracker.getTracker().sendEvent("Preferences", "Appwidget Background", newAppwidgetBackground, 0L);
			appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
			AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
		}
		else if (key.equals(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY))
		{
			final String newAverageTimeFrame = averageTimeFrameListPreference.getValue();
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Average Time Frame: " + newAverageTimeFrame);
			EasyTracker.getTracker().sendEvent("Preferences", "Average Time Frame", newAverageTimeFrame, 0L);
			final Editor editor = sharedPreferences.edit();
			editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true);
			editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true);
			editor.commit();
			averageTimeFrameListPreference.setSummary(getString(R.string.pref_settings_average_time_frame_summary)
					+ "\n" + averageTimeFrameListPreference.getEntry());
		}
		else if (key.equals(Preferences.ANALYTICS_PREFERENCE_KEY))
		{
			final boolean newCollectAnalytics = sharedPreferences.getBoolean(Preferences.ANALYTICS_PREFERENCE_KEY,
					getResources().getBoolean(R.bool.pref_privacy_analytics_default));
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Analytics: " + newCollectAnalytics);
			EasyTracker.getTracker().sendEvent("Preferences", "Analytics", Boolean.toString(newCollectAnalytics), 0L);
			GAServiceManager.getInstance().dispatch();
			GoogleAnalytics.getInstance(this).setAppOptOut(newCollectAnalytics);
		}
		BackupController.createInstance().dataChanged(this);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
		EasyTracker.getInstance().activityStart(this);
		EasyTracker.getTracker().sendView("Preferences");
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}
