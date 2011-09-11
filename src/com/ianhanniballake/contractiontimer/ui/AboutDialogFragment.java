package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.ianhanniballake.contractiontimer.R;

/**
 * About Dialog for the application
 */
public class AboutDialogFragment extends DialogFragment
{
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getActivity());
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View layout = inflater.inflate(R.layout.dialog_about, null);
		builder.setTitle(R.string.app_name).setIcon(R.drawable.icon)
				.setView(layout)
				.setPositiveButton(getText(R.string.close), null);
		return builder.create();
	}
}
