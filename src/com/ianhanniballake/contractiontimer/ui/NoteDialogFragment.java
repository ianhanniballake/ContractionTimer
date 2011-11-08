package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Reset Confirmation Dialog box
 */
public class NoteDialogFragment extends DialogFragment
{
	/**
	 * Argument key for storing/retrieving the contraction id associated with
	 * this dialog
	 */
	public final static String CONTRACTION_ID_ARGUMENT = "com.ianhanniballake.contractiontimer.ContractionId";
	/**
	 * Argument key for storing/retrieving the existing note associated with
	 * this dialog
	 */
	public final static String EXISTING_NOTE_ARGUMENT = "com.ianhanniballake.contractiontimer.ExistingNote";
	/**
	 * Left and right margin to use for the EditText in the dialog
	 */
	private final static float LEFT_RIGHT_MARGIN_DP = 10.0f;

	@Override
	public void onCancel(final DialogInterface dialog)
	{
		Log.d(getClass().getSimpleName(), "Received cancelation event");
		final String existingNote = getArguments().getString(
				EXISTING_NOTE_ARGUMENT);
		GoogleAnalyticsTracker.getInstance().trackEvent("Note", "Cancel",
				existingNote.equals("") ? "Add Note" : "Edit Note", 0);
		super.onCancel(dialog);
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final long contractionId = getArguments().getLong(
				CONTRACTION_ID_ARGUMENT);
		final String existingNote = getArguments().getString(
				EXISTING_NOTE_ARGUMENT);
		final FrameLayout layout = new FrameLayout(getActivity());
		// Get the screen's density scale
		final float scale = getResources().getDisplayMetrics().density;
		// Convert the dps to pixels, based on density scale
		final int leftRightMarginPixels = (int) (LEFT_RIGHT_MARGIN_DP * scale + 0.5f);
		layout.setPadding(leftRightMarginPixels, 0, leftRightMarginPixels, 0);
		final EditText input = new EditText(getActivity());
		input.setText(existingNote);
		layout.addView(input);
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				getActivity());
		if (existingNote.equals(""))
			builder.setTitle(R.string.note_dialog_title_add);
		else
			builder.setTitle(R.string.note_dialog_title_edit);
		return builder
				.setView(layout)
				.setPositiveButton(R.string.note_dialog_save,
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								Log.d(NoteDialogFragment.this.getClass()
										.getSimpleName(),
										"Received positive event");
								GoogleAnalyticsTracker
										.getInstance()
										.trackEvent(
												"Note",
												"Positive",
												existingNote.equals("") ? "Add Note"
														: "Edit Note", 0);
								final Uri updateUri = ContentUris
										.withAppendedId(
												ContractionContract.Contractions.CONTENT_ID_URI_BASE,
												contractionId);
								final ContentValues values = new ContentValues();
								values.put(
										ContractionContract.Contractions.COLUMN_NAME_NOTE,
										input.getText().toString());
								new AsyncQueryHandler(getActivity()
										.getContentResolver())
								{
								}.startUpdate(0, 0, updateUri, values, null,
										null);
							}
						})
				.setNegativeButton(R.string.note_dialog_cancel,
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								Log.d(NoteDialogFragment.this.getClass()
										.getSimpleName(),
										"Received negative event");
								GoogleAnalyticsTracker
										.getInstance()
										.trackEvent(
												"Note",
												"Negative",
												existingNote.equals("") ? "Add Note"
														: "Edit Note", 0);
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
