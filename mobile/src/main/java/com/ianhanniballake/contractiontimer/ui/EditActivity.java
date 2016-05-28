package com.ianhanniballake.contractiontimer.ui;

import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

/**
 * Stand alone activity used to view the details of an individual contraction
 */
public class EditActivity extends AppCompatActivity {
    private final static String TAG = EditActivity.class.getSimpleName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        if (findViewById(R.id.edit) == null) {
            // A null details view means we no longer need this activity
            finish();
            return;
        }
        Intent intent = getIntent();
        if (intent == null) {
            // Invalid intent
            finish();
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            Uri data = intent.getData();
            if (data == null) {
                // Invalid data
                finish();
                return;
            }
            try {
                ContentUris.parseId(data);
            } catch (NumberFormatException e) {
                // Invalid content uri
                finish();
                return;
            }
        } else if (!Intent.ACTION_INSERT.equals(action)) {
            // Invalid action
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_lock_portrait_default));
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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            actionBar.setTitle(R.string.add_activity_name);
        } else {
            actionBar.setTitle(R.string.edit_activity_name);
        }
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
