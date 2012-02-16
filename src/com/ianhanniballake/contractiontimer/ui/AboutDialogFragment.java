package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
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
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View layout = inflater.inflate(R.layout.dialog_about, null);
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon)
				.setView(layout)
				.setInverseBackgroundForced(true)
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
						}).create();
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
