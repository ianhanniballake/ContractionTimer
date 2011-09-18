package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Reset Confirmation Dialog box
 */
public class ResetDialogFragment extends DialogFragment
{
	@Override
	public void onCancel(final DialogInterface dialog)
	{
		Log.d(getClass().getSimpleName(), "Received cancelation event");
		GoogleAnalyticsTracker.getInstance()
				.trackEvent("Note", "Cancel", "", 0);
		super.onCancel(dialog);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.reset_dialog_title)
				.setMessage(R.string.reset_dialog_message)
				.setPositiveButton(R.string.reset_dialog_confirm,
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								Log.d(ResetDialogFragment.this.getClass()
										.getSimpleName(),
										"Received positive event");
								GoogleAnalyticsTracker.getInstance()
										.trackEvent("Reset", "Positive", "", 0);
								new AsyncQueryHandler(getActivity()
										.getContentResolver())
								{
								}.startDelete(
										0,
										0,
										ContractionContract.Contractions.CONTENT_URI,
										null, null);
							}
						})
				.setNegativeButton(R.string.reset_dialog_cancel,
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								Log.d(ResetDialogFragment.this.getClass()
										.getSimpleName(),
										"Received negative event");
								GoogleAnalyticsTracker.getInstance()
										.trackEvent("Reset", "Negative", "", 0);
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
