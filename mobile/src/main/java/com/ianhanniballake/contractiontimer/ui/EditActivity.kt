package com.ianhanniballake.contractiontimer.ui

import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R

/**
 * Stand alone activity used to view the details of an individual contraction
 */
class EditActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EditActivity"
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        if (findViewById<View>(R.id.edit) == null) {
            // A null details view means we no longer need this activity
            finish()
            return
        }
        if (intent?.action == Intent.ACTION_EDIT) {
            val data = intent.data
            if (data == null) {
                // Invalid data
                finish()
                return
            }
            try {
                ContentUris.parseId(data)
            } catch (e: NumberFormatException) {
                // Invalid content uri
                finish()
                return
            }
        } else if (intent?.action != Intent.ACTION_INSERT) {
            // Invalid action
            finish()
        }
        if (savedInstanceState == null)
            showFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Intent.ACTION_INSERT == intent.action)
            menuInflater.inflate(R.menu.activity_add, menu)
        else
            menuInflater.inflate(R.menu.activity_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun getSupportParentActivityIntent(): Intent {
        return if (Intent.ACTION_INSERT == intent.action) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(Intent.ACTION_VIEW, intent.data)
                .setComponent(ComponentName(this, ViewActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (Intent.ACTION_INSERT == intent.action) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Add selected home")
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Edit selected home")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, resources
                .getBoolean(R.bool.pref_lock_portrait_default))
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: $isLockPortrait")
        requestedOrientation = if (isLockPortrait)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onStart() {
        super.onStart()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            if (Intent.ACTION_INSERT == intent.action) {
                setTitle(R.string.add_activity_name)
            } else {
                setTitle(R.string.edit_activity_name)
            }
        }
    }

    /**
     * Creates and shows the fragment associated with the current contraction
     */
    private fun showFragment() {
        val viewFragment = EditFragment()
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.edit, viewFragment)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.commit()
    }
}
