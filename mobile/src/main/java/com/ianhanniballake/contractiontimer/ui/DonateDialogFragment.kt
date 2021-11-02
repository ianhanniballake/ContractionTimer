package com.ianhanniballake.contractiontimer.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import java.util.ArrayList

class DonateDialogFragment : DialogFragment(), BillingClientStateListener, PurchasesUpdatedListener {
    private lateinit var adapter: ArrayAdapter<CharSequence>
    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(requireContext())
                .setListener(this)
                .build()
    }
    private var skuDetailsList: List<SkuDetails> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (!isAdded) {
            return
        }
        val allSkus = ArrayList<String>()
        if (BuildConfig.DEBUG) {
            allSkus.add("android.test.purchased")
            allSkus.add("android.test.canceled")
            allSkus.add("android.test.refunded")
            allSkus.add("android.test.item_unavailable")
        }
        val skuArray = resources.getStringArray(R.array.donate_in_app_sku_array)
        allSkus.addAll(skuArray)
        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
                .setSkusList(allSkus)
                .setType(BillingClient.SkuType.INAPP)
                .build(),
                SkuDetailsResponseListener { _, skuDetailsList ->
                    val context = context ?: return@SkuDetailsResponseListener
                    if (skuDetailsList == null) {
                        return@SkuDetailsResponseListener
                    }
                    skuDetailsList.sortWith(Comparator { details1, details2 ->
                        (details1.priceAmountMicros - details2.priceAmountMicros).toInt()
                    })
                    this@DonateDialogFragment.skuDetailsList = skuDetailsList
                    adapter.clear()
                    for (details in skuDetailsList) {
                        adapter.add(details.description + " (" + details.price + ")")
                    }
                    adapter.notifyDataSetChanged()
                    val bundle = Bundle().apply {
                        putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate")
                    }
                    FirebaseAnalytics.getInstance(context).logEvent(
                        FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle
                    )
                })
        val purchasesResult = billingClient.queryPurchases(
            BillingClient.SkuType.INAPP
        )
        val purchasesList = purchasesResult.purchasesList
        if (purchasesList != null) {
            for (purchase in purchasesList) {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.consumeAsync(consumeParams) { _, _ -> }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (purchases == null) {
            return
        }
        for (purchase in purchases) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
                if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(
                        requireContext(), R.string.donate_thank_you, Toast
                            .LENGTH_LONG
                    ).show()
                }
                dismiss()
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        billingClient.startConnection(this)
    }

    override fun onDestroy() {
        billingClient.endConnection()
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.donate_header)
            .setAdapter(adapter, null)
            .create()
        alertDialog.listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val skuDetails = skuDetailsList[position]
            val sku = skuDetails.sku
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, sku)
                putString(FirebaseAnalytics.Param.ITEM_NAME, skuDetails.title)
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "donate")
            }
            FirebaseAnalytics.getInstance(requireContext()).logEvent(
                FirebaseAnalytics.Event.VIEW_ITEM, bundle
            )
            val billingResult = billingClient.launchBillingFlow(
                requireActivity(),
                BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()
            )
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                dismiss()
            }
        }
        return alertDialog
    }
}
