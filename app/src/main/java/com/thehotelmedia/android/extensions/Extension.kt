package com.thehotelmedia.android.extensions

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.thehotelmedia.android.R
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.provider.Settings
import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.activity.PostPreviewActivity
import com.thehotelmedia.android.activity.UserPostsViewerActivity
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.activity.userTypes.business.bottomNavigation.BottomNavigationBusinessMainActivity
import com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity
import com.thehotelmedia.android.services.CreatePostWorker
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import android.app.DatePickerDialog
import android.view.ContextThemeWrapper
import com.thehotelmedia.android.customClasses.CustomSnackBar
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Context.navigateToMainActivity(isIndividual: Boolean) {
    val targetActivity = if (isIndividual) {
        BottomNavigationIndividualMainActivity::class.java
    } else {
        BottomNavigationBusinessMainActivity::class.java
    }
    val intent = Intent(this, targetActivity)
    startActivity(intent)
    if (this is Activity) {
        finish()
    }
}

fun convertTo24HourFormat(time: String): String {
    val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val date = inputFormat.parse(time)  // Parse input time
    return outputFormat.format(date!!)  // Convert and return formatted time
}

fun Context.getAndroidDeviceId(): String {
    return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
}

fun String.capitalizeFirstLetter(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}

fun EditText.setEmailTextWatcher() {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No implementation needed here
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val emailText = s.toString()
            if (emailText != emailText.lowercase()) {
                // Update EditText with lowercase text
                setText(emailText.lowercase())
                setSelection(text?.length ?: 0) // Move cursor to the end
            }
        }

        override fun afterTextChanged(s: Editable?) {
            // No implementation needed here
        }
    })
}

/** Hide Keyboard **/
fun AppCompatActivity.hideKeyboard() {
    val view = currentFocus
    if (view != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isAcceptingText) { // Check if keyboard is open
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

fun Context.isInternetAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo?.isConnected ?: false
    }
}

