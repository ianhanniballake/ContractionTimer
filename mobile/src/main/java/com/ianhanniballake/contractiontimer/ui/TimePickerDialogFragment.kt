package com.ianhanniballake.contractiontimer.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.LocalBroadcastManager
import android.text.format.DateFormat
import android.util.Log
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ikovac.timepickerwithseconds.view.MyTimePickerDialog
import java.util.Calendar

/**
 * Provides a DialogFragment for selecting a time
 */
class TimePickerDialogFragment : DialogFragment() {
    companion object {
        private const val TAG = "TimePickerDialog"
        /**
         * Argument key for storing/retrieving the callback action
         */
        const val CALLBACK_ACTION = "com.ianhanniballake.contractiontimer.CALLBACK_ACTION_ARGUMENT"
        /**
         * Extra corresponding with the hour of the day that was set
         */
        const val HOUR_OF_DAY_EXTRA = "com.ianhanniballake.contractionTimer.HOUR_OF_DAY_EXTRA"
        /**
         * Extra corresponding with the minute that was set
         */
        const val MINUTE_EXTRA = "com.ianhanniballake.contractionTimer.MINUTE_EXTRA"
        /**
         * Extra corresponding with the second that was set
         */
        const val SECOND_EXTRA = "com.ianhanniballake.contractionTimer.SECOND_EXTRA"
        /**
         * Argument key for storing/retrieving the time associated with this dialog
         */
        const val TIME_ARGUMENT = "com.ianhanniballake.contractiontimer.TIME_ARGUMENT"

        /**
         * Gets an API level specific implementation of the time picker
         *
         * @param context  context used to create the Dialog
         * @param callback Callback to pass the returned time to
         * @param date     starting date
         * @return A valid TimePickerDialog
         */
        private fun getTimePickerDialog(
                context: Context,
                callback: TimePickerDialogFragment,
                date: Calendar
        ): Dialog {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                val onTimeSetListener = MyTimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, seconds ->
                    callback.onTimeSet(hourOfDay, minute, seconds)
                }
                return MyTimePickerDialog(context,
                        onTimeSetListener,
                        date.get(Calendar.HOUR_OF_DAY),
                        date.get(Calendar.MINUTE),
                        date.get(Calendar.SECOND),
                        DateFormat.is24HourFormat(context))
            } else {
                val onTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    callback.onTimeSet(hourOfDay, minute, 0)
                }
                return TimePickerDialog(context,
                        onTimeSetListener,
                        date.get(Calendar.HOUR_OF_DAY),
                        date.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(context))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments.getSerializable(TimePickerDialogFragment.TIME_ARGUMENT) as Calendar
        val dialog = getTimePickerDialog(activity, this, date)
        dialog.setOnDismissListener(this)
        return dialog
    }

    internal fun onTimeSet(hourOfDay: Int, minute: Int, second: Int) {
        val action = arguments.getString(TimePickerDialogFragment.CALLBACK_ACTION)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onTimeSet: $action")
        val broadcast = Intent(action).apply {
            putExtra(TimePickerDialogFragment.HOUR_OF_DAY_EXTRA, hourOfDay)
            putExtra(TimePickerDialogFragment.MINUTE_EXTRA, minute)
            putExtra(TimePickerDialogFragment.SECOND_EXTRA, second)
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity)
        localBroadcastManager.sendBroadcast(broadcast)
    }
}
