package com.ianhanniballake.contractiontimer;

import android.app.Application;
import android.preference.PreferenceManager;

import com.ianhanniballake.contractiontimer.strictmode.StrictModeController;

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
public class ContractionTimerApplication extends Application
{
	/**
	 * Sets strict mode if we are in debug mode
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate()
	{
		if (BuildConfig.DEBUG)
			StrictModeController.createInstance().setStrictMode();
		PreferenceManager.setDefaultValues(this, R.xml.preferences_settings,
				false);
	}
}