// For eg -> (* 5)
fun TextView.setRatingWithStar(rating: Double, starIconResId: Int) {
    if (rating == 0.0) {
        this.visibility = View.GONE
        return
    }
    // Load the star icon from drawable resources
    val starIcon: Drawable? = ContextCompat.getDrawable(this.context, starIconResId)
    starIcon?.setBounds(0, 0, starIcon.intrinsicWidth, starIcon.intrinsicHeight)

    // Create the text to display the star icon and the rating
    val ratingText = if (rating % 1.0 == 0.0) {
        // If it's a whole number, show only the number (e.g., "1", "2", ...)
        "★ ${rating.toInt()}"
    } else {
        // If it's a decimal, show the full rating (e.g., "1.7", "2.5", ...)
        "★ $rating"
    }
    val spannableString = SpannableString(ratingText)

    // Set the icon at the start
    starIcon?.let {
        val imageSpan = ImageSpan(it, ImageSpan.ALIGN_BOTTOM)
        spannableString.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Wrap the spannableString in parentheses
    val finalText = SpannableString("  ($spannableString)")

    // Apply color based on the rating
    val color = when (rating) {
        in 1.0..1.9 -> Color.parseColor("#FF0000") //Red
        in 2.0..2.9 -> Color.parseColor("#FFA500") //Orange
        in 3.0..3.9 -> Color.parseColor("#F2C94C") //Yellow
        in 4.0..4.9 -> Color.parseColor("#08BA08") //Lime Green
        in 5.0..5.0 -> Color.parseColor("#00FF00") //Pure Green
        else -> Color.BLACK // Default color for out of range values
    }

    // Apply ForegroundColorSpan to the entire range of finalText
    finalText.setSpan(
        ForegroundColorSpan(color),
        0,
        finalText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    // Set the final text with the colored rating and star icon to the TextView
    this.text = finalText
}

// For eg -> * 5
fun TextView.setRatingWithStarWithoutBracket(rating: Double, starIconResId: Int) {
    if (rating == 0.0) {
        // Set the TextView visibility to GONE when the rating is 0
        this.visibility = View.GONE
        return
    } else {
        // Ensure TextView is visible if the rating is not 0
        this.visibility = View.VISIBLE
    }
    // Load the star icon from drawable resources
    val starIcon: Drawable? = ContextCompat.getDrawable(this.context, starIconResId)
    starIcon?.setBounds(0, 0, starIcon.intrinsicWidth, starIcon.intrinsicHeight)
    // Create the text to display the star icon and the rating
//    val ratingText = "★ $rating"

    // Create the text based on whether the rating is a whole number or not
    val ratingText = if (rating % 1.0 == 0.0) {
        // If it's a whole number, show only the number (e.g., "1", "2", ...)
        "★ ${rating.toInt()}"
    } else {
        // If it's a decimal, show the full rating (e.g., "1.7", "2.5", ...)
        "★ $rating"
    }

    val spannableString = SpannableString(ratingText)

    // Set the icon at the start
    starIcon?.let {
        val imageSpan = ImageSpan(it, ImageSpan.ALIGN_BOTTOM)
        spannableString.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Wrap the spannableString in parentheses
    val finalText = SpannableString(" $spannableString")
    // Apply color based on the rating
    val color = when (rating) {
        in 1.0..1.9 -> Color.parseColor("#E63946") // Soft Red
        in 2.0..2.9 -> Color.parseColor("#F77F00") // Warm Orange
        in 3.0..3.9 -> Color.parseColor("#F2C94C") // Soft Yellow
        in 4.0..4.9 -> Color.parseColor("#06D6A0") // Fresh Teal Green
        in 5.0..5.0 -> Color.parseColor("#118AB2") // Vibrant Blue-Green
        else -> Color.GRAY // Default Grey
    }

    // Apply ForegroundColorSpan to the entire range of finalText
    finalText.setSpan(
        ForegroundColorSpan(color),
        0,
        finalText.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    // Set the final text with the colored rating and star icon to the TextView
    this.text = finalText
}

fun Context.blurTheView(blurView: BlurView, radius: Float = 4f) {
    val decorView = when (this) {
        is Activity -> window.decorView
        is Fragment -> requireActivity().window.decorView
        else -> null
    }

    decorView?.let {
        val rootView = it.findViewById<ViewGroup>(android.R.id.content)
        val windowBackground = it.background

        blurView.setupWith(rootView, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(radius)
    }
}

fun TextView.setRatingWithStars(rating: Int, maxRating: Int = 5) {
    val starSize = textSize.toInt()  // Size of the stars based on the text size
    val drawablePadding = 12  // Padding between stars and text

    val starText = StringBuilder()
    for (i in 1..maxRating) {
        starText.append("★")
    }

    // Create a SpannableStringBuilder to replace stars with colored icons
    val spannableString = android.text.SpannableString(starText.toString())
    for (i in 0 until maxRating) {
        val starDrawable = if (i < rating) {
            getTintedDrawable(context, R.drawable.ic_rating_star, ContextCompat.getColor(context, R.color.star_yellow))
        } else {
            getTintedDrawable(context, R.drawable.ic_rating_star, ContextCompat.getColor(context, R.color.star_normal))
        }
        starDrawable.setBounds(0, 0, starSize, starSize)
        // Align the drawable to the bottom of the text, ensuring it's not cropped
        val imageSpan = android.text.style.ImageSpan(starDrawable, android.text.style.DynamicDrawableSpan.ALIGN_BOTTOM)
        spannableString.setSpan(imageSpan, i, i + 1, android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    text = spannableString
    setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    compoundDrawablePadding = drawablePadding
}

private fun getTintedDrawable(context: Context, drawableRes: Int, color: Int): Drawable {
    val drawable = ContextCompat.getDrawable(context, drawableRes)?.mutate()
    drawable?.let {
        DrawableCompat.setTint(it, color)
    }
    return drawable!!
}

fun ImageView.toggleEnable(isEnabled: Boolean) {
    this.isEnabled = isEnabled
    this.alpha = if (isEnabled) 1f else 0.5f
}

fun getEmojiForRating(rating: Int): String {
    return when (rating) {
        1 -> "😥" // Sad emoji for rating 1
        2 -> "😟" // Neutral emoji for rating 2
        3 -> "😐" // Happy emoji for rating 3
        4 -> "🙂" // Grinning emoji for rating 4
        5 -> "😍" // Star-struck emoji for rating 5
        else -> "Rating"
    }
}

fun calculateDaysAgo(utcString: String,context: Context): String {
    // Parse the input timestamp string (ISO 8601 format)
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val inputDate = ZonedDateTime.parse(utcString, formatter)

    // Get the current date and time in UTC
    val currentDate = ZonedDateTime.now(inputDate.zone) // Uses the same zone as inputDate

    // Calculate the difference between current time and input date
    val daysAgo = ChronoUnit.DAYS.between(inputDate, currentDate)
    val hoursAgo = ChronoUnit.HOURS.between(inputDate, currentDate)
    val minutesAgo = ChronoUnit.MINUTES.between(inputDate, currentDate)

    // Determine the output message based on the time difference
    return when {
        daysAgo > 0 -> "$daysAgo ${context.getString(R.string.days_ago)}"
        hoursAgo > 0 -> "$hoursAgo ${context.getString(R.string.hours_ago)}"
        minutesAgo > 0 -> "$minutesAgo ${context.getString(R.string.minutes_ago)}"
        else -> context.getString(R.string.just_now)
    }
}

/**
 * Checks if a post was created within the last 24 hours (recent post)
 * @param utcString The UTC timestamp string in ISO 8601 format
 * @return true if the post is less than 24 hours old, false otherwise
 */
fun isRecentPost(utcString: String): Boolean {
    if (utcString.isEmpty()) return false
    return try {
        // Parse the input timestamp string (ISO 8601 format)
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val inputDate = ZonedDateTime.parse(utcString, formatter)
        
        // Get the current date and time in UTC
        val currentDate = ZonedDateTime.now(inputDate.zone)
        
        // Calculate the difference in hours
        val hoursAgo = ChronoUnit.HOURS.between(inputDate, currentDate)
        
        // Consider posts less than 24 hours old as recent
        hoursAgo < 24
    } catch (e: Exception) {
        false
    }
}

/**
 * Checks if a business profile was created less than 11 months ago (grace period)
 * Business users should not be routed to subscription page during the first 11 months
 * @param createdAtString The UTC timestamp string in ISO 8601 format (can be null or empty)
 * @return true if the profile was created less than 11 months ago, false otherwise
 */
fun isWithinGracePeriod(createdAtString: String?): Boolean {
    if (createdAtString.isNullOrEmpty()) return false
    return try {
        // Parse the input timestamp string (ISO 8601 format)
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val profileCreatedDate = ZonedDateTime.parse(createdAtString, formatter)
        
        // Get the current date and time in UTC
        val currentDate = ZonedDateTime.now(profileCreatedDate.zone)
        
        // Calculate the difference in months
        val monthsAgo = ChronoUnit.MONTHS.between(profileCreatedDate, currentDate)
        
        // Return true if less than 11 months have passed
        monthsAgo < 11
    } catch (e: Exception) {
        // If parsing fails, return false to allow normal subscription flow
        false
    }
}

fun calculateDaysAgoInSmall(utcString: String): String {
    // Parse the input timestamp string (ISO 8601 format)
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val inputDate = ZonedDateTime.parse(utcString, formatter)

    // Get the current date and time in UTC
    val currentDate = ZonedDateTime.now(inputDate.zone) // Uses the same zone as inputDate

    // Calculate the difference between current time and input date
    val daysAgo = ChronoUnit.DAYS.between(inputDate, currentDate)
    val hoursAgo = ChronoUnit.HOURS.between(inputDate, currentDate)
    val minutesAgo = ChronoUnit.MINUTES.between(inputDate, currentDate)
    val secondsAgo = ChronoUnit.SECONDS.between(inputDate, currentDate)

    // Determine the output message based on the time difference
    return when {
        daysAgo > 0 -> "${daysAgo}d"
        hoursAgo > 0 -> "${hoursAgo}h"
        minutesAgo > 0 -> "${minutesAgo}min"
        secondsAgo > 0 -> "${secondsAgo}sec"
        else -> "Just now"
    }
}

fun getMimeType(file: File): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString())
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
}


// Date Format "Dec 19, 2023"
fun String.toReadableDate(): String {
    // Define the input date format (the format of the string you currently have)
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC time

    // Define the output date format (the format you want)
    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Parse the input date string
    val date: Date? = inputFormat.parse(this)

    // Format the parsed date into the desired format
    return if (date != null) {
        outputFormat.format(date)
    } else {
        "Invalid Date"
    }
}

// Time Format "10:00 AM"
fun String.toReadableTime(): String {
    // Define the input date format (the format of the string you currently have)
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Parse as UTC time

    // Define the output time format (the format you want for time)
    val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Parse the input date string
    val date: Date? = inputFormat.parse(this)

    // Format the parsed date into the desired time format
    return if (date != null) {
        outputFormat.format(date)
    } else {
        "Invalid Time"
    }
}

fun TextView.setUpReadMore(fullText: String, maxChars: Int, readMoreText: String = "Read more", readLessText: String = "Read less", readMoreColorRes: Int = R.color.blue) {
    if (fullText.length > maxChars) {
        val shortText = fullText.substring(0, maxChars).trim() + ""
        this.text = getSpannableText(shortText, fullText, true, readMoreText, readLessText, readMoreColorRes,maxChars)
        this.movementMethod = LinkMovementMethod.getInstance()
    } else {
        this.text = fullText
    }
}

private fun TextView.getSpannableText(displayText: String, fullText: String, isReadMore: Boolean, readMoreText: String, readLessText: String, readMoreColorRes: Int, maxChars: Int): SpannableString {
    val spannableText = SpannableString(displayText + if (isReadMore) " ...$readMoreText" else " $readLessText")
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            if (isReadMore) {
                // Expand to show full text
                this@getSpannableText.text = getSpannableText(fullText, fullText, false, readMoreText, readLessText, readMoreColorRes, maxChars)
            } else {
                // Collapse to show truncated text
                val shortText = fullText.substring(0, maxChars).trim() + "..."
                this@getSpannableText.text = getSpannableText(shortText, fullText, true, readMoreText, readLessText, readMoreColorRes, maxChars)
            }
            this@getSpannableText.movementMethod = LinkMovementMethod.getInstance()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.color = ContextCompat.getColor(this@getSpannableText.context, readMoreColorRes)
            ds.isUnderlineText = false
        }
    }
    spannableText.setSpan(clickableSpan, displayText.length + 1, spannableText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannableText
}

fun calculateDistanceInKm(
    currentUserLat: Double,
    currentUserLng: Double,
    otherUserLat: Double,
    otherUserLng: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        currentUserLat,
        currentUserLng,
        otherUserLat,
        otherUserLng,
        results
    )
    // results[0] contains the distance in meters, convert it to kilometers
    return results[0] / 1000
}

//  Sat. 2 Nov 2024
fun formatDate(inputDate: String): String {
    return try {
        // Define the input and output date formats
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE. d MMM yyyy", Locale.getDefault())

        // Parse the input date string to a Date object
        val date = inputFormat.parse(inputDate)

        // Format the Date object to the desired output format
        outputFormat.format(date!!)
    } catch (e: Exception) {
        inputDate // Return the original input in case of a parsing error
    }
}

// 04:00pm
fun formatTime(inputTime: String): String {
    return try {
        // Define the input and output time formats
        val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())  // 24-hour format
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())  // 12-hour format with AM/PM

        // Parse the input time string to a Date object
        val time = inputFormat.parse(inputTime)

        // Format the Date object to the desired output format
        outputFormat.format(time!!)
    } catch (e: Exception) {
        inputTime // Return the original input in case of a parsing error
    }
}

fun isFutureDateOrTime(startDate: String, startTime: String): Boolean {

    // Date format for parsing
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    dateFormat.isLenient = false

    // Parse the current date
    val currentDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0) // Start of the day
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Parse the current time dynamically
    val currentTime = Calendar.getInstance().apply {
        val currentHour = get(Calendar.HOUR_OF_DAY)
        val currentMinute = get(Calendar.MINUTE)
        set(Calendar.HOUR_OF_DAY, currentHour)
        set(Calendar.MINUTE, currentMinute)
    }

    // Parse the start date and time
    val parsedStartDate = dateFormat.parse(startDate)?.time ?: 0
    val (endHour, endMinute) = startTime.split(":").map { it.toInt() }

    val endTimeCalendar = Calendar.getInstance().apply {
        timeInMillis = parsedStartDate
        set(Calendar.HOUR_OF_DAY, endHour)
        set(Calendar.MINUTE, endMinute)
    }

    // Check if start date is in the future
    if (parsedStartDate > currentDate.timeInMillis) {
        // If the start date is in the future, return true
        return true
    }

    // If dates are the same, check time
    if (currentDate.timeInMillis == parsedStartDate) {
        val diffInMinutes = ((endTimeCalendar.timeInMillis - currentTime.timeInMillis) / (1000 * 60)).toInt()

        return when {
            diffInMinutes > 30 -> true // Time is more than 30 minutes ahead
            else -> false // Time is within 30 minutes or past
        }
    } else {
        // If the start date is in the past, return false
        return false
    }
}

