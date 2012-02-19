package com.ianhanniballake.contractiontimer.actionbar;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * An extension of {@link ActionBarHelper} that provides access to the native
 * ActionBar in Android 3.0+ devices
 */
public class ActionBarHelperNative extends ActionBarHelper
{
	/**
	 * @param activity
	 *            Activity to decorate with ActionBar
	 */
	protected ActionBarHelperNative(final Activity activity)
	{
		super(activity);
	}

	/** {@inheritDoc} */
	@Override
	public MenuInflater getMenuInflater(final MenuInflater superMenuInflater)
	{
		return superMenuInflater;
	}

	/** {@inheritDoc} */
	@Override
	public void onCreate()
	{
		// Nothing to do
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		return menu.hasVisibleItems();
	}

	@Override
	public void onPostCreate()
	{
		// Nothing to do
	}

	@Override
	public void setDisplayHomeAsUpEnabled(final boolean showHomeAsUp)
	{
		mActivity.getActionBar().setDisplayHomeAsUpEnabled(showHomeAsUp);
	}
}
