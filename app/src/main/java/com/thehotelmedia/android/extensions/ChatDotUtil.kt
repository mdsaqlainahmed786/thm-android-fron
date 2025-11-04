package com.thehotelmedia.android.extensions

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.navigation.NavigationBarView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.PreferenceManager

//object ChatDotUtil {
//
//    private var chatDotReceiver: BroadcastReceiver? = null
//
//    fun initializeAndUpdateChatDot(
//        context: Context,
//        bottomNavigationView: NavigationBarView,
//        preferenceManager: PreferenceManager,
//        count: Int
//    ) {
//        val chatBadge = bottomNavigationView.getOrCreateBadge(R.id.chatFrag)
//
//        if (count > 0) {
//            chatBadge.isVisible = true
//            chatBadge.backgroundColor = ContextCompat.getColor(context, R.color.red)
//            chatBadge.badgeGravity = BadgeDrawable.TOP_END
////            chatBadge.number = count
//            Log.d("ChatDotUtil", "Chat Badge updated with count: $count")
//        } else {
//            chatBadge.isVisible = false
//            Log.d("ChatDotUtil", "Chat Badge hidden due to zero count")
//        }
//
//        // Register BroadcastReceiver
//        chatDotReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                val unreadCount = intent?.getIntExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, 0) ?: 0
//                Log.d("ChatDotUtil", "Broadcast received, unreadCount: $unreadCount")
//                initializeAndUpdateChatDot(context!!, bottomNavigationView, preferenceManager, unreadCount)
//            }
//        }
//
//        LocalBroadcastManager.getInstance(context)
//            .registerReceiver(chatDotReceiver!!, IntentFilter(Constants.UPDATE_CHAT_DOT))
//    }
//
//    fun unregisterReceiver(context: Context) {
//        chatDotReceiver?.let {
//            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
//        }
//    }
//
//    fun setUnreadMessagesAndBroadcast(context: Context, preferenceManager: PreferenceManager, count: Int) {
//        // Set the unread messages flag to true
//        preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, count > 0)
//
//        // Broadcast the event to update the chat dot
//        val updateIntent = Intent(Constants.UPDATE_CHAT_DOT)
//        updateIntent.putExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, count)
//        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
//    }
//
//    fun clearUnreadMessagesAndBroadcast(context: Context, preferenceManager: PreferenceManager) {
//        // Clear the unread messages flag
//
//        // Check if there are any unread messages
//        val hasUnreadMessages = preferenceManager.getBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, false)
//
//        if (hasUnreadMessages) {
//            // Clear the unread messages flag
//            preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, false)
//
//            // Broadcast the event to update the chat dot
//            val updateIntent = Intent(Constants.UPDATE_CHAT_DOT)
//            updateIntent.putExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, 0)
//            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
//
//            // Hide badge when clearing unread messages
//            (context as? Activity)?.findViewById<NavigationBarView>(R.id.bottomNavigationView)
//                ?.getOrCreateBadge(R.id.chatFrag)?.isVisible = false
//
//            Log.d("ChatDotUtil", "Unread messages cleared and badge hidden.")
//        }
//
////        preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, false)
////
////        // Broadcast the event to update the chat dot
////        val updateIntent = Intent(Constants.UPDATE_CHAT_DOT)
////        updateIntent.putExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, 0)
////        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
////
////        // Hide badge when clearing unread messages
////        (context as? Activity)?.findViewById<NavigationBarView>(R.id.bottomNavigationView)?.getOrCreateBadge(R.id.chatFrag)?.isVisible = false
//    }
//}


object ChatDotUtil {

    private var chatDotReceiver: BroadcastReceiver? = null

    fun initializeAndUpdateChatDot(
        context: Context,
        bottomNavigationView: NavigationBarView,
        preferenceManager: PreferenceManager,
        count: Int
    ) {
        val chatBadge = bottomNavigationView.getOrCreateBadge(R.id.chatFrag)

        if (count > 0) {
            chatBadge.apply {
                isVisible = true
                backgroundColor = ContextCompat.getColor(context, R.color.red)
                badgeGravity = BadgeDrawable.TOP_END
//                number = count
            }
            Log.d("ChatDotUtil", "Chat Badge updated with count: $count")
        } else {
            chatBadge.isVisible = false
            Log.d("ChatDotUtil", "Chat Badge hidden due to zero count")
        }

        registerChatDotReceiver(context, bottomNavigationView, preferenceManager)
    }

    private fun registerChatDotReceiver(
        context: Context,
        bottomNavigationView: NavigationBarView,
        preferenceManager: PreferenceManager
    ) {
        if (chatDotReceiver == null) {
            chatDotReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context?.let {
                        val unreadCount = intent?.getIntExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, 0) ?: 0
                        Log.d("ChatDotUtil", "Broadcast received, unreadCount: $unreadCount")
                        initializeAndUpdateChatDot(it, bottomNavigationView, preferenceManager, unreadCount)
                    }
                }
            }
            LocalBroadcastManager.getInstance(context)
                .registerReceiver(chatDotReceiver!!, IntentFilter(Constants.UPDATE_CHAT_DOT))
        }
    }

    fun unregisterReceiver(context: Context) {
        chatDotReceiver?.let {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
            chatDotReceiver = null
            Log.d("ChatDotUtil", "ChatDotReceiver unregistered")
        }
    }

    fun setUnreadMessagesAndBroadcast(context: Context, preferenceManager: PreferenceManager, count: Int) {
        preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, count > 0)

        val updateIntent = Intent(Constants.UPDATE_CHAT_DOT).apply {
            putExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, count)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)

        Log.d("ChatDotUtil", "Broadcast sent to update chat dot with count: $count")
    }

    fun clearUnreadMessagesAndBroadcast(context: Context, preferenceManager: PreferenceManager) {
        if (preferenceManager.getBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, false)) {
            preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_MESSAGES, false)

            val updateIntent = Intent(Constants.UPDATE_CHAT_DOT).apply {
                putExtra(Constants.EXTRA_CHAT_UNREAD_COUNT, 0)
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)

            (context as? Activity)?.findViewById<NavigationBarView>(R.id.bottomNavigationView)
                ?.getOrCreateBadge(R.id.chatFrag)?.isVisible = false

            Log.d("ChatDotUtil", "Unread messages cleared and badge hidden.")
        }
    }
}