fun getTimeAgo(timestamp: String): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC") // Ensure UTC parsing
    val time = format.parse(timestamp)?.time ?: return "Invalid timestamp"

    val now = System.currentTimeMillis()
    val diff = now - time

    return when {
        diff < TimeUnit.SECONDS.toMillis(1) -> "just now"
        diff < TimeUnit.MINUTES.toMillis(1) -> "${TimeUnit.MILLISECONDS.toSeconds(diff)}s"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d"
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days < 30) "${days / 7}w ago"
            else if (days < 365) "${days / 30}mo ago"
            else "${days / 365}y ago"
        }
    }
}

fun View.setTopMarginIfFirstItem(position: Int, context: Context, marginResId: Int) {
    updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = if (position == 0) {
            context.resources.getDimensionPixelSize(marginResId)
        } else {
            0
        }
    }
}

fun Date.toISO8601UTC(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC") // Set to UTC
    return sdf.format(this)
}

fun formatDateTime(input: String): String {
    // Define the input and output date formats
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Set the input format to UTC

    return try {
        // Parse the input string to a Date object
        val date = inputFormat.parse(input)

        // Format the date to 24-hour time
        outputFormat.format(date)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

//Share Event
fun Context.shareEventsWithDeepLink(postID: String, userID: String) {
    val encryptedPostID = EncryptionHelper.encrypt(postID)
    val encryptedUserID = EncryptionHelper.encrypt(userID)
    val baseUrl = "${BuildConfig.SHARE_DEEP_LINK_HOST}/share/events"
    val deepLink = "$baseUrl?postID=$encryptedPostID&userID=$encryptedUserID"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, deepLink)
    }
    startActivity(Intent.createChooser(intent, "Share via"))
}

