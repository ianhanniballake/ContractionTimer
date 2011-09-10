package com.ianhanniballake.contractiontimer.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ianhanniballake.contractiontimer.R;

/**
 * Fragment which controls starting and stopping the contraction timer
 */
public class ContractionControlsFragment extends Fragment
{
	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_contraction_controls,
				container, false);
	}
}
