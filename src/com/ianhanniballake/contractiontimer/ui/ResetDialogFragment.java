package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.analytics.AnalyticsManagerService;
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
		AnalyticsManagerService.trackEvent(getActivity(), "Note", "Cancel");
		super.onCancel(dialog);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View layout = inflater.inflate(R.layout.dialog_reset, null);
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.reset_dialog_title)
				.setView(layout)
				.setInverseBackgroundForced(true)
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
								AnalyticsManagerService.trackEvent(
										getActivity(), "Reset", "Positive");
								new AsyncQueryHandler(getActivity()
										.getContentResolver())
								{
									// No call backs needed
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
								AnalyticsManagerService.trackEvent(
										getActivity(), "Reset", "Negative");
							}
						}).create();
	}
}
