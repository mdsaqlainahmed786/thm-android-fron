package com.thehotelmedia.android.customClasses

import java.util.regex.Pattern

object Constants {

    val URL_PATTERN = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]".toRegex()

    val URL_PATTERN_MATCHER: Pattern = Pattern.compile(
        "(https?://[\\w-]+(\\.[\\w-]+)+(/[\\w-.,@?^=%&:/~+#]*)?)",
        Pattern.CASE_INSENSITIVE
    )


    var FROM = "FROM"
    var notification = "notification"
    var business_type_individual = "Individual"
    var business_type_business = "Business"
    var UPDATE_NOTIFICATION_ICON = "UPDATE_NOTIFICATION_ICON"
    var UPDATE_CHAT_DOT = "UPDATE_CHAT_DOT"
    var EXTRA_CHAT_UNREAD_COUNT = "EXTRA_CHAT_UNREAD_COUNT"
    var LANGUAGE_CODE = "LANGUAGE_CODE"
    var CREATE_POST_BROADCAST = "com.thehotelmedia.CREATE_POST_SERVICE_STARTED"


    var IMAGE = "image"
    var VIDEO = "video"


    var OFFICIAL = "official"
    var ADMINISTRATOR = "administrator"
    var USER = "user"
    var MODERATOR = "moderator"
    var N_A = "N/A"


    var DEFAULT_LAT = 20.5937
    var DEFAULT_LNG = 78.9629

    var DEFAULT_VIDEO_LENGTH = 30 //In Sec
    var DEFAULT_PDF_MB = 5 //In MB






}