package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.dropDown.BusinessTypeAdapter
import com.thehotelmedia.android.adapters.dropDown.Businesses
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants.business_type_individual
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.customClasses.SuccessGiff
import com.thehotelmedia.android.databinding.ActivityPostJobBinding
import com.thehotelmedia.android.extensions.navigateToMainActivity
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PostJobActivity : BaseActivity() {

    private lateinit var binding: ActivityPostJobBinding

    private lateinit var designationList: List<Businesses>
    private lateinit var jobTypeList: List<Businesses>
    private lateinit var experienceList: List<Businesses>
    private lateinit var salaryOfferList: List<Businesses>
    private lateinit var vacancyList: List<Businesses>

    private var selectedJobTitle= ""
    private var selectedJobDescription= ""

    private var selectedDesignation= ""
    private var selectedDesignationId = ""

    private var selectedJobType = ""
    private var selectedJobTypeId = ""

    private var selectedExperience = ""
    private var selectedExperienceId = ""

    private var selectedSalaryOffer = ""
    private var selectedSalaryOfferId = ""

    private var selectedVacancy = ""
    private var selectedVacancyId = ""

    private var selectedJoiningCalendar: Calendar = Calendar.getInstance() // Store selected date
    private var selectedJoiningDate = ""


    private lateinit var successGiff: SuccessGiff
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager
    private var businessesType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)
        successGiff = SuccessGiff(this)
        preferenceManager = PreferenceManager.getInstance(this@PostJobActivity)
        businessesType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE,"").toString()


        initJobData()
        initUi()
    }

    private fun initJobData() {
        designationList = listOf(
            Businesses("Hotel Manager", "", "1"),
            Businesses("Front Desk Receptionist", "", "2"),
            Businesses("Housekeeping Supervisor", "", "3"),
            Businesses("Chef", "", "4"),
            Businesses("Sous Chef", "", "5"),
            Businesses("Bartender", "", "6"),
            Businesses("Waiter/Waitress", "", "7"),
            Businesses("Concierge", "", "8"),
            Businesses("Event Coordinator", "", "9"),
            Businesses("Hotel Accountant", "", "10")
        )


        jobTypeList = listOf(
            Businesses("Full Time", "", "101"),
            Businesses("Part Time", "", "102"),
            Businesses("Internship", "", "103"),
            Businesses("Freelance", "", "104"),
            Businesses("Contract", "", "105"),
            Businesses("Temporary", "", "106"),
            Businesses("Volunteer", "", "107")
        )

        experienceList = listOf(
            Businesses("Entry Level", "", "201"),
            Businesses("1-3 Years", "", "202"),
            Businesses("3-5 Years", "", "203"),
            Businesses("5-10 Years", "", "204"),
            Businesses("10+ Years", "", "205")
        )

        salaryOfferList = listOf(
            Businesses("15,000 - 25,000", "", "301"),
            Businesses("25,000 - 35,000", "", "302"),
            Businesses("35,000 - 50,000", "", "303"),
            Businesses("50,000 - 75,000", "", "304"),
            Businesses("75,000+", "", "305")
        )

        vacancyList = listOf(
            Businesses("1 Vacancy", "", "401"),
            Businesses("2-3 Vacancies", "", "402"),
            Businesses("4-5 Vacancies", "", "403"),
            Businesses("6+ Vacancies", "", "404"),
            Businesses("Multiple Openings", "", "405")
        )
    }

    private fun initUi() {

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupDropdown(binding.designationTv, designationList, ::setSelectedDesignation)
        setupDropdown(binding.jobTypeTv, jobTypeList, ::setSelectedJobType)
        setupDropdown(binding.experienceTv, experienceList, ::setSelectedExperience)
        setupDropdown(binding.salaryOfferTv, salaryOfferList, ::setSelectedSalaryOffer)
        setupDropdown(binding.vacancyTv, vacancyList, ::setSelectedVacancy)


        binding.joiningDateEt.setOnClickListener {
            openDatePicker()
        }

        binding.submitBtn.setOnClickListener {
            if (validateFields()) {

                val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToPostJob(this))
                bottomSheet.onYesClicked = {
                    // Proceed with job post submission logic here
                    individualViewModal.postJob(selectedJobTitle,selectedDesignation,selectedJobDescription,selectedJobType,selectedSalaryOffer,selectedJoiningDate,selectedVacancy,selectedExperience)
                }
                bottomSheet.onNoClicked = {

                }
                bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")

            }
        }

        individualViewModal.postJobResult.observe(this@PostJobActivity){result->
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

        individualViewModal.loading.observe(this@PostJobActivity){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this@PostJobActivity){
            showSnackBar(it)
        }


    }

    private fun setupDropdown(
        dropdownView: AutoCompleteTextView,
        dataList: List<Businesses>,
        onItemSelected: (Businesses) -> Unit
    ) {
        dropdownView.apply {
            setDropDownBackgroundDrawable(ContextCompat.getDrawable(this@PostJobActivity, R.drawable.blured_background))
            dropDownVerticalOffset = height + 30
            val adapter = BusinessTypeAdapter(this@PostJobActivity, dataList)
            setAdapter(adapter)
            setOnItemClickListener { parent, _, position, _ ->
                val selectedItem = parent.getItemAtPosition(position) as Businesses
                setTextColor(ContextCompat.getColor(this@PostJobActivity, R.color.text_color))
                onItemSelected(selectedItem)
            }
        }
    }


    private fun setSelectedDesignation(item: Businesses) {
        selectedDesignation = item.name
        selectedDesignationId = item.id
    }

    private fun setSelectedJobType(item: Businesses) {
        selectedJobType = item.name
        selectedJobTypeId = item.id
    }

    private fun setSelectedExperience(item: Businesses) {
        selectedExperience = item.name
        selectedExperienceId = item.id
    }

    private fun setSelectedSalaryOffer(item: Businesses) {
        selectedSalaryOffer = item.name
        selectedSalaryOfferId = item.id
    }

    private fun setSelectedVacancy(item: Businesses) {
        selectedVacancy = item.name
        selectedVacancyId = item.id
    }

    private fun openDatePicker() {
        val today = Calendar.getInstance()

        val year = selectedJoiningCalendar.get(Calendar.YEAR)
        val month = selectedJoiningCalendar.get(Calendar.MONTH)
        val day = selectedJoiningCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            ContextThemeWrapper(this, R.style.BlackDatePickerDialog), // Apply the custom style
            { _, selectedYear, selectedMonth, selectedDay ->
                // Store the selected date
                selectedJoiningCalendar.set(selectedYear, selectedMonth, selectedDay)

                // Format and display the date
                val formattedDate1 = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(selectedJoiningCalendar.time)
                selectedJoiningDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(selectedJoiningCalendar.time)

                binding.joiningDateEt.setText(formattedDate1.toString())

            },
            year, month, day
        )

        //Correct way to apply drawable with rounded corners
        datePickerDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background_14)
        )

        // Set the minimum selectable date to today (restrict past dates)
        datePickerDialog.datePicker.minDate = today.timeInMillis

        datePickerDialog.show()
    }



    private fun validateFields(): Boolean {
        selectedJobTitle = binding.jobTitleEt.text.toString().trim()
        selectedJobDescription = binding.jobDescriptionEt.text.toString().trim()

        if (selectedJobTitle.isBlank()) {
            showSnackBar(getString(R.string.please_enter_job_title))
            return false
        }
        if (selectedDesignationId.isEmpty()) {
            showSnackBar(getString(R.string.please_select_designation))
            return false
        }
        if (selectedJobDescription.isBlank()) {
            showSnackBar(getString(R.string.please_enter_description))
            return false
        }
        if (selectedJobTypeId.isEmpty()) {
            showSnackBar(getString(R.string.please_select_job_type))
            return false
        }
        if (selectedExperienceId.isEmpty()) {
            showSnackBar(getString(R.string.please_select_experience))
            return false
        }
        if (selectedSalaryOfferId.isEmpty()) {
            showSnackBar(getString(R.string.please_select_salary_offer))
            return false
        }
        if (selectedVacancyId.isEmpty()) {
            showSnackBar(getString(R.string.please_select_vacancy))
            return false
        }
        if (selectedJoiningDate.isBlank()) {
            showSnackBar(getString(R.string.please_select_joining_date))
            return false
        }
        return true
    }

    private fun showSnackBar(message: String) {
        CustomSnackBar.showSnackBar(binding.root,message)
    }

}
