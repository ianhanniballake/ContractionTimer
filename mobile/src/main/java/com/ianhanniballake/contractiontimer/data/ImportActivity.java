package com.ianhanniballake.contractiontimer.data;

import android.content.Intent;
import android.content.IntentSender;
import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.CreateFileActivityBuilder;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.ianhanniballake.contractiontimer.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * Imports contractions from a CSV file on Google Drive
 */
public class ImportActivity extends AbstractDriveApiActivity {
    private static final String TAG = ImportActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN = 2;
    private final ResultCallback<DriveApi.DriveContentsResult> mOpenFileCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        finish();
                        return;
                    }
                    IntentSender intentSender = new OpenFileActivityBuilder()
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
        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(mOpenFileCallback);
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
            DriveFile file = params[0].asDriveFile();
            DriveApi.DriveContentsResult result = file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY,
                    null).await();
            if (!result.getStatus().isSuccess()) {
                return getString(R.string.drive_error_open_file);
            }
            DriveContents driveContents = result.getDriveContents();
            InputStream is = driveContents.getInputStream();
            try {
                CSVTransformer.readContractions(ImportActivity.this, is);
                is.close();
            } catch (IOException e) {
                Log.e(TAG, "Error reading file", e);
                return getString(R.string.drive_error_reading_file);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid file format", e);
                return getString(R.string.drive_error_invalid_file_format);
            } catch (RemoteException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.drive_error_saving_contractions);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Error saving contractions", e);
                return getString(R.string.drive_error_saving_contractions);
            }
            driveContents.commit(mGoogleApiClient, null).await();
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
