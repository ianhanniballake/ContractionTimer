package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ikovac.timepickerwithseconds.view.MyTimePickerDialog;

import java.util.Calendar;

/**
 * Provides a DialogFragment for selecting a time
 */
public class TimePickerDialogFragment extends DialogFragment {
    /**
     * Gets an API level specific implementation of the time picker
     * @param context context used to create the Dialog
     * @param callback Callback to pass the returned time to
     * @param date starting date
     * @return A valid TimePickerDialog
     */
    private static AlertDialog getTimePickerDialog(Context context, final TimePickerDialogFragment callback,
                                                   Calendar date) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            MyTimePickerDialog.OnTimeSetListener onTimeSetListener =
                    new MyTimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(final com.ikovac.timepickerwithseconds.view.TimePicker view,
                                              final int hourOfDay, final int minute, final int seconds) {
                            callback.onTimeSet(hourOfDay, minute, seconds);
                        }
                    };
            return new MyTimePickerDialog(context,
                    onTimeSetListener,
                    date.get(Calendar.HOUR_OF_DAY),
                    date.get(Calendar.MINUTE),
                    date.get(Calendar.SECOND),
                    DateFormat.is24HourFormat(context));
        } else {
            TimePickerDialog.OnTimeSetListener onTimeSetListener =
                    new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(final TimePicker view,
                                              final int hourOfDay, final int minute) {
                            callback.onTimeSet(hourOfDay, minute, 0);
                        }
                    };
            return new TimePickerDialog(context,
                    onTimeSetListener,
                    date.get(Calendar.HOUR_OF_DAY),
                    date.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(context));
        }
    }
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
     * Extra corresponding with the second that was set
     */
    public final static String SECOND_EXTRA = "com.ianhanniballake.contractionTimer.SECOND_EXTRA";
    /**
     * Argument key for storing/retrieving the time associated with this dialog
     */
    public final static String TIME_ARGUMENT = "com.ianhanniballake.contractiontimer.TIME_ARGUMENT";
    /**
     * Action associated with this fragment closing
     */
    public final static String TIME_PICKER_CLOSE_ACTION = "com.ianhanniballake.contractiontimer.TIME_PICKER_CLOSE";
    private final static String TAG = TimePickerDialogFragment.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Calendar date = (Calendar) getArguments().getSerializable(TimePickerDialogFragment.TIME_ARGUMENT);
        final AlertDialog dialog = getTimePickerDialog(getActivity(), this, date);
        dialog.setOnDismissListener(this);
        return dialog;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.sendBroadcast(new Intent(TIME_PICKER_CLOSE_ACTION));
        super.onDismiss(dialog);
    }

    void onTimeSet(final int hourOfDay, final int minute, final int second) {
        final String action = getArguments().getString(TimePickerDialogFragment.CALLBACK_ACTION);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onTimeSet: " + action);
        final Intent broadcast = new Intent(action);
        broadcast.putExtra(TimePickerDialogFragment.HOUR_OF_DAY_EXTRA, hourOfDay);
        broadcast.putExtra(TimePickerDialogFragment.MINUTE_EXTRA, minute);
        broadcast.putExtra(TimePickerDialogFragment.SECOND_EXTRA, second);
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.sendBroadcast(broadcast);
    }
}
