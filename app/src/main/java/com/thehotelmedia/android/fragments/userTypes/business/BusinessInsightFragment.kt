package com.thehotelmedia.android.fragments.userTypes.business

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.NotificationActivity
import com.thehotelmedia.android.adapters.dropDown.MonthsAdapter
import com.thehotelmedia.android.adapters.dropDown.TimeingAdapter
import com.thehotelmedia.android.adapters.dropDown.WeeksAdapter
import com.thehotelmedia.android.adapters.dropDown.YearsAdapter
import com.thehotelmedia.android.adapters.userTypes.business.InsightMediaAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentBusinessInsightBinding
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.extensions.capitalizeFirstLetterToLowercase
import com.thehotelmedia.android.modals.insight.Engagements
import com.thehotelmedia.android.modals.insight.InsightModal
import com.thehotelmedia.android.modals.insight.TotalFollowers
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class BusinessInsightFragment : Fragment() {

    private lateinit var binding: FragmentBusinessInsightBinding
    private var selectedOption: String? = "Week"
    private var selectedTiming: String? = ""
    private var selectedDateFromWeek: String? = ""
    private var selectedDateFromYear: String? = ""
    private var selectedStartDateFromMonth: String? = ""
    private var selectedEndDateFromMonth: String? = ""
    private var popupWindow: PopupWindow? = null
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private var isPostAvailable = false
    private var isStoryAvailable = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_business_insight, container, false)
        inItUI()
        initializeAndUpdateNotificationDot()
        return binding.root
    }

    private fun initializeAndUpdateNotificationDot() {
        // Call this in onCreate or whenever needed
        NotificationDotUtil.initializeAndUpdateNotificationDot(
            requireContext(), // Context (Activity)
            binding.redDotView, // The red dot view
            preferenceManager // Your preference manager
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to avoid memory leaks
        NotificationDotUtil.unregisterReceiver(requireContext())
    }


    private fun inItUI() {
        progressBar = CustomProgressBar(requireContext())
        preferenceManager = PreferenceManager.getInstance(requireContext())
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        getInsightData(selectedOption.toString())

        setUpAdapterCurrentDate(selectedOption)


        binding.notificationBtn.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            requireContext().startActivity(intent)
        }

        binding.filterBtn.setOnClickListener { view ->
            showFiltersDropdown(view)
        }

// Set the click listener for timingTv
        binding.timingTv.setOnClickListener {
            when (selectedOption) {
                "Week" -> {
                    val startDate = LocalDate.now().withDayOfMonth(1)
                    val weeksList = getWeeksListWithDates(startDate).map { it.first }
                    setupDropdown(weeksList, it)
                }
                "Month" -> {
                    setupDropdown(getMonthsList(), it)
                }
                "Year" -> {
                    setupDropdown(getYearsList(), it)
                }
            }
        }


        individualViewModal.getInsightResult.observe(viewLifecycleOwner) { result ->

            println("asdkfldasg   $result")
            if (result.status == true) {
                handelInsightDataResults(result)
            } else {
                Toast.makeText(activity, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModal.loading.observe(viewLifecycleOwner) {
            if (it == true) {
                progressBar.show()
            } else {
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(viewLifecycleOwner) {

            Toast.makeText(activity, " insight $it", Toast.LENGTH_SHORT).show()
        }


    }

    private fun handelInsightDataResults(result: InsightModal?) {
        val dashboard = result?.data?.dashboard

        // Data transformation
        val accountReachedData = result?.data?.dashboardData?.accountReached
        val totalFollowersData = result?.data?.dashboardData?.totalFollowers
        val websiteRedirectionData = result?.data?.dashboardData?.websiteRedirection
        val engagementsData = result?.data?.dashboardData?.engagements

        val accountReached      = dashboard?.accountReached ?: "0"
        val websiteRedirection  = dashboard?.websiteRedirection ?: "0"
        val totalFollowers      = dashboard?.totalFollowers ?: "0"
        val engagements         = dashboard?.engagements ?: "0"

        val posts = result?.data?.posts
        val stories = result?.data?.stories



        if (!stories.isNullOrEmpty()){
            isStoryAvailable = true
            val insightMediaAdapterForStories = InsightMediaAdapter(requireContext(), stories, ArrayList())
            binding.storiesRv.adapter = insightMediaAdapterForStories
            binding.storiesTv.visibility  = View.VISIBLE
        }else{
            isStoryAvailable = false
            binding.storiesTv.visibility  = View.GONE
        }

        if (!posts.isNullOrEmpty()){
            val filteredPosts = ArrayList(posts.filter { it.mediaRef.isNotEmpty() })
            val insightMediaAdapterForPosts = InsightMediaAdapter(requireContext(),ArrayList(),filteredPosts)
            binding.postsRv.adapter = insightMediaAdapterForPosts

            if (filteredPosts.isEmpty()){
                isPostAvailable = false
                binding.postsTv.visibility  = View.GONE
            }else{
                isPostAvailable = true
                binding.postsTv.visibility  = View.VISIBLE
            }
        }else{
            binding.postsTv.visibility  = View.GONE
        }


        if (isPostAvailable == false && isStoryAvailable == false){
            binding.contentSharedTv.visibility = View.GONE
        }



        binding.accountReachedTv.text = accountReached.toString()
        binding.websiteRedirectionTv.text = websiteRedirection.toString()
        binding.totalFollowerTv.text = totalFollowers.toString()
        binding.engagedTv.text = engagements.toString()

        val accountReachedEntries = accountReachedData?.mapIndexed { index, accountReached ->
            Entry(index.toFloat(), (accountReached.accountReach ?: 0).toFloat())
        } ?: emptyList()
        val totalFollowerEntries = totalFollowersData?.mapIndexed { index, totalFollower ->
            Entry(index.toFloat(), (totalFollower.followers ?: 0).toFloat())
        } ?: emptyList()
        val websiteRedirectionEntries = websiteRedirectionData?.mapIndexed { index, totalWebsiteRedirection ->
            Entry(index.toFloat(), (totalWebsiteRedirection.redirection ?: 0).toFloat())
        } ?: emptyList()
        val engagementsDataEntries = engagementsData?.mapIndexed { index, totalWebsiteRedirection ->
            Entry(index.toFloat(), (totalWebsiteRedirection.engagement ?: 0).toFloat())
        } ?: emptyList()

        // Setting the transformed data in the chart
        setLineChart(binding.lineChart,accountReachedEntries, totalFollowerEntries,websiteRedirectionEntries,engagementsDataEntries,engagementsData)

    }

    private fun getInsightData(filter: String) {
        val smallFilters = capitalizeFirstLetterToLowercase(filter)
        individualViewModal.getInsight(smallFilters)
    }

    private fun setUpAdapterCurrentDate(selectedOption: String?) {
        // Get the current date
        val today = LocalDate.now()

        // Get the weeks with dates and days
        val weeksListWithDates = getWeeksListWithDates(today.withDayOfMonth(1))

        when (selectedOption) {
            "Week" -> {
                // Find the current week index
                val currentWeekIndex = weeksListWithDates.indexOfFirst { week ->
                    val weekStartDate = LocalDate.parse(
                        String.format("%d-%02d-01", today.year, today.monthValue)
                    ).plusWeeks(weeksListWithDates.indexOf(week).toLong())
                    val weekEndDate = weekStartDate.plusDays(6)
                    today.isAfter(weekStartDate.minusDays(1)) && today.isBefore(weekEndDate.plusDays(1))
                }

                // Ensure we have a valid week index
                if (currentWeekIndex != -1) {
                    // Get the selected week (dates and days)
                    val selectedWeek = weeksListWithDates[currentWeekIndex]
                    val weekDaysAndDates = selectedWeek.second
                    selectedTiming = "Week ${(currentWeekIndex + 1)}"
                    // Pass the selected week's days and dates to the adapter
                    val weeksAdapter = WeeksAdapter(requireContext(), weekDaysAndDates, ::onDateSelected,selectedTiming!!)
                    // Get the current date
                    val currentDate = LocalDate.now()
                    // Format the current date to "dd" format
                    val dayOfMonth = currentDate.format(DateTimeFormatter.ofPattern("dd"))
                    weeksAdapter.setSelectedDate(dayOfMonth)
                    binding.dynamicDateRv.adapter = weeksAdapter


                } else {
                    // Handle case where no current week is found
                    println("No current week found.")
                }

            }

            "Month" -> {
                // Get weeks for the current month
                val weeksInMonth = getWeeksInMonth(today.month, today.year)
// Determine and print the ongoing month
                val currentMonths = today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                selectedTiming = currentMonths
                // Set up the months adapter with the weeks for the current month
                val monthsAdapter = MonthsAdapter(requireContext(), weeksInMonth,selectedTiming!!,::handleSelectedTime)
                binding.dynamicDateRv.adapter = monthsAdapter



                println("Ongoing Month: $currentMonths")
                println("Month: ${today.month}, Year: ${today.year}")
                println("Weeks in month: $weeksInMonth")
            }

            "Year" -> {
                // Get the selected year based on week index (or some other index)
                val weekIndex = 0  // Replace with actual index from your logic
                val selectedYear = getYearsList()[weekIndex].toInt()

                // Map the months from Month.entries, formatting the month name and number
                val monthsList = Month.values().mapIndexed { i, month ->
                    Pair(
                        month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        String.format("%02d", i + 1)
                    )
                }
                selectedTiming = selectedYear.toString()
                // Set up the YearsAdapter with the months list and a callback function
                val yearsAdapter = YearsAdapter(requireContext(), monthsList,selectedTiming,::onYearsDateSelected)
                yearsAdapter.updateSelectedPositionForCurrentDate()
                // Set the adapter to the RecyclerView
                binding.dynamicDateRv.adapter = yearsAdapter

                println("Ongoing Year: $selectedYear")
                println("Selected year: $selectedYear, months: $monthsList")
            }
        }

        binding.timingTv.text = selectedTiming
    }






    // Setup AutoCompleteTextView for dropdown without dialog
    private fun setupDropdown(options: List<String>, anchor: View) {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.insight_dynamic_dropdown_item, null)

        popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val timingAdapter = TimeingAdapter(requireContext(), options, ::onSelected,selectedTiming)
        val rv: RecyclerView = dropdownView.findViewById(R.id.mutiItemsRv)
        rv.adapter = timingAdapter

        popupWindow?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.popup_background))
        popupWindow?.showAsDropDown(anchor)

        popupWindow?.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }


    private fun onSelected(weekIndex: Int) {
        when (selectedOption) {
            "Week" -> {
                // Get the weeks with dates and days
                val weeksListWithDates = getWeeksListWithDates(LocalDate.now().withDayOfMonth(1))

                // Get the selected week (dates and days)
                val selectedWeek = weeksListWithDates[weekIndex]
                val weekLabel = selectedWeek.first
                val weekDaysAndDates = selectedWeek.second

                println("sadfjakldfjkaslj  weeksListWithDates $weeksListWithDates")
                println("sadfjakldfjkaslj   selectedWeek $selectedWeek")
                println("sadfjakldfjkaslj   weekDaysAndDates $weekDaysAndDates")
                selectedTiming = weekLabel
                // Pass the selected week's days and dates to the adapter
                val weeksAdapter = WeeksAdapter(requireContext(), weekDaysAndDates, ::onDateSelected,selectedTiming!!)
                val currentDate = LocalDate.now()
                // Format the current date to "dd" format
                val dayOfMonth = currentDate.format(DateTimeFormatter.ofPattern("dd"))
                weeksAdapter.setSelectedDate(dayOfMonth)
                binding.dynamicDateRv.adapter = weeksAdapter

            }

            "Month" -> {
                println("sadfjakldfjkaslj   enetrew")
                val selectedMonth = Month.entries[weekIndex].getDisplayName(TextStyle.FULL, Locale.getDefault())
                val selectedYear = LocalDate.now().year

                println("sadfjakldfjkaslj   $selectedMonth $selectedYear")
                val weeksInMonth = getWeeksInMonth(Month.entries[weekIndex], selectedYear)
                selectedTiming = selectedMonth
                val monthsAdapter = MonthsAdapter(requireContext(), weeksInMonth, selectedTiming!!,::handleSelectedTime)
                binding.dynamicDateRv.adapter = monthsAdapter

            }

            "Year" -> {
                val selectedYear = getYearsList()[weekIndex].toInt()
                val monthsList = Month.entries.mapIndexed { i, month ->
                    Pair(month.getDisplayName(TextStyle.FULL, Locale.getDefault()), String.format("%02d", i + 1))
                }
                selectedTiming = selectedYear.toString()
                val yearsAdapter = YearsAdapter(requireContext(), monthsList,selectedTiming,::onYearsDateSelected)
                yearsAdapter.updateSelectedPositionForCurrentDate()
                binding.dynamicDateRv.adapter = yearsAdapter

            }
        }
        binding.timingTv.text = selectedTiming

        popupWindow?.dismiss()
    }



    private fun onDateSelected(selectedDateData: String) {
//        Toast.makeText(requireContext(), "selectedDate: $selectedDateData", Toast.LENGTH_SHORT).show()
        selectedDateFromWeek = selectedDateData
    }

    private fun onYearsDateSelected(formattedDate: String) {
//        Toast.makeText(requireContext(), "Selected Date: $formattedDate", Toast.LENGTH_SHORT).show()
        selectedDateFromYear = formattedDate
    }

    private fun handleSelectedTime(startTime: String, endTime: String) {
        // Print the start and end time to the console (or any other logic)
        println("Selected Start Time: $startTime")
        println("Selected End Time: $endTime")
        selectedStartDateFromMonth = startTime
        selectedEndDateFromMonth = endTime

        // Show the start and end time in a Toast message
//        Toast.makeText(context, "Start: $startTime\nEnd: $endTime", Toast.LENGTH_SHORT).show()
    }

    private fun getWeeksInMonth(month: Month, year: Int): List<Pair<String, Pair<String, String>>> {
        val startDate = LocalDate.of(year, month, 1)
        val weeksListWithDates = getWeeksListWithDates(startDate)
        return weeksListWithDates.map { week ->
            val weekLabel = week.first
            val weekDays = week.second
            val startDate = weekDays.first().second
            val endDate = weekDays.last().second
            Pair(weekLabel, Pair(startDate, endDate))
        }
    }

    private fun getWeeksListWithDates(startDate: LocalDate): List<Pair<String, List<Pair<String, String>>>> {
        val weeksList = mutableListOf<Pair<String, List<Pair<String, String>>>>()
        var currentDay = startDate
        var weekNumber = 1

        while (currentDay.month == startDate.month) {
            val weekStart = currentDay
            val weekEnd = currentDay.plusDays(6).withMonth(startDate.monthValue)

            // Create a list of day names and dates for the current week
            val weekDays = (0..6).map { i ->
                val day = weekStart.plusDays(i.toLong())
                Pair(day.dayOfWeek.name.take(3), day.dayOfMonth.toString())
            }

            weeksList.add(Pair("Week $weekNumber", weekDays))
            weekNumber++
            currentDay = currentDay.plusWeeks(1)
        }

        return weeksList
    }

    // Generate list of months in the current year
    private fun getMonthsList(): List<String> {
        return Month.entries.map { month ->
            month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
    }

    // Generate list of last 10 years
    private fun getYearsList(): List<String> {
        val currentYear = LocalDate.now().year
        return (currentYear downTo currentYear - 10).map { it.toString() }
    }



    private fun showFiltersDropdown(anchor: View) {
        // Inflate the dropdown menu layout
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.insight_filter_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
        val textWeek: androidx.appcompat.widget.LinearLayoutCompat = dropdownView.findViewById(R.id.textWeek)
        val textMonth: androidx.appcompat.widget.LinearLayoutCompat = dropdownView.findViewById(R.id.textMonth)
        val textYear: androidx.appcompat.widget.LinearLayoutCompat = dropdownView.findViewById(R.id.textYear)

        // Set background based on the selected option
        updateBackgrounds(textWeek, textMonth, textYear)

        textWeek.setOnClickListener {
            handleDropdownItemClick(popupWindow, "Week", textWeek, textMonth, textYear)
        }

        textMonth.setOnClickListener {
            handleDropdownItemClick(popupWindow, "Month", textWeek, textMonth, textYear)
        }

        textYear.setOnClickListener {
            handleDropdownItemClick(popupWindow, "Year", textWeek, textMonth, textYear)
        }

        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(anchor)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    private fun handleDropdownItemClick(
        popupWindow: PopupWindow,
        selectedItem: String,
        vararg textViews: androidx.appcompat.widget.LinearLayoutCompat
    ) {
        // Handle the dropdown item click
        println("Selected item: $selectedItem")


        // Set the selected option
        selectedOption = selectedItem
        setUpAdapterCurrentDate(selectedItem)

        // Determine and print the ongoing week, month, or year
        val currentDate = LocalDate.now()

        when (selectedItem) {
            "Week" -> {
                val weeksListWithDates = getWeeksListWithDates(currentDate.withDayOfMonth(1))
                val currentWeekIndex = weeksListWithDates.indexOfFirst { week ->
                    val weekStartDate = LocalDate.parse(
                        String.format("%d-%02d-01", currentDate.year, currentDate.monthValue)
                    ).plusWeeks(weeksListWithDates.indexOf(week).toLong())
                    val weekEndDate = weekStartDate.plusDays(6)

                    currentDate.isAfter(weekStartDate.minusDays(1)) && currentDate.isBefore(
                        weekEndDate.plusDays(1)
                    )
                }

                if (currentWeekIndex != -1) {
                    println("Ongoing Week: Week ${currentWeekIndex + 1}")
                    selectedTiming = "Week ${(currentWeekIndex + 1).toString()}"
                } else {
                    println("No ongoing week found.")
                }
            }

            "Month" -> {
                val currentMonth = currentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                selectedTiming = currentMonth
                println("Ongoing Month: $currentMonth")
            }

            "Year" -> {
                val currentYear = currentDate.year
                selectedTiming = currentYear.toString()
                println("Ongoing Year: $currentYear")
            }
        }

        getInsightData(selectedOption.toString())

        updateBackgrounds(*textViews)
        binding.timingTv.text = selectedTiming
        // Dismiss the popup
        popupWindow.dismiss()
    }


    private fun updateBackgrounds(vararg textViews: androidx.appcompat.widget.LinearLayoutCompat) {
        textViews.forEach { textView ->
            when (textView.id) {
                R.id.textWeek -> {
                    if (selectedOption == "Week") {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_selected)
                    } else {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_unselected)
                    }
                }
                R.id.textMonth -> {
                    if (selectedOption == "Month") {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_selected)
                    } else {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_unselected)
                    }
                }
                R.id.textYear -> {
                    if (selectedOption == "Year") {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_selected)
                    } else {
                        textView.setBackgroundResource(R.drawable.selected_text_popup_background_unselected)
                    }
                }
            }
        }
    }
    private fun setLineChart(
        chart: LineChart,
        accountReachedEntries: List<Entry>,
        totalFollowerEntries: List<Entry>,
        websiteRedirectionEntries: List<Entry>,
        engagementsDataEntries: List<Entry>,
        engagementsData: ArrayList<Engagements>?
    ) {

        // Create LineDataSet
        val accountReachedDataSet = LineDataSet(accountReachedEntries, "AccountReached").apply {
            setDrawValues(false)
            setDrawCircles(false) // Show circles on points
            color = ContextCompat.getColor(requireContext(), R.color.graph_line_yellow)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.graph_line_yellow)) // Circle color
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }
        // Create LineDataSet
        val totalFollowerDataSet = LineDataSet(totalFollowerEntries, "TotalFollowers").apply {
            setDrawValues(false)
            setDrawCircles(false) // Show circles on points
            color = ContextCompat.getColor(requireContext(), R.color.graph_line_purple)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.graph_line_purple)) // Circle color
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        // Second Line (Green)
        val websiteRedirectionEntriesDataSet = LineDataSet(websiteRedirectionEntries, "WebsiteRedirection").apply {
            setDrawValues(false)
            setDrawCircles(false) // Show circles on points
            color = ContextCompat.getColor(requireContext(), R.color.graph_line_pink)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.graph_line_pink)) // Circle color
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        // Second Line (Green)
        val engagementsDataEntriesDataSet = LineDataSet(engagementsDataEntries, "Engaged").apply {
            setDrawValues(false)
            setDrawCircles(false) // Show circles on points
            color = ContextCompat.getColor(requireContext(), R.color.graph_line_green)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.graph_line_green)) // Circle color
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        val data = LineData(accountReachedDataSet,totalFollowerDataSet,websiteRedirectionEntriesDataSet,engagementsDataEntriesDataSet)

        // Configure XAxis
        val xAxis: XAxis = chart.xAxis
        xAxis.apply {
            textColor = ContextCompat.getColor(requireContext(), R.color.text_color_60)
            axisLineColor = ContextCompat.getColor(requireContext(), R.color.faded_round_btn)
            axisLineWidth = 1.5f
            setDrawGridLines(false)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = totalFollowerEntries.size

            // Custom value formatter for day labels
            valueFormatter = object : ValueFormatter() {
                private val days = engagementsData?.map { it.labelName } ?: emptyList()
                override fun getAxisLabel(value: Float, axis: AxisBase?): String? {
                    val index = value.toInt()
                    return if (index >= 0 && index < days.size) days[index] else ""
                }
            }
        }

        // Configure YAxis (left axis)
        val yAxisLeft: YAxis = chart.axisLeft
        yAxisLeft.apply {
            textColor = ContextCompat.getColor(requireContext(), R.color.text_color_60)
            axisLineColor = ContextCompat.getColor(requireContext(), R.color.faded_round_btn)
            axisLineWidth = 1.5f
            axisMinimum = 0f
            setDrawGridLines(false)
            setDrawAxisLine(true)
        }

        // Disable the right YAxis
        chart.axisRight.isEnabled = false

        // Chart configurations
        chart.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setPinchZoom(false)
            setScaleEnabled(false)

