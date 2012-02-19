package com.ianhanniballake.contractiontimer.analytics;

/**
 * Interface providing call back methods used in connecting with the Analytics
 * service
 */
public interface AnalyticsTrackingActivity
{
	/**
	 * Callback for when the analytics service is connected. At this point, it
	 * is safe to call any Analytics methods
	 */
	public void onAnalyticsServiceConnected();
}
