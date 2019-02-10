package com.ianhanniballake.contractiontimer.notification

import android.app.Activity
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.app.RemoteInput
import android.support.v4.app.TaskStackBuilder
import android.util.Log
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.extensions.closeable
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Google Now's 'Note to self' only calls startActivity so we redirect to our service to run the update in the
 * background
 */
class NoteTranslucentActivity : Activity() {
    companion object {
        private const val TAG = "NoteTranslucentActivity"
        /**
         * Action Google Now uses for 'Note to self' voice input
         */
        private const val GOOGLE_NOW_INPUT = "com.google.android.gm.action.AUTO_SEND"

        /**
         * Ensures that the NoteTransparentActivity is only enabled when there is at least one contraction
         *
         * @param context Context to be used to query the ContentProvider and enable/disable this activity
         */
        fun checkServiceState(context: Context) {
            GlobalScope.launch {
                val hasContractions = context.contentResolver.query(
                        ContractionContract.Contractions.CONTENT_URI, null,
                        null, null,  null)?.closeable()?.use { data ->
                    data.moveToFirst()
                } ?: false
                if (BuildConfig.DEBUG)
                    Log.d(TAG, (if (hasContractions) "Has" else "No") + " contractions")
                val state = if (hasContractions)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                val packageManager = context.packageManager
                val componentName = ComponentName(context, NoteTranslucentActivity::class.java)
                packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val text = if (remoteInput != null)
            remoteInput.getCharSequence(Intent.EXTRA_TEXT)?.toString()
        else
            intent.getStringExtra(Intent.EXTRA_TEXT)
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received text: $text")
        val context = this
        GlobalScope.launch {
            val projection = arrayOf(BaseColumns._ID,
                    ContractionContract.Contractions.COLUMN_NAME_NOTE)
            contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection,
                    null, null, null)?.closeable()?.use { data ->
                if (!data.moveToFirst()) {
                    // This shouldn't happen as checkServiceState ensures at least one contraction exists
                    Log.w(TAG, "Could not find contraction")
                    return@use
                }
                val id = data.getLong(data.getColumnIndex(BaseColumns._ID))
                val note = data.getString(data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_NOTE))

                if (text.isNullOrBlank()) {
                    val taskStackBuilder = TaskStackBuilder.create(context)
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA, true)
                        putExtra(BaseColumns._ID, id)
                        putExtra(ContractionContract.Contractions.COLUMN_NAME_NOTE, note)
                    }
                    taskStackBuilder.addNextIntent(mainIntent)
                    taskStackBuilder.startActivities()
                    return@use
                }
                val values = ContentValues().apply {
                    put(ContractionContract.Contractions.COLUMN_NAME_NOTE, text)
                }
                val contractionUri = ContentUris.withAppendedId(
                        ContractionContract.Contractions.CONTENT_ID_URI_BASE, id)
                val count = contentResolver.update(contractionUri, values, null, null)
                if (count == 1) {
                    Toast.makeText(context, R.string.saving_note, Toast.LENGTH_SHORT).show()
                    val voiceInputSource = if (intent.action == GOOGLE_NOW_INPUT)
                        "google_now"
                    else
                        "remote_input"
                    val noteEvent = if (note.isNullOrBlank()) "note_add" else "note_edit_$voiceInputSource"
                    FirebaseAnalytics.getInstance(context).logEvent(noteEvent, null)
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                    NotificationUpdateService.updateNotification(context)
                } else {
                    Log.e(TAG, "Error updating contraction's note")
                }
            }
            finish()
        }
    }
}
