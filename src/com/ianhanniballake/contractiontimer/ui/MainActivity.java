package com.ianhanniballake.contractiontimer.ui;

import android.os.Bundle;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.service.AnalyticTrackingActivity;

/**
 * Main Activity for managing contractions
 */
public class MainActivity extends AnalyticTrackingActivity
{
	@Override
	public void onAnalyticsServiceConnected()
	{
		Log.d(getClass().getSimpleName(), "Showing activity");
		GoogleAnalyticsTracker.getInstance().trackPageView(
				"/" + getClass().getSimpleName());
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		final MenuItem resetMenuItem = menu.findItem(R.id.menu_reset);
		resetMenuItem.setIcon(android.R.drawable.ic_menu_delete);
		resetMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_reset:
				Log.d(getClass().getSimpleName(), "Menu selected Reset");
				GoogleAnalyticsTracker.getInstance().trackEvent("Menu",
						"Reset", "", 0);
				final ResetDialogFragment resetDialogFragment = new ResetDialogFragment();
				resetDialogFragment.show(getSupportFragmentManager(), "reset");
				return true;
			case R.id.menu_about:
				Log.d(getClass().getSimpleName(), "Menu selected About");
				GoogleAnalyticsTracker.getInstance().trackEvent("Menu",
						"About", "", 0);
				final AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
				aboutDialogFragment.show(getSupportFragmentManager(), "about");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}