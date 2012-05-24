package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment showing the details of an individual contraction
 */
public class EditFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	/**
	 * AsyncTask used to check to see if the current end time overlaps any other
	 * existing contraction, displaying an error message if it does
	 */
	private class EndTimeOverlapCheck extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected Boolean doInBackground(final Void... params)
		{
			final Activity activity = getActivity();
			if (activity == null)
				return false;
			final String[] projection = { BaseColumns._ID };
			final String selection = BaseColumns._ID + "<>? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ "<=? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ ">=?";
			final String currentEndTimeMillis = Long.toString(endTime
					.getTimeInMillis());
			final long contractionId = Intent.ACTION_INSERT.equals(activity
					.getIntent().getAction()) ? 0 : ContentUris
					.parseId(activity.getIntent().getData());
			final String[] selectionArgs = { Long.toString(contractionId),
					currentEndTimeMillis, currentEndTimeMillis };
			final Cursor data = activity.getContentResolver().query(
					ContractionContract.Contractions.CONTENT_URI, projection,
					selection, selectionArgs, null);
			final boolean overlapExists = data.moveToFirst();
			data.close();
			return overlapExists;
		}

		@Override
		protected void onPostExecute(final Boolean overlapExists)
		{
			if (BuildConfig.DEBUG)
				Log.d(EditFragment.this.getClass().getSimpleName(),
						"End time overlap: " + overlapExists);
			final View view = getFragmentView();
			if (view == null)
				return;
			final TextView endTimeErrorOverlapView = (TextView) view
					.getTag(R.id.end_time_error_overlap);
			if (endTimeErrorOverlapView == null)
				return;
			if (overlapExists)
				endTimeErrorOverlapView.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				endTimeErrorOverlapView.setVisibility(View.GONE);
			}
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	/**
	 * AsyncTask used to check to see if the current start time overlaps any
	 * other existing contraction, displaying an error message if it does
	 */
	private class StartTimeOverlapCheck extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected Boolean doInBackground(final Void... params)
		{
			final Activity activity = getActivity();
			if (activity == null)
				return false;
			final String[] projection = { BaseColumns._ID };
			final String selection = BaseColumns._ID + "<>? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ "<=? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ ">=?";
			final String currentStartTimeMillis = Long.toString(startTime
					.getTimeInMillis());
			final long contractionId = Intent.ACTION_INSERT.equals(activity
					.getIntent().getAction()) ? 0 : ContentUris
					.parseId(activity.getIntent().getData());
			final String[] selectionArgs = { Long.toString(contractionId),
					currentStartTimeMillis, currentStartTimeMillis };
			final Cursor data = activity.getContentResolver().query(
					ContractionContract.Contractions.CONTENT_URI, projection,
					selection, selectionArgs, null);
			final boolean overlapExists = data.moveToFirst();
			data.close();
			return overlapExists;
		}

		@Override
		protected void onPostExecute(final Boolean overlapExists)
		{
			if (BuildConfig.DEBUG)
				Log.d(EditFragment.this.getClass().getSimpleName(),
						"Start time overlap: " + overlapExists);
			final View view = getFragmentView();
			if (view == null)
				return;
			final TextView startTimeErrorOverlapView = (TextView) view
					.getTag(R.id.start_time_error_overlap);
			if (startTimeErrorOverlapView == null)
				return;
			if (overlapExists)
				startTimeErrorOverlapView.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				startTimeErrorOverlapView.setVisibility(View.GONE);
			}
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	/**
	 * AsyncTask used to check to see if the current start/end time overlaps any
	 * other existing contraction, displaying an error message if it does
	 */
	private class TimeOverlapCheck extends AsyncTask<Void, Void, Boolean>
	{
		@Override
		protected Boolean doInBackground(final Void... params)
		{
			final Activity activity = getActivity();
			if (activity == null)
				return false;
			final String[] projection = { BaseColumns._ID };
			final String selection = BaseColumns._ID + "<>? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ ">=? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ "<=?";
			final String currentStartTimeMillis = Long.toString(startTime
					.getTimeInMillis());
			final String currentEndTimeMillis = Long.toString(endTime
					.getTimeInMillis());
			final long contractionId = Intent.ACTION_INSERT.equals(activity
					.getIntent().getAction()) ? 0 : ContentUris
					.parseId(activity.getIntent().getData());
			final String[] selectionArgs = { Long.toString(contractionId),
					currentStartTimeMillis, currentEndTimeMillis };
			final Cursor data = activity.getContentResolver().query(
					ContractionContract.Contractions.CONTENT_URI, projection,
					selection, selectionArgs, null);
			final boolean overlapExists = data.moveToFirst();
			data.close();
			return overlapExists;
		}

		@Override
		protected void onPostExecute(final Boolean overlapExists)
		{
			if (BuildConfig.DEBUG)
				Log.d(EditFragment.this.getClass().getSimpleName(),
						"Time overlap: " + overlapExists);
			final View view = getFragmentView();
			if (view == null)
				return;
			final TextView timeErrorOverlapView = (TextView) view
					.getTag(R.id.time_error_overlap);
			if (timeErrorOverlapView == null)
				return;
			if (overlapExists)
				timeErrorOverlapView.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				timeErrorOverlapView.setVisibility(View.GONE);
			}
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	/**
	 * Total number of error checks
	 */
	private final static int ALL_ERROR_CHECK_PASSED = 4;
	/**
	 * Action associated with the end time's date being changed
	 */
	public final static String END_DATE_ACTION = "com.ianhanniballake.contractiontimer.END_DATE";
	/**
	 * Action associated with the end time's time being changed
	 */
	public final static String END_TIME_ACTION = "com.ianhanniballake.contractiontimer.END_TIME";
	/**
	 * Action associated with the start time's date being changed
	 */
	public final static String START_DATE_ACTION = "com.ianhanniballake.contractiontimer.START_DATE";
	/**
	 * Action associated with the start time's time being changed
	 */
	public final static String START_TIME_ACTION = "com.ianhanniballake.contractiontimer.START_TIME";
	/**
	 * Adapter to display the detailed data
	 */
	private CursorAdapter adapter;
	/**
	 * Handler for asynchronous updates of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;
	/**
	 * BroadcastReceiver listening for START_DATE_ACTION and END_DATE_ACTION
	 * actions
	 */
	private final BroadcastReceiver dateSetBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();
			final int year = intent.getIntExtra(
					DatePickerDialogFragment.YEAR_EXTRA,
					startTime.get(Calendar.YEAR));
			final int monthOfYear = intent.getIntExtra(
					DatePickerDialogFragment.MONTH_OF_YEAR_EXTRA,
					startTime.get(Calendar.MONTH));
			final int dayOfMonth = intent.getIntExtra(
					DatePickerDialogFragment.DAY_OF_MONTH_EXTRA,
					startTime.get(Calendar.DAY_OF_MONTH));
			if (BuildConfig.DEBUG)
				Log.d(EditFragment.this.getClass().getSimpleName(),
						"Date Receive: " + action + "; " + year + "-"
								+ monthOfYear + "-" + dayOfMonth);
			if (EditFragment.START_DATE_ACTION.equals(action))
			{
				final long oldStartTime = startTime.getTimeInMillis();
				startTime.set(Calendar.YEAR, year);
				startTime.set(Calendar.MONTH, monthOfYear);
				startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				final long timeOffset = startTime.getTimeInMillis()
						- oldStartTime;
				endTime.setTimeInMillis(endTime.getTimeInMillis() + timeOffset);
			}
			else if (EditFragment.END_DATE_ACTION.equals(action))
			{
				endTime.set(Calendar.YEAR, year);
				endTime.set(Calendar.MONTH, monthOfYear);
				endTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			}
			updateViews();
		}
	};
	/**
	 * Current end time of the contraction
	 */
	private Calendar endTime = null;
	/**
	 * Number of error checks that have passed in the start / end time
	 */
	private int errorCheckPass = 0;
	/**
	 * Current note of the contraction
	 */
	private String note = "";
	/**
	 * Current start time of the contraction
	 */
	private Calendar startTime = null;
	/**
	 * BroadcastReceiver listening for START_TIME_ACTION and END_TIME_ACTION
	 * actions
	 */
	private final BroadcastReceiver timeSetBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();
			final int hourOfDay = intent.getIntExtra(
					TimePickerDialogFragment.HOUR_OF_DAY_EXTRA,
					startTime.get(Calendar.HOUR_OF_DAY));
			final int minute = intent.getIntExtra(
					TimePickerDialogFragment.MINUTE_EXTRA,
					startTime.get(Calendar.MINUTE));
			if (BuildConfig.DEBUG)
				Log.d(EditFragment.this.getClass().getSimpleName(),
						"Time Receive: " + action + "; " + hourOfDay + ", "
								+ minute);
			if (EditFragment.START_TIME_ACTION.equals(action))
			{
				final long oldStartTime = startTime.getTimeInMillis();
				startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				startTime.set(Calendar.MINUTE, minute);
				startTime.set(Calendar.SECOND, 0);
				startTime.set(Calendar.MILLISECOND, 0);
				final long timeOffset = startTime.getTimeInMillis()
						- oldStartTime;
				endTime.setTimeInMillis(endTime.getTimeInMillis() + timeOffset);
			}
			else if (EditFragment.END_TIME_ACTION.equals(action))
			{
				endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				endTime.set(Calendar.MINUTE, minute);
				endTime.set(Calendar.SECOND, 0);
				endTime.set(Calendar.MILLISECOND, 0);
			}
			updateViews();
		}
	};

	/**
	 * Reset the current data to empty/current dates
	 */
	private void clear()
	{
		startTime = Calendar.getInstance();
		endTime = Calendar.getInstance();
		endTime.setTimeInMillis(startTime.getTimeInMillis());
		startTime.add(Calendar.MINUTE, -1);
		note = "";
	}

	/**
	 * Gets the current values from the edit fields for updating the contraction
	 * 
	 * @return ContentValues associated with the current (possibly edited) data
	 */
	private ContentValues getContentValues()
	{
		final ContentValues values = new ContentValues();
		values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				startTime.getTimeInMillis());
		values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
				endTime.getTimeInMillis());
		values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, note);
		return values;
	}

	/**
	 * We need to find the exact edit_fragment view as there is a
	 * NoSaveStateFrameLayout view inserted in between the parent and the view
	 * we created in onCreateView
	 * 
	 * @return View created in onCreateView
	 */
	private View getFragmentView()
	{
		final View rootView = getView();
		return rootView == null ? null : rootView
				.findViewById(R.id.edit_fragment);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		final Context applicationContext = getActivity()
				.getApplicationContext();
		contractionQueryHandler = new AsyncQueryHandler(getActivity()
				.getContentResolver())
		{
			@Override
			protected void onInsertComplete(final int token,
					final Object cookie, final Uri uri)
			{
				AppWidgetUpdateHandler.createInstance().updateAllWidgets(
						applicationContext);
				final Activity activity = getActivity();
				if (activity != null)
					activity.finish();
			}

			@Override
			protected void onUpdateComplete(final int token,
					final Object cookie, final int result)
			{
				AppWidgetUpdateHandler.createInstance().updateAllWidgets(
						applicationContext);
				final Activity activity = getActivity();
				if (activity != null)
					activity.finish();
			}
		};
		adapter = new CursorAdapter(getActivity(), null, 0)
		{
			@Override
			public void bindView(final View view, final Context context,
					final Cursor cursor)
			{
				final int startTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
				startTime = Calendar.getInstance();
				startTime.setTimeInMillis(cursor.getLong(startTimeColumnIndex));
				final int endTimeColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
				if (cursor.isNull(endTimeColumnIndex))
					endTime = null;
				else
				{
					endTime = Calendar.getInstance();
					endTime.setTimeInMillis(cursor.getLong(endTimeColumnIndex));
				}
				final int noteColumnIndex = cursor
						.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE);
				note = cursor.getString(noteColumnIndex);
			}

			@Override
			public View newView(final Context context, final Cursor cursor,
					final ViewGroup parent)
			{
				// View is already inflated in onCreateView
				return null;
			}
		};
		if (savedInstanceState != null)
		{
			startTime = (Calendar) savedInstanceState
					.getSerializable(ContractionContract.Contractions.COLUMN_NAME_START_TIME);
			endTime = (Calendar) savedInstanceState
					.getSerializable(ContractionContract.Contractions.COLUMN_NAME_END_TIME);
			note = savedInstanceState
					.getString(ContractionContract.Contractions.COLUMN_NAME_NOTE);
			// No longer need the loader as we'll use our local (possibly
			// changed) copies from now on
			getLoaderManager().destroyLoader(0);
			updateViews();
		}
		else if (Intent.ACTION_EDIT.equals(getActivity().getIntent()
				.getAction()))
			getLoaderManager().initLoader(0, null, this);
		else
		{
			clear();
			updateViews();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(getActivity(), getActivity().getIntent()
				.getData(), null, null, null, null);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		final boolean allErrorCheckPassed = errorCheckPass == EditFragment.ALL_ERROR_CHECK_PASSED;
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "All error check passed: "
					+ allErrorCheckPassed);
		menu.findItem(R.id.menu_save).setEnabled(allErrorCheckPassed);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.fragment_edit, container,
				false);
		view.setTag(R.id.start_time, view.findViewById(R.id.start_time));
		view.setTag(R.id.start_date, view.findViewById(R.id.start_date));
		view.setTag(R.id.end_time, view.findViewById(R.id.end_time));
		view.setTag(R.id.end_date, view.findViewById(R.id.end_date));
		view.setTag(R.id.duration, view.findViewById(R.id.duration));
		view.setTag(R.id.note, view.findViewById(R.id.note));
		view.setTag(R.id.time_error_overlap,
				view.findViewById(R.id.time_error_overlap));
		view.setTag(R.id.start_time_error_overlap,
				view.findViewById(R.id.start_time_error_overlap));
		view.setTag(R.id.end_time_error_overlap,
				view.findViewById(R.id.end_time_error_overlap));
		view.setTag(R.id.end_time_error_order,
				view.findViewById(R.id.end_time_error_order));
		final TextView startTimeView = (TextView) view.getTag(R.id.start_time);
		startTimeView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final TimePickerDialogFragment timePicker = new TimePickerDialogFragment();
				final Bundle args = new Bundle();
				args.putString(TimePickerDialogFragment.CALLBACK_ACTION,
						EditFragment.START_TIME_ACTION);
				args.putSerializable(TimePickerDialogFragment.TIME_ARGUMENT,
						startTime);
				timePicker.setArguments(args);
				if (BuildConfig.DEBUG)
					Log.d(timePicker.getClass().getSimpleName(),
							"Showing Start Time Dialog");
				AnalyticsManagerService
						.trackPageView(getActivity(), timePicker);
				timePicker.show(getFragmentManager(), "startTime");
			}
		});
		final TextView startDateView = (TextView) view.getTag(R.id.start_date);
		startDateView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final DatePickerDialogFragment datePicker = new DatePickerDialogFragment();
				final Bundle args = new Bundle();
				args.putString(DatePickerDialogFragment.CALLBACK_ACTION,
						EditFragment.START_DATE_ACTION);
				args.putSerializable(DatePickerDialogFragment.DATE_ARGUMENT,
						startTime);
				datePicker.setArguments(args);
				if (BuildConfig.DEBUG)
					Log.d(datePicker.getClass().getSimpleName(),
							"Showing Start Date Dialog");
				AnalyticsManagerService
						.trackPageView(getActivity(), datePicker);
				datePicker.show(getFragmentManager(), "startDate");
			}
		});
		final TextView endTimeView = (TextView) view.getTag(R.id.end_time);
		endTimeView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final TimePickerDialogFragment timePicker = new TimePickerDialogFragment();
				final Bundle args = new Bundle();
				args.putString(TimePickerDialogFragment.CALLBACK_ACTION,
						EditFragment.END_TIME_ACTION);
				args.putSerializable(TimePickerDialogFragment.TIME_ARGUMENT,
						endTime);
				timePicker.setArguments(args);
				if (BuildConfig.DEBUG)
					Log.d(timePicker.getClass().getSimpleName(),
							"Showing End Time Dialog");
				AnalyticsManagerService
						.trackPageView(getActivity(), timePicker);
				timePicker.show(getFragmentManager(), "endTime");
			}
		});
		final TextView endDateView = (TextView) view.getTag(R.id.end_date);
		endDateView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final DatePickerDialogFragment datePicker = new DatePickerDialogFragment();
				final Bundle args = new Bundle();
				args.putString(DatePickerDialogFragment.CALLBACK_ACTION,
						EditFragment.END_DATE_ACTION);
				args.putSerializable(DatePickerDialogFragment.DATE_ARGUMENT,
						endTime);
				datePicker.setArguments(args);
				if (BuildConfig.DEBUG)
					Log.d(datePicker.getClass().getSimpleName(),
							"Showing End Date Dialog");
				AnalyticsManagerService
						.trackPageView(getActivity(), datePicker);
				datePicker.show(getFragmentManager(), "endDate");
			}
		});
		final EditText noteView = (EditText) view.getTag(R.id.note);
		noteView.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void afterTextChanged(final Editable s)
			{
				note = s.toString();
			}

			@Override
			public void beforeTextChanged(final CharSequence s,
					final int start, final int count, final int after)
			{
				// Nothing to do
			}

			@Override
			public void onTextChanged(final CharSequence s, final int start,
					final int before, final int count)
			{
				// Nothing to do
			}
		});
		return view;
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
		else
			clear();
		updateViews();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_save:
				final ContentValues values = getContentValues();
				if (Intent.ACTION_INSERT.equals(getActivity().getIntent()
						.getAction()))
				{
					if (BuildConfig.DEBUG)
						Log.d(getClass().getSimpleName(), "Add selected save");
					AnalyticsManagerService.trackEvent(getActivity(), "Add",
							"Save");
					contractionQueryHandler.startInsert(0, null, getActivity()
							.getIntent().getData(), values);
				}
				else
				{
					if (BuildConfig.DEBUG)
						Log.d(getClass().getSimpleName(), "Edit selected save");
					AnalyticsManagerService.trackEvent(getActivity(), "Edit",
							"Save");
					contractionQueryHandler.startUpdate(0, null, getActivity()
							.getIntent().getData(), values, null, null);
				}
				return true;
			case R.id.menu_cancel:
				if (BuildConfig.DEBUG)
					Log.d(getClass().getSimpleName(), "Edit selected cancel");
				AnalyticsManagerService.trackEvent(getActivity(), "Edit",
						"Cancel");
				getActivity().finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putSerializable(
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				startTime);
		outState.putSerializable(
				ContractionContract.Contractions.COLUMN_NAME_END_TIME, endTime);
		outState.putString(ContractionContract.Contractions.COLUMN_NAME_NOTE,
				note);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
				.getInstance(getActivity());
		final IntentFilter timeFilter = new IntentFilter();
		timeFilter.addAction(EditFragment.START_TIME_ACTION);
		timeFilter.addAction(EditFragment.END_TIME_ACTION);
		localBroadcastManager.registerReceiver(timeSetBroadcastReceiver,
				timeFilter);
		final IntentFilter dateFilter = new IntentFilter();
		dateFilter.addAction(EditFragment.START_DATE_ACTION);
		dateFilter.addAction(EditFragment.END_DATE_ACTION);
		localBroadcastManager.registerReceiver(dateSetBroadcastReceiver,
				dateFilter);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
				.getInstance(getActivity());
		localBroadcastManager.unregisterReceiver(timeSetBroadcastReceiver);
		localBroadcastManager.unregisterReceiver(dateSetBroadcastReceiver);
	}

	/**
	 * Updates the edit views based on the current internal data
	 */
	private void updateViews()
	{
		errorCheckPass = 0;
		new StartTimeOverlapCheck().execute();
		new EndTimeOverlapCheck().execute();
		new TimeOverlapCheck().execute();
		final View view = getFragmentView();
		if (view == null)
			return;
		final TextView startTimeView = (TextView) view.getTag(R.id.start_time);
		final TextView startDateView = (TextView) view.getTag(R.id.start_date);
		String timeFormat = "hh:mm:ssaa";
		if (DateFormat.is24HourFormat(getActivity()))
			timeFormat = "kk:mm:ss";
		startTimeView.setText(DateFormat.format(timeFormat, startTime));
		startDateView.setText(DateFormat.getDateFormat(getActivity()).format(
				startTime.getTime()));
		final TextView endTimeView = (TextView) view.getTag(R.id.end_time);
		final TextView endDateView = (TextView) view.getTag(R.id.end_date);
		final TextView durationView = (TextView) view.getTag(R.id.duration);
		final boolean isContractionOngoing = endTime == null;
		if (isContractionOngoing)
		{
			endTimeView.setText("");
			endDateView.setText("");
			durationView.setText(getString(R.string.duration_ongoing));
		}
		else
		{
			endTimeView.setText(DateFormat.format(timeFormat, endTime));
			endDateView.setText(DateFormat.getDateFormat(getActivity()).format(
					endTime.getTime()));
			final TextView endTimeErrorOrderView = (TextView) view
					.getTag(R.id.end_time_error_order);
			if (endTime.before(startTime))
			{
				endTimeErrorOrderView.setVisibility(View.VISIBLE);
				durationView.setText("");
			}
			else
			{
				errorCheckPass++;
				endTimeErrorOrderView.setVisibility(View.GONE);
				final long durationInSeconds = (endTime.getTimeInMillis() - startTime
						.getTimeInMillis()) / 1000;
				durationView.setText(DateUtils
						.formatElapsedTime(durationInSeconds));
			}
			getActivity().supportInvalidateOptionsMenu();
		}
		final EditText noteView = (EditText) view.getTag(R.id.note);
		noteView.setText(note);
	}
}
