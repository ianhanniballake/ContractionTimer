package com.ianhanniballake.contractiontimer.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R

/**
 * About screen for the application. Shows as a dialog on large devices
 */
class AboutActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AboutActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val version = findViewById<TextView>(R.id.version)
        version.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    }

    override fun onStart() {
        super.onStart()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                resources.getBoolean(R.bool.pref_lock_portrait_default))
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: $isLockPortrait")
        requestedOrientation = if (isLockPortrait)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }
}
