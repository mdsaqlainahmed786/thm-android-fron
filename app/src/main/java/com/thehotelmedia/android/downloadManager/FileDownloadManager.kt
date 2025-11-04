package com.thehotelmedia.android.downloadManager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.thehotelmedia.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

//class FileDownloadManager(private val context: Context) {
//
//    fun downloadFileFromUrl(fileName: String, fileUrl: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//
//
//            val notificationManager =
//                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val channelId = "download_channel"
//
//            // Create notification channel for Android O and above
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val channel = NotificationChannel(
//                    channelId,
//                    "Downloads",
//                    NotificationManager.IMPORTANCE_HIGH
//                )
//                notificationManager.createNotificationChannel(channel)
//            }
//
//            val notificationBuilder = NotificationCompat.Builder(context, channelId)
//                .setContentTitle(context.getString(R.string.downloading_file))
//                .setContentText(fileName)
//                .setSmallIcon(R.drawable.ic_app_logo) // Replace with your app's icon
//                .setProgress(100, 0, true) // Indeterminate progress initially
//                .setOngoing(true) // Prevent dismissal until task is done
//
//            notificationManager.notify(1, notificationBuilder.build())
//
//            try {
//                val url = URL(fileUrl)
//                val connection = url.openConnection() as HttpURLConnection
//                connection.connect()
//
//                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
//                    val fileLength = connection.contentLength
//                    val dir = context.getExternalFilesDir(null)
//
//
//                    if (dir != null) {
//                        var file = File(dir, fileName)
//                        var counter = 1
//
//                        // Handle file naming for duplicates
//                        while (file.exists()) {
//                            val baseName = fileName.substringBeforeLast(".")
//                            val extension = fileName.substringAfterLast(".", "")
//                            val newName = if (extension.isNotEmpty()) {
//                                "$baseName($counter).$extension"
//                            } else {
//                                "$baseName($counter)"
//                            }
//                            file = File(dir, newName)
//                            counter++
//                        }
//
//                        var totalBytesRead = 0
//                        connection.inputStream.use { input ->
//                            FileOutputStream(file).use { output ->
//                                val buffer = ByteArray(1024)
//                                var bytesRead: Int
//                                while (input.read(buffer).also { bytesRead = it } != -1) {
//                                    output.write(buffer, 0, bytesRead)
//                                    totalBytesRead += bytesRead
//
//                                    // Update notification progress
//                                    if (fileLength > 0) {
//                                        val progress =
//                                            (totalBytesRead * 100 / fileLength).coerceIn(0, 100)
//                                        notificationBuilder.setProgress(100, progress, false)
//                                        notificationManager.notify(1, notificationBuilder.build())
//                                    }
//                                }
//                            }
//                        }
//
//                        // Notify user on success
//                        withContext(Dispatchers.Main) {
//                            notificationManager.cancel(1)
//                            showDownloadNotification(file)
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        showErrorNotification("Download failed: ${connection.responseMessage}")
//                    }
//                }
//                connection.disconnect()
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    showErrorNotification("Error: ${e.message}")
//                }
//            } finally {
////                // Clear progress notification when finished
////                notificationBuilder.setProgress(0, 0, false)
////                    .setContentText(context.getString(R.string.download_complete))
////                    .setOngoing(false)
////                notificationManager.notify(1, notificationBuilder.build())
//            }
//        }
//    }
//
//    private fun showDownloadNotification(file: File) {
//        val notificationManager =
//            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val channelId = "download_channel"
//
//        // Create notification channel for Android O and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Downloads",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val fileUri: Uri = FileProvider.getUriForFile(
//            context,
//            "${context.packageName}.provider",
//            file
//        )
//
//        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
//            data = fileUri
//            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            0,
//            openFileIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notification = NotificationCompat.Builder(context, channelId)
//            .setContentTitle(context.getString(R.string.file_download))
//            .setContentText("${context.getString(R.string.tap_to_open)} ${file.name}")
//            .setSmallIcon(R.drawable.ic_download_done) // Replace with your app's icon
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .setPriority(NotificationCompat.PRIORITY_MAX)
//            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
//            .build()
//
//        notificationManager.notify(2, notification)
//    }
//
//    private fun showErrorNotification(message: String) {
//        val notificationManager =
//            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val channelId = "download_error_channel"
//
//        // Create notification channel for Android O and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Download Errors",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val notification = NotificationCompat.Builder(context, channelId)
//            .setContentTitle(context.getString(R.string.download_failed))
//            .setContentText(message)
//            .setSmallIcon(R.drawable.ic_download_error) // Replace with your app's icon
//            .setAutoCancel(true)
//            .build()
//
//        notificationManager.notify(3, notification)
//    }
//}


import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import java.io.InputStream

class FileDownloadManager(private val context: Context) {

    fun downloadFile(fileName: String, fileUrl: String, mimeType: String = "*/*") {
        CoroutineScope(Dispatchers.IO).launch {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "download_channel"

            // ✅ Create notification channel (for Android 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, "Downloads", NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle("Downloading...")
                .setContentText(fileName)
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(100, 0, true)
                .setOngoing(true)

            notificationManager.notify(1, notificationBuilder.build())

            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val fileUri = saveFileToDownloads(fileName, inputStream, mimeType)

                    withContext(Dispatchers.Main) {
                        notificationManager.cancel(1)
                        if (fileUri != null) {
                            showDownloadNotification(fileName, fileUri)
                        } else {
                            showErrorNotification("Failed to save file.")
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
            }
        }
    }

    private fun saveFileToDownloads(fileName: String, inputStream: InputStream, mimeType: String): Uri? {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // ✅ Support all Android versions (API 24+)
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val fileUri = contentResolver.insert(collectionUri, contentValues)

        fileUri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return fileUri
    }

    private fun showDownloadNotification(fileName: String, fileUri: Uri) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"

        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
            data = fileUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, openFileIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun showErrorNotification(message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_error_channel"

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Download Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }
}