fun Context.sharePostWithDeepLink(postID: String, userID: String) {
    val encryptedPostID = EncryptionHelper.encrypt(postID)
    val encryptedUserID = EncryptionHelper.encrypt(userID)
    val baseUrl = "${BuildConfig.SHARE_DEEP_LINK_HOST}/share/posts"
    val deepLink = "$baseUrl?postID=$encryptedPostID&userID=$encryptedUserID"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, deepLink)
    }
    startActivity(Intent.createChooser(intent, "Share via"))
}

fun Context.shareProfileWithDeepLink(outerUserId: String, ownerUserId: String) {
    val encryptedId = EncryptionHelper.encrypt(outerUserId)
    val encryptedOwnerUserId = EncryptionHelper.encrypt(ownerUserId)
    val baseUrl = "${BuildConfig.SHARE_DEEP_LINK_HOST}/share/users"
    val deepLink = "$baseUrl?id=$encryptedId&userID=$encryptedOwnerUserId"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, deepLink)
    }
    startActivity(Intent.createChooser(intent, "Share via"))
}

fun capitalizeFirstLetterToLowercase(input: String): String {
    if (input.isEmpty()) return input
    return input[0].lowercaseChar() + input.substring(1)
}

fun Context.sendEmail(email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:") // Only email apps should handle this
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email)) // Email address
    }

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // Show a toast if no email app is found
        Toast.makeText(this, "No email app found!", Toast.LENGTH_SHORT).show()
    }
}

fun Context.makeCall(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber") // Prepares the phone number for the dialer
    }

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // Show a toast if no app can handle the call action
        Toast.makeText(this, "No app found to make a call!", Toast.LENGTH_SHORT).show()
    }
}

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url) // Parse the URL string
    }

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // Show a toast if no app can handle the URL
        Toast.makeText(this, "No browser app found!", Toast.LENGTH_SHORT).show()
    }
}

