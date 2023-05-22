package com.ianhanniballake.contractiontimer.data

import android.annotation.TargetApi
import android.content.Intent
import android.content.OperationApplicationException
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Imports contractions from a CSV file using [Intent.ACTION_OPEN_DOCUMENT]
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
class ImportActivity : FragmentActivity() {
    companion object {
        private const val TAG = "ImportActivity"
        private const val REQUEST_CODE_OPEN = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        if (savedInstanceState == null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN && resultCode == RESULT_OK) {
            val context = this
            GlobalScope.launch(Dispatchers.Main) {
                val documentUri = data?.data
                val error = if (documentUri != null) {
                    contentResolver.openInputStream(documentUri)?.use {
                        try {
                            CSVTransformer.readContractions(context, it)
                            null
                        } catch (e: IOException) {
                            Log.e(TAG, "Error reading file", e)
                            getString(R.string.backup_error_reading_file)
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid file format", e)
                            getString(R.string.backup_error_invalid_file_format)
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Error saving contractions", e)
                            getString(R.string.backup_error_saving_contractions)
                        } catch (e: OperationApplicationException) {
                            Log.e(TAG, "Error saving contractions", e)
                            getString(R.string.backup_error_saving_contractions)
                        }
                    }
                } else {
                    Log.w(TAG, "Null Uri returned by ACTION_OPEN_DOCUMENT")
                    getString(R.string.backup_error_open_file)
                }
                if (error.isNullOrBlank()) {
                    FirebaseAnalytics.getInstance(context).logEvent("import_complete", null)
                    Toast.makeText(context, getString(R.string.backup_import_successful),
                            Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, getString(R.string.backup_error_import, error),
                            Toast.LENGTH_LONG).show()
                }
                finish()
            }
        } else {
            finish()
        }
    }
}
