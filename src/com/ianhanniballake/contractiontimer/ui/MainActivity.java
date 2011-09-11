package com.ianhanniballake.contractiontimer.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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

	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		new MenuInflater(this).inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_reset:
				final ResetDialogFragment resetDialogFragment = new ResetDialogFragment();
				resetDialogFragment.show(getSupportFragmentManager(), "reset");
				return true;
			case R.id.menu_about:
				final AboutDialogFragment aboutDialogFragment = new AboutDialogFragment();
				aboutDialogFragment.show(getSupportFragmentManager(), "about");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}