fun Context.moveToViewer(type: String, mediaUrl: String, id: String? = "", postId: String? = "", likedByMe: Boolean? = false) {
    val intent = Intent(this, VideoImageViewer::class.java).apply {
        putExtra("MEDIA_URL", mediaUrl)
        putExtra("MEDIA_TYPE", type)
        putExtra("MEDIA_ID", id)
        putExtra("POST_ID", postId)
        putExtra("LIKED_BY_ME", likedByMe)
    }
    startActivity(intent)
}

fun String?.toFormattedDate(): String {
    if (this.isNullOrEmpty()) return ""

    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    inputFormat.timeZone = TimeZone.getTimeZone("UTC")

    val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    return try {
        val date = inputFormat.parse(this) ?: return ""
        val currentDate = Calendar.getInstance()

        val createdDate = Calendar.getInstance().apply { time = date }

        return when {
            isSameDay(currentDate, createdDate) -> "Today"
            isYesterday(currentDate, createdDate) -> "Yesterday"
            else -> outputFormat.format(date)
        }
    } catch (e: Exception) {
        ""
    }
}

private fun isSameDay(current: Calendar, other: Calendar): Boolean {
    return current.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            current.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(current: Calendar, other: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return yesterday.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

fun Context.openGoogleMaps(lat: Double, lng: Double) {
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps") // Ensure it opens in Google Maps
    }
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_SHORT).show()
    }
}

fun TextView.updateTextWithAnimation(
    isActive: Boolean,
    locationText: String,
    alternateText: String,
    locationDuration: Long = 6800,
    alternateDuration: Long = 6800
) {
    // Ensure the handler is unique for each item
    val ANIMATION_HANDLER_TAG = "animation_handler_tag"

    val handler = getTag(ANIMATION_HANDLER_TAG.hashCode()) as? Handler
        ?: Handler(Looper.getMainLooper()).also {
            setTag(ANIMATION_HANDLER_TAG.hashCode(), it)
        }

    // Stop any ongoing animations
    handler.removeCallbacksAndMessages(null)

    if (!isActive) {
        // Static text for inactive state
        this.text = locationText
        return
    }

    // Variables to track the current text
    var isShowingLocation = true

    val runnable = object : Runnable {
        override fun run() {
            // Determine the next text to display
            val nextText = if (isShowingLocation) locationText else alternateText
            this@updateTextWithAnimation.animateTextChange(nextText)

            // Schedule the next toggle based on the current text's duration
            val nextDuration = if (isShowingLocation) locationDuration else alternateDuration
            isShowingLocation = !isShowingLocation
            handler.postDelayed(this, nextDuration)
        }
    }

    // Start the animation loop
    handler.post(runnable)
}

// Internal function to handle animations
private fun TextView.animateTextChange(newText: String) {
    val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
        duration = 800 // Fade-out duration
    }

    fadeOut.addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {}
        override fun onAnimationEnd(animation: Animator) {
            this@animateTextChange.text = newText
            ObjectAnimator.ofFloat(this@animateTextChange, "alpha", 0f, 1f).apply {
                duration = 800 // Fade-in duration
            }.start()
        }

        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
    })

    fadeOut.start()
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri: Uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}

fun ViewGroup.animateVisibilityToggle(
    alternateView: View,
    thirdView: View,
    interval: Long = 8000L,
    fadeDuration: Long = 1000L
) {
    val handler = Handler(Looper.getMainLooper())
    var showThisView = 3 // 1 for the first view, 2 for the second, 3 for the third view

    fun animateFadeOut(view: View, onEnd: () -> Unit) {
        view.animate()
            .alpha(0f)
            .setDuration(fadeDuration)
            .withEndAction { onEnd() }
            .start()
    }

    fun animateFadeIn(view: View) {
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(fadeDuration)
            .start()
    }

    val toggleRunnable = object : Runnable {
        override fun run() {
            when (showThisView) {
                1 -> {
                    animateFadeOut(this@animateVisibilityToggle) {
                        this@animateVisibilityToggle.isVisible = false
                        alternateView.isVisible = true
                        animateFadeIn(alternateView)
                    }
                }
                2 -> {
                    animateFadeOut(alternateView) {
                        alternateView.isVisible = false
                        thirdView.isVisible = true
                        animateFadeIn(thirdView)
                    }
                }
                3 -> {
                    animateFadeOut(thirdView) {
                        thirdView.isVisible = false
                        this@animateVisibilityToggle.isVisible = true
                        animateFadeIn(this@animateVisibilityToggle)
                    }
                }
            }
            showThisView = when (showThisView) {
                3 -> 1
                1 -> 2
                else -> 3
            }
            handler.postDelayed(this, interval)
        }
    }

    handler.post(toggleRunnable) // Start the animation immediately
}

