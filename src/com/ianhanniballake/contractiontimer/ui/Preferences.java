package com.ianhanniballake.contractiontimer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.Log;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarPreferenceActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetToggleService;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends ActionBarPreferenceActivity implements
		OnSharedPreferenceChangeListener
{
	/**
	 * Appwidget Background preference name
	 */
	public static final String APPWIDGET_BACKGROUND_PREFERENCE_KEY = "appwidget_background";
	/**
	 * Average Time Frame recently changed for ContractionAverageFragment
	 * preference name
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
	 * Reference to the ListPreference corresponding with the Appwidget
	 * background
	 */
	private ListPreference appwidgetBackgroundListPreference;
	/**
	 * Reference to the ListPreference corresponding with the average time frame
	 */
	private ListPreference averageTimeFrameListPreference;

	@Override
	public void onAnalyticsServiceConnected()
	{
		Log.d(getClass().getSimpleName(), "Showing activity");
		AnalyticsManagerService.trackPageView(this);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_settings);
		appwidgetBackgroundListPreference = (ListPreference) getPreferenceScreen()
				.findPreference(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY);
		appwidgetBackgroundListPreference
				.setSummary(appwidgetBackgroundListPreference.getEntry());
		averageTimeFrameListPreference = (ListPreference) getPreferenceScreen()
				.findPreference(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY);
		averageTimeFrameListPreference
				.setSummary(getString(R.string.pref_settings_average_time_frame_summary)
						+ "\n" + averageTimeFrameListPreference.getEntry());
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				final Intent intent = new Intent(this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(
			final SharedPreferences sharedPreferences, final String key)
	{
		if (key.equals(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY))
		{
			final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(
					Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
					getResources().getBoolean(
							R.bool.pref_settings_keep_screen_on_default));
			Log.d(getClass().getSimpleName(), "Keep Screen On: "
					+ newIsKeepScreenOn);
			AnalyticsManagerService.trackEvent(this, "Preferences",
					"Keep Screen On", Boolean.toString(newIsKeepScreenOn));
		}
		else if (key.equals(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY))
		{
			final String newAppwidgetBackground = appwidgetBackgroundListPreference
					.getValue();
			Log.d(getClass().getSimpleName(), "Appwidget Background: "
					+ newAppwidgetBackground);
			AnalyticsManagerService.trackEvent(this, "Preferences",
					"Appwidget Background", newAppwidgetBackground);
			appwidgetBackgroundListPreference
					.setSummary(appwidgetBackgroundListPreference.getEntry());
			AppWidgetToggleService.updateAllWidgets(this);
		}
		else if (key.equals(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY))
		{
			final String newAverageTimeFrame = averageTimeFrameListPreference
					.getValue();
			Log.d(getClass().getSimpleName(), "Average Time Frame: "
					+ newAverageTimeFrame);
			AnalyticsManagerService.trackEvent(this, "Preferences",
					"Average Time Frame", newAverageTimeFrame);
			final Editor editor = sharedPreferences.edit();
			editor.putBoolean(
					Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY,
					true);
			editor.putBoolean(
					Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY,
					true);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
				editor.apply();
			else
				editor.commit();
			averageTimeFrameListPreference
					.setSummary(getString(R.string.pref_settings_average_time_frame_summary)
							+ "\n" + averageTimeFrameListPreference.getEntry());
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}
}
