package com.ianhanniballake.contractiontimer.analytics;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceActivity;

/**
 * FragmentActivity that utilizes GoogleAnalyticsTracker to provide usage
 * metrics
 */
public abstract class AnalyticTrackingPreferenceActivity extends
		PreferenceActivity implements AnalyticsTrackingActivity
{
	/**
	 * Connection to the analytics service
	 */
	private final AnalyticsServiceConnection service = new AnalyticsServiceConnection(
			this);

	@Override
	protected void onStart()
	{
		super.onStart();
		bindService(new Intent(this, AnalyticsService.class), service,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		unbindService(service);
	}
}
