package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ianhanniballake.contractiontimer.R;

/**
 * About Dialog for the application
 */
public class AboutDialogFragment extends DialogFragment
{
	@Override
	public void onCancel(final DialogInterface dialog)
	{
		Log.d(getClass().getSimpleName(), "Received cancelation event");
		GoogleAnalyticsTracker.getInstance().trackEvent("About", "Cancel", "",
				0);
		super.onCancel(dialog);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		AlertDialog.Builder builder;
		LayoutInflater inflater;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			builder = new AlertDialog.Builder(getActivity());
			inflater = getActivity().getLayoutInflater();
		}
		else
		{
			final ContextThemeWrapper wrapper = new ContextThemeWrapper(
					getActivity(), R.style.DialogTheme);
			builder = new AlertDialog.Builder(getActivity());
			inflater = (LayoutInflater) wrapper
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		final View layout = inflater.inflate(R.layout.dialog_about, null);
		builder.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon)
				.setView(layout)
				.setNeutralButton(getText(R.string.close),
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								Log.d(AboutDialogFragment.this.getClass()
										.getSimpleName(),
										"Received neutral event");
								GoogleAnalyticsTracker.getInstance()
										.trackEvent("About", "Neutral", "", 0);
							}
						});
		return builder.create();
	}

	@Override
	public void show(final FragmentManager manager, final String tag)
	{
		Log.d(getClass().getSimpleName(), "Showing Dialog");
		GoogleAnalyticsTracker.getInstance().trackPageView(
				"/" + getClass().getSimpleName());
		super.show(manager, tag);
	}
}
