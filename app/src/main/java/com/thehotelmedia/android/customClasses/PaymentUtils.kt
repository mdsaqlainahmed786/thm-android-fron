package com.thehotelmedia.android.customClasses

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.razorpay.Checkout
import com.thehotelmedia.android.BuildConfig
import org.json.JSONObject

object PaymentUtils {
    fun startPayment(
        context: Context,
        amount: String,
        currency: String,
        entity: String,
        receipt: String,
        orderId: String,
        userNumber: String,
        email: String,
        description: String,
        tag: String
    ) {
        try {
            val co = Checkout()
            // Be explicit about which key we use (avoids any meta-data/manifest placeholder confusion)
            // and makes debugging "order_id belongs to different key" issues much easier.
            co.setKeyID(BuildConfig.RAZORPAY_API_KEY)
            Log.d(tag, "RAZORPAY_KEY-> ${BuildConfig.RAZORPAY_API_KEY}")
            val options = JSONObject()
            options.put("name", "The Hotel Media")
            options.put("app_name", "The Hotel Media")
            options.put("description", description)
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg")
            options.put("theme.color", "#082C50")
            val hasOrder = orderId.isNotBlank()
            if (hasOrder) {
                // When using a Razorpay Order, prefer server-trusted order details.
                // Passing a mismatched amount/currency along with order_id can cause BAD_REQUEST during authentication.
                options.put("order_id", orderId)
            } else {
                options.put("currency", currency)
            }
            // If no order_id is provided, we must send amount/currency. If order_id is provided, omit amount/currency
            // to prevent mismatches with server-created order.
            var normalizedAmount = amount.trim()
            if (!hasOrder) {
                // Razorpay expects amount in the smallest currency unit (e.g. paise) as an integer string.
                // Also guard against accidental "23600.0" style values.
                normalizedAmount = normalizedAmount.let { raw ->
                    if (raw.contains(".")) {
                        ((raw.toDoubleOrNull() ?: 0.0) * 100.0).toLong().toString()
                    } else {
                        (raw.toLongOrNull() ?: 0L).toString()
                    }
                }
                options.put("amount", normalizedAmount)
            }

            Log.d(tag, "ORDER_ID-> $orderId")
            Log.d(tag, "AMOUNT-> $normalizedAmount (sent=${!hasOrder})")
            Log.d(tag, "CURRENCY-> $currency (sent=${!hasOrder})")

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            val prefill = JSONObject()
            prefill.put("email", email.trim())
            // Razorpay contact should be digits only; strip '+' and whitespace.
            prefill.put("contact", userNumber.filter { it.isDigit() })
            options.put("prefill", prefill)

            // Open the CheckoutActivity, catching any potential exception
            try {
                co.open(context as Activity, options)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag + "PaymentScreen", "Error opening $description : ${e.message}")
                // Handle the exception as needed, e.g., log it or show a toast
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(tag + "PaymentScreen", "Error in payment $description : ${e.message}")
            Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
