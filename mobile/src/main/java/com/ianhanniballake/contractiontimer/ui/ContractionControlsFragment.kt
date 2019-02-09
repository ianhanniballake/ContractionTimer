package com.ianhanniballake.contractiontimer.ui

import android.content.AsyncQueryHandler
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract

/**
 * Fragment which controls starting and stopping the contraction timer
 */
class ContractionControlsFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "ContractionControls"
    }

    /**
     * Cursor Adapter which holds the latest contraction
     */
    private lateinit var adapter: CursorAdapter
    /**
     * Handler for asynchronous inserts/updates of contractions
     */
    private lateinit var contractionQueryHandler: AsyncQueryHandler
    private var contractionOngoing = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = object : CursorAdapter(activity, null, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Nothing to do
            }

            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View? {
                return null
            }
        }
        loaderManager.initLoader(0, null, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = activity
        contractionQueryHandler = object : AsyncQueryHandler(context.contentResolver) {
            override fun onInsertComplete(token: Int, cookie: Any, uri: Uri) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                NotificationUpdateService.updateNotification(context)
            }

            override fun onUpdateComplete(token: Int, cookie: Any, result: Int) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                NotificationUpdateService.updateNotification(context)
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection = arrayOf(BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME)
        return CursorLoader(activity, activity.intent.data, projection,
                null, null, null)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_contraction_controls, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnClickListener {
            // Disable the button to ensure we give the database a chance to
            // complete the insert/update
            view.isEnabled = false
            val analytics = FirebaseAnalytics.getInstance(context)
            if (!contractionOngoing) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Starting contraction")
                analytics.logEvent("control_start", null)
                // Start a new contraction
                contractionQueryHandler.startInsert(0, null, ContractionContract.Contractions.CONTENT_URI,
                        ContentValues())
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Stopping contraction")
                analytics.logEvent("control_stop", null)
                val newEndTime = ContentValues()
                newEndTime.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, System.currentTimeMillis())
                val latestContractionId = adapter.getItemId(0)
                val updateUri = ContentUris.withAppendedId(
                        ContractionContract.Contractions.CONTENT_ID_URI_BASE, latestContractionId)
                // Add the new end time to the last contraction
                contractionQueryHandler.startUpdate(0, 0, updateUri, newEndTime, null, null)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        adapter.swapCursor(data)
        val view = view as FloatingActionButton? ?: return
        view.isEnabled = true
        contractionOngoing = (data != null && data.moveToFirst()
                && data.isNull(data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME)))
        if (contractionOngoing) {
            view.setImageResource(R.drawable.ic_notif_action_stop)
        } else {
            view.setImageResource(R.drawable.ic_notif_action_start)
        }
    }
}
