package com.thehotelmedia.android.activity.booking

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.bookTable.TimeSlot
import com.thehotelmedia.android.adapters.bookTable.TimeSlotAdapter
import com.thehotelmedia.android.adapters.bookTable.SelectedDateAdapter
import com.thehotelmedia.android.adapters.bookTable.SelectedYearMonthAdapter
import com.thehotelmedia.android.customClasses.ColorFilterTransformation
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.databinding.ActivityBookTableBinding
import com.thehotelmedia.android.extensions.convertTo24HourFormat
import com.thehotelmedia.android.extensions.generateTimeSlots
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.setRatingWithStarWithoutBracket
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.modals.booking.bookTable.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BookTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookTableBinding
    private var guestCount = 1
    private val minGuestRequirement = 1

    private var selectedSlotTiming = ""
    private var selectedSlotDate = ""

    private var popupWindow: PopupWindow? = null


    private var selectedYear: Int = LocalDate.now().year
    private var selectedMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM")) // Get current month in "Jan", "Feb" format
    private var businessesType: String = ""
    private lateinit var successGiff: SuccessGiff
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)
        successGiff = SuccessGiff(this)
        preferenceManager = PreferenceManager.getInstance(this@BookTableActivity)
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()


        initUi()
    }

    private fun initUi() = binding.apply {

        val businessProfileId = intent.getStringExtra("KEY_BUSINESS_PROFILE_ID") ?: ""
        val userLargeProfilePic = intent.getStringExtra("KEY_USER_LARGE_PROFILE_PIC") ?: ""
        val userFullName = intent.getStringExtra("KEY_USER_FULL_NAME") ?: ""
        val businessName = intent.getStringExtra("KEY_BUSINESS_NAME") ?: ""
        val businessIcon = intent.getStringExtra("KEY_BUSINESS_ICON") ?: ""
        val fullAddress = intent.getStringExtra("KEY_FULL_ADDRESS") ?: ""
        val rating = intent.getDoubleExtra("KEY_RATING", 0.0) // Default to 0.0 if not found

        Glide.with(this@BookTableActivity).load(businessIcon)
            .placeholder(R.drawable.ic_hotel)
            .transform(ColorFilterTransformation(ContextCompat.getColor(this@BookTableActivity, R.color.white_40)))
            .into(binding.businessIconIv)

        Glide.with(this@BookTableActivity).load(userLargeProfilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.hotelProfileIv)
        binding.hotelNameTv.text = userFullName
        binding.addressTv.text = fullAddress
        binding.businessTypeTv.text = businessName
        binding.averageRatingTv.setRatingWithStarWithoutBracket(rating, R.drawable.ic_rating_star)



        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Set default selected values
        binding.timingTv.text = selectedYear.toString()
        binding.monthTv.text = selectedMonth


        binding.timingTv.setOnClickListener {
            setupDropdown(getYearsList(), it, true)
        }

        binding.monthTv.setOnClickListener {
            setupDropdown(getAvailableMonths(selectedYear), it, false)
        }

        roomNumberTv.text = guestCount.toString()
        decBtn.toggleEnable(guestCount > minGuestRequirement)

        addBtn.setOnClickListener {
            guestCount++
            updateGuestCount()
        }

        decBtn.setOnClickListener {
            if (guestCount > minGuestRequirement) {
                guestCount--
                updateGuestCount()
            }
        }

        backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }



        setupRecyclerView(selectedMonth,selectedYear.toString())



        binding.submitBtn.setOnClickListener {

            if (selectedSlotDate.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.select_date_message))
                return@setOnClickListener
            }
            if (selectedSlotTiming.isEmpty()) {
                CustomSnackBar.showSnackBar(binding.root, getString(R.string.select_time_slot_message))
                return@setOnClickListener
            }

            val time24Hour = convertTo24HourFormat(selectedSlotTiming)

            println("asjklsjdgk    selectedSlotDate $selectedSlotDate")
            println("asjklsjdgk    selectedSlotTiming $selectedSlotTiming")
            println("asjklsjdgk    guestCount $guestCount")
            println("asjklsjdgk    businessProfileId $businessProfileId")

            individualViewModal.bookATable(guestCount.toString(),selectedSlotDate,time24Hour,businessProfileId)
        }


        individualViewModal.bookATableResult.observe(this@BookTableActivity){result->
            if (result.status==true){


                result.message?.let { msg ->
                    runOnUiThread {
                        successGiff.show(msg) {
                            navigateToMainActivity(businessesType == business_type_individual)
                        }
                    }
                }
            }else{
                val msg = result.message.toString()
                showToast(msg)
            }
        }

        individualViewModal.loading.observe(this@BookTableActivity){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this@BookTableActivity){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    private fun handelBookTableResult(data: Data?) {

    }

    private fun updateGuestCount() = binding.apply {
        roomNumberTv.text = guestCount.toString()
        decBtn.toggleEnable(guestCount > minGuestRequirement)
    }

    private fun setupRecyclerView(month: String, year: String) {

        val monthValue = getMonthNumber(month) // Convert "Apr" -> 4, etc.
        val yearValue = year.toInt()
        // Start date of the selected month & year
        val startDate = LocalDate.of(yearValue, monthValue, 1)

        // Generate weeks list based on selected month & year
        val weeksList = getWeeksList(startDate)

        // Find the current week index only if the selected month is the current month
        val today = LocalDate.now()
        val isCurrentMonth = today.monthValue == monthValue && today.year == yearValue

        val selectedWeekDays: List<Pair<String, String>> = if (isCurrentMonth) {
            // Get all days from today till end of the month
            weeksList.flatMap { it.second }
                .filter { (_, day) -> day.toInt() >= today.dayOfMonth } // Filter dates from today onwards
        } else {
            // If future month is selected, show the full month data
            weeksList.flatMap { it.second }
        }

        // Set RecyclerView adapter with full month data if it's a future month
        binding.dynamicDateRv.adapter = SelectedDateAdapter(this, selectedWeekDays, ::onDateSelected, month,year)
    }

    // Function to generate weeks list for a given month & year
    private fun getWeeksList(startDate: LocalDate): List<Pair<String, List<Pair<String, String>>>> {
        val weeksList = mutableListOf<Pair<String, List<Pair<String, String>>>>()
        val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth()) // Last date of the month

        var currentDate = startDate
        var weekNumber = 1

        while (currentDate <= endDate) {
            val weekDays = mutableListOf<Pair<String, String>>()

            for (i in 0..6) {
                if (currentDate > endDate) break
                weekDays.add(Pair(currentDate.dayOfWeek.name.take(3), currentDate.dayOfMonth.toString()))
                currentDate = currentDate.plusDays(1)
            }

            weeksList.add("Week $weekNumber" to weekDays)
            weekNumber++
        }
        return weeksList
    }

    // Function to convert "Jan", "Feb", etc., to month number
    private fun getMonthNumber(month: String): Int {
        return when (month) {
            "Jan" -> 1
            "Feb" -> 2
            "Mar" -> 3
            "Apr" -> 4
            "May" -> 5
            "Jun" -> 6
            "Jul" -> 7
            "Aug" -> 8
            "Sep" -> 9
            "Oct" -> 10
            "Nov" -> 11
            "Dec" -> 12
            else -> LocalDate.now().monthValue
        }
    }

    // Function to handle date selection
    private fun onDateSelected(selectedDateData: String) {

        selectedSlotDate = selectedDateData

        if (selectedSlotDate.isNotEmpty()){

            binding.recyclerViewTimeSlots.visibility = View.VISIBLE
            binding.slotTv.visibility = View.VISIBLE

            val selectedDate = LocalDate.parse(selectedDateData) // Parse selected date from parameter
            val todayDate = LocalDate.now() // Get today's date
            val currentTime = LocalTime.now() // Get current time
            // Declare slots variable
            val slots: List<String>
            when {
                selectedDate.isAfter(todayDate) -> {
                    // Future date: Show full list of time slots
                    slots = generateTimeSlots()
                    println("Available Time Slots for Future Date: $slots")
                }
                selectedDate.isEqual(todayDate) -> {
                    // Today: Show only slots after current time
                    slots = generateTimeSlots(currentTime)
                    println("Available Time Slots for Today (After Current Time): $slots")
                }
                else -> {
                    // Past date: No slots available
                    slots = emptyList()
                    println("No available slots! Selected date is in the past.")
                }
            }
            setUptimeSlotRecycelrView(slots)
        }else{
            selectedSlotTiming = ""
            binding.recyclerViewTimeSlots.visibility = View.GONE
            binding.slotTv.visibility = View.GONE
        }
    }

    private fun setUptimeSlotRecycelrView(slots: List<String>) {
        val timeSlotList = slots.map { TimeSlot(it) } // Convert to TimeSlot objects

        val adapter = TimeSlotAdapter(this,timeSlotList) { selectedSlot ->
            println("Selected Time Slot: ${selectedSlot.time}")
            selectedSlotTiming = selectedSlot.time
        }
//        val layoutManager = GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false)

        // Dynamically set layout manager
        val layoutManager = if (slots.size > 12) {
            GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false) // Horizontal scrolling
        } else if(slots.size > 6){
            GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false) // Horizontal scrolling
        } else {
            GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false) // Horizontal scrolling
        }


        binding.recyclerViewTimeSlots.layoutManager = layoutManager
        binding.recyclerViewTimeSlots.adapter = adapter
    }


    private fun setupDropdown(options: List<String>, anchor: View, isYear: Boolean) {
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.insight_dynamic_dropdown_item, null)

        popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val adapter = SelectedYearMonthAdapter(this, options, isYear, ::onItemSelected, if (isYear) selectedYear.toString() else selectedMonth)
        val rv: RecyclerView = dropdownView.findViewById(R.id.mutiItemsRv)
        rv.adapter = adapter

        popupWindow?.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))
        popupWindow?.showAsDropDown(anchor)

        popupWindow?.setOnDismissListener {
            // Handle actions when popup is dismissed
        }
    }

    private fun onItemSelected(index: Int, isYear: Boolean) {
        if (isYear) {
            selectedYear = getYearsList()[index].toInt()
            binding.timingTv.text = selectedYear.toString()
            // Reset month selection if current year is selected
            if (selectedYear == LocalDate.now().year) {
                selectedMonth = LocalDate.now().month.name.take(3)
            }
        } else {
            selectedMonth = getAvailableMonths(selectedYear)[index]
            binding.monthTv.text = selectedMonth
        }
        setupRecyclerView(selectedMonth,selectedYear.toString())
        popupWindow?.dismiss()
    }

    // Generates a list of last 10 years including 3 future years
    private fun getYearsList(): List<String> {
        val currentYear = LocalDate.now().year
        return (currentYear..currentYear + 3).map { it.toString() }
    }

    // Generates months based on the selected year
    private fun getAvailableMonths(year: Int): List<String> {
        val allMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val currentMonthIndex = LocalDate.now().monthValue - 1 // 0-based index

        return if (year == LocalDate.now().year) {
            allMonths.subList(currentMonthIndex, allMonths.size) // Only current & future months
        } else {
            allMonths // All months selectable
        }
    }

}
