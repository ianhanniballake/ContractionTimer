package com.ianhanniballake.contractiontimer.data;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.CreateFileActivityBuilder;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * Imports contractions from a CSV file on Google Drive
 */
public class ImportActivity extends AbstractDriveApiActivity {
    private static final String TAG = ImportActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN = 2;
    private ResultCallback<DriveApi.ContentsResult> mOpenFileCallback = new ResultCallback<DriveApi.ContentsResult>() {
        @Override
        public void onResult(DriveApi.ContentsResult result) {
            if (!result.getStatus().isSuccess()) {
                finish();
                return;
            }
            IntentSender intentSender = Drive.DriveApi
                    .newOpenFileActivityBuilder()
                    .setActivityTitle(getString(R.string.drive_open_file_title))
                    .setMimeType(new String[]{"text/csv"})
                    .build(mGoogleApiClient);
            try {
                startIntentSenderForResult(intentSender, REQUEST_CODE_OPEN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                finish();
            }
        }
    };

    @Override
    public void onConnected(final Bundle bundle) {
        Drive.DriveApi.newContents(mGoogleApiClient).setResultCallback(mOpenFileCallback);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN && resultCode == RESULT_OK) {
            DriveId driveId = data.getParcelableExtra(CreateFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
            new ImportContractionsAsyncTask().execute(driveId);
        }
    }

    private class ImportContractionsAsyncTask extends AsyncTask<DriveId, Void, String> {
        @Override
        protected String doInBackground(DriveId... params) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, params[0]);
            DriveApi.ContentsResult result = file.openContents(mGoogleApiClient, DriveFile.MODE_READ_ONLY,
                    null).await();
            if (!result.getStatus().isSuccess()) {
                return getString(R.string.drive_error_open_file);
            }
            InputStream is = result.getContents().getInputStream();
            CSVReader reader = new CSVReader(new InputStreamReader(is), CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER, 1);
            try {
                List<String[]> contractions = reader.readAll();
                reader.close();
                file.commitAndCloseContents(mGoogleApiClient, result.getContents());
                ContentResolver resolver = getContentResolver();
                String[] projection = new String[]{BaseColumns._ID};
                for (String[] contraction : contractions) {
                    if (contraction.length != 3) {
                        throw new IllegalArgumentException();
                    }
                    ContentValues values = new ContentValues();
                    final long startTime = Long.parseLong(contraction[0]);
                    values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                            startTime);
                    if (!TextUtils.isEmpty(contraction[1]))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                                Long.parseLong(contraction[1]));
                    if (!TextUtils.isEmpty(contraction[2]))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE,
                                contraction[2]);
                    Cursor existingRow = resolver.query(ContractionContract.Contractions.CONTENT_URI,
                            projection, ContractionContract.Contractions.COLUMN_NAME_START_TIME
                                    + "=?", new String[]{Long.toString(startTime)}, null
                    );
                    long existingRowId = AdapterView.INVALID_ROW_ID;
                    if (existingRow != null) {
                        existingRowId = existingRow.moveToFirst() ? existingRow.getLong(0) : AdapterView.INVALID_ROW_ID;
                        existingRow.close();
                    }
                    if (existingRowId == AdapterView.INVALID_ROW_ID)
                        operations.add(ContentProviderOperation.newInsert(ContractionContract.Contractions.CONTENT_URI)
                                .withValues(values).build());
                    else
                        operations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId
                                (ContractionContract.Contractions.CONTENT_ID_URI_BASE,
                                        existingRowId)).withValues(values).build());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading file", e);
                return getString(R.string.drive_error_reading_file);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid file format", e);
                return getString(R.string.drive_error_invalid_file_format);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid file format", e);
                return getString(R.string.drive_error_invalid_file_format);
            }
            try {
                getContentResolver().applyBatch(ContractionContract.AUTHORITY, operations);
            } catch (RemoteException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.drive_error_saving_contractions);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.drive_error_saving_contractions);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (TextUtils.isEmpty(error)) {
                Toast.makeText(ImportActivity.this, getString(R.string.drive_import_successful),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ImportActivity.this, getString(R.string.drive_error_import, error),
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
