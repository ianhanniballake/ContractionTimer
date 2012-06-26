package com.ianhanniballake.contractiontimer;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpPostSender;
import org.acra.sender.ReportSender;

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
		{
			ACRA.init(this);
			final ReportSender bugsenseReportSender = new HttpPostSender(
					"http://www.bugsense.com/api/acra?api_key=6ebe60f4", null);
			ErrorReporter.getInstance().addReportSender(bugsenseReportSender);
		}
		PreferenceManager.setDefaultValues(this, R.xml.preferences_settings,
				false);
		super.onCreate();
	}
}