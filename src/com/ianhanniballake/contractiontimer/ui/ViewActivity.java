package com.ianhanniballake.contractiontimer.ui;

import org.acra.ACRA;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.provider.ContractionContract.Contractions;

/**
 * Stand alone activity used to view the details of an individual contraction
 */
public class ViewActivity extends ActionBarFragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor>, ViewPager.OnPageChangeListener
{
	/**
	 * Creates ViewFragments as necessary
	 */
	private class ViewFragmentPagerAdapter extends FragmentStatePagerAdapter
	{
		/**
		 * Creates a new ViewFragmentPagerAdapter
		 * 
		 * @param fm
		 *            FragmentManager used to manage fragments
		 */
		public ViewFragmentPagerAdapter(final FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return adapter == null ? 0 : adapter.getCount();
		}

		@Override
		public Fragment getItem(final int position)
		{
			return ViewFragment.createInstance(adapter.getItemId(position));
		}

		@Override
		public CharSequence getPageTitle(final int position)
		{
			if (position + 1 == currentPosition)
				return getText(R.string.detail_previous_page);
			else if (position - 1 == currentPosition)
				return getText(R.string.detail_next_page);
			else
				return null;
		}
	}

	/**
	 * Adapter for all contractions
	 */
	CursorAdapter adapter = null;
	/**
	 * Currently shown page
	 */
	int currentPosition = -1;
	/**
	 * Pager Adapter to manage view contraction pages
	 */
	private ViewFragmentPagerAdapter pagerAdapter;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view);
		if (findViewById(R.id.pager) == null)
		{
			// A null pager means we no longer need this activity
			finish();
			return;
		}
		adapter = new CursorAdapter(this, null, 0)
		{
			@Override
			public void bindView(final View view, final Context context,
					final Cursor cursor)
			{
				// Nothing to do
			}

			@Override
			public View newView(final Context context, final Cursor cursor,
					final ViewGroup parent)
			{
				return null;
			}
		};
		final ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setOnPageChangeListener(this);
		pagerAdapter = new ViewFragmentPagerAdapter(getSupportFragmentManager());
		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(this,
				ContractionContract.Contractions.CONTENT_URI, null, null, null,
				Contractions.COLUMN_NAME_START_TIME);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_view, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
		pagerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
		pagerAdapter.notifyDataSetChanged();
		// A null intent/data means we should cancel out as we are in an
		// indeterminate state
		if (getIntent() == null || getIntent().getData() == null)
			finish();
		final long contractionId;
		try
		{
			contractionId = ContentUris.parseId(getIntent().getData());
		} catch (final NumberFormatException e)
		{
			if (BuildConfig.DEBUG)
				Log.e(getClass().getSimpleName(),
						"NumberFormatException in onLoadFinished", e);
			else
			{
				EasyTracker.getTracker().trackException(
						Thread.currentThread().getName(), e, false);
				ACRA.getErrorReporter().handleSilentException(e);
			}
			finish();
			return;
		}
		final int count = adapter.getCount();
		for (int position = 0; position < count; position++)
		{
			final long id = adapter.getItemId(position);
			if (id == contractionId)
			{
				currentPosition = position;
				break;
			}
		}
		if (currentPosition == -1)
			finish();
		final ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(pagerAdapter);
		pager.setCurrentItem(currentPosition, false);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected home");
				EasyTracker.getTracker().trackEvent("View", "Home", "", 0L);
				final Intent upIntent = NavUtils.getParentActivityIntent(this);
				if (NavUtils.shouldUpRecreateTask(this, upIntent))
				{
					TaskStackBuilder.create(this).addParentStack(this)
							.startActivities();
					finish();
				}
				else
					NavUtils.navigateUpTo(this, upIntent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onPageScrolled(final int position, final float positionOffset,
			final int positionOffsetPixels)
	{
		// Nothing to do
	}

	@Override
	public void onPageScrollStateChanged(final int state)
	{
		// Nothing to do
	}

	@Override
	public void onPageSelected(final int position)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Swapped to " + position);
		if (currentPosition != -1)
			if (position > currentPosition)
				EasyTracker.getTracker().trackEvent("View", "Scroll", "Next",
						(long) position);
			else
				EasyTracker.getTracker().trackEvent("View", "Scroll",
						"Previous", (long) position);
		currentPosition = position;
		final long newContractionId = adapter.getItemId(position);
		getIntent().setData(
				ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						newContractionId));
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean isLockPortrait = preferences
				.getBoolean(
						Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
						getResources().getBoolean(
								R.bool.pref_settings_lock_portrait_default));
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Lock Portrait: "
					+ isLockPortrait);
		if (isLockPortrait)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
		if (getIntent().hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA))
		{
			final String widgetIdentifier = getIntent().getExtras().getString(
					MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Launched from "
						+ widgetIdentifier);
			EasyTracker.getTracker().trackEvent(widgetIdentifier, "LaunchView",
					"", 0L);
			getIntent().removeExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA);
		}
		EasyTracker.getInstance().activityStart(this);
		EasyTracker.getTracker().trackView("View");
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}
