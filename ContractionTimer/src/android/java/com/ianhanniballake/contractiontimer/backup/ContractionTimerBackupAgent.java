package com.ianhanniballake.contractiontimer.backup;

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Agent which handles com.ianhanniballake.contractiontimer.backup of user settings
 */
@TargetApi(8)
public class ContractionTimerBackupAgent extends BackupAgentHelper {
    /**
     * Gets the name associated with PreferenceManager.getDefaultPreferences(), as given by the Android source code
     *
     * @return The name associated with PreferenceManager.getDefaultPreferences()
     */
    public String getDefaultSharedPreferencesName() {
        return getPackageName() + "_preferences";
    }

    @Override
    public void onCreate() {
        final SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this,
                getDefaultSharedPreferencesName());
        addHelper("preferences", helper);
    }
}