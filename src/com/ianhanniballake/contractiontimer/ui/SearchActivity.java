package com.ianhanniballake.contractiontimer.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;

/**
 * Provides a Search interface
 */
public class SearchActivity extends ActionBarFragmentActivity
{
	private void handleIntent(final Intent intent)
	{
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			final String query = intent.getStringExtra(SearchManager.QUERY);
			Log.d(getClass().getSimpleName(), query);
			// use the query to search your data somehow
		}
	}

	@Override
	public void onAnalyticsServiceConnected()
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Showing activity");
		AnalyticsManagerService.trackPageView(this);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		handleIntent(getIntent());
	}

	@Override
	protected void onNewIntent(final Intent intent)
	{
		handleIntent(intent);
	}
}
