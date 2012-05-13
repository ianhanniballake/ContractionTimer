package com.ianhanniballake.contractiontimer.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Provides a Search interface
 */
public class SearchActivity extends ActionBarFragmentActivity implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	/**
	 * Cursor Adapter for creating and binding contraction list view items
	 */
	private class SearchResultCursorAdapter extends CursorAdapter
	{
		/**
		 * Local reference to the layout inflater service
		 */
		private final LayoutInflater inflater;

		/**
		 * @param context
		 *            The context where the ListView associated with this
		 *            SimpleListItemFactory is running
		 * @param c
		 *            The database cursor. Can be null if the cursor is not
		 *            available yet.
		 * @param flags
		 *            Flags used to determine the behavior of the adapter, as
		 *            per
		 *            {@link CursorAdapter#CursorAdapter(Context, Cursor, int)}.
		 */
		public SearchResultCursorAdapter(final Context context, final Cursor c,
				final int flags)
		{
			super(context, c, flags);
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor)
		{
			final Object viewTag = view.getTag();
			ViewHolder holder;
			if (viewTag == null)
			{
				holder = new ViewHolder();
				holder.startTime = (TextView) view
						.findViewById(R.id.start_time);
				holder.endTime = (TextView) view.findViewById(R.id.end_time);
				holder.note = (TextView) view.findViewById(R.id.note);
				view.setTag(holder);
			}
			else
				holder = (ViewHolder) viewTag;
			String timeFormat = "hh:mm:ssaa";
			if (DateFormat.is24HourFormat(context))
				timeFormat = "kk:mm:ss";
			final int startTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			final long startTime = cursor.getLong(startTimeColumnIndex);
			holder.startTime.setText(DateFormat.format(timeFormat, startTime));
			final int endTimeColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			final boolean isContractionOngoing = cursor
					.isNull(endTimeColumnIndex);
			if (isContractionOngoing)
				holder.endTime.setText(" ");
			else
			{
				final long endTime = cursor.getLong(endTimeColumnIndex);
				holder.endTime.setText(DateFormat.format(timeFormat, endTime));
			}
			final int noteColumnIndex = cursor
					.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
			final String note = cursor.getString(noteColumnIndex);
			holder.note.setText(note);
			if (note.equals(""))
				holder.note.setVisibility(View.GONE);
			else
				holder.note.setVisibility(View.VISIBLE);
		}

		@Override
		public View newView(final Context context, final Cursor cursor,
				final ViewGroup parent)
		{
			return inflater.inflate(R.layout.list_item_search, parent, false);
		}
	}

	/**
	 * Helper class used to store temporary references to list item views
	 */
	static class ViewHolder
	{
		/**
		 * TextView representing the formatted end time of the contraction
		 */
		TextView endTime;
		/**
		 * TextView representing the note attached to the contraction
		 */
		TextView note;
		/**
		 * TextView representing the formatted start time of the contraction
		 */
		TextView startTime;
	}

	/**
	 * List Adapter
	 */
	private SearchResultCursorAdapter adapter;
	/**
	 * Last submitted query
	 */
	private String query = null;
	/**
	 * Current SearchView
	 */
	private SearchView searchView;

	/**
	 * Handle the incoming intent
	 * 
	 * @param intent
	 *            Incoming intent
	 */
	private void handleIntent(final Intent intent)
	{
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			query = intent.getStringExtra(SearchManager.QUERY);
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), query);
			if (searchView != null)
				searchView.setQuery(query, false);
			intent.setData(Uri.withAppendedPath(
					ContractionContract.Contractions.SEARCH_URI,
					Uri.encode(query)));
			getSupportLoaderManager().restartLoader(0, null, this);
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
		adapter = new SearchResultCursorAdapter(this, null, 0);
		final ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);
		getSupportLoaderManager().initLoader(0, null, this);
		handleIntent(getIntent());
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(this, getIntent().getData(), null, null, null,
				null);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		getMenuInflater().inflate(R.menu.activity_search, menu);
		final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) menu.findItem(R.id.menu_search)
				.getActionView();
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(false);
		if (query != null)
			searchView.setQuery(query, false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
	}

	@Override
	protected void onNewIntent(final Intent intent)
	{
		setIntent(intent);
		handleIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_search:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Menu selected Search");
				AnalyticsManagerService.trackEvent(this, "Menu", "Search");
				onSearchRequested();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