fun Context.setWeatherAndTemperature(
    tempInKelvin: Double,
    weather: String,
    weatherTv: TextView,
    weatherIv: AppCompatImageView
) {
    val tempInCelsius = (tempInKelvin - 273).toInt()
    // Set temperature
    weatherTv.text = "$tempInCelsius ${getString(R.string.degree_celsius)}"
    // Get the current hour of the day
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    // Determine if it's day or night (6 AM to 6 PM is day, otherwise night)
    val isDay = currentHour in 6..18
    // Load corresponding weather GIF using Glide
    val weatherGifResId = when (weather) {
        "clear" -> {
            if (isDay) {
                R.raw.clear // Day GIF
            } else {
                R.raw.night // Night GIF (replace with your moon GIF resource)
            }
        }
        "clouds" -> R.raw.clouds
        "rain" -> R.raw.rain
        "drizzle" -> R.raw.drizzle
        "thunderstorm" -> R.raw.thunderstorm
        "snow" -> R.raw.snow
        "mist" -> R.raw.mist
        "smoke" -> R.raw.smoke
        "haze" -> R.raw.haze
        "fog" -> R.raw.fog
        "dust" -> R.raw.dust
        "sand" -> R.raw.sand
        "ash" -> R.raw.ash
        "squall" -> R.raw.squall
        "tornado" -> R.raw.tornado
        else -> null
    }
    weatherGifResId?.let {
        Glide.with(this).asGif().load(it).into(weatherIv)
    }
}

fun Int.toAqiType(): String {
    return when (this) {
        1 -> "Good"
        2 -> "Fair"
        3 -> "Moderate"
        4 -> "Poor"
        5 -> "Very Poor"
        else -> "Unknown"
    }
}

fun Double.toAQI(): Int {
    return when (this) {
        in 0.0..12.0 -> calculateAqiForRange(this, 0.0, 12.0, 0, 50)
        in 12.1..35.4 -> calculateAqiForRange(this, 12.1, 35.4, 51, 100)
        in 35.5..55.4 -> calculateAqiForRange(this, 35.5, 55.4, 101, 150)
        in 55.5..150.4 -> calculateAqiForRange(this, 55.5, 150.4, 151, 200)
        in 150.5..250.4 -> calculateAqiForRange(this, 150.5, 250.4, 201, 300)
        in 250.5..350.4 -> calculateAqiForRange(this, 250.5, 350.4, 301, 400)
        in 350.5..500.4 -> calculateAqiForRange(this, 350.5, 500.4, 401, 500)
        else -> 600
    }
}

private fun calculateAqiForRange(pm25: Double, lowConcentration: Double, highConcentration: Double, lowAqi: Int, highAqi: Int): Int {
    return ((pm25 - lowConcentration) / (highConcentration - lowConcentration) * (highAqi - lowAqi) + lowAqi).toInt()
}

fun View.openWeatherApp() {
    this.setOnClickListener {
        val weatherAppsKeywords = listOf(
            "weather", "forecast", "storm", "rain", "snow"
        )
        val packageManager = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedApps = packageManager.queryIntentActivities(launchIntent, 0)
        var foundWeatherApp = false
        installedApps.forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString().lowercase()
            // Check if the app name contains any weather-related keywords
            if (weatherAppsKeywords.any { appName.contains(it) }) {
                try {
                    val weatherIntent = packageManager.getLaunchIntentForPackage(packageName)
                    weatherIntent?.let {
                        context.startActivity(it)
                        foundWeatherApp = true
                        return@forEach // Exit the lambda after opening the first weather-related app
                    }
                } catch (e: Exception) {
                    // Handle error in case the app can't be launched
                    e.printStackTrace()
                }
            }
        }
        // If no weather app is found, open Chrome with a weather search
        if (!foundWeatherApp) {
            try {
                val weatherSearchUrl = "https://www.google.com/search?q=weather"
                val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(weatherSearchUrl)).apply {
                    setPackage("com.android.chrome")
                }
                if (chromeIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(chromeIntent)
                } else {
                    // Open in any available browser if Chrome is not installed
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(weatherSearchUrl))
                    context.startActivity(browserIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}

data class AQIBreakpoint(
    val concentrationLow: Double,
    val concentrationHigh: Double,
    val aqiLow: Int,
    val aqiHigh: Int
)

fun calculateAQI(concentration: Double, breakpoints: List<AQIBreakpoint>): Int {
    for (bp in breakpoints) {
        if (concentration in bp.concentrationLow..bp.concentrationHigh) {
            return ((bp.aqiHigh - bp.aqiLow) / (bp.concentrationHigh - bp.concentrationLow) * (concentration - bp.concentrationLow) + bp.aqiLow).toInt()
        }
    }
    return -1 // Invalid concentration
}

fun Context.moveToPostPreviewScreen(postId: String) {
    val intent = Intent(this, PostPreviewActivity::class.java).apply {
        putExtra("FROM", "Notification")
        putExtra("POST_ID", postId)
    }
    startActivity(intent)
}

fun Context.moveToUserPostsViewer(
    userId: String,
    initialPostId: String? = null,
    filterMediaType: String? = null, // "image" for photos only, "video" for videos only, null for all
    initialMediaId: String? = null, // Media ID to scroll to (used when post ID is not available)
    initialMediaUrl: String? = null, // Media sourceUrl to scroll to (used as fallback for matching)
    initialIndex: Int? = null // Index of the clicked media in the profile grid (used as a final fallback)
) {
    val intent = Intent(this, UserPostsViewerActivity::class.java).apply {
        putExtra("USER_ID", userId)
        initialPostId?.let { putExtra("INITIAL_POST_ID", it) }
        filterMediaType?.let { putExtra("FILTER_MEDIA_TYPE", it) }
        initialMediaId?.let { putExtra("INITIAL_MEDIA_ID", it) }
        initialMediaUrl?.let { putExtra("INITIAL_MEDIA_URL", it) }
        initialIndex?.let { putExtra("INITIAL_INDEX", it) }
    }
    startActivity(intent)
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0) // Billion (B)
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)         // Million (M)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun String.toTimePeriod(): String {
    return when (this.lowercase()) {
        "monthly" -> "/M"
        "quarterly" -> "/3M"
        "yearly" -> "/Y"
        "half-yearly" -> "/6M"
        else -> ""
    }
}

