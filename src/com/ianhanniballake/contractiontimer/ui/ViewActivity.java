package com.ianhanniballake.contractiontimer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;

/**
 * Stand alone activity used to view the details of an individual contraction
 */
public class ViewActivity extends ActionBarFragmentActivity
{
	@Override
	public void onAnalyticsServiceConnected()
	{
		if (getIntent().hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA))
		{
			final String widgetIdentifier = getIntent().getExtras().getString(
					MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
			Log.d(getClass().getSimpleName(), "Launched from "
					+ widgetIdentifier);
			AnalyticsManagerService.trackEvent(this, widgetIdentifier,
					"LaunchView");
		}
		Log.d(getClass().getSimpleName(), "Showing activity");
		AnalyticsManagerService.trackPageView(this);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view);
		if (findViewById(R.id.view) == null)
		{
			// A null details view means we no longer need this activity
			finish();
			return;
		}
		long contractionId = 0;
		if (getIntent() != null && getIntent().getExtras() != null)
			contractionId = getIntent().getExtras().getLong(BaseColumns._ID, 0);
		final ViewFragment viewFragment = new ViewFragment();
		final Bundle args = new Bundle();
		args.putLong(BaseColumns._ID, contractionId);
		viewFragment.setArguments(args);
		// Execute a transaction, replacing any existing fragment
		// with this one inside the frame.
		final FragmentTransaction ft = getSupportFragmentManager()
				.beginTransaction();
		ft.replace(R.id.view, viewFragment);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				final Intent intent = new Intent(this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}
}
