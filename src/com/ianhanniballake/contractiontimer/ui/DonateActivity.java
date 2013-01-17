package com.ianhanniballake.contractiontimer.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.inappbilling.IabHelper;
import com.ianhanniballake.contractiontimer.inappbilling.IabHelper.QueryInventoryFinishedListener;
import com.ianhanniballake.contractiontimer.inappbilling.IabResult;
import com.ianhanniballake.contractiontimer.inappbilling.Inventory;
import com.ianhanniballake.contractiontimer.inappbilling.Purchase;
import com.ianhanniballake.contractiontimer.inappbilling.SkuDetails;

/**
 * Activity controlling donations, including Paypal and In-App Billing
 */
public class DonateActivity extends ActionBarFragmentActivity implements QueryInventoryFinishedListener,
		IabHelper.OnIabSetupFinishedListener, IabHelper.OnIabPurchaseFinishedListener,
		IabHelper.OnConsumeFinishedListener
{
	/**
	 * In-App Billing Helper
	 */
	IabHelper iabHelper;
	private final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApmwDry4kZ8n3DulD1UxcJ89+TRI/DGSvFbhtjNkO1yWki16Q3MzOHwZ4Opyykn3cfiuexMNQYWZfQBqrvkdWWXf+iwBmG6PlOPzgYHV/0ohQhADCUb71SPihmf2WX2zejyNt71sMMUuIklB9HgXukO2uspdWYjKy8CkaMSHK+pQZdG2reACtLjgLMIm1tOlU2C7kGbsL+xodGyh29bO/6cn1/IPrnLZVgAfMm3UDGrqrK2PlgRlLZsoVQKvdi2vbQ8e4LH90rYlXrqEHHgRQw4ozXsj0QmaUx2b2EzRu4q17yvKvhmlFzZSShCkAJgPCOLds0A2SBbOAAX15lB8RmQIDAQAB";
	private final String PURCHASED_SKU = "com.ianhanniballake.contractiontimer.PURCHASED_SKU";
	/**
	 * Recently purchased SKU, if any. Should be saved in the instance state
	 */
	String purchasedSku = "";
	/**
	 * List of valid SKUs
	 */
	String[] skus;

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
		if (!iabHelper.handleActivityResult(requestCode, resultCode, data))
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onConsumeFinished(final Purchase purchase, final IabResult result)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Consume Completed: " + result.getMessage());
		if (result.isSuccess())
		{
			EasyTracker.getTracker().trackEvent("Donate", "Purchased", purchase.getSku(), 0L);
			Toast.makeText(this, R.string.donate_thank_you, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Set up SKUs
		final ArrayList<String> allSkus = new ArrayList<String>();
		if (BuildConfig.DEBUG)
		{
			allSkus.add("android.test.purchased");
			allSkus.add("android.test.canceled");
			allSkus.add("android.test.refunded");
			allSkus.add("android.test.item_unavailable");
		}
		allSkus.addAll(Arrays.asList(getResources().getStringArray(R.array.donate_in_app_sku_array)));
		skus = allSkus.toArray(new String[allSkus.size()]);
		// Set up the UI
		setContentView(R.layout.activity_donate);
		final Button paypal_button = (Button) findViewById(R.id.paypal_button);
		paypal_button.setOnClickListener(new View.OnClickListener()
		{
			/**
			 * Donate button with PayPal by opening browser with defined URL For possible parameters see:
			 * https://cms.paypal.com/us/cgi-bin/?cmd=_render -content&content_ID=
			 * developer/e_howto_html_Appx_websitestandard_htmlvariables
			 * 
			 * @param v
			 *            View that was clicked
			 */
			@Override
			public void onClick(final View v)
			{
				if (BuildConfig.DEBUG)
					Log.d(DonateActivity.this.getClass().getSimpleName(), "Clicked Paypal");
				EasyTracker.getTracker().trackEvent("Donate", "Paypal", "", 0L);
				final Uri.Builder uriBuilder = new Uri.Builder();
				uriBuilder.scheme("https").authority("www.paypal.com").path("cgi-bin/webscr");
				uriBuilder.appendQueryParameter("cmd", "_donations");
				uriBuilder.appendQueryParameter("business", "ian.hannibal.lake@gmail.com");
				uriBuilder.appendQueryParameter("lc", "US");
				uriBuilder.appendQueryParameter("item_name", "Contraction Timer Donation");
				uriBuilder.appendQueryParameter("no_note", "1");
				uriBuilder.appendQueryParameter("no_shipping", "1");
				uriBuilder.appendQueryParameter("currency_code", "USD");
				final Uri payPalUri = uriBuilder.build();
				// Start your favorite browser
				final Intent viewIntent = new Intent(Intent.ACTION_VIEW, payPalUri);
				startActivity(viewIntent);
				// Close this activity
				finish();
			}
		});
		final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
		final Button inAppButton = (Button) findViewById(R.id.donate__in_app_button);
		inAppButton.setOnClickListener(new View.OnClickListener()
		{
			private final int RC_REQUEST = 1;

			@Override
			public void onClick(final View v)
			{
				final int selectedInAppAmount = inAppSpinner.getSelectedItemPosition();
				purchasedSku = skus[selectedInAppAmount];
				if (BuildConfig.DEBUG)
					Log.d(DonateActivity.this.getClass().getSimpleName(), "Clicked " + purchasedSku);
				EasyTracker.getTracker().trackEvent("Donate", "Click", purchasedSku, 0L);
				iabHelper.launchPurchaseFlow(DonateActivity.this, purchasedSku, RC_REQUEST, DonateActivity.this);
			}
		});
		// Start the In-App Billing process, only if on Froyo or higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
		{
			iabHelper = new IabHelper(this, publicKey);
			iabHelper.enableDebugLogging(BuildConfig.DEBUG);
			iabHelper.startSetup(this);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (iabHelper != null)
			iabHelper.dispose();
		iabHelper = null;
	}

	/**
	 * Called when a purchase is completed. Immediately attempts to consume the donation to ready for the next donation
	 */
	@Override
	public void onIabPurchaseFinished(final IabResult result, final Purchase info)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Purchase Completed: " + result.getMessage());
		switch (result.getResponse())
		{
			case IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED:
				EasyTracker.getTracker().trackEvent("Donate", "Canceled", purchasedSku, 0L);
				break;
			default:
				EasyTracker.getTracker().trackEvent("Donate", "Error", purchasedSku, 0L);
				break;
		}
		if (result.isSuccess())
			iabHelper.consumeAsync(info, this);
	}

	@Override
	public void onIabSetupFinished(final IabResult result)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Billing supported: " + result.getMessage());
		if (result.isSuccess())
			iabHelper.queryInventoryAsync(true, Arrays.asList(skus), DonateActivity.this);
		// In-App Billing UI is hidden by default, so nothing to do if it wasn't
		// successful
	}

	@Override
	public void onQueryInventoryFinished(final IabResult result, final Inventory inv)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Inventory Returned: " + result.getMessage() + ": " + inv);
		// If we failed to get the inventory, then leave the in-app billing UI
		// hidden
		if (result.isFailure())
			return;
		// Make sure we've consumed any previous purchases
		final List<Purchase> purchases = inv.getAllPurchases();
		if (!purchases.isEmpty())
			iabHelper.consumeAsync(purchases, null);
		final int offset = BuildConfig.DEBUG ? 4 : 0;
		final String[] inAppName = new String[offset + skus.length];
		if (BuildConfig.DEBUG)
		{
			inAppName[0] = "android.test.purchased";
			inAppName[1] = "android.test.canceled";
			inAppName[2] = "android.test.refunded";
			inAppName[3] = "android.test.item_unavailable";
		}
		for (int h = 0; h < skus.length; h++)
		{
			final String currentSku = skus[h];
			final SkuDetails sku = inv.getSkuDetails(currentSku);
			inAppName[h + offset] = sku.getDescription() + " (" + sku.getPrice() + ")";
		}
		final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				inAppName);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		inAppSpinner.setAdapter(adapter);
		// And finally show the In-App Billing UI
		final RelativeLayout inAppLayout = (RelativeLayout) findViewById(R.id.in_app_layout);
		inAppLayout.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		purchasedSku = savedInstanceState.containsKey(PURCHASED_SKU) ? savedInstanceState.getString(PURCHASED_SKU) : "";
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
				.getBoolean(R.bool.pref_settings_lock_portrait_default));
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Lock Portrait: " + isLockPortrait);
		if (isLockPortrait)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(PURCHASED_SKU, purchasedSku);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		getActionBarHelper().setDisplayHomeAsUpEnabled(true);
		EasyTracker.getInstance().activityStart(this);
		EasyTracker.getTracker().trackView("Donate");
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}
}
