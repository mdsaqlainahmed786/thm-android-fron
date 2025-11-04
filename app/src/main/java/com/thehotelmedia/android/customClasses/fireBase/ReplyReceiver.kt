package com.thehotelmedia.android.customClasses.fireBase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.thehotelmedia.android.R

class ReplyReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReplyReceiver"
        const val KEY_TEXT_REPLY = "key_text_reply" // Must match the remote input key in your notification
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && context != null) {
            val replyText = getMessageText(intent) // Retrieve the user's input
            if (replyText != null) {
                Log.d(TAG, "Reply received: $replyText")

                // You can handle the reply text here, such as sending it to the server or displaying it in the app
                // Example: showToast(context, replyText)
            } else {
                Log.d(TAG, "No reply text found.")
            }
        }
    }

    /**
     * Retrieves the input text from the notification's RemoteInput.
     */
    private fun getMessageText(intent: Intent): CharSequence? {
        return RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
    }
}
