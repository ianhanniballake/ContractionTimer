package com.ianhanniballake.contractiontimer.ui

import android.content.AsyncQueryHandler
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import java.util.Date

/**
 * Fragment showing the details of an individual contraction
 */
class ViewFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "ViewFragment"

        /**
         * Creates a new Fragment to display the given contraction
         *
         * @param contractionId Id of the Contraction to display
         * @return ViewFragment associated with the given id
         */
        fun createInstance(contractionId: Long): ViewFragment {
            val viewFragment = ViewFragment()
            val args = Bundle()
            args.putLong(BaseColumns._ID, contractionId)
            viewFragment.arguments = args
            return viewFragment
        }
    }

    /**
     * Whether the current contraction is ongoing (i.e., not yet ended).
     * Null indicates that we haven't checked yet,
     * while true or false indicates whether the contraction is ongoing
     */
    internal var isContractionOngoing: Boolean? = null
    /**
     * Adapter to display the detailed data
     */
    private lateinit var adapter: CursorAdapter
    /**
     * Id of the current contraction to show
     */
    private var contractionId: Long = -1
    /**
     * Handler for asynchronous deletes of contractions
     */
    private lateinit var contractionQueryHandler: AsyncQueryHandler

    /**
     * We need to find the exact view_fragment view as there is a NoSaveStateFrameLayout view inserted in between the
     * parent and the view we created in onCreateView
     *
     * @return View created in onCreateView
     */
    private val fragmentView: View?
        get() {
            val rootView = view
            return rootView?.findViewById(R.id.view_fragment)
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        val applicationContext = activity.applicationContext
        contractionQueryHandler = object : AsyncQueryHandler(activity.contentResolver) {
            override fun onDeleteComplete(token: Int, cookie: Any, result: Int) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(applicationContext)
                NotificationUpdateService.updateNotification(applicationContext)
                activity?.finish()
            }
        }
        adapter = object : CursorAdapter(activity, null, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                val startTimeView = view.findViewById<TextView>(R.id.start_time)
                val timeFormat = if (DateFormat.is24HourFormat(context))
                    "kk:mm:ss"
                else
                    "hh:mm:ssa"
                val startTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                val startTime = cursor.getLong(startTimeColumnIndex)
                startTimeView.text = DateFormat.format(timeFormat, startTime)
                val startDateView = view.findViewById<TextView>(R.id.start_date)
                val startDate = Date(startTime)
                startDateView.text = DateFormat.getDateFormat(activity).format(startDate)
                val endTimeView = view.findViewById<TextView>(R.id.end_time)
                val endDateView = view.findViewById<TextView>(R.id.end_date)
                val durationView = view.findViewById<TextView>(R.id.duration)
                val endTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME)
                isContractionOngoing = cursor.isNull(endTimeColumnIndex)
                if (isContractionOngoing == true) {
                    endTimeView.text = " "
                    endDateView.text = " "
                    durationView.text = getString(R.string.duration_ongoing)
                } else {
                    val endTime = cursor.getLong(endTimeColumnIndex)
                    endTimeView.text = DateFormat.format(timeFormat, endTime)
                    val endDate = Date(endTime)
                    endDateView.text = DateFormat.getDateFormat(activity).format(endDate)
                    val durationInSeconds = (endTime - startTime) / 1000
                    durationView.text = DateUtils.formatElapsedTime(durationInSeconds)
                }
                activity.supportInvalidateOptionsMenu()
                val noteView = view.findViewById<TextView>(R.id.note)
                val noteColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_NOTE)
                val note = cursor.getString(noteColumnIndex)
                noteView.text = note
            }

            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View? {
                // View is already inflated in onCreateView
                return null
            }
        }
        if (arguments != null) {
            contractionId = arguments.getLong(BaseColumns._ID, 0)
            if (contractionId != -1L)
                loaderManager.initLoader(0, null, this)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val contractionUri = ContentUris.withAppendedId(
                ContractionContract.Contractions.CONTENT_ID_URI_PATTERN,
                contractionId)
        return CursorLoader(activity, contractionUri, null,
                null, null, null)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Only allow editing contractions that have already finished
        val showEdit = isContractionOngoing != true
        val editItem = menu.findItem(R.id.menu_edit)
        editItem.isVisible = showEdit
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_view, container, false)

    override fun onLoaderReset(data: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        if (data.moveToFirst())
            adapter.bindView(fragmentView, activity, data)
        activity.supportInvalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val uri = ContentUris.withAppendedId(
                ContractionContract.Contractions.CONTENT_ID_URI_BASE, contractionId)
        val analytics = FirebaseAnalytics.getInstance(context)
        when (item.itemId) {
            R.id.menu_edit -> {
                // isContractionOngoing should be non-null at this point, but
                // just in case
                if (isContractionOngoing == null)
                    return true
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "View selected edit")
                if (isContractionOngoing == true)
                    Toast.makeText(activity, R.string.edit_ongoing_error, Toast.LENGTH_SHORT).show()
                else {
                    analytics.logEvent("edit_open_view", null)
                    startActivity(Intent(Intent.ACTION_EDIT, uri)
                            .setComponent(ComponentName(activity, EditActivity::class.java)))
                }
                return true
            }
            R.id.menu_delete -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "View selected delete")
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.VALUE, Integer.toString(1))
                analytics.logEvent("delete_view", bundle)
                contractionQueryHandler.startDelete(0, 0, uri, null, null)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
