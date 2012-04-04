package com.ianhanniballake.contractiontimer;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.preference.PreferenceManager;

import com.ianhanniballake.contractiontimer.strictmode.StrictModeController;

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
@ReportsCrashes(formKey = "dEpZSmw0bUcycDFCcDNBcTRrR29fUkE6MQ")
public class ContractionTimerApplication extends Application
{
	/**
	 * Sets strict mode if we are in debug mode, init ACRA if we are not.
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate()
	{
		if (BuildConfig.DEBUG)
			StrictModeController.createInstance().setStrictMode();
		else
			ACRA.init(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences_settings,
				false);
		super.onCreate();
	}
}