fun isDarkThemeEnabled(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
}

fun convertTo12HourFormat(time: String): String {
    val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour format
    val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // 12-hour format with AM/PM

    return try {
        val date = inputFormat.parse(time) // Parse the input time
        outputFormat.format(date!!) // Format it to 12-hour format
    } catch (e: Exception) {
        e.printStackTrace()
        time // Return original if there's an error
    }
}

fun formatBookingDates(checkInDate: String?, checkOutDate: String?): Triple<String, String, Long> {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC") // Ensure UTC timezone
    }
    val outputFormat = SimpleDateFormat("ddMMM", Locale.getDefault()) // e.g. 26May

    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")) // Use UTC timezone

    val checkIn = checkInDate?.let { inputFormat.parse(it) } ?: Date()
    val checkOut = checkOutDate?.let { inputFormat.parse(it) } ?: Date()

    // Normalize check-in
    calendar.time = checkIn
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val normalizedCheckIn = calendar.time

    // Normalize check-out
    calendar.time = checkOut
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val normalizedCheckOut = calendar.time

    // Calculate nights
    val nights = TimeUnit.MILLISECONDS.toDays(normalizedCheckOut.time - normalizedCheckIn.time)

    // Format
    val formattedCheckIn = outputFormat.format(normalizedCheckIn)
    val formattedCheckOut = outputFormat.format(normalizedCheckOut)

    return Triple(formattedCheckIn, formattedCheckOut, nights)
}


fun startCreatePostWorker(context: Context, mediaList: List<String>, selectedTagIdList: List<String>, content: String, selectedPlaceName: String, selectedLat: Double, selectedLng: Double, selectedFeeling: String, collaboratorIds: List<String>) {
    android.util.Log.d("startCreatePostWorker", "=== WORKER DATA DEBUG ===")
    android.util.Log.d("startCreatePostWorker", "collaboratorIds received: ${collaboratorIds.size} items")
    android.util.Log.d("startCreatePostWorker", "collaboratorIds values: $collaboratorIds")
    android.util.Log.d("startCreatePostWorker", "collaboratorIds.toTypedArray() size: ${collaboratorIds.toTypedArray().size}")
    android.util.Log.d("startCreatePostWorker", "==========================")
    
    val data = workDataOf(
        "mediaList" to mediaList.toTypedArray(),
        "selectedTagIdList" to selectedTagIdList.toTypedArray(),
        "content" to content,
        "selectedPlaceName" to selectedPlaceName,
        "selectedLat" to selectedLat,
        "selectedLng" to selectedLng,
        "selectedFeeling" to selectedFeeling,
        "collaboratorUserIDs" to collaboratorIds.toTypedArray()
    )

    val workRequest = OneTimeWorkRequestBuilder<CreatePostWorker>()
        .setInputData(data)
        .setConstraints(
            Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        )
        .build()
    WorkManager.getInstance(context).enqueue(workRequest)
}



// Function to load abusive words from JSON file
fun Context.loadAbusiveWordsFromJson(): List<String> {
    return try {
        val fileName = "abusive_words.json"
        val jsonString = assets.open(fileName).bufferedReader().use { it.readText() } // Read JSON file
        val jsonObject = JSONObject(jsonString)
        val jsonArray = jsonObject.getJSONArray("abusiveWords")
        List(jsonArray.length()) { jsonArray.getString(it) } // Convert to List
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}
//
//fun String.censorAbusiveWords(abusiveWords: List<String>): String {
//    return this.split("\\s+".toRegex()).joinToString(" ") { word ->
//        val cleanWord = word.lowercase().replace(Regex("[^a-z]"), "") // Remove non-alphabet characters for checking
//        if (abusiveWords.any { it.equals(cleanWord, ignoreCase = true) }) {
//            word.replace(Regex("(?i)$cleanWord"), cleanWord.first() + "*".repeat(cleanWord.length - 1))
//        } else {
//            word
//        }
//    }
//}

fun String.censorAbusiveWords(abusiveWords: List<String>): String {
    return this.split("\\s+".toRegex()).joinToString(" ") { word ->
        val cleanWord = word.lowercase().replace(Regex("[^a-z]"), "") // clean for comparison
        if (abusiveWords.any { it.equals(cleanWord, ignoreCase = true) }) {
            // Preserve the case of the first letter
            val firstChar = word.firstOrNull { it.isLetter() } ?: '*'
            val masked = "*".repeat(cleanWord.length - 1)
            val censoredWord = if (firstChar.isUpperCase()) {
                firstChar.uppercaseChar() + masked
            } else {
                firstChar.lowercaseChar() + masked
            }
            word.replace(Regex("(?i)$cleanWord"), censoredWord)
        } else {
            word
        }
    }
}


// Output: 3 Mar 2025, 02:18 PM (Indian Time Zone me convert hoga)
fun String.toFormattedDateTime(): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    inputFormat.timeZone = TimeZone.getTimeZone("UTC") // Z ka matlab UTC time hota hai
    val outputFormat = SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault())

    val date: Date = inputFormat.parse(this) ?: return this // Agar parse fail ho jaye to original string return karo
    return outputFormat.format(date)
}

