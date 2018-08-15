package com.ianhanniballake.contractiontimer.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DonateDialogFragment extends DialogFragment
        implements BillingClientStateListener, PurchasesUpdatedListener {
    private ArrayAdapter<CharSequence> adapter;
    private BillingClient billingClient;
    private List<SkuDetails> skuDetailsList;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        billingClient = BillingClient.newBuilder(getContext())
                .setListener(this)
                .build();
        billingClient.startConnection(this);
    }

    @Override
    public void onBillingSetupFinished(int responseCode) {
        final ArrayList<String> allSkus = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            allSkus.add("android.test.purchased");
            allSkus.add("android.test.canceled");
            allSkus.add("android.test.refunded");
            allSkus.add("android.test.item_unavailable");
        }
        final String[] skuArray = getResources().getStringArray(R.array.donate_in_app_sku_array);
        allSkus.addAll(Arrays.asList(skuArray));
        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
                        .setSkusList(allSkus)
                        .setType(BillingClient.SkuType.INAPP)
                        .build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(int responseCode,
                            List<SkuDetails> skuDetailsList) {
                        if (skuDetailsList == null) {
                            return;
                        }
                        Collections.sort(skuDetailsList, new Comparator<SkuDetails>() {
                            @Override
                            public int compare(final SkuDetails details1, final SkuDetails details2) {
                                return (int) (details1.getPriceAmountMicros() - details2.getPriceAmountMicros());
                            }
                        });
                        DonateDialogFragment.this.skuDetailsList = skuDetailsList;
                        adapter.clear();
                        for (SkuDetails details : skuDetailsList) {
                            adapter.add(details.getDescription() + " (" + details.getPrice() + ")");
                        }
                        adapter.notifyDataSetChanged();
                        Bundle bundle = new Bundle();
                        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate");
                        FirebaseAnalytics.getInstance(getContext()).logEvent(
                                FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle);
                    }
                });
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(
                BillingClient.SkuType.INAPP);
        if (purchasesResult.getPurchasesList() != null) {
            for (Purchase purchase : purchasesResult.getPurchasesList()) {
                billingClient.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(final int responseCode, final String purchaseToken) {
                    }
                });
            }
        }
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (purchases == null) {
            return;
        }
        for (Purchase purchase : purchases) {
            billingClient.consumeAsync(purchase.getPurchaseToken(), new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(final int responseCode, final String purchaseToken) {
                    if (responseCode == BillingClient.BillingResponse.OK) {
                        Toast.makeText(getContext(), R.string.donate_thank_you, Toast
                                .LENGTH_LONG).show();
                    }
                    dismiss();
                }
            });
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        billingClient.startConnection(this);
    }

    @Override
    public void onDestroy() {
        billingClient.endConnection();
        super.onDestroy();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.donate_header)
                .setAdapter(adapter, null)
                .create();
        alertDialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                SkuDetails skuDetails = skuDetailsList.get(position);
                String sku = skuDetails.getSku();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, sku);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, skuDetails.getTitle());
                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate");
                FirebaseAnalytics.getInstance(getContext()).logEvent(
                        FirebaseAnalytics.Event.VIEW_ITEM, bundle);
                int responseCode = billingClient.launchBillingFlow(getActivity(),
                        BillingFlowParams.newBuilder()
                                .setSku(sku)
                                .setType(BillingClient.SkuType.INAPP)
                                .build());
                if (responseCode != BillingClient.BillingResponse.OK) {
                    dismiss();
                }
            }
        });
        return alertDialog;
    }
}
