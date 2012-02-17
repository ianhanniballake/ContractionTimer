package com.ianhanniballake.contractiontimer;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.StrictMode;

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
		final ApplicationInfo appInfo = getApplicationContext()
				.getApplicationInfo();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
		{
			final StrictMode.ThreadPolicy.Builder threadPolicy = new StrictMode.ThreadPolicy.Builder()
					.detectAll().penaltyLog();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				threadPolicy.penaltyFlashScreen();
			StrictMode.setThreadPolicy(threadPolicy.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll().penaltyLog().penaltyDeath().build());
		}
	}
}