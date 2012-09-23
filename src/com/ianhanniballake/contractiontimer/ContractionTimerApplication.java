package com.ianhanniballake.contractiontimer;

import java.lang.Thread.UncaughtExceptionHandler;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpPostSender;
import org.acra.sender.ReportSender;

import android.app.Application;
import android.preference.PreferenceManager;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.ianhanniballake.contractiontimer.strictmode.StrictModeController;

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
@ReportsCrashes(formKey = "dFdXWHJ6SDRJREh2M0FRMFFqdFk2R1E6MQ")
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
		GoogleAnalytics.getInstance(this).setDebug(BuildConfig.DEBUG);
		EasyTracker.getInstance().setContext(this);
		EasyTracker.getTracker().setUseSecure(true);
		if (BuildConfig.DEBUG)
		{
			// Never dispatch in debug mode
			GAServiceManager.getInstance().setDispatchPeriod(-1);
			StrictModeController.createInstance().setStrictMode();
		}
		else
		{
			GAServiceManager.getInstance().setDispatchPeriod(120);
			// Initialize Google Analytics Error Handling first
			final UncaughtExceptionHandler myHandler = new ExceptionReporter(
					EasyTracker.getTracker(), GAServiceManager.getInstance(),
					Thread.getDefaultUncaughtExceptionHandler());
			Thread.setDefaultUncaughtExceptionHandler(myHandler);
			ACRA.init(this);
			final ReportSender bugsenseReportSender = new HttpPostSender(
					"http://www.bugsense.com/api/acra?api_key=6ebe60f4", null);
			ACRA.getErrorReporter().addReportSender(bugsenseReportSender);
		}
		PreferenceManager.setDefaultValues(this, R.xml.preferences_settings,
				false);
		super.onCreate();
	}
}