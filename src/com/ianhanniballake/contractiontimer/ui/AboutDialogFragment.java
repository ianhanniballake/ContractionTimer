package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

/**
 * About Dialog for the application
 */
public class AboutDialogFragment extends DialogFragment
{
	/**
	 * Action associated with this fragment closing
	 */
	public final static String ABOUT_CLOSE_ACTION = "com.ianhanniballake.contractiontimer.ABOUT_CLOSE";

	@Override
	public void onCancel(final DialogInterface dialog)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Received cancelation event");
		EasyTracker.getTracker().trackEvent("About", "Cancel", "", 0L);
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
								if (BuildConfig.DEBUG)
									Log.d(AboutDialogFragment.this.getClass()
											.getSimpleName(),
											"Received neutral event");
								EasyTracker.getTracker().trackEvent("About",
										"Neutral", "", 0L);
							}
						}).create();
	}

	@Override
	public void onDismiss(final DialogInterface dialog)
	{
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
				.getInstance(getActivity());
		localBroadcastManager.sendBroadcast(new Intent(ABOUT_CLOSE_ACTION));
		super.onDismiss(dialog);
	}
}
