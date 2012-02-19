package com.ianhanniballake.contractiontimer.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarPreferenceActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends ActionBarPreferenceActivity implements
		OnSharedPreferenceChangeListener
{
	/**
	 * Keep Screen On preference name
	 */
	public static final String KEEP_SCREEN_ON_PREFERENCE_KEY = "keepScreenOn";

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
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
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
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}
}
