package com.ianhanniballake.contractiontimer;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;

/**
 * Creates the Contraction Timer application, setting strict mode in debug mode
 */
public class ContractionTimerApplication extends Application
{
	/**
	 * Sets the StrictMode to log all violations
	 */
	@TargetApi(10)
	private static void setStrictMode()
	{
		final StrictMode.ThreadPolicy.Builder threadPolicy = new StrictMode.ThreadPolicy.Builder()
				.detectAll().penaltyLog();
		StrictMode.setThreadPolicy(threadPolicy.build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
				.penaltyLog().build());
	}

	/**
	 * Sets strict mode if we are in debug mode
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate()
	{
		final ApplicationInfo appInfo = getApplicationContext()
				.getApplicationInfo();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
			ContractionTimerApplication.setStrictMode();
		PreferenceManager.setDefaultValues(this, R.xml.preferences_settings,
				false);
	}
}