package com.ianhanniballake.contractiontimer.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.BaseColumns
import android.support.v4.app.LoaderManager
import android.support.v4.app.ShareCompat
import android.support.v4.content.CursorLoader
import android.support.v4.content.FileProvider
import android.support.v4.content.Loader
import android.support.v4.widget.CursorAdapter
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.data.CSVTransformer
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Main Activity for managing contractions
 */
class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        private const val TAG = "MainActivity"
        /**
         * Intent extra used to signify that this activity was launched from a widget
         */
        const val LAUNCHED_FROM_WIDGET_EXTRA = "com.ianhanniballake.contractiontimer.LaunchedFromWidget"
        /**
         * Intent extra used to signify that this activity was launched from the notification
         */
        const val LAUNCHED_FROM_NOTIFICATION_EXTRA = "com.ianhanniballake.contractiontimer.LaunchedFromNotification"
        /**
         * Intent extra used to signify that this activity was launched from the notification's
         * Add/Edit Note action
         */
        const val LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA = "com.ianhanniballake.contractiontimer.LaunchedFromNotificationActionNote"
    }

    /**
     * Adapter to store and manage the current cursor
     */
    private lateinit var adapter: CursorAdapter

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        // If there is no data associated with the Intent, sets the data to the
        // default URI, which accesses all contractions.
        if (intent.data == null)
            intent.data = ContractionContract.Contractions.CONTENT_URI
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.elevation = resources.getDimension(R.dimen.action_bar_elevation)
        if (savedInstanceState == null)
            showFragments()
        adapter = object : CursorAdapter(this, null, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Nothing to do
            }

            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View? {
                return null
            }
        }
        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection = arrayOf(BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                ContractionContract.Contractions.COLUMN_NAME_NOTE)
        val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val averagesTimeFrame = preferences.getString(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                getString(R.string.pref_average_time_frame_default))!!.toLong()
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        val selectionArgs = arrayOf(timeCutoff.toString())
        return CursorLoader(this, intent.data, projection, selection, selectionArgs, null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
        supportInvalidateOptionsMenu()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        supportInvalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val analytics = FirebaseAnalytics.getInstance(this)
        when (item.itemId) {
            R.id.menu_share -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Menu selected Share")
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "contraction")
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, Integer.toString(adapter.cursor.count))
                analytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
                shareContractions()
                return true
            }
            R.id.menu_add -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Menu selected Add")
                analytics.logEvent("add_open", null)
                val addIntent = Intent(Intent.ACTION_INSERT, intent.data)
                        .setComponent(ComponentName(this, EditActivity::class.java))
                startActivity(addIntent)
                return true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, Preferences::class.java))
                return true
            }
            R.id.menu_donate -> {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Menu selected Donate")
                analytics.logEvent("donate_open", null)
                DonateDialogFragment().show(supportFragmentManager, "donate")
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        val contractionCount = adapter.count
        val hasContractions = contractionCount > 0
        val share = menu.findItem(R.id.menu_share)
        share.isVisible = hasContractions
        return true
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isKeepScreenOn = preferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                resources.getBoolean(R.bool.pref_keep_screen_on_default))
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Keep Screen On: $isKeepScreenOn")
        if (isKeepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                resources.getBoolean(R.bool.pref_lock_portrait_default))
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: $isLockPortrait")
        requestedOrientation = if (isLockPortrait)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
        val averageTimeFrameChanged = preferences.getBoolean(
                Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, false)
        if (averageTimeFrameChanged) {
            preferences.edit().run {
                remove(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY)
                commit()
            }
            supportLoaderManager.restartLoader(0, null, this)
        }
    }

    override fun onStart() {
        super.onStart()
        val analytics = FirebaseAnalytics.getInstance(this)
        val intent = intent
        if (intent.hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)) {
            val widgetIdentifier = intent.getStringExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Launched from $widgetIdentifier")
            analytics.logEvent("${widgetIdentifier}_launch", null)
            intent.removeExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)
        }
        if (intent.hasExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_EXTRA)) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Launched from Notification")
            analytics.logEvent("notification_launch", null)
            intent.removeExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_EXTRA)
        }
        if (intent.hasExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA)) {
            val id = intent.getLongExtra(BaseColumns._ID, -1L)
            val existingNote = intent.getStringExtra(ContractionContract.Contractions.COLUMN_NAME_NOTE)
            val type = if (existingNote.isNullOrBlank()) "Add Note" else "Edit Note"
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Launched from Notification $type action")
            val noteDialogFragment = NoteDialogFragment()
            noteDialogFragment.arguments = Bundle().apply {
                putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, id)
                putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, existingNote)
            }
            analytics.logEvent(if (existingNote.isNullOrBlank()) "note_add_launch" else "note_edit_launch", null)
            noteDialogFragment.show(supportFragmentManager, "note")
            intent.removeExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA)
        }
        NotificationUpdateReceiver.updateNotification(this)
    }

    /**
     * Builds the averages data to share and opens the Intent chooser
     */
    private fun shareContractions() {
        val data = adapter.cursor
        if (data.count == 0)
            return
        val averageDurationView = findViewById(R.id.average_duration) as TextView
        val averageFrequencyView = findViewById(R.id.average_frequency) as TextView
        data.moveToLast()
        val startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_START_TIME)
        val lastStartTime = data.getLong(startTimeColumnIndex)
        val count = adapter.count
        val relativeTimeSpan = DateUtils.getRelativeTimeSpanString(lastStartTime)
        val formattedData = resources.getQuantityString(
                R.plurals.share_average,
                count,
                relativeTimeSpan,
                count,
                averageDurationView.text,
                averageFrequencyView.text)
        val context = this
        GlobalScope.launch(Dispatchers.Main) {
            val uri = withContext(Dispatchers.IO) {
                val exportPath = File(cacheDir, "export")
                if (!exportPath.mkdirs() && !exportPath.isDirectory) {
                    Log.e(TAG, "Error creating export directory")
                    return@withContext null
                }
                val file = File(exportPath, getString(R.string.backup_default_filename) + ".csv")
                try {
                    FileOutputStream(file).use {
                        CSVTransformer.writeContractions(context, it)
                        FileProvider.getUriForFile(context, BuildConfig.FILES_AUTHORITY, file)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error writing contractions to file", e)
                    null
                }
            }
            if (uri != null) {
                ShareCompat.IntentBuilder.from(context)
                        .setSubject(getString(R.string.share_subject))
                        .setType("text/plain")
                        .setText(formattedData)
                        .addStream(uri)
                        .setChooserTitle(R.string.share_pick_application)
                        .startChooser()
            }
        }
    }

    /**
     * Creates and shows the fragments for the MainActivity
     */
    private fun showFragments() {
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        val ft = supportFragmentManager.beginTransaction()
        ft.add(ResetMenuControllerFragment(), "reset_menu")
        ft.commit()
    }
}
