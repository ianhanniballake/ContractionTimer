package com.ianhanniballake.contractiontimer.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.inappbilling.Inventory;
import com.ianhanniballake.contractiontimer.inappbilling.Purchase;
import com.ianhanniballake.contractiontimer.inappbilling.Security;
import com.ianhanniballake.contractiontimer.inappbilling.SkuDetails;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Activity controlling donations, including Paypal and In-App Billing
 */
public class DonateActivity extends AppCompatActivity {
    private final static String TAG = DonateActivity.class.getSimpleName();
    private final static String ITEM_TYPE_INAPP = "inapp";
    private final static String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApmwDry4kZ8n3DulD1UxcJ89+TRI/DGSvFbhtjNkO1yWki16Q3MzOHwZ4Opyykn3cfiuexMNQYWZfQBqrvkdWWXf+iwBmG6PlOPzgYHV/0ohQhADCUb71SPihmf2WX2zejyNt71sMMUuIklB9HgXukO2uspdWYjKy8CkaMSHK+pQZdG2reACtLjgLMIm1tOlU2C7kGbsL+xodGyh29bO/6cn1/IPrnLZVgAfMm3UDGrqrK2PlgRlLZsoVQKvdi2vbQ8e4LH90rYlXrqEHHgRQw4ozXsj0QmaUx2b2EzRu4q17yvKvhmlFzZSShCkAJgPCOLds0A2SBbOAAX15lB8RmQIDAQAB";
    private final static String PURCHASED_SKU = "com.ianhanniballake.contractiontimer.PURCHASED_SKU";
    private final static int RC_REQUEST = 1;
    private static final String RESPONSE_CODE = "RESPONSE_CODE";
    private static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    private static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    private static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    private static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    /**
     * SKU Product Names
     */
    final HashMap<String, String> skuNames = new HashMap<>();
    /**
     * US Prices for SKUs in micro-currency
     */
    final HashMap<String, Long> skuPrices = new HashMap<>();
    /**
     * InAppBillingService connection
     */
    IInAppBillingService mService;
    /**
     * Recently purchased SKU, if any. Should be saved in the instance state
     */
    String purchasedSku = "";
    /**
     * List of valid SKUs
     */
    String[] skus = new String[0];
    private ServiceConnection mServiceConn;

    /**
     * Gets the response code from the given Bundle. Workaround to bug where sometimes response codes come as Long
     * instead of Integer
     *
     * @param b Bundle to get response code
     * @return response code
     */
    static int getResponseCodeFromBundle(final Bundle b) {
        final Object o = b.get(RESPONSE_CODE);
        if (o == null)
            return 0;
        else if (o instanceof Integer)
            return (Integer) o;
        else if (o instanceof Long)
            return ((Long) o).intValue();
        else
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
    }

