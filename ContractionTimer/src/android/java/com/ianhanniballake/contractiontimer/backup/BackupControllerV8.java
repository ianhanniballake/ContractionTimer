package com.ianhanniballake.contractiontimer.backup;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.Context;

/**
 * BackupController for devices that have access to the Android Backup API
 */
@TargetApi(8)
public class BackupControllerV8 extends BackupController {
    @Override
    public void dataChanged(final Context context) {
        new BackupManager(context).dataChanged();
    }
}
