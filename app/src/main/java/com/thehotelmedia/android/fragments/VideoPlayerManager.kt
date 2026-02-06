package com.thehotelmedia.android.fragments

import android.content.Context
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

object VideoPlayerManager {

    private var currentExoPlayer: ExoPlayer? = null
    fun getExoPlayerInstance(): ExoPlayer? {
        return currentExoPlayer
    }

    fun initializePlayer(context: Context, sourceUrl: String?): ExoPlayer {
        if (currentExoPlayer == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    1000,  // Playback buffer
                    2000   // Rebuffer duration
                )
                .build()

            currentExoPlayer = ExoPlayer.Builder(context)
                .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setLoadControl(loadControl)
                .build()
        }

        currentExoPlayer?.apply {
            stop() // Stop any ongoing playback
            clearMediaItems() // Clear previous media items
            // Validate sourceUrl before setting media item
            if (!sourceUrl.isNullOrEmpty() && 
                (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://") || 
                 sourceUrl.startsWith("file://") || sourceUrl.startsWith("content://"))) {
                try {
                    val mediaItem = MediaItem.fromUri(sourceUrl)
                    setMediaItem(mediaItem)
                    prepare()
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerManager", "Error creating MediaItem from URL: $sourceUrl", e)
                    // Clear media items if URL is invalid
                    clearMediaItems()
                }
            } else {
                android.util.Log.w("VideoPlayerManager", "Invalid or empty sourceUrl: $sourceUrl")
            }
        }

        return currentExoPlayer!!
    }

    fun playPlayer() {
        currentExoPlayer?.playWhenReady = true
    }

    fun pausePlayer() {
        currentExoPlayer?.playWhenReady = false
    }

    fun releasePlayer() {
        currentExoPlayer?.release()
        currentExoPlayer = null
    }
}
