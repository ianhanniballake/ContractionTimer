package com.ianhanniballake.contractiontimer.notification

import android.app.IntentService
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.provider.BaseColumns
import android.support.v4.app.TaskStackBuilder
import android.text.TextUtils
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.ianhanniballake.contractiontimer.ui.MainActivity

/**
 * Service which can automatically add/replace the note on the current contraction from a voice
 * input source or start the appropriate UI if no voice input is given
 */
class NoteIntentService : IntentService(TAG) {
    companion object {
        private const val TAG = "NoteIntentService"
        /**
         * Action Google Now uses for 'Note to self' voice input
         */
        private const val GOOGLE_NOW_INPUT = "com.google.android.gm.action.AUTO_SEND"
    }

    override fun onHandleIntent(intent: Intent?) {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: return
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received text: $text")
        val projection = arrayOf(BaseColumns._ID,
                ContractionContract.Contractions.COLUMN_NAME_NOTE)
        contentResolver.query(ContractionContract.Contractions.CONTENT_URI, projection,
                null, null, null)?.use { data ->
            if (!data.moveToFirst()) {
                // This shouldn't happen as checkServiceState ensures at least one contraction exists
                Log.w(TAG, "Could not find contraction")
                return
            }
            val id = data.getLong(data.getColumnIndex(BaseColumns._ID))
            val note = data.getString(data.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_NOTE))

            if (text.isBlank()) {
                val taskStackBuilder = TaskStackBuilder.create(this)
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.LAUNCHED_FROM_NOTIFICATION_ACTION_NOTE_EXTRA, true)
                    putExtra(BaseColumns._ID, id)
                    putExtra(ContractionContract.Contractions.COLUMN_NAME_NOTE, note)
                }
                taskStackBuilder.addNextIntent(mainIntent)
                taskStackBuilder.startActivities()
                return
            }
            val values = ContentValues().apply {
                put(ContractionContract.Contractions.COLUMN_NAME_NOTE, text)
            }
            val contractionUri = ContentUris.withAppendedId(
                    ContractionContract.Contractions.CONTENT_ID_URI_BASE, id)
            val count = contentResolver.update(contractionUri, values, null, null)
            if (count == 1) {
                val voiceInputSource = if (TextUtils.equals(intent.action, GOOGLE_NOW_INPUT))
                    "google_now"
                else
                    "remote_input"
                val noteEvent = if (note.isNullOrBlank()) "note_add" else "note_edit_$voiceInputSource"
                FirebaseAnalytics.getInstance(this).logEvent(noteEvent, null)
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(this)
                NotificationUpdateService.updateNotification(this)
            } else {
                Log.e(TAG, "Error updating contraction's note")
            }
        }
    }
}
