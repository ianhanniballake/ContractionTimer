package com.ianhanniballake.contractiontimer.backup;

import android.content.Context;
import android.os.Build;

/**
 * Manages access to the Android BackupManager
 */
public abstract class BackupController
{
	/**
	 * Factory method for creating {@link BackupController} objects
	 * 
	 * @return appropriate instance of BackupManager
	 */
	public static BackupController createInstance()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
			return new BackupControllerV8();
		return new BackupControllerBase();
	}

	/**
	 * Notifies the BackupManager that the data has changed
	 * 
	 * @param context
	 *            Context used to back up data
	 */
	public abstract void dataChanged(final Context context);
}
