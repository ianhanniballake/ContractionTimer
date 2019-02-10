package com.ianhanniballake.contractiontimer.ui

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract

/**
 * Headless fragment which controls the Reset action in the MainActivity ActionBar,
 * enabling/disabling it based on whether there are contractions to reset
 */
class ResetMenuControllerFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "ResetMenuController"
    }
    /**
     * Cursor Adapter which holds the current contractions
     */
    private lateinit var adapter: CursorAdapter

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
        setHasOptionsMenu(true)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection = arrayOf(BaseColumns._ID)
        return CursorLoader(activity, ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
        activity.supportInvalidateOptionsMenu()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        activity.supportInvalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_reset_menu_controller, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val contractionCount = adapter.count
        val hasContractions = contractionCount > 0
        val reset = menu.findItem(R.id.menu_reset)
        reset.isEnabled = hasContractions
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Menu selected Reset")
                val resetDialogFragment = ResetDialogFragment()
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Showing Dialog")
                FirebaseAnalytics.getInstance(context).logEvent("reset_open", null)
                resetDialogFragment.show(fragmentManager, "reset")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
