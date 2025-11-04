package com.thehotelmedia.android.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.thehotelmedia.android.customClasses.Constants.UPDATE_NOTIFICATION_ICON
import com.thehotelmedia.android.customClasses.PreferenceManager

object NotificationDotUtil {

    private var notificationReceiver: BroadcastReceiver? = null

    fun initializeAndUpdateNotificationDot(
        context: Context,
        redDotView: View,
        preferenceManager: PreferenceManager
    ) {
        // Initialize the receiver for updating the notification icon
        notificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // Update the notification icon here (show/hide red dot)
                updateNotificationIcon(redDotView, preferenceManager)
            }
        }

        // Register the receiver to listen for notification icon update broadcasts
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(notificationReceiver!!, IntentFilter(UPDATE_NOTIFICATION_ICON))

        // Call updateNotificationIcon to set the initial state of the red dot
        updateNotificationIcon(redDotView, preferenceManager)
    }

    private fun updateNotificationIcon(redDotView: View, preferenceManager: PreferenceManager) {
        val hasUnreadNotifications = preferenceManager.getBoolean(
            PreferenceManager.Keys.HAS_UNREAD_NOTIFICATIONS, false
        )
        if (hasUnreadNotifications) {
            // Show the red dot on the notification icon
            redDotView.visibility = View.VISIBLE
        } else {
            // Hide the red dot
            redDotView.visibility = View.GONE
        }
    }

    // Set the unread notifications flag and broadcast the update
    fun setUnreadNotificationsAndBroadcast(context: Context, preferenceManager: PreferenceManager) {
        // Set the unread notifications flag to true
        preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_NOTIFICATIONS, true)

        // Broadcast the event to update the red dot on all screens
        val updateIntent = Intent(UPDATE_NOTIFICATION_ICON)
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
    }


    // Clear unread notifications and broadcast the event
    fun clearUnreadNotificationsAndBroadcast(context: Context, preferenceManager: PreferenceManager) {
        // Clear unread notifications flag
        preferenceManager.putBoolean(PreferenceManager.Keys.HAS_UNREAD_NOTIFICATIONS, false)

        // Broadcast the event to update the red dot on all screens
        val intent = Intent(UPDATE_NOTIFICATION_ICON)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun unregisterReceiver(context: Context) {
        notificationReceiver?.let {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
        }
    }
}
