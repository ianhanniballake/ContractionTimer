package com.ianhanniballake.contractiontimer.ui;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

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
				new AlertDialog.Builder(this)
						.setTitle(R.string.reset_dialog_title)
						.setMessage(R.string.reset_dialog_message)
						.setPositiveButton(R.string.reset_dialog_confirm,
								new OnClickListener()
								{
									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which)
									{
										new AsyncQueryHandler(
												getContentResolver())
										{
										}.startDelete(
												0,
												0,
												ContractionContract.Contractions.CONTENT_URI,
												null, null);
									}
								})
						.setNegativeButton(R.string.reset_dialog_cancel, null)
						.show();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}