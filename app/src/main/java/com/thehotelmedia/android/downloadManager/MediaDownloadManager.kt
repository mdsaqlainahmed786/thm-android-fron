package com.thehotelmedia.android.downloadManager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
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

class MediaDownloadManager(private val context: Context) {

    fun downloadFileFromUrl(fileName: String, fileUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {


            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "download_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Downloads",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.downloading_file))
                .setContentText(fileName)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setProgress(100, 0, true)
                .setOngoing(true)

            notificationManager.notify(1, notificationBuilder.build())

            try {
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val mimeType = connection.contentType ?: "application/octet-stream"
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                    val dir = context.getExternalFilesDir(null)


                    if (dir != null) {
                        var file = File(dir, "$fileName.$extension")
                        var counter = 1

                        while (file.exists()) {
                            file = File(dir, "$fileName($counter).$extension")
                            counter++
                        }

                        var totalBytesRead = 0
                        val fileLength = connection.contentLength

                        connection.inputStream.use { input ->
                            FileOutputStream(file).use { output ->
                                val buffer = ByteArray(1024)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead

                                    if (fileLength > 0) {
                                        val progress = (totalBytesRead * 100 / fileLength).coerceIn(0, 100)
                                        notificationBuilder.setProgress(100, progress, false)
                                        notificationManager.notify(1, notificationBuilder.build())
                                    }
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            notificationManager.cancel(1)
                            showDownloadNotification(file, mimeType)
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
//                notificationBuilder.setProgress(0, 0, false)
//                    .setContentText(context.getString(R.string.download_complete))
//                    .setOngoing(false)
//                notificationManager.notify(1, notificationBuilder.build())
            }
        }
    }


    fun downloadM3U8Video(fileName: String, m3u8Url: String) {
        CoroutineScope(Dispatchers.IO).launch {

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "download_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Downloads",
                    NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }


            try {
                val dir = context.getExternalFilesDir(null)
                val playlistContent = URL(m3u8Url).readText()
                val tsUrls = playlistContent.lines().filter { it.endsWith(".ts") }

                val tsFiles = mutableListOf<File>()
                tsUrls.forEachIndexed { index, tsPath ->
                    val tsUrl = if (tsPath.startsWith("http")) tsPath else m3u8Url.substringBeforeLast("/") + "/" + tsPath
                    val tsFile = File(dir, "$fileName-$index.ts")
                    URL(tsUrl).openStream().use { input ->
                        FileOutputStream(tsFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tsFiles.add(tsFile)
                }

                val mergedFile = File(dir, "$fileName.mp4")
                FileOutputStream(mergedFile).use { output ->
                    tsFiles.forEach { tsFile ->
                        tsFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                        tsFile.delete()
                    }
                }

                withContext(Dispatchers.Main) {
                    showDownloadNotification(mergedFile, "video/mp4")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorNotification("Error downloading video: ${e.message}")
                }
            }
        }
    }


    private fun showDownloadNotification(file: File, mimeType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
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
            .setSmallIcon(R.drawable.ic_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun showErrorNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_error_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Download Errors",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.download_failed))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_download_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }
}


