package com.ianhanniballake.contractiontimer.strictmode;

import android.os.Build;

/**
 * Sets up the StrictMode
 */
public abstract class StrictModeController
{
	/**
	 * Factory method for creating {@link StrictModeController} objects
	 * 
	 * @return appropriate instance of StrictModeController
	 */
	public static StrictModeController createInstance()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			return new StrictModeControllerV10();
		return new StrictModeControllerBase();
	}

	/**
	 * Set the strict mode appropriately
	 */
	public abstract void setStrictMode();
}
