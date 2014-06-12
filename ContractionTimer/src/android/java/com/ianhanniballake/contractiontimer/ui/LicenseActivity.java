package com.ianhanniballake.contractiontimer.ui;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * About screen for the application. Shows as a dialog on large devices
 */
public class LicenseActivity extends ActionBarActivity {
    private final static String TAG = LicenseActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);
        TextView googlePlayServicesLicense = (TextView) findViewById(R.id.googleplayservices_license);
        googlePlayServicesLicense.setText(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        GtmManager.getInstance(this).pushOpenScreen("License");
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_settings_lock_portrait_default));
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
}
