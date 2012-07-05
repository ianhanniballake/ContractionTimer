package com.ianhanniballake.contractiontimer.actionbar;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * A base activity that defers common functionality across app activities to an
 * {@link ActionBarHelper}.
 */
public abstract class ActionBarPreferenceActivity extends PreferenceActivity
{
	/**
	 * Instance of the ActionBarHelper appropriate to the current Android
	 * version
	 */
	final ActionBarHelper mActionBarHelper = ActionBarHelper
			.createInstance(this);

	/**
	 * Returns the {@link ActionBarHelper} for this activity.
	 * 
	 * @return our instance of the ActionBarHelper
	 */
	protected ActionBarHelper getActionBarHelper()
	{
		return mActionBarHelper;
	}

	/** {@inheritDoc} */
	@Override
	public MenuInflater getMenuInflater()
	{
		return mActionBarHelper.getMenuInflater(super.getMenuInflater());
	}

	/** {@inheritDoc} */
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		// Note calls must be in this order as PreferenceActivity's onCreate
		// adds UI elements, which is a problem for setting window features to
		// support the custom action bar on pre-Honeycomb devices
		mActionBarHelper.onCreate();
		super.onCreate(savedInstanceState);
	}

	/**
	 * Base action bar-aware implementation for
	 * {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		boolean retValue = mActionBarHelper.onCreateOptionsMenu(menu);
		retValue |= super.onCreateOptionsMenu(menu);
		return retValue;
	}

	/** {@inheritDoc} */
	@Override
	protected void onPostCreate(final Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		mActionBarHelper.onPostCreate();
	}
}
