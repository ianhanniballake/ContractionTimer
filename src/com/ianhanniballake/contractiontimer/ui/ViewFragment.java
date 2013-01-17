package com.ianhanniballake.contractiontimer.ui;

import java.util.Date;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.SimpleMenuItem;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment showing the details of an individual contraction
 */
public class ViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
	/**
	 * Creates a new Fragment to display the given contraction
	 * 
	 * @param contractionId
	 *            Id of the Contraction to display
	 * @return ViewFragment associated with the given id
	 */
	public static ViewFragment createInstance(final long contractionId)
	{
		final ViewFragment viewFragment = new ViewFragment();
		final Bundle args = new Bundle();
		args.putLong(BaseColumns._ID, contractionId);
		viewFragment.setArguments(args);
		return viewFragment;
	}

	/**
	 * Adapter to display the detailed data
	 */
	private CursorAdapter adapter;
	/**
	 * Id of the current contraction to show
	 */
	private long contractionId = -1;
	/**
	 * Handler for asynchronous deletes of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;
	/**
	 * Whether the current contraction is ongoing (i.e., not yet ended). Null indicates that we haven't checked yet,
	 * while true or false indicates whether the contraction is ongoing
	 */
	Boolean isContractionOngoing = null;

	/**
	 * We need to find the exact view_fragment view as there is a NoSaveStateFrameLayout view inserted in between the
	 * parent and the view we created in onCreateView
	 * 
	 * @return View created in onCreateView
	 */
	private View getFragmentView()
	{
		final View rootView = getView();
		return rootView == null ? null : rootView.findViewById(R.id.view_fragment);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		final Context applicationContext = getActivity().getApplicationContext();
		contractionQueryHandler = new AsyncQueryHandler(getActivity().getContentResolver())
		{
			@Override
			protected void onDeleteComplete(final int token, final Object cookie, final int result)
			{
				AppWidgetUpdateHandler.createInstance().updateAllWidgets(applicationContext);
				final Activity activity = getActivity();
				if (activity != null)
					activity.finish();
			}
		};
		adapter = new CursorAdapter(getActivity(), null, 0)
		{
			@Override
			public void bindView(final View view, final Context context, final Cursor cursor)
			{
				final TextView startTimeView = (TextView) view.findViewById(R.id.start_time);
				String timeFormat = "hh:mm:ssaa";
				if (DateFormat.is24HourFormat(context))
					timeFormat = "kk:mm:ss";
				final int startTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
				final long startTime = cursor.getLong(startTimeColumnIndex);
				startTimeView.setText(DateFormat.format(timeFormat, startTime));
				final TextView startDateView = (TextView) view.findViewById(R.id.start_date);
				final Date startDate = new Date(startTime);
				startDateView.setText(DateFormat.getDateFormat(getActivity()).format(startDate));
				final TextView endTimeView = (TextView) view.findViewById(R.id.end_time);
				final TextView endDateView = (TextView) view.findViewById(R.id.end_date);
				final TextView durationView = (TextView) view.findViewById(R.id.duration);
				final int endTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
				isContractionOngoing = cursor.isNull(endTimeColumnIndex);
				if (isContractionOngoing)
				{
					endTimeView.setText(" ");
					endDateView.setText(" ");
					durationView.setText(getString(R.string.duration_ongoing));
				}
				else
				{
					final long endTime = cursor.getLong(endTimeColumnIndex);
					endTimeView.setText(DateFormat.format(timeFormat, endTime));
					final Date endDate = new Date(endTime);
					endDateView.setText(DateFormat.getDateFormat(getActivity()).format(endDate));
					final long durationInSeconds = (endTime - startTime) / 1000;
					durationView.setText(DateUtils.formatElapsedTime(durationInSeconds));
				}
				getActivity().supportInvalidateOptionsMenu();
				final TextView noteView = (TextView) view.findViewById(R.id.note);
				final int noteColumnIndex = cursor.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
				final String note = cursor.getString(noteColumnIndex);
				noteView.setText(note);
			}

			@Override
			public View newView(final Context context, final Cursor cursor, final ViewGroup parent)
			{
				// View is already inflated in onCreateView
				return null;
			}
		};
		if (getArguments() != null)
		{
			contractionId = getArguments().getLong(BaseColumns._ID, 0);
			if (contractionId != -1)
				getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		final Uri contractionUri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_PATTERN,
				contractionId);
		return new CursorLoader(getActivity(), contractionUri, null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		// Only allow editing contractions that have already finished
		final boolean showEdit = isContractionOngoing != null && !isContractionOngoing;
		final MenuItem editItem = menu.findItem(R.id.menu_edit);
		editItem.setEnabled(showEdit);
		// Don't directly set visibility unless it is one of our SimpleMenuItems
		if (editItem instanceof SimpleMenuItem)
			editItem.setVisible(showEdit);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_view, container, false);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> data)
	{
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
		if (data.moveToFirst())
			adapter.bindView(getFragmentView(), getActivity(), data);
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		final Uri uri = ContentUris.withAppendedId(ContractionContract.Contractions.CONTENT_ID_URI_BASE, contractionId);
		switch (item.getItemId())
		{
			case R.id.menu_edit:
				// isContractionOngoing should be non-null at this point, but
				// just in case
				if (isContractionOngoing == null)
					return true;
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected edit");
				EasyTracker.getTracker().trackEvent("View", "Edit", Boolean.toString(isContractionOngoing), 0L);
				if (isContractionOngoing)
					Toast.makeText(getActivity(), R.string.edit_ongoing_error, Toast.LENGTH_SHORT).show();
				else
					startActivity(new Intent(Intent.ACTION_EDIT, uri));
				return true;
			case R.id.menu_delete:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "View selected delete");
				EasyTracker.getTracker().trackEvent("View", "Delete", "", 0L);
				contractionQueryHandler.startDelete(0, 0, uri, null, null);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
