package com.ianhanniballake.contractiontimer.ui;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment which controls starting and stopping the contraction timer
 */
public class ContractionControlsFragment extends Fragment
{
	/**
	 * Handler for asynchronous inserts/updates of contractions
	 */
	private AsyncQueryHandler contractionQueryHandler;

	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		contractionQueryHandler = new AsyncQueryHandler(getActivity()
				.getContentResolver())
		{
			@Override
			protected void onQueryComplete(final int token,
					final Object cookie, final Cursor cursor)
			{
				if (!cursor.moveToFirst())
					return;
				// There should be exactly one row in this cursor and that row
				// represents the last contraction
				final ContentValues newEndTime = new ContentValues();
				newEndTime.put(
						ContractionContract.Contractions.COLUMN_NAME_END_TIME,
						System.currentTimeMillis());
				final long contractionId = cursor.getLong(cursor
						.getColumnIndex(BaseColumns._ID));
				final Uri updateUri = ContentUris.withAppendedId(
						ContractionContract.Contractions.CONTENT_ID_URI_BASE,
						contractionId);
				// Add the new end time to the last contraction
				contractionQueryHandler.startUpdate(0, 0, updateUri,
						newEndTime, null, null);
			}
		};
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		final View view = inflater.inflate(
				R.layout.fragment_contraction_controls, container, false);
		final ToggleButton toggleContraction = (ToggleButton) view
				.findViewById(R.id.toggleContraction);
		toggleContraction.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				if (toggleContraction.isChecked())
					// Start a new contraction
					contractionQueryHandler.startInsert(0, null,
							ContractionContract.Contractions.CONTENT_URI,
							new ContentValues());
				else
					// Get the latest contraction so we know which contraction
					// to update
					contractionQueryHandler.startQuery(0, null,
							ContractionContract.Contractions.CONTENT_ID_LATEST,
							null, null, null, null);
				Toast.makeText(getActivity(),
						"Clicked: " + toggleContraction.isChecked(),
						Toast.LENGTH_SHORT).show();
			}
		});
		return view;
	}
}
