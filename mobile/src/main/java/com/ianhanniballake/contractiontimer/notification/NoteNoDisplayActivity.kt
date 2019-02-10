package com.ianhanniballake.contractiontimer.notification

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.RemoteInput
import android.util.Log
import android.widget.Toast
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Google Now's 'Note to self' only calls startActivity so we redirect to our service to run the update in the
 * background
 */
class NoteNoDisplayActivity : Activity() {
    companion object {
        private const val TAG = "NoteNoDisplayActivity"

        /**
         * Ensures that the NoteTransparentActivity is only enabled when there is at least one contraction
         *
         * @param context Context to be used to query the ContentProvider and enable/disable this activity
         */
        fun checkServiceState(context: Context) {
            GlobalScope.launch {
                val hasContractions = context.contentResolver.query(
                        ContractionContract.Contractions.CONTENT_URI, null,
                        null, null,  null)?.use { data ->
                    data.moveToFirst()
                } ?: false
                if (BuildConfig.DEBUG)
                    Log.d(TAG, (if (hasContractions) "Has" else "No") + " contractions")
                val state = if (hasContractions)
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                val packageManager = context.packageManager
                val componentName = ComponentName(context, NoteNoDisplayActivity::class.java)
                packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val note = if (remoteInput != null)
            remoteInput.getCharSequence(Intent.EXTRA_TEXT)?.toString()
        else
            intent.getStringExtra(Intent.EXTRA_TEXT)
        val serviceIntent = Intent(this, NoteIntentService::class.java).apply {
            action = intent.action
            putExtra(Intent.EXTRA_TEXT, note)
        }
        startService(serviceIntent)
        if (note?.isNotBlank() == true) {
            Toast.makeText(this, R.string.saving_note, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
