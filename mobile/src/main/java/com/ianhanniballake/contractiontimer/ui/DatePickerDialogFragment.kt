package com.ianhanniballake.contractiontimer.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.DatePicker

import com.ianhanniballake.contractiontimer.BuildConfig

import java.util.Calendar

/**
 * Provides a DialogFragment for selecting a date
 */
class DatePickerDialogFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {
    companion object {
        private const val TAG = "DatePickerDialog"
        /**
         * Argument key for storing/retrieving the callback action
         */
        const val CALLBACK_ACTION = "com.ianhanniballake.contractiontimer.CALLBACK_ACTION_ARGUMENT"
        /**
         * Argument key for storing/retrieving the date associated with this dialog
         */
        const val DATE_ARGUMENT = "com.ianhanniballake.contractiontimer.Date"
        /**
         * Extra corresponding with the day of the month that was set
         */
        const val DAY_OF_MONTH_EXTRA = "com.ianhanniballake.contractionTimer.DAY_OF_MONTH_EXTRA"
        /**
         * Extra corresponding with the month of the year that was set
         */
        const val MONTH_OF_YEAR_EXTRA = "com.ianhanniballake.contractionTimer.MONTH_OF_YEAR_EXTRA"
        /**
         * Extra corresponding with the year that was set
         */
        const val YEAR_EXTRA = "com.ianhanniballake.contractionTimer.YEAR_EXTRA"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments.getSerializable(DatePickerDialogFragment.DATE_ARGUMENT) as Calendar
        val dialog = DatePickerDialog(activity, this, date.get(Calendar.YEAR),
                date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
        dialog.setOnDismissListener(this)
        return dialog
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val action = arguments.getString(DatePickerDialogFragment.CALLBACK_ACTION)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onDateSet: $action")
        val broadcast = Intent(action).apply {
            putExtra(DatePickerDialogFragment.YEAR_EXTRA, year)
            putExtra(DatePickerDialogFragment.MONTH_OF_YEAR_EXTRA, monthOfYear)
            putExtra(DatePickerDialogFragment.DAY_OF_MONTH_EXTRA, dayOfMonth)
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity)
        localBroadcastManager.sendBroadcast(broadcast)
    }
}
