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
import android.widget.EditText;
import android.widget.FrameLayout;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Reset Confirmation Dialog box
 */
public class NoteDialogFragment extends DialogFragment
{
	/**
	 * Left and right margin to use for the EditText in the dialog
	 */
	private final static float LEFT_RIGHT_MARGIN_DP = 10.0f;
	/**
	 * ID of the contraction to update on save
	 */
	private final long contractionId;
	/**
	 * Existing note to pre-populate the dialog with
	 */
	private final String existingNote;

	/**
	 * Creates a NoteDialogFragment which will update the given contraction
	 * 
	 * @param contractionId
	 *            Contraction to associate note with
	 * @param existingNote
	 *            Contraction's existing note to pre-populate the dialog with
	 */
	public NoteDialogFragment(final long contractionId,
			final String existingNote)
	{
		this.contractionId = contractionId;
		this.existingNote = existingNote;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
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
						}).setNegativeButton(R.string.note_dialog_cancel, null)
				.create();
	}
}
