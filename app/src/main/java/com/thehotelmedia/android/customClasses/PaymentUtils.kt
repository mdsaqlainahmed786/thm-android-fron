package com.thehotelmedia.android.customClasses

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.razorpay.Checkout
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
            val options = JSONObject()
            options.put("name", "The Hotel Media")
            options.put("app_name", "The Hotel Media")
            options.put("description", description)
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg")
            options.put("theme.color", "#082C50")
            options.put("currency", currency)
            options.put("order_id", orderId)
            options.put("amount", amount)

            Log.d(tag, "AMOUNT-> $amount")

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            val prefill = JSONObject()
            prefill.put("email", email)
            prefill.put("contact", userNumber)
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