    /**
     * Gets the response code from the given Intent. Workaround to bug where sometimes response codes come as Long
     * instead of Integer
     *
     * @param i Intent to get response code
     * @return response code
     */
    static int getResponseCodeFromIntent(final Intent i) {
        final Object o = i.getExtras() == null ? null : i.getExtras().get(RESPONSE_CODE);
        if (o == null)
            return 0;
        else if (o instanceof Integer)
            return (Integer) o;
        else if (o instanceof Long)
            return ((Long) o).intValue();
        else
            throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");
        if (requestCode != RC_REQUEST) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (data == null) {
            FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: null intent");
            return;
        }
        final int responseCode = getResponseCodeFromIntent(data);
        final String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
        final String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
        if (resultCode == Activity.RESULT_OK && responseCode == 0) {
            if (purchaseData == null || dataSignature == null) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: Invalid data fields");
                return;
            }
            Purchase purchase;
            try {
                purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, dataSignature);
                final String sku = purchase.getSku();
                // Verify signature
                if (!Security.verifyPurchase(publicKey, purchaseData, dataSignature)) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: Signature verification failed " + sku);
                    return;
                }
            } catch (final JSONException e) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: Parsing error");
                FirebaseCrash.report(e);
                return;
            }
            new ConsumeAsyncTask(mService, true).execute(purchase);
        } else if (resultCode == Activity.RESULT_OK) {
            FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: bad response " + responseCode);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Purchase: canceled");
        } else {
            FirebaseCrash.logcat(Log.ERROR, TAG, "Purchase: Unknown response");
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up SKUs
        final ArrayList<String> allSkus = new ArrayList<>();
        if (BuildConfig.DEBUG) {
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
        paypal_button.setOnClickListener(new View.OnClickListener() {
            /**
             * Donate button with PayPal by opening browser with defined URL For possible parameters see:
             * https://cms.paypal.com/us/cgi-bin/?cmd=_render -content&content_ID=
             * developer/e_howto_html_Appx_websitestandard_htmlvariables
             *
             * @param v
             *            View that was clicked
             */
            @Override
            public void onClick(final View v) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Clicked Paypal");
                FirebaseAnalytics.getInstance(DonateActivity.this).logEvent("paypal", null);
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
        inAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
                final int selectedInAppAmount = inAppSpinner.getSelectedItemPosition();
                if (selectedInAppAmount == AdapterView.INVALID_POSITION) {
                    return;
                }
                purchasedSku = skus[selectedInAppAmount];
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Clicked " + purchasedSku);
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, purchasedSku);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                        inAppSpinner.getItemAtPosition(selectedInAppAmount).toString());
                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate");
                FirebaseAnalytics.getInstance(DonateActivity.this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
                try {
                    final Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), purchasedSku,
                            ITEM_TYPE_INAPP, "");
                    final int response = getResponseCodeFromBundle(buyIntentBundle);
                    if (response != 0) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "Buy bad response " + response);
                        return;
                    }
                    final PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(pendingIntent.getIntentSender(), RC_REQUEST, new Intent(), 0, 0, 0);
                } catch (final SendIntentException e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Buy: Send intent failed");
                    FirebaseCrash.report(e);
                } catch (final RemoteException e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Buy: Remote exception");
                    FirebaseCrash.report(e);
                }
            }
        });
        // Start the In-App Billing process, only if on Froyo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mServiceConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final IBinder service) {
                    mService = IInAppBillingService.Stub.asInterface(service);
                    final String packageName = getPackageName();
                    try {
                        // check for in-app billing v3 support
                        final int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                        if (response == 0)
                            new InventoryQueryAsyncTask(mService).execute(skus);
                        else {
                            FirebaseCrash.logcat(Log.WARN, TAG, "Initialize: In app not supported");
                        }
                    } catch (final RemoteException e) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "Initialize: Remote exception");
                        FirebaseCrash.report(e);
                    }
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    mService = null;
                }
            };
            final Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            serviceIntent.setPackage("com.android.vending");
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> services = packageManager != null
                    ? packageManager.queryIntentServices(serviceIntent, 0)
                    : null;
            if (services != null && !services.isEmpty())
                // service available to handle that Intent
                bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            else {
                // no service available to handle that Intent
                FirebaseCrash.logcat(Log.WARN, TAG, "Initialize: Billing unavailable");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConn != null) {
            try {
                unbindService(mServiceConn);
            } catch (final IllegalArgumentException e) {
                FirebaseCrash.logcat(Log.WARN, TAG, "Error unbinding service");
                FirebaseCrash.report(e);
            }
            mServiceConn = null;
            mService = null;
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        purchasedSku = savedInstanceState.containsKey(PURCHASED_SKU) ? savedInstanceState.getString(PURCHASED_SKU) : "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_lock_portrait_default));
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PURCHASED_SKU, purchasedSku);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Donate selected home");
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConsumeAsyncTask extends AsyncTask<Purchase, Void, List<Purchase>> {
        private final boolean finishActivity;
        private final WeakReference<IInAppBillingService> mBillingService;

        ConsumeAsyncTask(final IInAppBillingService service, final boolean finishActivity) {
            mBillingService = new WeakReference<>(service);
            this.finishActivity = finishActivity;
        }

        @Override
        protected List<Purchase> doInBackground(final Purchase... purchases) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting Consume of " + Arrays.toString(purchases));
            final List<Purchase> consumedPurchases = new ArrayList<>();
            for (final Purchase purchase : purchases) {
                final String sku = purchase.getSku();
                try {
                    final String token = purchase.getToken();
                    if (TextUtils.isEmpty(token)) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "Consume: Invalid token " + token);
                        break;
                    }
                    final IInAppBillingService service = mBillingService.get();
                    if (service == null) {
                        Log.w(TAG, "Consume: Billing service is null");
                        break;
                    }
                    final int response = service.consumePurchase(3, getPackageName(), token);
                    if (response == 0)
                        consumedPurchases.add(purchase);
                    else {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "Consume: Bad response " + response);
                    }
                } catch (final RemoteException e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Consume: Remote exception " + sku);
                    FirebaseCrash.report(e);
                }
            }
            return consumedPurchases;
        }

        @Override
        protected void onPostExecute(final List<Purchase> result) {
            if (result == null || result.isEmpty()) {
                Log.w(TAG, "Consume: No purchases consumed");
                return;
            }
            for (final Purchase purchase : result) {
                final String sku = purchase.getSku();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Consume completed successfully " + sku);
            }
            Toast.makeText(DonateActivity.this, R.string.donate_thank_you, Toast.LENGTH_LONG).show();
            if (finishActivity) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Finishing Donate Activity");
                finish();
            }
        }
    }

    private class InventoryQueryAsyncTask extends AsyncTask<String, Void, Inventory> {
        private final WeakReference<IInAppBillingService> mBillingService;

        InventoryQueryAsyncTask(final IInAppBillingService service) {
            mBillingService = new WeakReference<>(service);
        }

        @Override
        protected Inventory doInBackground(final String... moreSkus) {
            try {
                final Inventory inv = new Inventory();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Starting query inventory");
                int r = queryPurchases(inv);
                if (r != 0)
                    return null;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Starting sku details query");
                r = querySkuDetails(inv, moreSkus);
                if (r != 0)
                    return null;
                return inv;
            } catch (final RemoteException e) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Inventory: Remote exception");
                FirebaseCrash.report(e);
            } catch (final JSONException e) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Inventory: Parsing error");
                FirebaseCrash.report(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Inventory inv) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Inventory Returned: " + inv);
            // If we failed to get the inventory, then leave the in-app billing UI hidden
            if (inv == null)
                return;
            // Make sure we've consumed any previous purchases
            final List<Purchase> purchases = inv.getAllPurchases();
            if (!purchases.isEmpty()) {
                final IInAppBillingService service = mBillingService.get();
                if (service != null)
                    new ConsumeAsyncTask(service, false).execute(purchases.toArray(new Purchase[purchases.size()]));
                else
                    Log.w(TAG, "Inventory: Billing service is null");
            }
            ArrayList<String> inAppName = new ArrayList<>();
            for (final String currentSku : skus) {
                final SkuDetails sku = inv.getSkuDetails(currentSku);
                if (sku != null) {
                    skuNames.put(currentSku, sku.getTitle());
                    inAppName.add(sku.getDescription() + " (" + sku.getPrice() + ")");
                }
            }
            if (inAppName.isEmpty()) {
                return;
            }
            final Spinner inAppSpinner = (Spinner) findViewById(R.id.donate_in_app_spinner);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(DonateActivity.this,
                    android.R.layout.simple_spinner_item, inAppName);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            inAppSpinner.setAdapter(adapter);
            // And finally show the In-App Billing UI
            final RelativeLayout inAppLayout = (RelativeLayout) findViewById(R.id.in_app_layout);
            inAppLayout.setVisibility(View.VISIBLE);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate");
            FirebaseAnalytics.getInstance(DonateActivity.this).logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle);
        }

        int queryPurchases(final Inventory inv) throws JSONException, RemoteException {
            // Query purchases
            boolean verificationFailed = false;
            String continueToken = null;
            do {
                final IInAppBillingService service = mBillingService.get();
                if (service == null) {
                    Log.w(TAG, "Purchases: Billing service is null");
                    return -1;
                }
                final Bundle ownedItems = service.getPurchases(3, getPackageName(), ITEM_TYPE_INAPP, continueToken);
                final int response = getResponseCodeFromBundle(ownedItems);
                if (response != 0) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Purchases: Bad response " + response);
                    return response;
                }
                if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)
                        || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)
                        || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Purchases: Invalid data");
                    return -1;
                }
                final ArrayList<String> purchaseDataList = ownedItems
                        .getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
                final ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);
                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    final String purchaseData = purchaseDataList.get(i);
                    final String signature = signatureList.get(i);
                    final Purchase purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, signature);
                    if (purchase.getSku().startsWith("android.test") || Security.verifyPurchase(publicKey, purchaseData, signature)) {
                        // Record ownership and token
                        inv.addPurchase(purchase);
                    } else
                        verificationFailed = true;
                }
                continueToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
            } while (!TextUtils.isEmpty(continueToken));
            return verificationFailed ? -1 : 0;
        }

        int querySkuDetails(final Inventory inv, final String[] moreSkus) throws RemoteException, JSONException {
            final ArrayList<String> skuList = new ArrayList<>();
            skuList.addAll(inv.getAllOwnedSkus(ITEM_TYPE_INAPP));
            if (moreSkus != null)
                skuList.addAll(Arrays.asList(moreSkus));
            if (skuList.size() == 0)
                return 0;
            final Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
            final IInAppBillingService service = mBillingService.get();
            if (service == null) {
                Log.w(TAG, "SkuDetails: Billing service is null");
                return -1;
            }
            final Bundle skuDetails = service.getSkuDetails(3, getPackageName(), ITEM_TYPE_INAPP, querySkus);
            if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
                final int response = getResponseCodeFromBundle(skuDetails);
                if (response != 0) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "SkuDetails: Bad response " + response);
                    return response;
                }
                return -1;
            }
            final ArrayList<String> responseList = skuDetails.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST);
            for (final String thisResponse : responseList) {
                final SkuDetails d = new SkuDetails(ITEM_TYPE_INAPP, thisResponse);
                inv.addSkuDetails(d);
            }
            return 0;
        }
    }
}
