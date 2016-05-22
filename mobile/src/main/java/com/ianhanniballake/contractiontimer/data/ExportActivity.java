package com.ianhanniballake.contractiontimer.data;

import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.CreateFileActivityBuilder;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.ianhanniballake.contractiontimer.R;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Exports contractions to a CSV file on Google Drive
 */
public class ExportActivity extends AbstractDriveApiActivity {
    private static final String TAG = ExportActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CREATE = 2;
    private final ResultCallback<DriveApi.DriveContentsResult> mCreateFileCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        finish();
                        return;
                    }
                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                            .setTitle(getString(R.string.drive_default_filename) + ".csv")
                            .setMimeType("text/csv").build();
                    IntentSender intentSender = new CreateFileActivityBuilder()
                            .setActivityTitle(getString(R.string.drive_create_file_title))
                            .setInitialMetadata(metadataChangeSet)
                            .setInitialDriveContents(result.getDriveContents())
                            .build(mGoogleApiClient);
                    try {
                        startIntentSenderForResult(intentSender, REQUEST_CODE_CREATE, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        finish();
                    }
                }
            };

    @Override
    public void onConnected(final Bundle connectionHint) {
        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(mCreateFileCallback);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CREATE && resultCode == RESULT_OK) {
            DriveId driveId = data.getParcelableExtra(CreateFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
            new ExportContractionsAsyncTask().execute(driveId);
        }
    }

    private class ExportContractionsAsyncTask extends AsyncTask<DriveId, Void, Boolean> {
        @Override
        protected Boolean doInBackground(DriveId... params) {
            if (params[0] == null) {
                Log.w(TAG, "Null DriveId returned by DriveApi");
                return false;
            }
            DriveFile file = params[0].asDriveFile();
            DriveApi.DriveContentsResult result = file.open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY,
                    null).await();
            if (!result.getStatus().isSuccess()) {
                Log.w(TAG, "Failure getting file result");
                return false;
            }
            DriveContents driveContents = result.getDriveContents();
            OutputStream os = driveContents.getOutputStream();
            try {
                CSVTransformer.writeContractions(ExportActivity.this, os);
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "Error writing contractions", e);
                return false;
            }
            driveContents.commit(mGoogleApiClient, null).await();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ExportActivity.this, getString(R.string.drive_export_successful),
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ExportActivity.this, getString(R.string.drive_error_export),
                        Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
