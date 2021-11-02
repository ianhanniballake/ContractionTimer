package com.ianhanniballake.contractiontimer.ui

import android.app.Dialog
import android.content.ContentUris
import android.content.ContentValues
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Reset Confirmation Dialog box
 */
class NoteDialogFragment : DialogFragment() {
    companion object {
        private const val TAG = "NoteDialogFragment"
        /**
         * Argument key for storing/retrieving the contraction id associated with this dialog
         */
        const val CONTRACTION_ID_ARGUMENT = "com.ianhanniballake.contractiontimer.ContractionId"
        /**
         * Argument key for storing/retrieving the existing note associated with this dialog
         */
        const val EXISTING_NOTE_ARGUMENT = "com.ianhanniballake.contractiontimer.ExistingNote"
    }

    override fun onCancel(dialog: DialogInterface) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received cancellation event")
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contractionId = requireArguments().getLong(CONTRACTION_ID_ARGUMENT)
        val existingNote = requireArguments().getString(EXISTING_NOTE_ARGUMENT)
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.dialog_note, null)
        val input = layout.findViewById<EditText>(R.id.dialog_note_input)
        if (existingNote.isNullOrBlank())
            builder.setTitle(R.string.note_dialog_title_add)
        else
            builder.setTitle(R.string.note_dialog_title_edit)
        input.setText(existingNote)
        @Suppress("DEPRECATION")
        return builder.setView(layout).setInverseBackgroundForced(true)
            .setPositiveButton(R.string.note_dialog_save) { _, _ ->
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Received positive event")
                    val noteEvent = if (existingNote.isNullOrBlank()) "note_add_saved" else "note_edit_saved"
                FirebaseAnalytics.getInstance(requireContext()).logEvent(noteEvent, null)
                    val updateUri = ContentUris.withAppendedId(
                            ContractionContract.Contractions.CONTENT_ID_URI_BASE, contractionId)
                val values = ContentValues().apply {
                    put(ContractionContract.Contractions.COLUMN_NAME_NOTE, input.text.toString())
                }
                val context = requireContext()
                    GlobalScope.launch {
                        context.contentResolver.update(updateUri, values, null, null)
                        AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                        NotificationUpdateReceiver.updateNotification(context)
                    }
                }.setNegativeButton(R.string.note_dialog_cancel) { _, _ ->
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Received negative event")
                }.create()
    }
}
