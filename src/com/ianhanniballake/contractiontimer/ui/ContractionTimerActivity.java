package com.ianhanniballake.contractiontimer.ui;

import android.app.Activity;
import android.os.Bundle;

import com.ianhanniballake.contractiontimer.R;

/**
 * Main Activity for managing contractions
 */
public class ContractionTimerActivity extends Activity
{
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
}