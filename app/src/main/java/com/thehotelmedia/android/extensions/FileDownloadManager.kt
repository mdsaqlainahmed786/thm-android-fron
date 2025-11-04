package com.thehotelmedia.android.extensions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.thehotelmedia.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloadManager(private val context: Context) {

    fun downloadFileFromUrl(fileName: String, fileUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {


            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "download_channel"

            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Downloads",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.downloading_file))
                .setContentText(fileName)
                .setSmallIcon(R.drawable.ic_app_logo) // Replace with your app's icon
                .setProgress(100, 0, true) // Indeterminate progress initially
                .setOngoing(true) // Prevent dismissal until task is done

            notificationManager.notify(1, notificationBuilder.build())

            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val fileLength = connection.contentLength
                    val dir = context.getExternalFilesDir(null)

                    if (dir != null) {
                        var file = File(dir, fileName)
                        var counter = 1

                        // Handle file naming for duplicates
                        while (file.exists()) {
                            val baseName = fileName.substringBeforeLast(".")
                            val extension = fileName.substringAfterLast(".", "")
                            val newName = if (extension.isNotEmpty()) {
                                "$baseName($counter).$extension"
                            } else {
                                "$baseName($counter)"
                            }
                            file = File(dir, newName)
                            counter++
                        }

                        var totalBytesRead = 0
                        connection.inputStream.use { input ->
                            FileOutputStream(file).use { output ->
                                val buffer = ByteArray(1024)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead

                                    // Update notification progress
                                    if (fileLength > 0) {
                                        val progress =
                                            (totalBytesRead * 100 / fileLength).coerceIn(0, 100)
                                        notificationBuilder.setProgress(100, progress, false)
                                        notificationManager.notify(1, notificationBuilder.build())
                                    }
                                }
                            }
                        }

                        // Notify user on success
                        withContext(Dispatchers.Main) {
                            notificationManager.cancel(1)
                            showDownloadNotification(file)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showErrorNotification("Download failed: ${connection.responseMessage}")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorNotification("Error: ${e.message}")
                }
            } finally {
//                // Clear progress notification when finished
//                notificationBuilder.setProgress(0, 0, false)
//                    .setContentText(context.getString(R.string.download_complete))
//                    .setOngoing(false)
//                notificationManager.notify(1, notificationBuilder.build())
            }
        }
    }

    private fun showDownloadNotification(file: File) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloads",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
            data = fileUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openFileIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.file_download))
            .setContentText("${context.getString(R.string.tap_to_open)} ${file.name}")
            .setSmallIcon(R.drawable.ic_download_done) // Replace with your app's icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun showErrorNotification(message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_error_channel"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Download Errors",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.download_failed))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download_error) // Replace with your app's icon
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }
}
