package com.ianhanniballake.contractiontimer.analytics;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ianhanniballake.contractiontimer.BuildConfig;

/**
 * Starts and stops the Google Analytics session based on the number of attached
 * activities
 */
public class AnalyticsService extends Service
{
	@Override
	public IBinder onBind(final Intent intent)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Binding service");
		AnalyticsManagerService.startSession(this);
		return new Binder();
	}

	@Override
	public boolean onUnbind(final Intent intent)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Unbinding service");
		AnalyticsManagerService.stopSession(this);
		return false;
	}
}
