package com.ianhanniballake.contractiontimer.analytics;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.ianhanniballake.contractiontimer.BuildConfig;

/**
 * Simple Service Connection used to confirm that the AnalyticsService bound
 * successfully
 */
public class AnalyticsServiceConnection implements ServiceConnection
{
	/**
	 * Activity instance to provide call back on service connected
	 */
	private final AnalyticsTrackingActivity analyticTrackingActivity;

	/**
	 * Constructs a service connection with the appropriate call back activity
	 * 
	 * @param analyticTrackingActivity
	 *            Activity to provide call back on service connected
	 */
	public AnalyticsServiceConnection(
			final AnalyticsTrackingActivity analyticTrackingActivity)
	{
		this.analyticTrackingActivity = analyticTrackingActivity;
	}

	@Override
	public void onServiceConnected(final ComponentName name,
			final IBinder service)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Service connected");
		analyticTrackingActivity.onAnalyticsServiceConnected();
	}

	@Override
	public void onServiceDisconnected(final ComponentName name)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Service disconnected");
	}
}
