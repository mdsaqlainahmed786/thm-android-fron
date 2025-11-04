package com.thehotelmedia.android.customClasses

import android.content.Context
import com.thehotelmedia.android.R

object MessageStore {

    fun enterFullName(context: Context): String {
        return context.getString(R.string.enter_full_name)
    }
    fun enterEmailAddress(context: Context): String {
        return context.getString(R.string.enter_email_address)
    }
    fun enterValidEmailAddress(context: Context): String {
        return context.getString(R.string.enter_valid_email_address)
    }
    fun selectTypeFirst(context: Context): String {
        return context.getString(R.string.please_select_type_first)
    }
    fun somethingWentWrong(context: Context): String {
        return context.getString(R.string.something_went_wrong)
    }
    fun googleLoginFailed(context: Context): String {
        return context.getString(R.string.google_login_failed)
    }


    fun pleaseEnter(context: Context): String {
        return context.getString(R.string.please_enter)
    }

    fun pleaseSelect(context: Context): String {
        return context.getString(R.string.please_select)
    }

    fun type(context: Context): String {
        return context.getString(R.string.type)
    }

    fun name(context: Context): String {
        return context.getString(R.string.name)
    }

    fun enterPassword(context: Context): String {
        return context.getString(R.string.enter_password)
    }

    fun passwordAtLeast8Characters(context: Context): String {
        return context.getString(R.string.password_at_least_8_characters)
    }

    fun passwordOneUppercase(context: Context): String {
        return context.getString(R.string.password_one_uppercase)
    }

    fun passwordOneLowercase(context: Context): String {
        return context.getString(R.string.password_one_lowercase)
    }

    fun passwordOneNumber(context: Context): String {
        return context.getString(R.string.password_one_number)
    }

    fun passwordOneSpecialCharacter(context: Context): String {
        return context.getString(R.string.password_one_special_character)
    }

    fun enterContactNumber(context: Context): String {
        return context.getString(R.string.enter_contact_number)
    }

    fun contactShouldBe10Digits(context: Context): String {
        return context.getString(R.string.contact_should_be_10_digits)
    }

    fun enterValidContactNumber(context: Context): String {
        return context.getString(R.string.enter_valid_contact_number)
    }

    fun noImageSelected(context: Context): String {
        return context.getString(R.string.no_image_selected)
    }

    fun cameraPermissionDenied(context: Context): String {
        return context.getString(R.string.camera_permission_denied)
    }

    fun galleryPermissionDenied(context: Context): String {
        return context.getString(R.string.gallery_permission_denied)
    }

    fun pleaseEnterYourPassword(context: Context): String {
        return context.getString(R.string.please_enter_your_password)
    }

    fun passwordNotStrongEnough(context: Context): String {
        return context.getString(R.string.password_not_strong_enough)
    }

    fun pleaseConfirmYourPassword(context: Context): String {
        return context.getString(R.string.please_confirm_your_password)
    }

    fun passwordDontMatch(context: Context): String {
        return context.getString(R.string.password_dont_match)
    }

    fun pleaseEnterVerificationCode(context: Context): String {
        return context.getString(R.string.please_enter_verification_code)
    }

    fun pleaseAnswerAllQuestions(context: Context): String {
        return context.getString(R.string.please_answer_all_questions)
    }

    fun errorFetchingAddress(context: Context): String {
        return context.getString(R.string.error_fetching_address)
    }

    fun pleaseSelectAddress(context: Context): String {
        return context.getString(R.string.please_select_address)
    }

    fun pleaseEnterContactNumber(context: Context): String {
        return context.getString(R.string.please_enter_contact_number)
    }

    fun enterValid10DigitNumber(context: Context): String {
        return context.getString(R.string.enter_valid_10_digit_number)
    }

    fun pleaseEnterValidWebsiteLink(context: Context): String {
        return context.getString(R.string.please_enter_valid_website_link)
    }

    fun pleaseEnterYourEmail(context: Context): String {
        return context.getString(R.string.please_enter_your_email)
    }

    fun pleaseEnterValidEmail(context: Context): String {
        return context.getString(R.string.please_enter_valid_email)
    }

    fun pleaseEnterGstinNumber(context: Context): String {
        return context.getString(R.string.please_enter_gstin_number)
    }

    fun pleaseEnterDescription(context: Context): String {
        return context.getString(R.string.please_enter_description)
    }

    fun pleaseSelectLogo(context: Context): String {
        return context.getString(R.string.please_select_logo)
    }

    fun pleaseSelectPropertyPicture(context: Context): String {
        return context.getString(R.string.please_select_property_picture)
    }

    fun pleaseSelectTypeFirst(context: Context): String {
        return context.getString(R.string.please_select_type_first)
    }

    fun cameraPermissionRequired(context: Context): String {
        return context.getString(R.string.camera_permission_required)
    }

