package com.ianhanniballake.contractiontimer.ui;

import java.util.Calendar;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

/**
 * Provides a DialogFragment for selecting a time
 */
public class TimePickerDialogFragment extends DialogFragment implements
		TimePickerDialog.OnTimeSetListener
{
	/**
	 * Argument key for storing/retrieving the time associated with this dialog
	 */
	public final static String TIME_ARGUMENT = "com.ianhanniballake.contractiontimer.Time";
	/**
	 * Callback listener to flow time set events to
	 */
	private TimePickerDialog.OnTimeSetListener callbackListener;

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final Calendar date = (Calendar) getArguments().getSerializable(
				TimePickerDialogFragment.TIME_ARGUMENT);
		return new TimePickerDialog(getActivity(), this,
				date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE),
				DateFormat.is24HourFormat(getActivity()));
	}

	@Override
	public void onTimeSet(final TimePicker view, final int hourOfDay,
			final int minute)
	{
		callbackListener.onTimeSet(view, hourOfDay, minute);
	}

	/**
	 * Sets the listener to call on TimeSet events
	 * 
	 * @param listener
	 *            callback listener to use
	 */
	public void setOnTimeSetListener(
			final TimePickerDialog.OnTimeSetListener listener)
	{
		callbackListener = listener;
	}
}
