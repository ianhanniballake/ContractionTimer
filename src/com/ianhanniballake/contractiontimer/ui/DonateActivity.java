package com.ianhanniballake.contractiontimer.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.acra.ACRA;
import org.json.JSONException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Item;
import com.google.analytics.tracking.android.Transaction;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.actionbar.ActionBarFragmentActivity;
import com.ianhanniballake.contractiontimer.inappbilling.IabException;
import com.ianhanniballake.contractiontimer.inappbilling.Inventory;
import com.ianhanniballake.contractiontimer.inappbilling.Purchase;
import com.ianhanniballake.contractiontimer.inappbilling.Security;
import com.ianhanniballake.contractiontimer.inappbilling.SkuDetails;

/**
 * Activity controlling donations, including Paypal and In-App Billing
 */
public class DonateActivity extends ActionBarFragmentActivity
{
	private class ConsumeAsyncTask extends AsyncTask<Purchase, Void, List<Purchase>>
	{
		private final boolean finishActivity;

		ConsumeAsyncTask(final boolean finishActivity)
		{
			this.finishActivity = finishActivity;
		}

		@Override
		protected List<Purchase> doInBackground(final Purchase... purchases)
		{
			final List<Purchase> consumedPurchases = new ArrayList<Purchase>();
			for (final Purchase purchase : purchases)
				try
				{
					final String token = purchase.getToken();
					if (TextUtils.isEmpty(token))
					{
						Log.e(DonateActivity.class.getSimpleName(), "Invalid consume token: " + token);
						EasyTracker.getTracker().trackEvent("Donate", "Invalid consume", purchase.getSku(), -1L);
					}
					final int response = mService.consumePurchase(3, getPackageName(), token);
					if (response == 0)
						consumedPurchases.add(purchase);
					else
						EasyTracker.getTracker().trackEvent("Donate", "Bad consume response", purchase.getSku(), -1L);
				} catch (final RemoteException e)
				{
					Log.e(DonateActivity.class.getSimpleName(), "Error consuming " + purchase.getSku(), e);
					EasyTracker.getTracker().trackEvent("Donate", "Remote Exception consume", purchase.getSku(), -1L);
				}
			return consumedPurchases;
		}

