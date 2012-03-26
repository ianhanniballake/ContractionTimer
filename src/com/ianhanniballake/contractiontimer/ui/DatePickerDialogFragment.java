package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.DatePicker;

import com.ianhanniballake.contractiontimer.BuildConfig;

/**
 * Provides a DialogFragment for selecting a date
 */
public class DatePickerDialogFragment extends DialogFragment implements
		DatePickerDialog.OnDateSetListener
{
	/**
	 * Argument key for storing/retrieving the callback action
	 */
	public final static String CALLBACK_ACTION = "com.ianhanniballake.contractiontimer.CALLBACK_ACTION_ARGUMENT";
	/**
	 * Argument key for storing/retrieving the date associated with this dialog
	 */
	public final static String DATE_ARGUMENT = "com.ianhanniballake.contractiontimer.Date";
	/**
	 * Extra corresponding with the day of the month that was set
	 */
	public final static String DAY_OF_MONTH_EXTRA = "com.ianhanniballake.contractionTimer.DAY_OF_MONTH_EXTRA";
	/**
	 * Extra corresponding with the month of the year that was set
	 */
	public final static String MONTH_OF_YEAR_EXTRA = "com.ianhanniballake.contractionTimer.MONTH_OF_YEAR_EXTRA";
	/**
	 * Extra corresponding with the year that was set
	 */
	public final static String YEAR_EXTRA = "com.ianhanniballake.contractionTimer.YEAR_EXTRA";

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final Calendar date = (Calendar) getArguments().getSerializable(
				DatePickerDialogFragment.DATE_ARGUMENT);
		return new DatePickerDialog(getActivity(), this,
				date.get(Calendar.YEAR), date.get(Calendar.MONTH),
				date.get(Calendar.DAY_OF_MONTH));
	}

	@Override
	public void onDateSet(final DatePicker view, final int year,
			final int monthOfYear, final int dayOfMonth)
	{
		final String action = getArguments().getString(
				DatePickerDialogFragment.CALLBACK_ACTION);
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "onDateSet: " + action);
		final Intent broadcast = new Intent(action);
		broadcast.putExtra(DatePickerDialogFragment.YEAR_EXTRA, year);
		broadcast.putExtra(DatePickerDialogFragment.MONTH_OF_YEAR_EXTRA,
				monthOfYear);
		broadcast.putExtra(DatePickerDialogFragment.DAY_OF_MONTH_EXTRA,
				dayOfMonth);
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
				.getInstance(getActivity());
		localBroadcastManager.sendBroadcast(broadcast);
	}
}
