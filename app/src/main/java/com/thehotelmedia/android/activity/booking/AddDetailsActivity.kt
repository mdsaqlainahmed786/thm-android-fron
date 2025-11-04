package com.thehotelmedia.android.activity.booking

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.activity.booking.bottomSheet.ChildrenAgeBottomSheet
import com.thehotelmedia.android.activity.booking.bottomSheet.GuestAndRoomBottomSheet
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityAddDetailsBinding
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.showDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDetailsActivity : BaseActivity() {

    private lateinit var binding : ActivityAddDetailsBinding

//    private var checkInDate = ""
//    private var checkOutDate = ""

    private var checkInDate: String? = null
    private var checkOutDate: String? = null
    private var checkInCalendar: Calendar? = null  // Store check-in date


    private var guestCount = 1
    private var childrenCount = 0
    private var hasPet = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {

        val businessProfileId = intent.getStringExtra("KEY_BUSINESS_PROFILE_ID") ?: ""
        val userLargeProfilePic = intent.getStringExtra("KEY_USER_LARGE_PROFILE_PIC") ?: ""
        val userFullName = intent.getStringExtra("KEY_USER_FULL_NAME") ?: ""
        val businessName = intent.getStringExtra("KEY_BUSINESS_NAME") ?: ""
        val businessIcon = intent.getStringExtra("KEY_BUSINESS_ICON") ?: ""
        val fullAddress = intent.getStringExtra("KEY_FULL_ADDRESS") ?: ""
        val rating = intent.getDoubleExtra("KEY_RATING", 0.0) // Default to 0.0 if not found

        Glide.with(this).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this, R.color.white_40)))
            .into(binding.businessIconIv)

        Glide.with(this).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = userFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.checkInBtn.setOnClickListener {
            showDatePicker(
                isCheckInDate = true,
                checkInCalendar = checkInCalendar,
                checkOutDate = checkOutDate
            ) { selectedDate, selectedCalendar ->
                checkInDate = selectedDate
                checkInCalendar = selectedCalendar
                binding.checkInBtn.text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(selectedCalendar.time)
                binding.checkOutBtn.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                checkOutDate = null
                binding.checkOutBtn.text = ""
            }
        }

        binding.checkOutBtn.setOnClickListener {

            if (checkInDate == null) {
                // Show toast if user tries to select check-out before check-in
                Toast.makeText(this, getString(R.string.select_checkin_date_first), Toast.LENGTH_SHORT).show()
            } else {
                showDatePicker(
                    isCheckInDate = false,
                    checkInCalendar = checkInCalendar,
                    checkOutDate = checkOutDate
                ) { selectedDate, _ ->
                    checkOutDate = selectedDate
                    binding.checkOutBtn.text = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!)
                }
            }
        }


        binding.guestBtn.setOnClickListener {
//            val intent = Intent(this, RoomDetailsActivity::class.java)
//            startActivity(intent)




            if (checkInDate.isNullOrEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, this.getString(R.string.select_checkin_date))
                return@setOnClickListener
            }

            if (checkOutDate.isNullOrEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, this.getString(R.string.select_checkout_date))
                return@setOnClickListener
            }




            val bottomSheet = GuestAndRoomBottomSheet.newInstance(guestCount, childrenCount, hasPet) { guests, children, pet ->
                guestCount = guests
                childrenCount = children
                hasPet = pet

                val guestMessage = when {
                    childrenCount == 0 && hasPet -> "$guestCount ${getString(R.string.adult_with_pets)}"
                    childrenCount == 0 -> "$guestCount ${getString(R.string.adult)}"
                    hasPet -> "$guestCount ${getString(R.string.adult)}, $childrenCount ${getString(R.string.children_with_pets)}"
                    else -> "$guestCount ${getString(R.string.adult)}, $childrenCount ${getString(R.string.children)}"
                }

                binding.guestBtn.text = guestMessage

                // Show ChildrenAgeBottomSheet only if childrenCount is greater than 0
                if (childrenCount > 0) {
                    val childrenAgeBottomSheet = ChildrenAgeBottomSheet.newInstance(businessProfileId,
                        userLargeProfilePic,userFullName,businessName,businessIcon,fullAddress
                        ,rating,checkInDate,checkOutDate,guestCount,childrenCount,hasPet,guestMessage)
                    childrenAgeBottomSheet.show(supportFragmentManager, childrenAgeBottomSheet.tag)
                }else{
                    val intent = Intent(this, PlanDetailsActivity::class.java)
                    intent.putExtra("KEY_BUSINESS_PROFILE_ID", businessProfileId)
                    intent.putExtra("KEY_USER_LARGE_PROFILE_PIC", userLargeProfilePic)
                    intent.putExtra("KEY_USER_FULL_NAME", userFullName)
                    intent.putExtra("KEY_BUSINESS_NAME", businessName)
                    intent.putExtra("KEY_BUSINESS_ICON", businessIcon)
                    intent.putExtra("KEY_FULL_ADDRESS", fullAddress)
                    intent.putExtra("KEY_RATING", rating)
                    intent.putExtra("KEY_CHECK_IN_DATE", checkInDate)
                    intent.putExtra("KEY_CHECK_OUT_DATE", checkOutDate)
                    intent.putExtra("KEY_GUEST_COUNT", guestCount)
                    intent.putExtra("KEY_CHILDREN_COUNT", childrenCount)
                    intent.putExtra("KEY_HAS_PET", hasPet)
                    intent.putExtra("KEY_GUEST_MESSAGE", guestMessage)
                    startActivity(intent)
                }
            }
            bottomSheet.show(supportFragmentManager, "GuestAndRoomBottomSheet")
        }



    }





}