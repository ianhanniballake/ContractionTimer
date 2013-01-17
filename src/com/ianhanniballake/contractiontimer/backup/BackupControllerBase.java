package com.ianhanniballake.contractiontimer.backup;

import android.content.Context;

/**
 * BackupController for pre-Froyo devices that do not have access to the Backup API
 */
public class BackupControllerBase extends BackupController
{
	@Override
	public void dataChanged(final Context context)
	{
		// Nothing to do
	}
}
