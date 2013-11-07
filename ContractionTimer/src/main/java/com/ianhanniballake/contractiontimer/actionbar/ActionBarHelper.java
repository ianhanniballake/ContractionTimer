package com.ianhanniballake.contractiontimer.actionbar;

import android.app.Activity;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An abstract base class for supporting ActionBar implementations on both pre-3.0 devices and 3.0+ devices
 */
public abstract class ActionBarHelper {
    /**
     * Activity to decorate with ActionBar
     */
    protected Activity mActivity;

    /**
     * @param activity Activity to decorate with ActionBar
     */
    protected ActionBarHelper(final Activity activity) {
        mActivity = activity;
    }

    /**
     * Factory method for creating {@link ActionBarHelper} objects for a given activity based on the current Android
     * version
     *
     * @param activity Activity to decorate with ActionBar
     * @return appropriate instance of ActionBarHelper
     */
    public static ActionBarHelper createInstance(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            return new ActionBarHelperNative(activity);
        return new ActionBarHelperBase(activity);
    }

    /**
     * Returns a {@link MenuInflater} for use when inflating menus. The implementation of this method in
     * {@link ActionBarHelperBase} returns a wrapped menu inflater that can read action bar metadata from a menu
     * resource pre-Honeycomb.
     *
     * @param superMenuInflater default activity menu inflater
     * @return ActionBar compatible menu inflater
     */
    public abstract MenuInflater getMenuInflater(final MenuInflater superMenuInflater);

    /**
     * Action bar helper code to be run during the activity's onCreate lifecycle phase.
     */
    public abstract void onCreate();

    /**
     * Action bar helper code to be run during the activity's onCreateOptionsMenu.
     *
     * @param menu Menu to set up
     * @return Whether the menu should be shown
     */
    public abstract boolean onCreateOptionsMenu(final Menu menu);

    /**
     * Action bar helper code to be run during the activity's onPostCreate lifecycle phase.
     */
    public abstract void onPostCreate();

    /**
     * Sets whether the home button should display the 'up' carret
     *
     * @param showHomeAsUp Whether the 'up' carret should appear on the home button
     */
    public abstract void setDisplayHomeAsUpEnabled(final boolean showHomeAsUp);

    /**
     * Callback for when a MenuItem is enabled or disabled
     *
     * @param item    MenuItem that was enabled/disabled
     * @param enabled current state of the MenuItem
     */
    public abstract void setEnabled(MenuItem item, boolean enabled);

    /**
     * Callback for when a MenuItem is set to visible/invisible
     *
     * @param item    MenuItem that was set to visible/invisible
     * @param visible current state of the MenuItem
     */
    public abstract void setVisible(MenuItem item, boolean visible);

    /**
     * Invalidate the option menu
     */
    public abstract void supportInvalidateOptionsMenu();
}
