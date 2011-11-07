package com.ianhanniballake.contractiontimer.service;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * Handles Google Analytics session starting and stopping
 */
public class AnalyticSessionManager
{
	/**
	 * Valid Google Analytics Web Property ID to log analytics to
	 */
	private final static String ANALYTICS_PROPERTY_ID = "UA-25785295-1";

	/**
	 * Starts a new session
	 * 
	 * @param context
	 *            context of the session
	 */
	public static void startNewSession(final Context context)
	{
		final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
				.getInstance();
		// Start the tracker in manual dispatch mode
		tracker.startNewSession(ANALYTICS_PROPERTY_ID, context);
		tracker.setAnonymizeIp(true);
		final ApplicationInfo appInfo = context.getApplicationInfo();
		tracker.setDebug((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
	}

	/**
	 * Stops an existing session, dispatching events if we are not in debug mode
	 * 
	 * @param context
	 *            context of the session
	 */
	public static void stopSession(final Context context)
	{
		final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
				.getInstance();
		final ApplicationInfo appInfo = context.getApplicationInfo();
		if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0)
			tracker.dispatch();
		tracker.stopSession();
	}
}
