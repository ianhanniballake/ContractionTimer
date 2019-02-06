package com.ianhanniballake.contractiontimer.data;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.R;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Exports contractions to a CSV file using {@link Intent#ACTION_CREATE_DOCUMENT}
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ExportActivity extends FragmentActivity {
    private static final String TAG = ExportActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CREATE = 2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        if (savedInstanceState == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.backup_default_filename) + ".csv");
            startActivityForResult(intent, REQUEST_CODE_CREATE);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CREATE && resultCode == RESULT_OK) {
            new ExportContractionsAsyncTask().execute(data);
        } else {
            finish();
        }
    }

    private class ExportContractionsAsyncTask extends AsyncTask<Intent, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Intent... params) {
            if (params[0] == null) {
                Log.w(TAG, "Null Intent returned by ACTION_CREATE_DOCUMENT");
                return false;
            }
            Uri documentUri = params[0].getData();
            if (documentUri == null) {
                Log.w(TAG, "Null Uri returned by ACTION_CREATE_DOCUMENT");
                return false;
            }
            OutputStream os = null;
            try {
                os = getContentResolver().openOutputStream(documentUri);
                CSVTransformer.writeContractions(ExportActivity.this, os);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error writing contractions", e);
                return false;
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing contractions", e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                FirebaseAnalytics.getInstance(ExportActivity.this).logEvent("export_complete", null);
                Toast.makeText(ExportActivity.this, getString(R.string.backup_export_successful),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ExportActivity.this, getString(R.string.backup_error_export),
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
