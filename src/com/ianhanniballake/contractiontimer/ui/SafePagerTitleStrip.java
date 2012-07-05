package com.ianhanniballake.contractiontimer.ui;

import org.acra.ErrorReporter;

import android.content.Context;
import android.support.v4.view.PagerTitleStrip;
import android.util.AttributeSet;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;

/**
 * PagerTitleStrip that ensures that onDetachedFromWindow does not propagate
 * NullPointerExceptions
 */
public class SafePagerTitleStrip extends PagerTitleStrip
{
	/**
	 * Default constructor
	 * 
	 * @param context
	 *            Context
	 */
	public SafePagerTitleStrip(final Context context)
	{
		super(context);
	}

	/**
	 * Default constructor
	 * 
	 * @param context
	 *            Context
	 * @param attrs
	 *            XML Attributes
	 */
	public SafePagerTitleStrip(final Context context, final AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		try
		{
			super.onDetachedFromWindow();
		} catch (final NullPointerException e)
		{
			if (BuildConfig.DEBUG)
				Log.e(getClass().getSimpleName(),
						"NullPointerException in onDetachedFromWindow", e);
			else
			{
				EasyTracker.getTracker().trackException(
						Thread.currentThread().getName(), e, false);
				ErrorReporter.getInstance().handleSilentException(e);
			}
		}
	}
}