		@Override
		protected void onPostExecute(final List<Purchase> result)
		{
			if (result == null || result.isEmpty())
				return;
			for (final Purchase purchase : result)
			{
				EasyTracker.getTracker().trackEvent("Donate", "Purchased", purchasedSku, 0L);
				final long purchasedPriceMicro = skuPrices.containsKey(purchasedSku) ? skuPrices.get(purchasedSku)
						.longValue() : 0;
				final String purchasedName = skuNames.containsKey(purchasedSku) ? skuNames.get(purchasedSku)
						: purchasedSku;
				final Transaction transaction = new Transaction.Builder(purchase.getOrderId(), purchasedPriceMicro)
						.setAffiliation("Google Play").build();
				transaction.addItem(new Item.Builder(purchasedSku, purchasedName, purchasedPriceMicro, 1L)
						.setProductCategory("Donation").build());
				EasyTracker.getTracker().trackTransaction(transaction);
			}
			if (finishActivity)
			{
				Toast.makeText(DonateActivity.this, R.string.donate_thank_you, Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	private class InventoryQueryAsyncTask extends AsyncTask<String, Void, Inventory>
	{
		public InventoryQueryAsyncTask()
		{
		}

		@Override
		protected Inventory doInBackground(final String... moreSkus)
		{
			try
			{
				final Inventory inv = new Inventory();
				int r = queryPurchases(inv);
				if (r != 0)
					throw new IabException(r, "Error refreshing inventory (querying owned items).");
				r = querySkuDetails(inv, moreSkus);
				if (r != 0)
					throw new IabException(r, "Error refreshing inventory (querying prices of items).");
				return inv;
			} catch (final RemoteException e)
			{
				Log.e(DonateActivity.class.getSimpleName(), "Error loading inventory", e);
				EasyTracker.getTracker().trackEvent("Donate", "Remote Exception inventory", purchasedSku, -1L);
			} catch (final JSONException e)
			{
				Log.e(DonateActivity.class.getSimpleName(), "Error parsing inventory", e);
				EasyTracker.getTracker().trackEvent("Donate", "Bad inventory response", purchasedSku, -1L);
			} catch (final IabException e)
			{
				Log.e(DonateActivity.class.getSimpleName(), "Error parsing inventory", e);
				EasyTracker.getTracker().trackEvent("Donate", "Error loading inventory", purchasedSku, -1L);
			}
			return null;
		}

		@Override
		protected void onPostExecute(final Inventory inv)
		{
			if (BuildConfig.DEBUG)
				Log.d(getClass().getSimpleName(), "Inventory Returned: " + inv);
			// If we failed to get the inventory, then leave the in-app billing UI hidden
			if (inv == null)
				return;
			// Make sure we've consumed any previous purchases
			final List<Purchase> purchases = inv.getAllPurchases();
			if (!purchases.isEmpty())
				new ConsumeAsyncTask(false).execute(purchases.toArray(new Purchase[0]));
			final String[] inAppName = new String[skus.length];
			for (int h = 0; h < skus.length; h++)
			{
				final String currentSku = skus[h];
				final SkuDetails sku = inv.getSkuDetails(currentSku);
				skuNames.put(currentSku, sku.getTitle());
				inAppName[h] = sku.getDescription() + " (" + sku.getPrice() + ")";
			}
			final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>(DonateActivity.this,
					android.R.layout.simple_spinner_item, inAppName);
			// Specify the layout to use when the list of choices appears
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Apply the adapter to the spinner
			inAppSpinner.setAdapter(adapter);
			// And finally show the In-App Billing UI
			final RelativeLayout inAppLayout = (RelativeLayout) findViewById(R.id.in_app_layout);
			inAppLayout.setVisibility(View.VISIBLE);
		}

		int queryPurchases(final Inventory inv) throws JSONException, RemoteException
		{
			// Query purchases
			boolean verificationFailed = false;
			String continueToken = null;
			do
			{
				final Bundle ownedItems = mService.getPurchases(3, getPackageName(), ITEM_TYPE_INAPP, continueToken);
				final int response = getResponseCodeFromBundle(ownedItems);
				if (response != 0)
				{
					EasyTracker.getTracker().trackEvent("Donate", "Bad purchases response", "", -1L);
					return response;
				}
				if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
						|| !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
						|| !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST))
				{
					EasyTracker.getTracker().trackEvent("Donate", "Bad purchases response", "", -1L);
					return -1;
				}
				final ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
				final ArrayList<String> purchaseDataList = ownedItems
						.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
				final ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);
				for (int i = 0; i < purchaseDataList.size(); ++i)
				{
					final String purchaseData = purchaseDataList.get(i);
					final String signature = signatureList.get(i);
					ownedSkus.get(i);
					if (Security.verifyPurchase(publicKey, purchaseData, signature))
					{
						final Purchase purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, signature);
						// Record ownership and token
						inv.addPurchase(purchase);
					}
					else
						verificationFailed = true;
				}
				continueToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
			} while (!TextUtils.isEmpty(continueToken));
			return verificationFailed ? -1 : 0;
		}

