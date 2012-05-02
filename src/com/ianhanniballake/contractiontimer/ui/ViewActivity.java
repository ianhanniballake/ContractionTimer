package com.ianhanniballake.contractiontimer.ui;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
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
	private CursorAdapter adapter = null;
	/**
	 * Currently shown page
	 */
	private int currentPosition = -1;
	/**
	 * Pager Adapter to manage view contraction pages
	 */
	private ViewFragmentPagerAdapter pagerAdapter;

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
		final long contractionId = ContentUris.parseId(getIntent().getData());
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
				AnalyticsManagerService.trackEvent(this, "View", "Home");
				final Intent upIntent = NavUtils.getParentActivityIntent(this);
				if (NavUtils.shouldUpRecreateTask(this, upIntent))
				{
					TaskStackBuilder.from(this).addParentStack(this)
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
				AnalyticsManagerService.trackEvent(this, "View", "Scroll",
						"Next");
			else
				AnalyticsManagerService.trackEvent(this, "View", "Scroll",
						"Previous");
		currentPosition = position;
		final long newContractionId = adapter.getItemId(position);
		getIntent().setData(
				ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						newContractionId));
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
	}
}
