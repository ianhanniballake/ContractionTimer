package com.ianhanniballake.contractiontimer.ui

import android.app.Application
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.database.ContractionDatabase
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

class ResetDialogViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ContractionDatabase.getInstance(application).contractionDao()

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}

/**
 * Reset Confirmation Dialog box
 */
class ResetDialogFragment : DialogFragment() {
    companion object {
        private const val TAG = "ResetDialogFragment"
    }

    private val viewModel: ResetDialogViewModel by viewModels()

    override fun onCancel(dialog: DialogInterface) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received cancellation event")
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.dialog_reset, null)
        @Suppress("DEPRECATION")
        return AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.ic_dialog_alert)
            .setTitle(R.string.reset_dialog_title)
            .setView(layout)
            .setInverseBackgroundForced(true)
            .setPositiveButton(R.string.reset_dialog_confirm) { _, _ ->
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Received positive event")
                FirebaseAnalytics.getInstance(requireContext()).logEvent("reset_complete", null)
                val context = requireContext()
                lifecycleScope.launch(NonCancellable) {
                    viewModel.deleteAll()
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                    NotificationUpdateReceiver.updateNotification(context)
                }
                }.setNegativeButton(R.string.reset_dialog_cancel) { _, _ ->
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Received negative event")
                }.create()
    }
}