    fun storagePermissionRequired(context: Context): String {
        return context.getString(R.string.storage_permission_required)
    }

    fun permissionDeniedCannotAccessFile(context: Context): String {
        return context.getString(R.string.permission_denied_cannot_access_file)
    }

    fun fileNotSupported(context: Context): String {
        return context.getString(R.string.file_not_supported)
    }

    fun locationPermissionDenied(context: Context): String {
        return context.getString(R.string.location_permission_denied)
    }

    fun pressBackAgainToExit(context: Context): String {
        return context.getString(R.string.press_back_again_to_exit)
    }

    fun pleaseSelectBusinessType(context: Context): String {
        return context.getString(R.string.please_select_business_type)
    }

    fun pleaseSelectSubBusinessType(context: Context): String {
        return context.getString(R.string.please_select_sub_business_type)
    }

    fun pleaseAddBillingAddress(context: Context): String {
        return context.getString(R.string.please_add_billing_address)
    }

    fun freeSubscription(context: Context): String {
        return context.getString(R.string.free_subscription)
    }

    fun pleaseEnterYourName(context: Context): String {
        return context.getString(R.string.please_enter_your_name)
    }

    fun pleaseEnterYourBio(context: Context): String {
        return context.getString(R.string.please_enter_your_bio)
    }

    fun videoFileNotExist(context: Context): String {
        return context.getString(R.string.video_file_not_exist)
    }

    fun permissionRequiredToSaveVideo(context: Context): String {
        return context.getString(R.string.permission_required_to_save_video)
    }

    fun failedToSaveImage(context: Context): String {
        return context.getString(R.string.failed_to_save_image)
    }

    fun pleaseAddMedia(context: Context): String {
        return context.getString(R.string.please_add_media)
    }

    fun rateAllQuestions(context: Context): String {
        return context.getString(R.string.rate_all_questions)
    }

    fun descriptionBeforeProceeding(context: Context): String {
        return context.getString(R.string.description_before_proceeding)
    }

    fun selectImage(context: Context): String {
        return context.getString(R.string.select_image)
    }

    fun selectStartDate(context: Context): String {
        return context.getString(R.string.select_start_date)
    }

    fun selectStartTime(context: Context): String {
        return context.getString(R.string.select_start_time)
    }

    fun selectEndDate(context: Context): String {
        return context.getString(R.string.select_end_date)
    }

    fun selectEndTime(context: Context): String {
        return context.getString(R.string.select_end_time)
    }

    fun selectEventFormat(context: Context): String {
        return context.getString(R.string.select_event_format)
    }

    fun enterStreamLink(context: Context): String {
        return context.getString(R.string.enter_stream_link)
    }

    fun selectVenue(context: Context): String {
        return context.getString(R.string.select_venue)
    }

    fun whyReportingUser(context: Context): String {
        return context.getString(R.string.why_reporting_user)
    }

    fun whyReportingPost(context: Context): String {
        return context.getString(R.string.why_reporting_post)
    }
    fun whyReportingComment(context: Context): String {
        return context.getString(R.string.why_reporting_comment)
    }

    fun pickColor(context: Context): String {
        return context.getString(R.string.pick_color)
    }

    fun enterYourMessage(context: Context): String {
        return context.getString(R.string.enter_your_message)
    }

    fun deviceNoWhatsapp(context: Context): String {
        return context.getString(R.string.device_no_whatsapp)
    }

    fun helloHotelMediaTeam(context: Context): String {
        return context.getString(R.string.hello_hotel_media_team)
    }

    fun noStoriesAvailable(context: Context): String {
        return context.getString(R.string.no_stories_available)
    }

    fun doYouWantToBlock(context: Context): String {
        return context.getString(R.string.do_you_want_to_block)

    }

    fun blockUser(context: Context): String {
        return context.getString(R.string.block_user)

    }
    fun viewProfile(context: Context): String {
        return context.getString(R.string.view_profile)
    }

    fun sureWantToDeleteStory(context: Context): String {
        return context.getString(R.string.sure_want_to_delete_story)
    }
    fun sureWantToDeletePost(context: Context): String {
        return context.getString(R.string.sure_want_to_delete_post)
    }







    fun enterProfession(context: Context): String {
        return context.getString(R.string.enter_profession)
    }
    fun selectProfession(context: Context): String {
        return context.getString(R.string.select_profession)
    }



    fun sureWantToExportChat(context: Context): String {
        return context.getString(R.string.sure_want_to_export_chat)
    }
    fun sureWantToDownloadMedia(context: Context): String {
        return context.getString(R.string.sure_want_to_download_media)
    }



    fun sureWantToDeleteChat(context: Context): String {
        return context.getString(R.string.sure_want_to_delete_chat)
    }
    fun sureWantToPostJob(context: Context): String {
        return context.getString(R.string.sure_want_to_post_job)
    }


}