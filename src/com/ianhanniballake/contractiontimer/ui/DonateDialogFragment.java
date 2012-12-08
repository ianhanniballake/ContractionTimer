package com.ianhanniballake.contractiontimer.ui;

import java.util.ArrayList;
import java.util.Arrays;

import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.BillingController.IConfiguration;
import net.robotmedia.billing.BillingRequest.ResponseCode;
import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.model.Transaction.PurchaseState;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

/**
 * About Dialog for the application
 */
public class DonateDialogFragment extends DialogFragment implements
		IConfiguration
{
	/**
	 * Action associated with this fragment closing
	 */
	public final static String DONATE_CLOSE_ACTION = "com.ianhanniballake.contractiontimer.DONATE_CLOSE";
	/**
	 * Available In-App Purchase Names
	 */
	String[] inAppName;
	/**
	 * Available In-App Purchase SKUs
	 */
	String[] inAppSku;
	private boolean isBillingSupported;
	private AbstractBillingObserver mBillingObserver;

	@Override
	public byte[] getObfuscationSalt()
	{
		return new byte[] { 77, -5, -5, -81, 61, -50, 122, -101, -118, -16,
				-56, 9, 102, 1, 115, 45, 3, 67, 20, -16 };
	}

	@Override
	public String getPublicKey()
	{
		return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApmwDry4kZ8n3DulD1UxcJ89+TRI/DGSvFbhtjNkO1yWki16Q3MzOHwZ4Opyykn3cfiuexMNQYWZfQBqrvkdWWXf+iwBmG6PlOPzgYHV/0ohQhADCUb71SPihmf2WX2zejyNt71sMMUuIklB9HgXukO2uspdWYjKy8CkaMSHK+pQZdG2reACtLjgLMIm1tOlU2C7kGbsL+xodGyh29bO/6cn1/IPrnLZVgAfMm3UDGrqrK2PlgRlLZsoVQKvdi2vbQ8e4LH90rYlXrqEHHgRQw4ozXsj0QmaUx2b2EzRu4q17yvKvhmlFzZSShCkAJgPCOLds0A2SBbOAAX15lB8RmQIDAQAB";
	}

	/**
	 * Callback for whether in app billing is supported. Hides the In App
	 * Billing section if it is not supported
	 * 
	 * @param supported
	 *            Whether in app billing is supported
	 */
	protected void onBillingChecked(final boolean supported)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Billing supported: " + supported);
		isBillingSupported = supported;
		final Dialog dialog = getDialog();
		if (dialog != null)
		{
			final RelativeLayout inAppLayout = (RelativeLayout) dialog
					.findViewById(R.id.in_app_layout);
			if (isBillingSupported)
				inAppLayout.setVisibility(View.VISIBLE);
			else
				inAppLayout.setVisibility(View.GONE);
		}
	}

	@Override
	public void onCancel(final DialogInterface dialog)
	{
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Received cancelation event");
		EasyTracker.getTracker().trackEvent("Donate", "Cancel", "", 0L);
		super.onCancel(dialog);
	}

	@Override
	public void onCreate(final android.os.Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (BuildConfig.DEBUG)
		{
			final ArrayList<String> names = new ArrayList<String>();
			names.add("android.test.purchased");
			names.add("android.test.canceled");
			names.add("android.test.refunded");
			names.add("android.test.item_unavailable");
			names.addAll(Arrays.asList(getResources().getStringArray(
					R.array.donate_in_app_name_array)));
			inAppName = names.toArray(new String[names.size()]);
			final ArrayList<String> skus = new ArrayList<String>();
			skus.add("android.test.purchased");
			skus.add("android.test.canceled");
			skus.add("android.test.refunded");
			skus.add("android.test.item_unavailable");
			skus.addAll(Arrays.asList(getResources().getStringArray(
					R.array.donate_in_app_sku_array)));
			inAppSku = skus.toArray(new String[skus.size()]);
		}
		else
		{
			inAppName = getResources().getStringArray(
					R.array.donate_in_app_name_array);
			inAppSku = getResources().getStringArray(
					R.array.donate_in_app_sku_array);
		}
		mBillingObserver = new AbstractBillingObserver(getActivity())
		{
			@Override
			public void onBillingChecked(final boolean supported)
			{
				DonateDialogFragment.this.onBillingChecked(supported);
			}

			@Override
			public void onPurchaseStateChanged(final String itemId,
					final PurchaseState state)
			{
				switch (state)
				{
					case CANCELLED:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Canceled");
						EasyTracker.getTracker().trackEvent("Donate",
								"Canceled", itemId, 0L);
						break;
					case EXPIRED:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Expired");
						EasyTracker.getTracker().trackEvent("Donate",
								"Expired", itemId, 0L);
						break;
					case PURCHASED:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Purchased");
						EasyTracker.getTracker().trackEvent("Donate",
								"Purchased", itemId, 0L);
						break;
					case REFUNDED:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Refunded");
						EasyTracker.getTracker().trackEvent("Donate",
								"Refunded", itemId, 0L);
						break;
					default:
						break;
				}
				// Dismiss the dialog
				final Dialog dialog = getDialog();
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
			}

			@Override
			public void onRequestPurchaseResponse(final String itemId,
					final ResponseCode response)
			{
				switch (response)
				{
					case RESULT_BILLING_UNAVAILABLE:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Billing Unavailable");
						EasyTracker.getTracker().trackEvent("Donate",
								"Billing Unavilable", itemId, 0L);
						break;
					case RESULT_DEVELOPER_ERROR:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Developer Error");
						EasyTracker.getTracker().trackEvent("Donate",
								"Developer Error", itemId, 0L);
						break;
					case RESULT_ERROR:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Result Error");
						EasyTracker.getTracker().trackEvent("Donate",
								"Result Error", itemId, 0L);
						break;
					case RESULT_ITEM_UNAVAILABLE:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Item Unavailable");
						EasyTracker.getTracker().trackEvent("Donate",
								"Item Unavailable", itemId, 0L);
						break;
					case RESULT_OK:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Result OK");
						EasyTracker.getTracker().trackEvent("Donate", "OK",
								itemId, 0L);
						break;
					case RESULT_SERVICE_UNAVAILABLE:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "Service Unavailable");
						EasyTracker.getTracker().trackEvent("Donate",
								"Service Unavailable", itemId, 0L);
						break;
					case RESULT_USER_CANCELED:
						if (BuildConfig.DEBUG)
							Log.d(DonateDialogFragment.this.getClass()
									.getSimpleName(), "User Canceled");
						EasyTracker.getTracker().trackEvent("Donate",
								"User Canceled", itemId, 0L);
						break;
					default:
						break;
				}
				// Dismiss the dialog
				final Dialog dialog = getDialog();
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
			}

			@Override
			public void onSubscriptionChecked(final boolean supported)
			{
				// Nothing to do
			}
		};
		BillingController.registerObserver(mBillingObserver);
		BillingController.setDebug(BuildConfig.DEBUG);
		BillingController.setConfiguration(this); // This fragment will provide
													// the public key and salt
		BillingController.checkBillingSupported(getActivity());
		if (!mBillingObserver.isTransactionsRestored())
			BillingController.restoreTransactions(getActivity());
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState)
	{
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View layout = inflater.inflate(R.layout.dialog_donate, null);
		final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.icon)
				.setView(layout)
				.setInverseBackgroundForced(true)
				.setNeutralButton(getText(R.string.close),
						new OnClickListener()
						{
							@Override
							public void onClick(final DialogInterface dialog,
									final int which)
							{
								if (BuildConfig.DEBUG)
									Log.d(DonateDialogFragment.this.getClass()
											.getSimpleName(),
											"Received neutral event");
								EasyTracker.getTracker().trackEvent("Donate",
										"Neutral", "", 0L);
							}
						}).create();
		final Button paypal_button = (Button) layout
				.findViewById(R.id.paypal_button);
		paypal_button.setOnClickListener(new View.OnClickListener()
		{
			/**
			 * Donate button with PayPal by opening browser with defined URL For
			 * possible parameters see:
			 * https://cms.paypal.com/us/cgi-bin/?cmd=_render
			 * -content&content_ID=
			 * developer/e_howto_html_Appx_websitestandard_htmlvariables
			 * 
			 * @param v
			 *            View that was clicked
			 */
			@Override
			public void onClick(final View v)
			{
				if (BuildConfig.DEBUG)
					Log.d(DonateDialogFragment.this.getClass().getSimpleName(),
							"Paypal");
				EasyTracker.getTracker().trackEvent("Donate", "Paypal", "", 0L);
				final Uri.Builder uriBuilder = new Uri.Builder();
				uriBuilder.scheme("https").authority("www.paypal.com")
						.path("cgi-bin/webscr");
				uriBuilder.appendQueryParameter("cmd", "_donations");
				uriBuilder.appendQueryParameter("business",
						"ian.hannibal.lake@gmail.com");
				uriBuilder.appendQueryParameter("lc", "US");
				uriBuilder.appendQueryParameter("item_name",
						"Contraction Timer Donation");
				uriBuilder.appendQueryParameter("no_note", "1");
				uriBuilder.appendQueryParameter("no_shipping", "1");
				uriBuilder.appendQueryParameter("currency_code", "USD");
				final Uri payPalUri = uriBuilder.build();
				// Dismiss the dialog
				alertDialog.dismiss();
				// Start your favorite browser
				final Intent viewIntent = new Intent(Intent.ACTION_VIEW,
						payPalUri);
				startActivity(viewIntent);
			}
		});
		final RelativeLayout inAppLayout = (RelativeLayout) layout
				.findViewById(R.id.in_app_layout);
		if (isBillingSupported)
			inAppLayout.setVisibility(View.VISIBLE);
		else
			inAppLayout.setVisibility(View.GONE);
		final Spinner inAppSpinner = (Spinner) layout
				.findViewById(R.id.donate_in_app_spinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_item, inAppName);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		inAppSpinner.setAdapter(adapter);
		final Button inAppButton = (Button) layout
				.findViewById(R.id.donate__in_app_button);
		inAppButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View v)
			{
				final int selectedInAppAmount = inAppSpinner
						.getSelectedItemPosition();
				final String selectedSku = inAppSku[selectedInAppAmount];
				if (BuildConfig.DEBUG)
					Log.d(DonateDialogFragment.this.getClass().getSimpleName(),
							"Clicked " + selectedSku);
				EasyTracker.getTracker().trackEvent("Donate", "Click",
						selectedSku, 0L);
				BillingController.requestPurchase(getActivity(), selectedSku);
			}
		});
		return alertDialog;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		BillingController.unregisterObserver(mBillingObserver);
		BillingController.setConfiguration(null);
	}

	@Override
	public void onDismiss(final DialogInterface dialog)
	{
		final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
				.getInstance(getActivity());
		localBroadcastManager.sendBroadcast(new Intent(DONATE_CLOSE_ACTION));
		super.onDismiss(dialog);
	}
}
