package com.ianhanniballake.contractiontimer.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.AbsListView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Fragment to list contractions entered by the user
 */
public class ContractionListFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<Cursor>
{
	/**
	 * Adapter to display the list's data
	 */
	private SimpleCursorAdapter adapter;

	@Override
	public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		adapter = new SimpleCursorAdapter(
				getActivity(),
				R.layout.list_item_contraction,
				null,
				new String[] {
						ContractionContract.Contractions.COLUMN_NAME_START_TIME,
						ContractionContract.Contractions.COLUMN_NAME_END_TIME },
				new int[] { R.id.start_time, R.id.end_time }, 0);
		setListAdapter(adapter);
		getListView().setChoiceMode(AbsListView.CHOICE_MODE_NONE);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args)
	{
		return new CursorLoader(getActivity(),
				ContractionContract.Contractions.CONTENT_ID_URI_BASE, null,
				null, null, null);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data)
	{
		adapter.swapCursor(data);
	}
}
