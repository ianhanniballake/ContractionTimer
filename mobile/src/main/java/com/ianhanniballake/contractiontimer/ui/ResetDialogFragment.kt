package com.ianhanniballake.contractiontimer.ui

import android.app.Dialog
import android.content.AsyncQueryHandler
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService
import com.ianhanniballake.contractiontimer.provider.ContractionContract

/**
 * Reset Confirmation Dialog box
 */
class ResetDialogFragment : DialogFragment() {
    companion object {
        private const val TAG = "ResetDialogFragment"
    }

    override fun onCancel(dialog: DialogInterface?) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received cancellation event")
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity.layoutInflater
        val layout = inflater.inflate(R.layout.dialog_reset, null)
        val context = activity
        val asyncQueryHandler = object : AsyncQueryHandler(context.contentResolver) {
            override fun onDeleteComplete(token: Int, cookie: Any?, result: Int) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                NotificationUpdateService.updateNotification(context)
            }
        }
        @Suppress("DEPRECATION")
        return AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.reset_dialog_title)
                .setView(layout)
                .setInverseBackgroundForced(true)
                .setPositiveButton(R.string.reset_dialog_confirm) { _, _ ->
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Received positive event")
                    FirebaseAnalytics.getInstance(context).logEvent("reset_complete", null)
                    asyncQueryHandler.startDelete(0, 0,
                            ContractionContract.Contractions.CONTENT_URI, null, null)
                }.setNegativeButton(R.string.reset_dialog_cancel) { _, _ ->
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Received negative event")
                }.create()
    }
}
