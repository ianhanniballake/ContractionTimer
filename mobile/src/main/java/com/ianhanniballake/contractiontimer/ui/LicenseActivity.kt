package com.ianhanniballake.contractiontimer.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R

/**
 * License screen for the application. Shows as a dialog on large devices
 */
class LicenseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LicenseActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
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