//            // Draw borders for left, bottom lines
//            axisLeft.setDrawAxisLine(true)
//            xAxis.setDrawAxisLine(true)
            invalidate() // Refresh the chart
        }

    }




//    private fun setLineChart(
//        chart: LineChart,
//        entries1: List<Entry>,
//        entries2: List<Entry>,
//        entries3: List<Entry>,
//        entries4: List<Entry>
//    ) {
//        // First Line (Red)
//
//        val dataSet1 = LineDataSet(entries1, "Line 1").apply {
//            setDrawValues(false)
//            setDrawCircles(false)
//            color = ContextCompat.getColor(requireContext(), R.color.graph_line_pink)
//            setDrawFilled(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            cubicIntensity = 0.2f
//        }
//
//        // Second Line (Green)
//        val dataSet2 = LineDataSet(entries2, "Line 2").apply {
//            setDrawValues(false)
//            setDrawCircles(false)
//            color = ContextCompat.getColor(requireContext(), R.color.graph_line_green)
//            setDrawFilled(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            cubicIntensity = 0.2f
//        }
//
//        // Third Line (Orange)
//
//        val dataSet3 = LineDataSet(entries3, "Line 3").apply {
//            setDrawValues(false)
//            setDrawCircles(false)
//            color = ContextCompat.getColor(requireContext(), R.color.graph_line_purple)
//            setDrawFilled(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            cubicIntensity = 0.2f
//        }
//
//        // Fourth Line (Yellow)
//        val dataSet4 = LineDataSet(entries4, "Line 4").apply {
//            setDrawValues(false)
//            setDrawCircles(false)
//            color = ContextCompat.getColor(requireContext(), R.color.graph_line_yellow)
//            setDrawFilled(false)
//            mode = LineDataSet.Mode.CUBIC_BEZIER
//            cubicIntensity = 0.2f
//        }
//
//        val data = LineData(dataSet1, dataSet2, dataSet3, dataSet4)
//
//        // Configure XAxis (bottom axis)
//        val xAxis: XAxis = chart.xAxis
//        xAxis.apply {
//            textColor = ContextCompat.getColor(requireContext(), R.color.white_60) // Set text color to blue
//            axisLineColor = ContextCompat.getColor(requireContext(), R.color.blue_50) // Set axis line color to blue
//            axisLineWidth = 1.5f // Set axis line width (2dp)
//            setDrawGridLines(false) // Remove grid lines inside graph
//            position = XAxis.XAxisPosition.BOTTOM
//            granularity = 1f
//            labelCount = 12
//
//            // Custom value formatter for month labels
//            valueFormatter = object : ValueFormatter() {
//                private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
//                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
//                    val index = value.toInt()
//                    return if (index >= 0 && index < months.size) months[index] else ""
//                }
//            }
//
//            setDrawAxisLine(true) // Draw the X-axis line
//        }
//
//        // Configure YAxis (left axis)
//        val yAxisLeft: YAxis = chart.axisLeft
//        yAxisLeft.apply {
//            textColor = ContextCompat.getColor(requireContext(), R.color.white_60) // Set text color to blue
//            axisLineColor = ContextCompat.getColor(requireContext(), R.color.blue_50) // Set axis line color to blue
//            axisLineWidth = 1.5f // Set axis line width (2dp)
//            axisMinimum = 0f
//            axisMaximum = 100f
//            granularity = 10f
//            setDrawGridLines(false) // Remove grid lines inside graph
//            setDrawAxisLine(true) // Draw the Y-axis line
//        }
//
//        // Disable the right YAxis (no line or grid needed)
//        chart.axisRight.isEnabled = false
//
//        chart.apply {
//            this.data = data
//            description.isEnabled = false
//            legend.isEnabled = false
//            setDrawGridBackground(false)
//            setPinchZoom(false)
//            setScaleEnabled(false)
//
//            // Draw borders for left, bottom lines
//            axisLeft.setDrawAxisLine(true)
//            xAxis.setDrawAxisLine(true)
//
//            // Refresh the chart
//            invalidate()
//        }
//    }



}