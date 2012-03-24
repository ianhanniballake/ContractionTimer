package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Stand alone activity used to view the details of an individual contraction
 */
public class ViewActivity extends ActionBarFragmentActivity
{
	/**
	 * Handler for asynchronous deletes of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;

	@Override
	public void onAnalyticsServiceConnected()
	{
		if (getIntent().hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA))
		{
			final String widgetIdentifier = getIntent().getExtras().getString(
					MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Launched from "
						+ widgetIdentifier);
			AnalyticsManagerService.trackEvent(this, widgetIdentifier,
					"LaunchView");
		}
		if (BuildConfig.DEBUG)
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
		contractionQueryHandler = new AsyncQueryHandler(getContentResolver())
		{
			@Override
			protected void onDeleteComplete(final int token,
					final Object cookie, final int result)
			{
				AppWidgetUpdateHandler.updateAllWidgets(ViewActivity.this);
				finish();
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_view, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		long contractionId = 0;
		if (getIntent() != null && getIntent().getExtras() != null)
			contractionId = getIntent().getExtras().getLong(BaseColumns._ID, 0);
		switch (item.getItemId())
		{
			case android.R.id.home:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected home");
				AnalyticsManagerService.trackEvent(this, "View", "Home");
				final Intent intent = new Intent(this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_edit:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected edit");
				AnalyticsManagerService.trackEvent(this, "View", "Edit");
				final Intent editIntent = new Intent(this, EditActivity.class);
				editIntent.putExtra(BaseColumns._ID, contractionId);
				startActivity(editIntent);
				return true;
			case R.id.menu_delete:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected delete");
				AnalyticsManagerService.trackEvent(this, "View", "Delete");
				final Uri deleteUri = ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						contractionId);
				contractionQueryHandler
						.startDelete(0, 0, deleteUri, null, null);
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
