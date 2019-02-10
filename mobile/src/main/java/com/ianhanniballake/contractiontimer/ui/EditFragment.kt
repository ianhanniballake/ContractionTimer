package com.ianhanniballake.contractiontimer.ui

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.widget.CursorAdapter
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.extensions.closeable
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Fragment showing the details of an individual contraction
 */
class EditFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "EditFragment"
        /**
         * Action associated with the end time's date being changed
         */
        const val END_DATE_ACTION = "com.ianhanniballake.contractiontimer.END_DATE"
        /**
         * Action associated with the end time's time being changed
         */
        const val END_TIME_ACTION = "com.ianhanniballake.contractiontimer.END_TIME"
        /**
         * Action associated with the start time's date being changed
         */
        const val START_DATE_ACTION = "com.ianhanniballake.contractiontimer.START_DATE"
        /**
         * Action associated with the start time's time being changed
         */
        const val START_TIME_ACTION = "com.ianhanniballake.contractiontimer.START_TIME"
    }

    /**
     * Current end time of the contraction
     */
    internal var endTime: Calendar? = null
    /**
     * Current note of the contraction
     */
    internal var note: String? = ""
    /**
     * Whether the End Time Order check passed
     */
    private var passedEndTimeOrderCheck = false
    /**
     * Whether the End Time Overlap check passed
     */
    private var passedEndTimeOverlapCheck = false
    /**
     * Whether the Start Time Overlap check passed
     */
    private var passedStartTimeOverlapCheck = false
    /**
     * Whether the Time Overlap check passed
     */
    private var passedTimeOverlapCheck = false
    /**
     * Current start time of the contraction
     */
    internal var startTime: Calendar? = null
    /**
     * BroadcastReceiver listening for START_DATE_ACTION and END_DATE_ACTION actions
     */
    private val dateSetBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val year = intent.getIntExtra(DatePickerDialogFragment.YEAR_EXTRA, startTime!!.get(Calendar.YEAR))
            val monthOfYear = intent.getIntExtra(DatePickerDialogFragment.MONTH_OF_YEAR_EXTRA,
                    startTime!!.get(Calendar.MONTH))
            val dayOfMonth = intent.getIntExtra(DatePickerDialogFragment.DAY_OF_MONTH_EXTRA,
                    startTime!!.get(Calendar.DAY_OF_MONTH))
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Date Receive: $action; $year-$monthOfYear-$dayOfMonth")
            if (EditFragment.START_DATE_ACTION == action) {
                val oldStartTime = startTime!!.timeInMillis
                startTime!!.set(Calendar.YEAR, year)
                startTime!!.set(Calendar.MONTH, monthOfYear)
                startTime!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val timeOffset = startTime!!.timeInMillis - oldStartTime
                endTime!!.timeInMillis = endTime!!.timeInMillis + timeOffset
            } else if (EditFragment.END_DATE_ACTION == action) {
                endTime!!.set(Calendar.YEAR, year)
                endTime!!.set(Calendar.MONTH, monthOfYear)
                endTime!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            updateViews()
        }
    }
    /**
     * BroadcastReceiver listening for START_TIME_ACTION and END_TIME_ACTION actions
     */
    private val timeSetBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val hourOfDay = intent.getIntExtra(TimePickerDialogFragment.HOUR_OF_DAY_EXTRA, 0)
            val minute = intent.getIntExtra(TimePickerDialogFragment.MINUTE_EXTRA, 0)
            val second = intent.getIntExtra(TimePickerDialogFragment.SECOND_EXTRA, 0)
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Time Receive: $action; $hourOfDay, $minute")
            if (EditFragment.START_TIME_ACTION == action) {
                val oldStartTime = startTime!!.timeInMillis
                startTime!!.apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, 0)
                }
                val timeOffset = startTime!!.timeInMillis - oldStartTime
                endTime!!.timeInMillis = endTime!!.timeInMillis + timeOffset
            } else if (EditFragment.END_TIME_ACTION == action) {
                endTime!!.apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, second)
                    set(Calendar.MILLISECOND, 0)
                }
            }
            updateViews()
        }
    }
    /**
     * Adapter to display the detailed data
     */
    private lateinit var adapter: CursorAdapter

    /**
     * Gets the current values from the edit fields for updating the contraction
     *
     * @return ContentValues associated with the current (possibly edited) data
     */
    private val contentValues: ContentValues
        get() {
            val values = ContentValues()
            values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME, startTime!!.timeInMillis)
            values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, endTime!!.timeInMillis)
            values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, note)
            return values
        }

    /**
     * We need to find the exact edit_fragment view as there is a NoSaveStateFrameLayout view
     * inserted in between the parent and the view we created in onCreateView
     *
     * @return View created in onCreateView
     */
    private val fragmentView: View?
        get() {
            val rootView = view
            return rootView?.findViewById(R.id.edit_fragment)
        }

    /**
     * Reset the current data to empty/current dates
     */
    private fun clear() {
        startTime = Calendar.getInstance()
        endTime = Calendar.getInstance().apply {
            timeInMillis = startTime!!.timeInMillis
        }
        startTime!!.add(Calendar.MINUTE, -1)
        note = ""
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        adapter = object : CursorAdapter(activity, null, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                val startTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                startTime = Calendar.getInstance().apply {
                    timeInMillis = cursor.getLong(startTimeColumnIndex)
                }
                val endTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME)
                if (cursor.isNull(endTimeColumnIndex))
                    endTime = null
                else {
                    endTime = Calendar.getInstance().apply {
                        timeInMillis = cursor.getLong(endTimeColumnIndex)
                    }
                }
                val noteColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_NOTE)
                note = cursor.getString(noteColumnIndex)
            }

            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View? {
                // View is already inflated in onCreateView
                return null
            }
        }
        if (savedInstanceState != null) {
            startTime = savedInstanceState.getSerializable(
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME) as Calendar
            endTime = savedInstanceState.getSerializable(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME) as Calendar
            note = savedInstanceState.getString(ContractionContract.Contractions.COLUMN_NAME_NOTE)
            if (startTime == null) {
                // The user may have paused before the loader completed so re-query
                loaderManager.initLoader(0, null, this)
            } else {
                // No longer need the loader as we have valid local copies
                // (which may have changes) from now on
                loaderManager.destroyLoader(0)
                updateViews()
            }
        } else if (Intent.ACTION_EDIT == activity.intent.action)
            loaderManager.initLoader(0, null, this)
        else {
            clear()
            updateViews()
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(activity, activity.intent.data, null,
                null, null, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val allErrorCheckPassed = (passedEndTimeOverlapCheck && passedStartTimeOverlapCheck
                && passedTimeOverlapCheck && passedEndTimeOrderCheck)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "EndTimeOverlap Pass: $passedEndTimeOverlapCheck, " +
                    "StartTimeOverlap Pass: $passedStartTimeOverlapCheck, " +
                    "TimeOverlap Pass: $passedStartTimeOverlapCheck, " +
                    "EndTimeOrder Pass: $passedEndTimeOrderCheck. " +
                    "Allow save: $allErrorCheckPassed")
        menu.findItem(R.id.menu_save).isEnabled = allErrorCheckPassed
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val startTimeView = view.findViewById<TextView>(R.id.start_time)
        startTimeView.setOnClickListener {
            val timePicker = TimePickerDialogFragment()
            timePicker.arguments = Bundle().apply {
                putString(TimePickerDialogFragment.CALLBACK_ACTION, EditFragment.START_TIME_ACTION)
                putSerializable(TimePickerDialogFragment.TIME_ARGUMENT, startTime)
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Showing Start Time Dialog")
            timePicker.show(fragmentManager, "startTime")
        }
        val startDateView = view.findViewById<TextView>(R.id.start_date)
        startDateView.setOnClickListener {
            val datePicker = DatePickerDialogFragment()
            datePicker.arguments = Bundle().apply {
                putString(DatePickerDialogFragment.CALLBACK_ACTION, EditFragment.START_DATE_ACTION)
                putSerializable(DatePickerDialogFragment.DATE_ARGUMENT, startTime)
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Showing Start Date Dialog")
            datePicker.show(fragmentManager, "startDate")
        }
        val endTimeView = view.findViewById<TextView>(R.id.end_time)
        endTimeView.setOnClickListener {
            val timePicker = TimePickerDialogFragment()
            timePicker.arguments = Bundle().apply {
                putString(TimePickerDialogFragment.CALLBACK_ACTION, EditFragment.END_TIME_ACTION)
                putSerializable(TimePickerDialogFragment.TIME_ARGUMENT, endTime)
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Showing End Time Dialog")
            timePicker.show(fragmentManager, "endTime")
        }
        val endDateView = view.findViewById<TextView>(R.id.end_date)
        endDateView.setOnClickListener {
            val datePicker = DatePickerDialogFragment()
            datePicker.arguments = Bundle().apply {
                putString(DatePickerDialogFragment.CALLBACK_ACTION, EditFragment.END_DATE_ACTION)
                putSerializable(DatePickerDialogFragment.DATE_ARGUMENT, endTime)
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Showing End Date Dialog")
            datePicker.show(fragmentManager, "endDate")
        }
        val noteView = view.findViewById<EditText>(R.id.note)
        noteView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                note = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Nothing to do
            }
        })
    }

    override fun onLoaderReset(data: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        if (data.moveToFirst())
            adapter.bindView(fragmentView, activity, data)
        else
            clear()
        updateViews()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val values = contentValues
                if (Intent.ACTION_INSERT == activity.intent.action) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Add selected save")
                    FirebaseAnalytics.getInstance(context).logEvent("add_save", null)
                    val activity = activity
                    GlobalScope.launch {
                        context.contentResolver.insert(activity.intent.data!!, values)
                        AppWidgetUpdateHandler.createInstance().updateAllWidgets(activity)
                        NotificationUpdateService.updateNotification(activity)
                        activity.finish()
                    }
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Edit selected save")
                    FirebaseAnalytics.getInstance(context).logEvent("edit_save", null)
                    GlobalScope.launch {
                        context.contentResolver.update(activity.intent.data!!, values, null, null)
                        AppWidgetUpdateHandler.createInstance().updateAllWidgets(activity)
                        NotificationUpdateService.updateNotification(activity)
                        activity.finish()
                    }
                }
                return true
            }
            R.id.menu_cancel -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Edit selected cancel")
                activity.finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(ContractionContract.Contractions.COLUMN_NAME_START_TIME, startTime)
        outState.putSerializable(ContractionContract.Contractions.COLUMN_NAME_END_TIME, endTime)
        outState.putString(ContractionContract.Contractions.COLUMN_NAME_NOTE, note)
    }

    override fun onStart() {
        super.onStart()
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity)
        val timeFilter = IntentFilter().apply {
            addAction(EditFragment.START_TIME_ACTION)
            addAction(EditFragment.END_TIME_ACTION)
        }
        localBroadcastManager.registerReceiver(timeSetBroadcastReceiver, timeFilter)
        val dateFilter = IntentFilter().apply {
            addAction(EditFragment.START_DATE_ACTION)
            addAction(EditFragment.END_DATE_ACTION)
        }
        localBroadcastManager.registerReceiver(dateSetBroadcastReceiver, dateFilter)
    }

    override fun onStop() {
        super.onStop()
        val localBroadcastManager = LocalBroadcastManager.getInstance(activity)
        localBroadcastManager.unregisterReceiver(timeSetBroadcastReceiver)
        localBroadcastManager.unregisterReceiver(dateSetBroadcastReceiver)
    }

    /**
     * Updates the edit views based on the current internal data
     */
    internal fun updateViews() {
        passedEndTimeOverlapCheck = false
        passedStartTimeOverlapCheck = false
        passedTimeOverlapCheck = false
        passedEndTimeOrderCheck = false
        checkStartTimeOverlap()
        checkEndTimeOverlap()
        checkTimeOverlap()
        val view = fragmentView ?: return
        val startTimeView = view.findViewById<TextView>(R.id.start_time)
        val startDateView = view.findViewById<TextView>(R.id.start_date)
        val timeFormat = if (DateFormat.is24HourFormat(activity))
            "kk:mm:ss"
        else
            "hh:mm:ssa"
        startTimeView.text = DateFormat.format(timeFormat, startTime)
        startDateView.text = DateFormat.getDateFormat(activity).format(startTime!!.time)
        val endTimeView = view.findViewById<TextView>(R.id.end_time)
        val endDateView = view.findViewById<TextView>(R.id.end_date)
        val durationView = view.findViewById<TextView>(R.id.duration)
        val endTime = endTime
        if (endTime == null) {
            endTimeView.text = ""
            endDateView.text = ""
            durationView.text = getString(R.string.duration_ongoing)
        } else {
            endTimeView.text = DateFormat.format(timeFormat, endTime)
            endDateView.text = DateFormat.getDateFormat(activity).format(endTime.time)
            val endTimeErrorOrderView = view.findViewById<TextView>(R.id.end_time_error_order)
            passedEndTimeOrderCheck = startTime!!.before(endTime)
            if (passedEndTimeOrderCheck) {
                endTimeErrorOrderView.visibility = View.GONE
                val durationInSeconds = (endTime.timeInMillis - startTime!!.timeInMillis) / 1000
                durationView.text = DateUtils.formatElapsedTime(durationInSeconds)
            } else {
                endTimeErrorOrderView.visibility = View.VISIBLE
                durationView.text = ""
            }
            activity.supportInvalidateOptionsMenu()
        }
        val noteView = view.findViewById<EditText>(R.id.note)
        noteView.setText(note)
    }

    /**
     * Check to see if the current start time overlaps any other existing
     * contraction, displaying an error message if it does
     */
    private fun checkStartTimeOverlap() {
        GlobalScope.launch(Dispatchers.Main) {
            val startTime = startTime
            val overlapExists = startTime != null && withContext(Dispatchers.IO) {
                val projection = arrayOf(BaseColumns._ID)
                val selection = (BaseColumns._ID + "<>? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_START_TIME + "<=? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_END_TIME + ">=?")
                val currentStartTimeMillis = startTime.timeInMillis.toString()
                val contractionId = if (Intent.ACTION_INSERT == activity.intent.action)
                    0
                else
                    ContentUris.parseId(activity.intent.data)
                val selectionArgs = arrayOf(contractionId.toString(),
                        currentStartTimeMillis, currentStartTimeMillis)
                activity.contentResolver.query(
                        ContractionContract.Contractions.CONTENT_URI,
                        projection, selection, selectionArgs, null)?.closeable()?.use { data ->
                    data.moveToFirst()
                } ?: false
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Start time overlap: $overlapExists")
            val startTimeErrorOverlapView = fragmentView?.findViewById<TextView>(
                    R.id.start_time_error_overlap) ?: return@launch
            if (overlapExists)
                startTimeErrorOverlapView.visibility = View.VISIBLE
            else
                startTimeErrorOverlapView.visibility = View.GONE
            passedStartTimeOverlapCheck = !overlapExists
            activity.supportInvalidateOptionsMenu()
        }
    }

    /**
     * Check to see if the current end time overlaps any other existing
     * contraction, displaying an error message if it does
     */
    private fun checkEndTimeOverlap() {
        GlobalScope.launch(Dispatchers.Main) {
            val endTime = endTime
            val overlapExists = endTime != null && withContext(Dispatchers.IO) {
                val projection = arrayOf(BaseColumns._ID)
                val selection = (BaseColumns._ID + "<>? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_START_TIME + "<=? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_END_TIME + ">=?")
                val currentEndTimeMillis = java.lang.Long.toString(endTime.timeInMillis)
                val contractionId = if (Intent.ACTION_INSERT == activity.intent.action)
                    0
                else
                    ContentUris.parseId(activity.intent.data)
                val selectionArgs = arrayOf(contractionId.toString(),
                        currentEndTimeMillis, currentEndTimeMillis)
                activity.contentResolver.query(
                        ContractionContract.Contractions.CONTENT_URI,
                        projection, selection, selectionArgs, null)?.closeable()?.use { data ->
                    data.moveToFirst()
                } ?: false
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "End time overlap: $overlapExists")
            val endTimeErrorOverlapView = fragmentView?.findViewById<TextView>(
                    R.id.end_time_error_overlap) ?: return@launch
            if (overlapExists)
                endTimeErrorOverlapView.visibility = View.VISIBLE
            else
                endTimeErrorOverlapView.visibility = View.GONE
            passedEndTimeOverlapCheck = !overlapExists
            activity.supportInvalidateOptionsMenu()
        }
    }

    /**
     * Check to see if the current start/end time overlaps any other existing
     * contraction, displaying an error message if it does
     */
    private fun checkTimeOverlap() {
        GlobalScope.launch(Dispatchers.Main) {
            val startTime = startTime
            val endTime = endTime
            val overlapExists = startTime != null && endTime != null && withContext(Dispatchers.IO) {
                val projection = arrayOf(BaseColumns._ID)
                val selection = (BaseColumns._ID + "<>? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">=? AND "
                        + ContractionContract.Contractions.COLUMN_NAME_END_TIME + "<=?")
                val currentStartTimeMillis = startTime.timeInMillis.toString()
                val currentEndTimeMillis = endTime.timeInMillis.toString()
                val contractionId = if (Intent.ACTION_INSERT == activity.intent.action)
                    0
                else
                    ContentUris.parseId(activity.intent.data)
                val selectionArgs = arrayOf(contractionId.toString(),
                        currentStartTimeMillis, currentEndTimeMillis)
                activity.contentResolver.query(
                        ContractionContract.Contractions.CONTENT_URI,
                        projection, selection, selectionArgs, null)?.closeable()?.use { data ->
                    data.moveToFirst()
                } ?: false
            }
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Time overlap: $overlapExists")
            val timeErrorOverlapView = fragmentView?.findViewById<TextView>(
                    R.id.time_error_overlap) ?: return@launch
            if (overlapExists)
                timeErrorOverlapView.visibility = View.VISIBLE
            else
                timeErrorOverlapView.visibility = View.GONE
            passedTimeOverlapCheck = !overlapExists
            activity.supportInvalidateOptionsMenu()
        }
    }
}
