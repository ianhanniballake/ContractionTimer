package com.ianhanniballake.contractiontimer.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ianhanniballake.contractiontimer.R;

/**
 * Main Activity for managing contractions
 */
public class MainActivity extends FragmentActivity
{
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
}