package com.thehotelmedia.android.customClasses.fireBase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.NotificationActivity
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.extensions.ChatDotUtil
import com.thehotelmedia.android.extensions.NotificationDotUtil
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.provider.Settings
import com.thehotelmedia.android.extensions.censorAbusiveWords
import com.thehotelmedia.android.extensions.loadAbusiveWordsFromJson


class FirebaseMessaging : FirebaseMessagingService() {

    private val TAG = "FIRE_BASE_MESSAGING"
    private val CHANNEL_ID_ALERT = "AlertChannelId"
    private val CHANNEL_NAME_ALERT = "Alert Notifications"

    private val CHANNEL_ID_NORMAL = "NormalChannelId"
    private val CHANNEL_NAME_NORMAL = "Normal Notifications"

    override fun onNewToken(token: String) {
        Log.d(TAG, "New token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received: ${remoteMessage.data}")

        val preferenceManager = PreferenceManager.getInstance(this)
        val businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "") ?: ""

        val title = remoteMessage.data["title"] ?: ""

        val abusiveWords = this.loadAbusiveWordsFromJson()
        val rawBody = remoteMessage.data["body"] ?: ""
        val body = rawBody.censorAbusiveWords(abusiveWords)
//        val body = remoteMessage.data["body"] ?: ""
        val screen = remoteMessage.data["screen"] ?: ""
        val rating = remoteMessage.data["rating"]?.toIntOrNull() ?: 5

        val imageUrl = ""
        val profilePic = ""

        if (screen == "messaging") {
            showChatRedDot()
        } else {
            showNotificationRedDot()
        }

        if (rating <= 3) {
            showAlertNotification(title, body, screen, imageUrl, profilePic, businessType)
        } else {
            showNormalNotification(title, body, screen, imageUrl, profilePic, businessType)
        }
    }

    // ALERT NOTIFICATION (Sound + Strong Vibration)
    private fun showAlertNotification(
        title: String?,
        body: String?,
        screen: String,
        imageUrl: String,
        profilePic: String,
        businessType: String
    ) {
        val soundUri = Uri.parse("android.resource://$packageName/${R.raw.alarm_notification}")
        val vibrationPattern = longArrayOf(0, 1000, 1000, 500, 1000,0, 1000, 1000, 500, 1000,0, 1000, 1000, 500, 1000)

        createChannel(CHANNEL_ID_ALERT, CHANNEL_NAME_ALERT, NotificationManager.IMPORTANCE_HIGH, soundUri, vibrationPattern)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = createPendingIntent(this, screen, businessType)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_app_logo_png)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(vibrationPattern)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        addImagesToNotification(builder, imageUrl, profilePic)

        val notificationId = Random().nextInt(90000) + 10000
        notificationManager.notify(notificationId, builder.build())
    }

    // NORMAL NOTIFICATION (No Sound + Light Vibration)
    private fun showNormalNotification(
        title: String?,
        body: String?,
        screen: String,
        imageUrl: String,
        profilePic: String,
        businessType: String
    ) {
        val vibrationPattern = longArrayOf(0, 500, 500)

        createChannel(CHANNEL_ID_NORMAL, CHANNEL_NAME_NORMAL, NotificationManager.IMPORTANCE_DEFAULT, Settings.System.DEFAULT_NOTIFICATION_URI, vibrationPattern)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = createPendingIntent(this, screen, businessType)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID_NORMAL)
            .setSmallIcon(R.drawable.ic_app_logo_png)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(vibrationPattern)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        addImagesToNotification(builder, imageUrl, profilePic)

        val notificationId = Random().nextInt(90000) + 10000
        notificationManager.notify(notificationId, builder.build())
    }

    // Reusable function to create different channels
    private fun createChannel(channelId: String, name: String, importance: Int, soundUri: Uri?, pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                enableVibration(true)
                vibrationPattern = pattern
                setSound(soundUri, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(context: Context, screen: String, businessType: String): PendingIntent {
        val targetActivity = when (screen) {
            "orders" -> NotificationActivity::class.java
            "messaging" -> if (businessType == Constants.business_type_individual) {
                BottomNavigationIndividualMainActivity::class.java
            } else {
                BottomNavigationBusinessMainActivity::class.java
            }
            else -> NotificationActivity::class.java
        }

        val intent = Intent(context, targetActivity).apply {
            putExtra(Constants.FROM, Constants.notification)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_MUTABLE
        )
    }

    private fun addImagesToNotification(builder: NotificationCompat.Builder, imageUrl: String, profilePic: String) {
        if (profilePic.isNotEmpty()) {
            getBitmapFromUrl(profilePic)?.let {
                builder.setLargeIcon(getCircularBitmap(it))
            }
        }

        if (imageUrl.isNotEmpty()) {
            getBitmapFromUrl(imageUrl)?.let {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(it)
                        .bigLargeIcon(null as Bitmap?)
                )
            }
        }
    }

    private fun getBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Image fetch error: ${e.message}")
            null
        }
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }

        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun showNotificationRedDot() {
        val preferenceManager = PreferenceManager.getInstance(this)
        NotificationDotUtil.setUnreadNotificationsAndBroadcast(this, preferenceManager)
    }

    private fun showChatRedDot() {
        val preferenceManager = PreferenceManager.getInstance(this)
        ChatDotUtil.setUnreadMessagesAndBroadcast(this, preferenceManager, 1)
    }
}