		int querySkuDetails(final Inventory inv, final String[] moreSkus) throws RemoteException, JSONException
		{
			final ArrayList<String> skuList = new ArrayList<String>();
			skuList.addAll(inv.getAllOwnedSkus(ITEM_TYPE_INAPP));
			if (moreSkus != null)
				skuList.addAll(Arrays.asList(moreSkus));
			if (skuList.size() == 0)
				return 0;
			final Bundle querySkus = new Bundle();
			querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
			final Bundle skuDetails = mService.getSkuDetails(3, getPackageName(), ITEM_TYPE_INAPP, querySkus);
			if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST))
			{
				final int response = getResponseCodeFromBundle(skuDetails);
				if (response != 0)
				{
					EasyTracker.getTracker().trackEvent("Donate", "Bad sku details response", "", -1L);
					return response;
				}
				return -1;
			}
			final ArrayList<String> responseList = skuDetails.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
			for (final String thisResponse : responseList)
			{
				final SkuDetails d = new SkuDetails(ITEM_TYPE_INAPP, thisResponse);
				inv.addSkuDetails(d);
			}
			return 0;
		}
	}

	private final static String ITEM_TYPE_INAPP = "inapp";
	private static final String RESPONSE_CODE = "RESPONSE_CODE";
	private static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
	private static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
	private static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
	private static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";

	// Workaround to bug where sometimes response codes come as Long instead of Integer
	static int getResponseCodeFromBundle(final Bundle b)
	{
		final Object o = b.get(RESPONSE_CODE);
		if (o == null)
			return 0;
		else if (o instanceof Integer)
			return ((Integer) o).intValue();
		else if (o instanceof Long)
			return (int) ((Long) o).longValue();
		else
			throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
	}

	// Workaround to bug where sometimes response codes come as Long instead of Integer
	static int getResponseCodeFromIntent(final Intent i)
	{
		final Object o = i.getExtras().get(RESPONSE_CODE);
		if (o == null)
			return 0;
		else if (o instanceof Integer)
			return ((Integer) o).intValue();
		else if (o instanceof Long)
			return (int) ((Long) o).longValue();
		else
			throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
	}

	IInAppBillingService mService;
	private ServiceConnection mServiceConn;
	private final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApmwDry4kZ8n3DulD1UxcJ89+TRI/DGSvFbhtjNkO1yWki16Q3MzOHwZ4Opyykn3cfiuexMNQYWZfQBqrvkdWWXf+iwBmG6PlOPzgYHV/0ohQhADCUb71SPihmf2WX2zejyNt71sMMUuIklB9HgXukO2uspdWYjKy8CkaMSHK+pQZdG2reACtLjgLMIm1tOlU2C7kGbsL+xodGyh29bO/6cn1/IPrnLZVgAfMm3UDGrqrK2PlgRlLZsoVQKvdi2vbQ8e4LH90rYlXrqEHHgRQw4ozXsj0QmaUx2b2EzRu4q17yvKvhmlFzZSShCkAJgPCOLds0A2SBbOAAX15lB8RmQIDAQAB";
	private final String PURCHASED_SKU = "com.ianhanniballake.contractiontimer.PURCHASED_SKU";
	/**
	 * Recently purchased SKU, if any. Should be saved in the instance state
	 */
	String purchasedSku = "";
	private final int RC_REQUEST = 1;
	/**
	 * SKU Product Names
	 */
	HashMap<String, String> skuNames = new HashMap<String, String>();
	/**
	 * US Prices for SKUs in micro-currency
	 */
	HashMap<String, Long> skuPrices = new HashMap<String, Long>();
	/**
	 * List of valid SKUs
	 */
	String[] skus = new String[0];

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
		if (requestCode != RC_REQUEST)
		{
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		if (data == null)
		{
			EasyTracker.getTracker().trackEvent("Donate", "Bad purchase response", purchasedSku, -1L);
			return;
		}
		final int responseCode = getResponseCodeFromIntent(data);
		final String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
		final String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
		if (resultCode == Activity.RESULT_OK && responseCode == 0)
		{
			if (purchaseData == null || dataSignature == null)
			{
				EasyTracker.getTracker().trackEvent("Donate", "Invalid purchase response", purchasedSku, -1L);
				return;
			}
			Purchase purchase = null;
			try
			{
				purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, dataSignature);
				final String sku = purchase.getSku();
				// Verify signature
				if (!Security.verifyPurchase(publicKey, purchaseData, dataSignature))
				{
					EasyTracker.getTracker().trackEvent("Donate", "Signature verification failed", sku, -1L);
					return;
				}
			} catch (final JSONException e)
			{
				EasyTracker.getTracker().trackEvent("Donate", "Bad purchase response", purchasedSku, -1L);
				return;
			}
			new ConsumeAsyncTask(false).execute(purchase);
		}
		else if (resultCode == Activity.RESULT_OK)
			EasyTracker.getTracker().trackEvent("Donate", "Purchase error " + responseCode, purchasedSku, -1L);
		else if (resultCode == Activity.RESULT_CANCELED)
			EasyTracker.getTracker().trackEvent("Donate", "Canceled", purchasedSku, 0L);
		else
			EasyTracker.getTracker().trackEvent("Donate", "Unknown purchase response", purchasedSku, -1L);
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
		final String[] skuArray = getResources().getStringArray(R.array.donate_in_app_sku_array);
		allSkus.addAll(Arrays.asList(skuArray));
		skus = allSkus.toArray(new String[allSkus.size()]);
		final int[] skuPriceArray = getResources().getIntArray(R.array.donate_in_app_price_array);
		for (int h = 0; h < skuPriceArray.length; h++)
			skuPrices.put(skuArray[h], (long) skuPriceArray[h]);
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
		final Button inAppButton = (Button) findViewById(R.id.donate__in_app_button);
		inAppButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
				final int selectedInAppAmount = inAppSpinner.getSelectedItemPosition();
				purchasedSku = skus[selectedInAppAmount];
				if (BuildConfig.DEBUG)
					Log.d(DonateActivity.this.getClass().getSimpleName(), "Clicked " + purchasedSku);
				EasyTracker.getTracker().trackEvent("Donate", "Click", purchasedSku, 0L);
				try
				{
					final Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), purchasedSku,
							ITEM_TYPE_INAPP, "");
					final int response = getResponseCodeFromBundle(buyIntentBundle);
					if (response != 0)
					{
						EasyTracker.getTracker().trackEvent("Donate", "Purchase error " + response, purchasedSku, -1L);
						return;
					}
					final PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
					startIntentSenderForResult(pendingIntent.getIntentSender(), RC_REQUEST, new Intent(),
							Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
				} catch (final SendIntentException e)
				{
					Log.e(DonateActivity.class.getSimpleName(), "Failed to send intent", e);
					EasyTracker.getTracker().trackEvent("Donate", "Failed to send intent", purchasedSku, -1L);
				} catch (final RemoteException e)
				{
					Log.e(DonateActivity.class.getSimpleName(), "Failed to send intent", e);
					EasyTracker.getTracker()
							.trackEvent("Donate", "Remote Exception during purchase", purchasedSku, -1L);
				}
			}
		});
		// Start the In-App Billing process, only if on Froyo or higher
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
		{
			mServiceConn = new ServiceConnection()
			{
				@Override
				public void onServiceConnected(final ComponentName name, final IBinder service)
				{
					mService = IInAppBillingService.Stub.asInterface(service);
					final String packageName = getPackageName();
					try
					{
						// check for in-app billing v3 support
						final int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
						if (response == 0)
							new InventoryQueryAsyncTask().execute(skus);
						else
							EasyTracker.getTracker().trackEvent("Donate", "In app not supported", "", -1L);
					} catch (final RemoteException e)
					{
						EasyTracker.getTracker()
								.trackEvent("Donate", "Remote exception during initialization", "", -1L);
						return;
					}
				}

				@Override
				public void onServiceDisconnected(final ComponentName name)
				{
					mService = null;
				}
			};
			final Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
			if (!getPackageManager().queryIntentServices(serviceIntent, 0).isEmpty())
				// service available to handle that Intent
				bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
			else
				// no service available to handle that Intent
				EasyTracker.getTracker().trackEvent("Donate", "Billing Unavailable", "", -1L);
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (mServiceConn != null)
		{
			try
			{
				unbindService(mServiceConn);
			} catch (final IllegalArgumentException e)
			{
				// Assume the service has already been unbinded, so only log that it happened
				EasyTracker.getTracker().trackException(Thread.currentThread().getName(), e, false);
				if (!BuildConfig.DEBUG)
					ACRA.getErrorReporter().handleSilentException(e);
			}
			mServiceConn = null;
			mService = null;
		}
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
