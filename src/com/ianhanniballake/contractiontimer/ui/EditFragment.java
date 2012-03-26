package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment showing the details of an individual contraction
 */
public class EditFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
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
	}

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
			holder.startTime = (Button) view.findViewById(R.id.start_time);
			holder.startDate = (Button) view.findViewById(R.id.start_date);
			holder.endTime = (Button) view.findViewById(R.id.end_time);
			holder.endDate = (Button) view.findViewById(R.id.end_date);
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
				startTime.set(Calendar.YEAR, year);
				startTime.set(Calendar.MONTH, monthOfYear);
				startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
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
				startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				startTime.set(Calendar.MINUTE, minute);
				startTime.set(Calendar.SECOND, 0);
			}
			else if (EditFragment.END_TIME_ACTION.equals(action))
			{
				endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				endTime.set(Calendar.MINUTE, minute);
				endTime.set(Calendar.SECOND, 0);
			}
			updateViews();
		}
	};

	/**
	 * Reset the current data to empty/current dates
	 */
	private void clear()
	{
		final Calendar currentTime = Calendar.getInstance();
		startTime = currentTime;
		endTime = currentTime;
		note = "";
	}

	/**
	 * Gets the current values from the edit fields for updating the contraction
	 * 
	 * @return ContentValues associated with the current (possibly edited) data
	 */
	public ContentValues getContentValues()
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
			}
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
		if (startTime == null)
			if (data.moveToFirst())
				adapter.bindView(getView(), getActivity(), data);
			else
				clear();
		updateViews();
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
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "onStart");
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
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "onStop");
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
			holder.endTime.setText(" ");
			holder.endDate.setText(" ");
			holder.duration.setText(getString(R.string.duration_ongoing));
		}
		else
		{
			holder.endTime.setText(DateFormat.format(timeFormat, endTime));
			holder.endDate.setText(DateFormat.getDateFormat(getActivity())
					.format(endTime.getTime()));
			final long durationInSeconds = (endTime.getTimeInMillis() - startTime
					.getTimeInMillis()) / 1000;
			holder.duration.setText(DateUtils
					.formatElapsedTime(durationInSeconds));
		}
		holder.note.setText(note);
	}
}
