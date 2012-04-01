package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

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
import android.widget.Button;
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
			final String[] projection = { BaseColumns._ID };
			final String selection = BaseColumns._ID + "<>? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ "<=? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ ">=?";
			final String currentEndTimeMillis = Long.toString(endTime
					.getTimeInMillis());
			final String[] selectionArgs = { Long.toString(contractionId),
					currentEndTimeMillis, currentEndTimeMillis };
			final Context context = getActivity();
			if (context == null)
				return false;
			final Cursor data = context.getContentResolver().query(
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
			final View view = getView();
			if (view == null)
				return;
			final ViewHolder holder = EditFragment.getViewHolder(view);
			if (overlapExists)
				holder.endTimeErrorOverlap.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				holder.endTimeErrorOverlap.setVisibility(View.GONE);
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
			final String[] projection = { BaseColumns._ID };
			final String selection = BaseColumns._ID + "<>? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ "<=? AND "
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ ">=?";
			final String currentStartTimeMillis = Long.toString(startTime
					.getTimeInMillis());
			final String[] selectionArgs = { Long.toString(contractionId),
					currentStartTimeMillis, currentStartTimeMillis };
			final Context context = getActivity();
			if (context == null)
				return false;
			final Cursor data = context.getContentResolver().query(
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
			final View view = getView();
			if (view == null)
				return;
			final ViewHolder holder = EditFragment.getViewHolder(view);
			if (overlapExists)
				holder.startTimeErrorOverlap.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				holder.startTimeErrorOverlap.setVisibility(View.GONE);
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
			final String[] selectionArgs = { Long.toString(contractionId),
					currentStartTimeMillis, currentEndTimeMillis };
			final Context context = getActivity();
			if (context == null)
				return false;
			final Cursor data = context.getContentResolver().query(
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
			final View view = getView();
			if (view == null)
				return;
			final ViewHolder holder = EditFragment.getViewHolder(view);
			if (overlapExists)
				holder.timeErrorOverlap.setVisibility(View.VISIBLE);
			else
			{
				errorCheckPass++;
				holder.timeErrorOverlap.setVisibility(View.GONE);
			}
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	/**
	 * Helper class used to store temporary references to list item views
	 */
	static class ViewHolder
	{
		/**
		 * TextView representing the duration of the contraction
		 */
		TextView duration;
		/**
		 * TextView representing the formatted end date of the contraction
		 */
		Button endDate;
		/**
		 * TextView representing the formatted end time of the contraction
		 */
		Button endTime;
		/**
		 * TextView representing the end time order error message
		 */
		TextView endTimeErrorOrder;
		/**
		 * TextView representing the end time overlap error message
		 */
		TextView endTimeErrorOverlap;
		/**
		 * EditText representing the note attached to the contraction
		 */
		EditText note;
		/**
		 * TextView representing the formatted start date of the contraction
		 */
		Button startDate;
		/**
		 * TextView representing the formatted start time of the contraction
		 */
		Button startTime;
		/**
		 * TextView representing the start time overlap error message
		 */
		TextView startTimeErrorOverlap;
		/**
		 * TextView representing the time overlap error message
		 */
		TextView timeErrorOverlap;
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
	 * Gets a valid ViewHolder associated with the given view, creating one if
	 * it doesn't exist
	 * 
	 * @param view
	 *            View to retrieve a ViewHolder from
	 * @return A valid ViewHolder instance
	 */
	private static ViewHolder getViewHolder(final View view)
	{
		final Object viewTag = view.getTag();
		ViewHolder holder;
		if (viewTag == null)
		{
			holder = new ViewHolder();
			holder.timeErrorOverlap = (TextView) view
					.findViewById(R.id.time_error_overlap);
			holder.startTime = (Button) view.findViewById(R.id.start_time);
			holder.startDate = (Button) view.findViewById(R.id.start_date);
			holder.startTimeErrorOverlap = (TextView) view
					.findViewById(R.id.start_time_error_overlap);
			holder.endTime = (Button) view.findViewById(R.id.end_time);
			holder.endDate = (Button) view.findViewById(R.id.end_date);
			holder.endTimeErrorOrder = (TextView) view
					.findViewById(R.id.end_time_error_order);
			holder.endTimeErrorOverlap = (TextView) view
					.findViewById(R.id.end_time_error_overlap);
			holder.duration = (TextView) view.findViewById(R.id.duration);
			holder.note = (EditText) view.findViewById(R.id.note);
			view.setTag(holder);
		}
		else
			holder = (ViewHolder) viewTag;
		return holder;
	}

	/**
	 * Adapter to display the detailed data
	 */
	private CursorAdapter adapter;
	/**
	 * Id of the current contraction to show
	 */
	private long contractionId = 0;
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

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		contractionQueryHandler = new AsyncQueryHandler(getActivity()
				.getContentResolver())
		{
			@Override
			protected void onInsertComplete(final int token,
					final Object cookie, final Uri uri)
			{
				AppWidgetUpdateHandler.updateAllWidgets(getActivity());
				getActivity().finish();
			}

			@Override
			protected void onUpdateComplete(final int token,
					final Object cookie, final int result)
			{
				AppWidgetUpdateHandler.updateAllWidgets(getActivity());
				getActivity().finish();
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
		if (getArguments() != null)
		{
			contractionId = getArguments().getLong(BaseColumns._ID, 0);
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
			else
				getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		final Uri contractionUri = ContentUris.withAppendedId(
				ContractionContract.Contractions.CONTENT_ID_URI_PATTERN,
				contractionId);
		return new CursorLoader(getActivity(), contractionUri, null, null,
				null, null);
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
		final ViewHolder holder = EditFragment.getViewHolder(view);
		holder.startTime.setOnClickListener(new OnClickListener()
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
		holder.startDate.setOnClickListener(new OnClickListener()
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
		holder.endTime.setOnClickListener(new OnClickListener()
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
		holder.endDate.setOnClickListener(new OnClickListener()
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
		holder.note.addTextChangedListener(new TextWatcher()
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
			adapter.bindView(getView(), getActivity(), data);
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
				if (contractionId == 0)
				{
					if (BuildConfig.DEBUG)
						Log.d(getClass().getSimpleName(), "Add selected save");
					AnalyticsManagerService.trackEvent(getActivity(), "Add",
							"Save");
					contractionQueryHandler.startInsert(0, null,
							ContractionContract.Contractions.CONTENT_URI,
							values);
				}
				else
				{
					if (BuildConfig.DEBUG)
						Log.d(getClass().getSimpleName(), "Edit selected save");
					AnalyticsManagerService.trackEvent(getActivity(), "Edit",
							"Save");
					final Uri updateUri = ContentUris
							.withAppendedId(
									ContractionContract.Contractions.CONTENT_ID_URI_BASE,
									contractionId);
					contractionQueryHandler.startUpdate(0, null, updateUri,
							values, null, null);
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
		final ViewHolder holder = EditFragment.getViewHolder(getView());
		String timeFormat = "hh:mm:ssaa";
		if (DateFormat.is24HourFormat(getActivity()))
			timeFormat = "kk:mm:ss";
		holder.startTime.setText(DateFormat.format(timeFormat, startTime));
		holder.startDate.setText(DateFormat.getDateFormat(getActivity())
				.format(startTime.getTime()));
		final boolean isContractionOngoing = endTime == null;
		if (isContractionOngoing)
		{
			holder.endTime.setText("");
			holder.endDate.setText("");
			holder.duration.setText(getString(R.string.duration_ongoing));
		}
		else
		{
			holder.endTime.setText(DateFormat.format(timeFormat, endTime));
			holder.endDate.setText(DateFormat.getDateFormat(getActivity())
					.format(endTime.getTime()));
			if (endTime.before(startTime))
			{
				holder.endTimeErrorOrder.setVisibility(View.VISIBLE);
				holder.duration.setText("");
			}
			else
			{
				errorCheckPass++;
				holder.endTimeErrorOrder.setVisibility(View.GONE);
				final long durationInSeconds = (endTime.getTimeInMillis() - startTime
						.getTimeInMillis()) / 1000;
				holder.duration.setText(DateUtils
						.formatElapsedTime(durationInSeconds));
			}
			getActivity().supportInvalidateOptionsMenu();
		}
		holder.note.setText(note);
	}
}
