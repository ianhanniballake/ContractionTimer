package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
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
public class EditActivity extends ActionBarFragmentActivity
{
	/**
	 * Handler for asynchronous updates of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;

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
		setContentView(R.layout.activity_edit);
		if (findViewById(R.id.edit) == null)
		{
			// A null details view means we no longer need this activity
			finish();
			return;
		}
		contractionQueryHandler = new AsyncQueryHandler(getContentResolver())
		{
			@Override
			protected void onUpdateComplete(final int token,
					final Object cookie, final int result)
			{
				AppWidgetUpdateHandler.updateAllWidgets(EditActivity.this);
				finish();
			}
		};
		if (savedInstanceState == null)
			showFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_edit, menu);
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
					Log.d(getClass().getSimpleName(), "Edit selected home");
				AnalyticsManagerService.trackEvent(this, "Edit", "Home");
				final Intent intent = new Intent(this, ViewActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				intent.putExtra(BaseColumns._ID, contractionId);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_save:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Edit selected save");
				AnalyticsManagerService.trackEvent(this, "Edit", "Save");
				final Uri updateUri = ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						contractionId);
				final EditFragment editFragment = (EditFragment) getSupportFragmentManager()
						.findFragmentById(R.id.edit);
				final ContentValues values = editFragment.getContentValues();
				contractionQueryHandler.startUpdate(0, null, updateUri, values,
						null, null);
				return true;
			case R.id.menu_cancel:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Edit selected cancel");
				AnalyticsManagerService.trackEvent(this, "Edit", "Cancel");
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

	/**
	 * Creates and shows the fragment associated with the current contraction
	 */
	private void showFragment()
	{
		long contractionId = 0;
		if (getIntent() != null && getIntent().getExtras() != null)
			contractionId = getIntent().getExtras().getLong(BaseColumns._ID, 0);
		final EditFragment viewFragment = new EditFragment();
		final Bundle args = new Bundle();
		args.putLong(BaseColumns._ID, contractionId);
		viewFragment.setArguments(args);
		// Execute a transaction, replacing any existing fragment
		// with this one inside the frame.
		final FragmentTransaction ft = getSupportFragmentManager()
				.beginTransaction();
		ft.replace(R.id.edit, viewFragment);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
}
