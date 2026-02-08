package com.thehotelmedia.android.utils

import com.thehotelmedia.android.modals.feeds.feed.MediaRef
import java.util.Locale

private const val STORY_MAX_VIDEO_SECONDS = 15.0
// Server sometimes provides duration as seconds (e.g. 12.5) and sometimes as milliseconds (e.g. 12500).
// Values >= 1000 are almost always milliseconds in this app’s content constraints.
private const val DURATION_LIKELY_MILLISECONDS_THRESHOLD = 1_000.0

/**
 * @return the first video duration (in seconds) that exceeds the story limit, or null if all are within limit
 *         (or if duration is unknown).
 */
fun findTooLongStoryVideoSeconds(
    mediaRefs: List<MediaRef>?,
    maxSeconds: Double = STORY_MAX_VIDEO_SECONDS
): Double? {
    if (mediaRefs.isNullOrEmpty()) return null
    for (media in mediaRefs) {
        if (!media.isVideoLike()) continue
        val seconds = normalizeDurationToSeconds(media.duration) ?: continue
        if (seconds > maxSeconds) return seconds
    }
    return null
}

fun normalizeDurationToSeconds(duration: Double?): Double? {
    if (duration == null || duration <= 0) return null
    return if (duration >= DURATION_LIKELY_MILLISECONDS_THRESHOLD) duration / 1000.0 else duration
}

private fun MediaRef.isVideoLike(): Boolean {
    val type = mediaType?.lowercase(Locale.getDefault())
    val mime = mimeType?.lowercase(Locale.getDefault())
    if (type == "video" || (mime?.startsWith("video") == true)) return true

    // Fallback: some APIs omit mediaType/mimeType but provide duration for videos.
    val isExplicitImage = type == "image" || (mime?.startsWith("image") == true)
    val dur = duration
    return !isExplicitImage && (dur != null && dur > 0)
}


