package com.ianhanniballake.contractiontimer.strictmode

import android.os.Build

/**
 * Sets up the StrictMode
 */
abstract class StrictModeController {
    companion object {
        /**
         * Factory method for creating [StrictModeController] objects
         *
         * @return appropriate instance of StrictModeController
         */
        fun createInstance(): StrictModeController {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1)
                StrictModeControllerV10()
            else
                StrictModeControllerBase()
        }
    }

    /**
     * Set the strict mode appropriately
     */
    abstract fun setStrictMode()
}
