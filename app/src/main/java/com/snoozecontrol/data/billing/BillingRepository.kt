package com.snoozecontrol.data.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_PRO = "snooze_pro_unlock"
        private const val PREFS_NAME = "snooze_billing_prefs"
        private const val KEY_IS_PRO_USER = "key_is_pro_user"
        private const val KEY_PRO_CHECKSUM = "key_pro_checksum"
        private const val KEY_DEBUG_PRO = "key_debug_pro"
        private const val SALT = "SnoozeControlSecretSalt2026!"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _isProUser = MutableStateFlow(isProUserCached())
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Connection broken; will retry when user attempts billing operations
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            val details = productDetailsList.firstOrNull { it.productId == PRODUCT_ID_PRO }
            _productDetails.value = details
        }
    }

    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        var hasPro = false
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID_PRO) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            ) {
                hasPro = true
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }
        updateProStatus(hasPro)
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { _ -> }
    }

    fun launchBillingFlow(activity: Activity): Boolean {
        // Developer debug mode handling
        if (prefs.getBoolean(KEY_DEBUG_PRO, false)) {
            updateProStatus(true)
            return true
        }

        val details = _productDetails.value ?: return false
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    fun setDebugProOverride(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEBUG_PRO, enabled).apply()
        updateProStatus(enabled || isProUserCached())
    }

    fun toggleDebugProOverride(): Boolean {
        val current = prefs.getBoolean(KEY_DEBUG_PRO, false)
        val next = !current
        setDebugProOverride(next)
        return next
    }

    fun isDebugProEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEBUG_PRO, false)
    }

    private fun generateChecksum(isPro: Boolean): String {
        val input = "${context.packageName}_${isPro}_$SALT"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun isChecksumValid(isPro: Boolean): Boolean {
        val storedChecksum = prefs.getString(KEY_PRO_CHECKSUM, null) ?: return false
        val expected = generateChecksum(isPro)
        return storedChecksum == expected
    }

    private fun updateProStatus(isPro: Boolean) {
        val isDebug = prefs.getBoolean(KEY_DEBUG_PRO, false)
        val finalStatus = isPro || isDebug
        val checksum = generateChecksum(finalStatus)
        prefs.edit()
            .putBoolean(KEY_IS_PRO_USER, finalStatus)
            .putString(KEY_PRO_CHECKSUM, checksum)
            .apply()
        _isProUser.value = finalStatus
    }

    private fun isProUserCached(): Boolean {
        val isDebug = prefs.getBoolean(KEY_DEBUG_PRO, false)
        if (isDebug) return true

        val rawCached = prefs.getBoolean(KEY_IS_PRO_USER, false)
        return if (rawCached && isChecksumValid(true)) {
            true
        } else {
            false
        }
    }
}
