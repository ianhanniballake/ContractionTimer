package com.ianhanniballake.contractiontimer.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * Starts and stops the Google Analytics session based on the number of attached
 * activities
 */
public class AnalyticsService extends Service
{
	/**
	 * Valid Google Analytics Web Property ID to log analytics to
	 */
	private final static String ANALYTICS_PROPERTY_ID = "UA-25785295-1";

	@Override
	public IBinder onBind(final Intent intent)
	{
		Log.d(getClass().getSimpleName(), "Binding service");
		final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
				.getInstance();
		// Start the tracker in manual dispatch mode
		tracker.startNewSession(ANALYTICS_PROPERTY_ID, getApplicationContext());
		tracker.setAnonymizeIp(true);
		final ApplicationInfo appInfo = getApplicationContext()
				.getApplicationInfo();
		tracker.setDebug((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		return new Binder();
	}

	@Override
	public boolean onUnbind(final Intent intent)
	{
		Log.d(getClass().getSimpleName(), "Unbinding service");
		final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
				.getInstance();
		tracker.dispatch();
		tracker.stopSession();
		return false;
	}
}
