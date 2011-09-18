package com.ianhanniballake.contractiontimer.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Simple Service Connection used to confirm that the AnalyticsService bound
 * successfully
 */
public class AnalyticsServiceConnection implements ServiceConnection
{
	/**
	 * Activity instance to provide call back on service connected
	 */
	private final AnalyticTrackingActivity analyticTrackingActivity;

	/**
	 * Constructs a service connection with the appropriate call back activity
	 * 
	 * @param analyticTrackingActivity
	 *            Activity to provide call back on service connected
	 */
	public AnalyticsServiceConnection(
			final AnalyticTrackingActivity analyticTrackingActivity)
	{
		this.analyticTrackingActivity = analyticTrackingActivity;
	}

	@Override
	public void onServiceConnected(final ComponentName name,
			final IBinder service)
	{
		Log.d(getClass().getSimpleName(), "Service connected");
		analyticTrackingActivity.onAnalyticsServiceConnected();
	}

	@Override
	public void onServiceDisconnected(final ComponentName name)
	{
		Log.d(getClass().getSimpleName(), "Service disconnected");
	}
}
