package com.thehotelmedia.android.customClasses.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*

data class PurchaseDetails(
    val purchaseToken: String,
    val purchaseTime: Long,
    val isAcknowledged: Boolean,
    val skus: List<String>
)

class BillingManager(private val activity: Activity, private val purchaseCallback: (Boolean, String, PurchaseDetails?) -> Unit) {

    private var billingClient: BillingClient? = null
    private var onPurchaseSheetClosed: (() -> Unit)? = null

    init {
        setupBillingClient()
    }

    /**
     * Initializes and connects BillingClient.
     */
    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener { _, purchases -> handlePurchase(purchases) }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client connected successfully.")
                    checkExistingSubscriptions() // Ensure fresh subscription data
                } else {
                    handleError("Billing Client setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e("BillingManager", "Billing Service Disconnected. Retrying...")
                setupBillingClient() // Reconnect on disconnect
            }
        })
    }

    /**
     * Queries existing subscriptions when user logs in.
     */
    fun checkExistingSubscriptions() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isNotEmpty()) {
                    Log.d("BillingManager", "Existing active subscription found: ${purchases.map { it.skus }}")
                } else {
                    Log.d("BillingManager", "No active subscriptions found.")
                }
            } else {
                handleError("Failed to check subscriptions: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Queries subscription details.
     */
    private fun querySubscriptionDetails(subscriptionId: String, onSuccess: (ProductDetails?) -> Unit) {
        if (billingClient?.isReady != true) {
            handleError("Billing Client not ready.")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(subscriptionId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                onSuccess(productDetailsList.first())
            } else {
                handleError("Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }




    /**
     * Initiates the subscription purchase flow.
     */
    fun purchaseSubscription(subscriptionId: String, onPurchaseFlowFinished: (() -> Unit)? = null) {
        onPurchaseSheetClosed = onPurchaseFlowFinished

        querySubscriptionDetails(subscriptionId) { productDetails ->
            if (productDetails != null) {
                // Select the first base plan offer (you can customize this logic)
                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken

                if (offerToken == null) {
                    handleError("No offer available for this subscription.")
                    return@querySubscriptionDetails
                }



                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)


                if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
                    handleError("Purchase failed: ${billingResult?.debugMessage}")
                }

                onPurchaseSheetClosed?.invoke()

            } else {
                handleError("Subscription product not found.")
                onPurchaseSheetClosed?.invoke()
            }
        }
    }




    /**
     * Handles purchase response and acknowledges purchase.
     */
//    private fun handlePurchase(purchases: List<Purchase>?) {
//        purchases?.forEach { purchase ->
//            when (purchase.purchaseState) {
//                Purchase.PurchaseState.PURCHASED -> {
//
//                    val purchaseDetails = PurchaseDetails(
//                        purchaseToken = purchase.purchaseToken,
//                        purchaseTime = purchase.purchaseTime,
//                        isAcknowledged = purchase.isAcknowledged,
//                        skus = purchase.skus
//                    )
//                    println("BillingManager  purchase: $purchase")
//                    println("BillingManager  PurchaseDetails: $purchaseDetails")
//                    activity.runOnUiThread { purchaseCallback(true, "Subscription purchased successfully!", purchaseDetails) }
//                }
//                else -> {
//                    activity.runOnUiThread { purchaseCallback(false, "Subscription purchase failed.", null) }
//                }
//            }
//        }
//    }

    private fun handlePurchase(purchases: List<Purchase>?) {
        purchases?.forEach { purchase ->
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val params = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                        billingClient?.acknowledgePurchase(params) { billingResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Log.d("BillingManager", "Purchase acknowledged successfully.")

                                val purchaseDetails = PurchaseDetails(
                                    purchaseToken = purchase.purchaseToken,
                                    purchaseTime = purchase.purchaseTime,
                                    isAcknowledged = true,
                                    skus = purchase.skus
                                )
                                activity.runOnUiThread {
                                    purchaseCallback(true, "Subscription purchased and acknowledged!", purchaseDetails)
                                }
                            } else {
                                handleError("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                            }
                        }
                    } else {
                        Log.d("BillingManager", "Purchase already acknowledged.")

                        val purchaseDetails = PurchaseDetails(
                            purchaseToken = purchase.purchaseToken,
                            purchaseTime = purchase.purchaseTime,
                            isAcknowledged = true,
                            skus = purchase.skus
                        )
                        activity.runOnUiThread {
                            purchaseCallback(true, "Subscription purchased (already acknowledged).", purchaseDetails)
                        }
                    }
                }
                else -> {
                    activity.runOnUiThread {
                        purchaseCallback(false, "Subscription purchase failed or pending.", null)
                    }
                }
            }
        }
    }



    /**
     * Handles errors.
     */
    private fun handleError(message: String) {
        Log.e("BillingManager", message)
        activity.runOnUiThread { purchaseCallback(false, message, null) }
    }
}
