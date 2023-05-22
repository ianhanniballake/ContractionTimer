package com.ianhanniballake.contractiontimer.ui

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.ViewAnimator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.ActionMenuView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Fragment to list contractions entered by the user
 */
class ContractionListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "ContractionListFragment"
        /**
         * Key used to store the selected item note in the bundle
         */
        private const val SELECTED_ITEM_NOTE_KEY = "com.ianhanniballake.contractiontimer.SELECTED_ITEM_NOTE_KEY"
    }

    /**
     * Handler of live duration updates
     */
    internal val liveDurationHandler = Handler(Looper.getMainLooper())

    /**
     * Handler of time since last contraction updates
     */
    internal val timeSinceLastHandler = Handler(Looper.getMainLooper())

    /**
     * Note associated with the currently selected item
     */
    internal var selectedItemNote: String? = null
    /**
     * Current ActionMode, if any
     */
    internal var actionMode: ActionMode? = null
    /**
     * Start time of the current contraction
     */
    internal var currentContractionStartTime: Long = 0
    /**
     * Reference to the Runnable live duration updater
     */
    internal val liveDurationUpdate: Runnable = object : Runnable {
        /**
         * Updates the appropriate duration view to the current elapsed time and schedules this to rerun in 1 second
         */
        override fun run() {
            val rootView = view
            if (rootView != null) {
                val currentContractionDurationView = rootView.findViewWithTag<TextView>("durationView")
                if (currentContractionDurationView != null) {
                    val durationInSeconds = (System.currentTimeMillis() - currentContractionStartTime) / 1000
                    currentContractionDurationView.text = DateUtils.formatElapsedTime(durationInSeconds)
                }
            }
            liveDurationHandler.postDelayed(this, 1000)
        }
    }
    internal lateinit var listView: ListView
    private lateinit var emptyView: ViewAnimator
    /**
     * View for the header row
     */
    internal lateinit var headerView: View
    /**
     * Reference to the Runnable time since last contraction updater
     */
    private val timeSinceLastUpdate = object : Runnable {
        /**
         * Updates the time since last contraction and schedules this to rerun in 1 second
         */
        override fun run() {
            val timeSinceLastView = headerView.findViewById<TextView>(
                    R.id.list_header_time_since_last)
            if (timeSinceLastView != null && currentContractionStartTime != 0L) {
                val timeSinceLastInSeconds = (System.currentTimeMillis() - currentContractionStartTime) / 1000
                timeSinceLastView.text = DateUtils.formatElapsedTime(timeSinceLastInSeconds)
            }
            timeSinceLastHandler.postDelayed(this, 1000)
        }
    }
    /**
     * Column headers view
     */
    private lateinit var columnHeaders: ViewGroup
    /**
     * Adapter to display the list's data
     */
    private lateinit var adapter: CursorAdapter

    /**
     * Deletes a given contraction
     *
     * @param id contraction id to delete
     */
    private fun deleteContraction(id: Long) {
        // Ensure we don't attempt to delete contractions with invalid ids
        if (id < 0)
            return
        val deleteUri = ContentUris.withAppendedId(
            ContractionContract.Contractions.CONTENT_ID_URI_BASE, id
        )
        val context = requireContext()
        GlobalScope.launch {
            context.contentResolver.delete(deleteUri, null, null)
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
            NotificationUpdateReceiver.updateNotification(context)
        }
    }

    private fun itemSelected(listView: ListView, position: Int) {
        val checkedItems = listView.checkedItemIds
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Item clicked: " + checkedItems.size)
        if (checkedItems.isEmpty()) {
            actionMode?.finish()
            return
        } else if (checkedItems.size == 1) {
            val checked = listView.checkedItemPositions
            var selectedPosition = checked.keyAt(0)
            // The checked item positions sometime contain both the old and new items. We need to make sure we
            // pick the remaining selected item, rather than the recently de-selected item.
            if (selectedPosition == position && listView.isItemChecked(position)) {
                selectedPosition = checked.keyAt(1)
            }
            val adapter = listView.adapter
            if (adapter.isEmpty)
            // onLoaderReset swapped in a null cursor
                return
            val cursor = adapter.getItem(selectedPosition) as Cursor
            val noteColumnIndex = cursor
                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE)
            selectedItemNote = cursor.getString(noteColumnIndex)
        }
        actionMode?.invalidate()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SELECTED_ITEM_NOTE_KEY, selectedItemNote)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null)
            selectedItemNote = savedInstanceState.getString(SELECTED_ITEM_NOTE_KEY)
        headerView = layoutInflater.inflate(R.layout.list_header, listView, false)
        val headerFrame = FrameLayout(requireContext())
        headerFrame.addView(headerView)
        listView.addHeaderView(headerFrame, null, false)
        adapter = ContractionListCursorAdapter(requireContext())
        listView.adapter = adapter
        listView.isDrawSelectorOnTop = true
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
            if (actionMode == null) {
                viewContraction(id)
            } else {
                itemSelected(listView, position)
            }
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            if (actionMode != null) {
                listView.setItemChecked(position, !listView.isItemChecked(position))
                itemSelected(listView, position)
                return@OnItemLongClickListener true
            }
            listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
            listView.setItemChecked(position, true)
            val appCompatActivity = activity as AppCompatActivity
            appCompatActivity.startSupportActionMode(object : ActionMode.Callback {
                override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                    val analytics = FirebaseAnalytics.getInstance(requireContext())
                    val selectedIds = listView.checkedItemIds
                    if (selectedIds.isEmpty()) {
                        return false
                    }
                    val contractionId = selectedIds[0]
                    when (menuItem.itemId) {
                        R.id.menu_context_view -> {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Context Action Mode selected view")
                            analytics.logEvent("view_cab", null)
                            viewContraction(contractionId)
                            return true
                        }
                        R.id.menu_context_note -> {
                            val checkedPosition = listView.checkedItemPositions.keyAt(0)
                            val cursor = listView.adapter.getItem(checkedPosition) as Cursor
                            val noteColumnIndex = cursor
                                    .getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_NOTE)
                            val existingNote = cursor.getString(noteColumnIndex)
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Context Action Mode selected " + if (existingNote.isNullOrBlank())
                                    "Add Note"
                                else
                                    "Edit Note")
                            val noteEvent = if (existingNote.isNullOrBlank()) "note_add_cab" else "note_edit_cab"
                            analytics.logEvent(noteEvent, null)
                            showNoteDialog(contractionId, existingNote)
                            actionMode.finish()
                            return true
                        }
                        R.id.menu_context_delete -> {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Context Action Mode selected delete")
                            val bundle = Bundle()
                            bundle.putString(
                                FirebaseAnalytics.Param.VALUE,
                                selectedIds.size.toString()
                            )
                            analytics.logEvent("delete_cab", bundle)
                            for (selectedId in selectedIds)
                                deleteContraction(selectedId)
                            actionMode.finish()
                            return true
                        }
                        else -> return false
                    }
                }

                override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                    this@ContractionListFragment.actionMode = actionMode
                    val inflater = actionMode.menuInflater
                    inflater.inflate(R.menu.list_context, menu)
                    return true
                }

                override fun onDestroyActionMode(actionMode: ActionMode) {
                    val selectedItems = listView.checkedItemPositions
                    if (selectedItems != null) {
                        for (i in 0 until selectedItems.size()) {
                            listView.setItemChecked(selectedItems.keyAt(i), false)
                        }
                    }
                    listView.post { listView.choiceMode = ListView.CHOICE_MODE_NONE }
                    this@ContractionListFragment.actionMode = null
                }

                override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                    val selectedItemsSize = listView.checkedItemIds.size
                    // Show or hide the view menu item
                    val viewItem = menu.findItem(R.id.menu_context_view)
                    val showViewItem = selectedItemsSize == 1
                    viewItem.isVisible = showViewItem
                    // Set whether to display the note menu item
                    val noteItem = menu.findItem(R.id.menu_context_note)
                    val showNoteItem = selectedItemsSize == 1
                    // Set the title of the note menu item
                    if (showNoteItem)
                        if (selectedItemNote.isNullOrBlank())
                            noteItem.setTitle(R.string.note_dialog_title_add)
                        else
                            noteItem.setTitle(R.string.note_dialog_title_edit)
                    noteItem.isVisible = showNoteItem
                    // Set the title of the delete menu item
                    val deleteItem = menu.findItem(R.id.menu_context_delete)
                    val currentTitle = deleteItem.title
                    val newTitle = resources.getQuantityText(R.plurals.menu_context_delete,
                            selectedItemsSize)
                    deleteItem.title = newTitle
                    // Set the Contextual Action Bar title with the new item size
                    val modeTitle = actionMode.title
                    val newModeTitle = String.format(getString(R.string.menu_context_action_mode_title),
                            selectedItemsSize)
                    actionMode.title = newModeTitle
                    return newModeTitle != modeTitle || newTitle != currentTitle
                }
            })
            true
        }
        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireContext(), requireActivity().intent!!.data!!, null,
            null, null, null
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_contraction_list, container, false)
        listView = view.findViewById(android.R.id.list)
        emptyView = view.findViewById(android.R.id.empty)
        listView.emptyView = emptyView
        columnHeaders = view.findViewById(R.id.list_column_headers)
        return view
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate)
        columnHeaders.visibility = View.GONE
        adapter.swapCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        liveDurationHandler.removeCallbacks(liveDurationUpdate)
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate)
        adapter.swapCursor(data)
        if (data == null || data.count == 0) {
            columnHeaders.visibility = View.GONE
            emptyView.displayedChild = 1
        } else {
            columnHeaders.visibility = View.VISIBLE
            listView.setSelection(0)
            data.moveToFirst()
            val endTimeColumnIndex = data.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            val isContractionOngoing = data.isNull(endTimeColumnIndex)
            if (isContractionOngoing)
                headerView.visibility = View.GONE
            else {
                val startTimeColumnIndex = data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                currentContractionStartTime = data.getLong(startTimeColumnIndex)
                timeSinceLastHandler.post(timeSinceLastUpdate)
                headerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        liveDurationHandler.removeCallbacks(liveDurationUpdate)
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate)
    }

    override fun onResume() {
        super.onResume()
        val currentContractionDurationView = requireView().findViewWithTag<TextView>("durationView")
        if (currentContractionDurationView != null) {
            // Ensures the live duration update is running
            liveDurationHandler.removeCallbacks(liveDurationUpdate)
            liveDurationHandler.post(liveDurationUpdate)
        }
        // Ensures the live time since last update is running
        timeSinceLastHandler.removeCallbacks(timeSinceLastUpdate)
        timeSinceLastHandler.post(timeSinceLastUpdate)
    }

    /**
     * Shows the 'note' dialog
     *
     * @param id           contraction id
     * @param existingNote existing note attached to this contraction if it exists
     */
    private fun showNoteDialog(id: Long, existingNote: String) {
        // Ensure we don't attempt to change the note on contractions with
        // invalid ids
        if (id < 0)
            return
        val noteDialogFragment = NoteDialogFragment()
        noteDialogFragment.arguments = Bundle().apply {
            putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, id)
            putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, existingNote)
        }
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Showing Dialog")
        noteDialogFragment.show(parentFragmentManager, "note")
    }

    /**
     * View the details of the given contraction
     *
     * @param id contraction id
     */
    private fun viewContraction(id: Long) {
        // Ensure we don't attempt to view contractions with invalid ids
        if (id < 0)
            return
        if (isDetached)
        // Can't startActivity if we are detached
            return
        val contractionUri = ContentUris.withAppendedId(
                ContractionContract.Contractions.CONTENT_ID_URI_BASE, id)
        val intent = Intent(Intent.ACTION_VIEW, contractionUri)
            .setComponent(ComponentName(requireContext(), ViewActivity::class.java))
        startActivity(intent)
    }

    /**
     * Cursor Adapter for creating and binding contraction list view items
     */
    private inner class ContractionListCursorAdapter(
            context: Context
    ) : CursorAdapter(context, null, 0) {
        /**
         * Local reference to the layout inflater service
         */
        private val inflater = LayoutInflater.from(context)

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val timeFormat = if (DateFormat.is24HourFormat(context))
                "kk:mm:ss"
            else
                "hh:mm:ssa"
            val dateFormat = try {
                val dateFormatOrder = DateFormat.getDateFormatOrder(context)
                val dateFormatArray = charArrayOf(dateFormatOrder[0], dateFormatOrder[0], '/', dateFormatOrder[1], dateFormatOrder[1])
                String(dateFormatArray)
            } catch (e: IllegalArgumentException) {
                "MM/dd"
            }

            val startTimeColumnIndex = cursor.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME)
            val startTime = cursor.getLong(startTimeColumnIndex)
            val startCal = Calendar.getInstance().apply {
                timeInMillis = startTime
            }
            var showDateOnStartTime = false
            val startTimeView = view.findViewById<TextView>(R.id.start_time)
            val endTimeColumnIndex = cursor.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            val isContractionOngoing = cursor.isNull(endTimeColumnIndex)
            val endTimeView = view.findViewById<TextView>(R.id.end_time)
            val durationView = view.findViewById<TextView>(R.id.duration)
            val endCal = Calendar.getInstance()
            var showDateOnEndTime = false
            if (isContractionOngoing) {
                durationView.text = ""
                currentContractionStartTime = startTime
                durationView.tag = "durationView"
                liveDurationHandler.removeCallbacks(liveDurationUpdate)
                liveDurationHandler.post(liveDurationUpdate)
            } else {
                val endTime = cursor.getLong(endTimeColumnIndex)
                endCal.timeInMillis = endTime
                durationView.tag = ""
                val durationInSeconds = (endTime - startTime) / 1000
                durationView.text = DateUtils.formatElapsedTime(durationInSeconds)
            }
            if (startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) || startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR))
                showDateOnEndTime = true
            val frequencyView = view.findViewById<TextView>(R.id.frequency)
            // If we aren't the last entry, move to the next (previous in time)
            // contraction to get its start time to compute the frequency
            if (!cursor.isLast && cursor.moveToNext()) {
                val prevContractionStartTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                val prevContractionStartTime = cursor.getLong(prevContractionStartTimeColumnIndex)
                val frequencyInSeconds = (startTime - prevContractionStartTime) / 1000
                frequencyView.text = DateUtils.formatElapsedTime(frequencyInSeconds)
                // Check to see if the date changed between Contractions
                val prevContractionEndTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME)
                val prevContractionEndTime = cursor.getLong(prevContractionEndTimeColumnIndex)
                val prevEndCal = Calendar.getInstance()
                prevEndCal.timeInMillis = prevContractionEndTime
                if (startCal.get(Calendar.YEAR) != prevEndCal.get(Calendar.YEAR) || startCal.get(Calendar.DAY_OF_YEAR) != prevEndCal.get(Calendar.DAY_OF_YEAR))
                    showDateOnStartTime = true
                // Go back to the previous spot
                cursor.moveToPrevious()
            } else {
                frequencyView.text = ""
                // Always show the date on the very first start time
                showDateOnStartTime = true
            }
            startTimeView.text = DateFormat.format(timeFormat, startCal).toString() + if (showDateOnStartTime) " " + DateFormat.format(dateFormat, startCal) else ""
            if (isContractionOngoing)
                endTimeView.text = " "
            else
                endTimeView.text = DateFormat.format(timeFormat, endCal).toString() + if (showDateOnEndTime) " " + DateFormat.format(dateFormat, endCal) else ""
            val noteColumnIndex = cursor.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_NOTE)
            val note = cursor.getString(noteColumnIndex)
            val noteView = view.findViewById<TextView>(R.id.note)
            noteView.text = note
            if (note.isNullOrBlank())
                noteView.visibility = View.GONE
            else
                noteView.visibility = View.VISIBLE
            val idColumnIndex = cursor.getColumnIndex(BaseColumns._ID)
            val id = cursor.getLong(idColumnIndex)
            val showPopupView = view.findViewById<ActionMenuView>(R.id.show_popup)
            val noteItem = showPopupView.menu.findItem(R.id.menu_context_note)
            if (note.isNullOrBlank())
                noteItem.setTitle(R.string.note_dialog_title_add)
            else
                noteItem.setTitle(R.string.note_dialog_title_edit)
            val deleteItem = showPopupView.menu.findItem(R.id.menu_context_delete)
            deleteItem.title = resources.getQuantityText(R.plurals.menu_context_delete, 1)
            showPopupView.setOnMenuItemClickListener { item ->
                val analytics = FirebaseAnalytics.getInstance(requireContext())
                when (item.itemId) {
                    R.id.menu_context_view -> {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected view")
                        analytics.logEvent("view_popup", null)
                        viewContraction(id)
                        true
                    }
                    R.id.menu_context_note -> {
                        val type = if (note.isNullOrBlank()) "Add Note" else "Edit Note"
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected $type")
                        val noteEvent = if (note.isNullOrBlank()) "note_add_popup" else "note_edit_popup"
                        analytics.logEvent(noteEvent, null)
                        showNoteDialog(id, note)
                        true
                    }
                    R.id.menu_context_delete -> {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected delete")
                        val bundle = Bundle()
                        bundle.putString(FirebaseAnalytics.Param.VALUE, 1.toString())
                        analytics.logEvent("delete_popup", bundle)
                        deleteContraction(id)
                        true
                    }
                    else -> false
                }
            }
            // Don't allow popup menu while the Contextual Action Bar is present
            showPopupView.isEnabled = actionMode == null
        }

        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            val view = inflater.inflate(R.layout.list_item_contraction, parent, false)
            val showPopup = view.findViewById<ActionMenuView>(R.id.show_popup)
            val menuInflater = requireActivity().menuInflater
            menuInflater.inflate(R.menu.list_popup, showPopup.menu)
            return view
        }
    }
}
