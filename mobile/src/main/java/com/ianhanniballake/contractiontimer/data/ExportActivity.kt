package com.ianhanniballake.contractiontimer.data

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Exports contractions to a CSV file using [Intent.ACTION_CREATE_DOCUMENT]
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
class ExportActivity : FragmentActivity() {
    companion object {
        private const val TAG = "ExportActivity"
        private const val REQUEST_CODE_CREATE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        if (savedInstanceState == null) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, getString(R.string.backup_default_filename) + ".csv")
            }
            startActivityForResult(intent, REQUEST_CODE_CREATE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE && resultCode == RESULT_OK) {
            val context = this
            GlobalScope.launch(Dispatchers.Main) {
                val documentUri = data?.data
                val success = if (documentUri != null) {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(documentUri)?.use {
                            try {
                                CSVTransformer.writeContractions(context, it)
                                true
                            } catch (e: IOException) {
                                Log.e(TAG, "Error writing contractions", e)
                                false
                            }
                        } ?: false
                    }
                } else {
                    Log.w(TAG, "Null Uri returned by ACTION_CREATE_DOCUMENT")
                    false
                }
                if (success) {
                    FirebaseAnalytics.getInstance(context).logEvent("export_complete", null)
                    Toast.makeText(context, getString(R.string.backup_export_successful),
                            Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, getString(R.string.backup_error_export),
                            Toast.LENGTH_LONG).show()
                }
                finish()
            }
        } else {
            finish()
        }
    }
}
