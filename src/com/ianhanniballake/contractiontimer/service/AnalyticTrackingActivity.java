package com.ianhanniballake.contractiontimer.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * FragmentActivity that utilizes GoogleAnalyticsTracker to provide usage
 * metrics
 */
public abstract class AnalyticTrackingActivity extends FragmentActivity
{
	/**
	 * Connection to the analytics service
	 */
	private final AnalyticsServiceConnection service = new AnalyticsServiceConnection(
			this);

	/**
	 * Called when the analytics instance is successfully connected to this
	 * activity
	 */
	public abstract void onAnalyticsServiceConnected();

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(getClass().getSimpleName(), "Activity Paused");
		GoogleAnalyticsTracker.getInstance().dispatch();
	}

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
