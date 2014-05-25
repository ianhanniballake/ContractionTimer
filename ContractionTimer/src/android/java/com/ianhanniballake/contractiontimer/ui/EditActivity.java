package com.ianhanniballake.contractiontimer.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Stand alone activity used to view the details of an individual contraction
 */
public class EditActivity extends ActionBarActivity {
    private final static String TAG = EditActivity.class.getSimpleName();
    /**
     * BroadcastReceiver listening for TIME_PICKER_CLOSE_ACTION and DATE_PICKER_CLOSE_ACTION actions
     */
    private final BroadcastReceiver dialogFragmentClosedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "DialogFragmentClosedBR Received " + intent.getAction());
            final String screenName = Intent.ACTION_INSERT.equals(getIntent().getAction()) ? "Add" : "Edit";
            GtmManager.getInstance(EditActivity.this).pushOpenScreen(screenName);
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        if (findViewById(R.id.edit) == null) {
            // A null details view means we no longer need this activity
            finish();
            return;
        }
        if (savedInstanceState == null)
            showFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (Intent.ACTION_INSERT.equals(getIntent().getAction()))
            getMenuInflater().inflate(R.menu.activity_add, menu);
        else
            getMenuInflater().inflate(R.menu.activity_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            return new Intent(this, MainActivity.class);
        } else {
            return new Intent(Intent.ACTION_VIEW, getIntent().getData()).setPackage(getPackageName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Add selected home");
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Edit selected home");
            }
            GtmManager.getInstance(this).pushEvent("Home");
        }
        return super.onOptionsItemSelected(item);
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

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String screenName = Intent.ACTION_INSERT.equals(getIntent().getAction()) ? "Add" : "Edit";
        GtmManager.getInstance(this).pushOpenScreen(screenName);
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        final IntentFilter dialogCloseFilter = new IntentFilter();
        dialogCloseFilter.addAction(TimePickerDialogFragment.TIME_PICKER_CLOSE_ACTION);
        dialogCloseFilter.addAction(DatePickerDialogFragment.DATE_PICKER_CLOSE_ACTION);
        localBroadcastManager.registerReceiver(dialogFragmentClosedBroadcastReceiver, dialogCloseFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(dialogFragmentClosedBroadcastReceiver);
    }

    /**
     * Creates and shows the fragment associated with the current contraction
     */
    private void showFragment() {
        final EditFragment viewFragment = new EditFragment();
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.edit, viewFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
}
