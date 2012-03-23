package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

/**
 * Provides a DialogFragment for selecting a date
 */
public class DatePickerDialogFragment extends DialogFragment implements
		DatePickerDialog.OnDateSetListener
{
	/**
	 * Argument key for storing/retrieving the date associated with this dialog
	 */
	public final static String DATE_ARGUMENT = "com.ianhanniballake.contractiontimer.Date";
	/**
	 * Callback listener to flow date set events to
	 */
	private DatePickerDialog.OnDateSetListener callbackListener;

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
		callbackListener.onDateSet(view, year, monthOfYear, dayOfMonth);
	}

	/**
	 * Sets the listener to call on DateSet events
	 * 
	 * @param listener
	 *            callback listener to use
	 */
	public void setOnDateSetListener(
			final DatePickerDialog.OnDateSetListener listener)
	{
		callbackListener = listener;
	}
}
