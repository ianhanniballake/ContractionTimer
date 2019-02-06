package com.ianhanniballake.contractiontimer.data;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * Imports contractions from a CSV file using {@link Intent#ACTION_OPEN_DOCUMENT}
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class ImportActivity extends FragmentActivity {
    private static final String TAG = ImportActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN = 2;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        if (savedInstanceState == null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            startActivityForResult(intent, REQUEST_CODE_OPEN);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN && resultCode == RESULT_OK) {
            new ImportContractionsAsyncTask().execute(data);
        } else {
            finish();
        }
    }

    private class ImportContractionsAsyncTask extends AsyncTask<Intent, Void, String> {
        @Override
        protected String doInBackground(Intent... params) {
            if (params[0] == null) {
                Log.w(TAG, "Null Intent returned by ACTION_CREATE_DOCUMENT");
                return getString(R.string.backup_error_open_file);
            }
            Uri documentUri = params[0].getData();
            if (documentUri == null) {
                Log.w(TAG, "Null Uri returned by ACTION_CREATE_DOCUMENT");
                return getString(R.string.backup_error_open_file);
            }
            InputStream is = null;
            try {
                is = getContentResolver().openInputStream(documentUri);
                CSVTransformer.readContractions(ImportActivity.this, is);
                return null;
            } catch (IOException e) {
                Log.e(TAG, "Error reading file", e);
                return getString(R.string.backup_error_reading_file);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid file format", e);
                return getString(R.string.backup_error_invalid_file_format);
            } catch (RemoteException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.backup_error_saving_contractions);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.backup_error_saving_contractions);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Error closing file", e);
                }
            }
        }

        @Override
        protected void onPostExecute(String error) {
            if (TextUtils.isEmpty(error)) {
                FirebaseAnalytics.getInstance(ImportActivity.this).logEvent("import_complete", null);
                Toast.makeText(ImportActivity.this, getString(R.string.backup_import_successful),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ImportActivity.this, getString(R.string.backup_error_import, error),
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
