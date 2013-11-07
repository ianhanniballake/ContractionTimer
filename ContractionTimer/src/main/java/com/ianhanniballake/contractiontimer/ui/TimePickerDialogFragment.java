package com.ianhanniballake.contractiontimer.ui;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import com.ianhanniballake.contractiontimer.BuildConfig;

import java.util.Calendar;

/**
 * Provides a DialogFragment for selecting a time
 */
public class TimePickerDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    /**
     * Argument key for storing/retrieving the callback action
     */
    public final static String CALLBACK_ACTION = "com.ianhanniballake.contractiontimer.CALLBACK_ACTION_ARGUMENT";
    /**
     * Extra corresponding with the hour of the day that was set
     */
    public final static String HOUR_OF_DAY_EXTRA = "com.ianhanniballake.contractionTimer.HOUR_OF_DAY_EXTRA";
    /**
     * Extra corresponding with the minute that was set
     */
    public final static String MINUTE_EXTRA = "com.ianhanniballake.contractionTimer.MINUTE_EXTRA";
    /**
     * Argument key for storing/retrieving the time associated with this dialog
     */
    public final static String TIME_ARGUMENT = "com.ianhanniballake.contractiontimer.TIME_ARGUMENT";
    /**
     * Action associated with this fragment closing
     */
    public final static String TIME_PICKER_CLOSE_ACTION = "com.ianhanniballake.contractiontimer.TIME_PICKER_CLOSE";

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Calendar date = (Calendar) getArguments().getSerializable(TimePickerDialogFragment.TIME_ARGUMENT);
        final TimePickerDialog dialog = new TimePickerDialog(getActivity(), this, date.get(Calendar.HOUR_OF_DAY),
                date.get(Calendar.MINUTE), DateFormat.is24HourFormat(getActivity()));
        dialog.setOnDismissListener(this);
        return dialog;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.sendBroadcast(new Intent(TIME_PICKER_CLOSE_ACTION));
        super.onDismiss(dialog);
    }

    @Override
    public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
        final String action = getArguments().getString(TimePickerDialogFragment.CALLBACK_ACTION);
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "onTimeSet: " + action);
        final Intent broadcast = new Intent(action);
        broadcast.putExtra(TimePickerDialogFragment.HOUR_OF_DAY_EXTRA, hourOfDay);
        broadcast.putExtra(TimePickerDialogFragment.MINUTE_EXTRA, minute);
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.sendBroadcast(broadcast);
    }
}