fun MaterialCardView.updateBookingStatusColor(context: Context, status: String) {
    val color = when (status) {
        "created" -> ContextCompat.getColor(context, R.color.light_green)
        "pending" -> ContextCompat.getColor(context, R.color.star_yellow)
        "confirmed" -> ContextCompat.getColor(context, R.color.light_green)
        "checked in" -> ContextCompat.getColor(context, R.color.blue)
        "completed" -> ContextCompat.getColor(context, R.color.light_green)
        "canceled" -> ContextCompat.getColor(context, R.color.red_50)
        "no show" -> ContextCompat.getColor(context, R.color.purple)
        else -> ContextCompat.getColor(context, R.color.red_50) // Default color
    }
    this.setCardBackgroundColor(color)
}

fun ImageView.setRoomTypeImage(bedType: String) {
    val imageRes = when (bedType.lowercase()) {
        "king" -> R.drawable.ic_king_room
        "queen" -> R.drawable.ic_queen_room
        "single" -> R.drawable.ic_single_room
        "double" -> R.drawable.ic_double_room
        else -> R.drawable.ic_deluxe_room
    }
    this.setImageResource(imageRes)
}


fun Context.callPhoneNumber(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    startActivity(intent)
}


fun generateTimeSlots(currentTime: LocalTime? = null): List<String> {
    val timeSlots = mutableListOf<String>()
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")

    fun addSlots(start: String, end: String) {
        var time = LocalTime.parse(start)
        val endTime = LocalTime.parse(end)
        while (time.isBefore(endTime)) {
            // Agar currentTime provided hai (today ke case me), toh sirf future slots add karo
            if (currentTime == null || time.isAfter(currentTime)) {
                timeSlots.add(time.format(formatter))
            }
            time = time.plusMinutes(30) // 30 min ka gap
        }
    }

    addSlots("08:00", "11:00") // Breakfast
    addSlots("13:00", "15:00") // Lunch
    addSlots("19:00", "23:00") // Dinner

    return timeSlots
}




fun Context.showDatePicker(
    isCheckInDate: Boolean,
    checkInCalendar: Calendar?,
    checkOutDate: String?,
    onDateSelected: (String, Calendar) -> Unit
) {
    val calendar = Calendar.getInstance()

    if (isCheckInDate) {
        checkInCalendar?.let { calendar.time = it.time }
    } else {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = checkOutDate?.let { sdf.parse(it) }
        parsedDate?.let { calendar.time = it }

        if (parsedDate == null && checkInCalendar != null) {
            calendar.time = checkInCalendar.time
            calendar.add(Calendar.DAY_OF_MONTH, 1) // Minimum check-out date = check-in + 1
        }
    }

    val datePickerDialog = DatePickerDialog(
        ContextThemeWrapper(this, R.style.CustomTimePickerDialog),
        { _, year, monthOfYear, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply { set(year, monthOfYear, dayOfMonth) }
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

            onDateSelected(formattedDate, selectedDate) // Call the callback function
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (isCheckInDate) {
        val maxCheckInDate = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.datePicker.maxDate = maxCheckInDate
    } else {
        checkInCalendar?.let {
            val minCheckoutDate = it.timeInMillis + 24 * 60 * 60 * 1000
            val maxCheckoutDate = it.timeInMillis + 15 * 24 * 60 * 60 * 1000
            datePickerDialog.datePicker.minDate = minCheckoutDate
            datePickerDialog.datePicker.maxDate = maxCheckoutDate
        }
    }

    datePickerDialog.setOnShowListener {
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.blue))
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.blue))
    }

    datePickerDialog.show()
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}



fun getMealType(dateTimeString: String): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy | h:mma", Locale.ENGLISH)
    val localDateTime = LocalDateTime.parse(dateTimeString.replace("Pm", "PM").replace("Am", "AM"), formatter)
    val hour = localDateTime.hour
    return when (hour) {
        in 7..11 -> "Breakfast"
        in 12..16 -> "Lunch"
        in 18..23 -> "Dinner"
        else -> "Late Night Snack"
    }
}

fun View.showCustomSnackBar(message: String) {
    CustomSnackBar.showSnackBar(this, message)
}

fun Context.showComingSoonDialog() {
    val builder = AlertDialog.Builder(this)
    builder.setTitle("Coming Soon!")
    builder.setMessage("This feature will be available in the next version. Stay tuned!")
    builder.setPositiveButton("OK") { dialog, _ ->
        dialog.dismiss()
    }

    val dialog = builder.create()
    dialog.show()

    // Change the OK button color to blue
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
        ContextCompat.getColor(this, R.color.blue)
    )
}


fun Double?.roundToTwoDecimal(): Double {
    return this?.let {
        BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toDouble()
    } ?: 0.0
}
