package com.ianhanniballake.contractiontimer.actionbar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import com.ianhanniballake.contractiontimer.analytics.AnalyticTrackingFragmentActivity;

/**
 * A base activity that defers common functionality across app activities to an
 * {@link ActionBarHelper}.
 */
public abstract class ActionBarFragmentActivity extends
		AnalyticTrackingFragmentActivity
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
		super.onCreate(savedInstanceState);
		mActionBarHelper.onCreate();
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
