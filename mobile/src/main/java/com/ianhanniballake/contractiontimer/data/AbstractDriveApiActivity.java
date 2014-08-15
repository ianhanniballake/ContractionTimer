package com.ianhanniballake.contractiontimer.data;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.ianhanniballake.contractiontimer.R;

/**
 * Abstract activity which handles authentication and connection to Google Drive via the Drive.API
 */
public abstract class AbstractDriveApiActivity extends FragmentActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_CODE_CONNECT = 1;
    protected GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }
        if (requestCode == REQUEST_CODE_CONNECT) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionSuspended(final int cause) {
        finish();
    }

    @Override
    public void onConnectionFailed(final ConnectionResult result) {
        if (result == null) {
            Toast.makeText(this, getString(R.string.drive_error_generic_connect), Toast.LENGTH_LONG).show();
        } else if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_CODE_CONNECT);
            } catch (IntentSender.SendIntentException e) {
                Toast.makeText(this, getString(R.string.drive_error_connect, e.getLocalizedMessage()),
                        Toast.LENGTH_LONG).show();
            }
        } else if (!isFinishing() && !isDestroyed()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
        }
    }
}
