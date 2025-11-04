package com.thehotelmedia.android.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.thehotelmedia.android.UIState.Status
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.databinding.ActivityJobDetailBinding
import com.thehotelmedia.android.extensions.formatDate
import com.thehotelmedia.android.extensions.showToast
import com.thehotelmedia.android.modals.job.jobDetails.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal

class JobDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityJobDetailBinding

    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal


    private var name = ""
    private var userName = ""
    private var profilePic = ""
    private var businessProfileID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        initUi()
    }

    private fun initUi() {

        val jobId = intent.getStringExtra("JOB_ID") ?: ""
        getJobDetailsData(jobId)

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.applyBtn.setOnClickListener {
            val intent = Intent(this, InboxScreenActivity::class.java)
            intent.putExtra("FROM", "job")
            intent.putExtra("NAME",name)
            intent.putExtra("USER_NAME",userName)
            intent.putExtra("PROFILE_PIC",profilePic)
            intent.putExtra("USER_ID",businessProfileID)
            startActivity(intent)
        }

        individualViewModal.jobDetailsResult.observe(this@JobDetailActivity) { state ->
            when (state.stateStatus) {
                Status.SUCCESS -> {
                    progressBar.hide()
                    val data = state.stateData?.data
                    handelJobDetailData(data)
                }
                Status.ERROR -> {
                    progressBar.hide()
                    CustomSnackBar.showSnackBar(binding.root,state.stateMessage.toString())
                }
                Status.LOADING -> {
                    progressBar.show()
                }
            }
        }


    }

    private fun getJobDetailsData(jobId: String) {
        individualViewModal.getJobDetails(jobId)
    }

    private fun handelJobDetailData(data: Data?) {
        val title: String = data?.title ?: ""
        val designation: String = data?.designation ?: ""
        val description: String = data?.description ?: ""
        val jobType: String = data?.jobType ?: ""
        val salary: String = data?.salary ?: ""
        val joiningDate: String = data?.joiningDate ?: ""
        val numberOfVacancies: String = data?.numberOfVacancies ?: ""
        val experience: String = data?.experience ?: ""

        name = data?.postedBy?.name ?: ""
        userName = data?.postedBy?.username ?: ""
        profilePic = data?.postedBy?.businessProfileRef?.profilePic?.large ?: ""
        businessProfileID = data?.postedBy?.businessProfileID ?: ""

        binding.jobTitleTv.text = title
        binding.designationTv.text = designation
        binding.descriptionTv.text = description
        binding.jobTypeTv.text = jobType
        binding.salaryOfferTv.text = salary
        binding.joiningDateTv.text = formatDate(joiningDate)
        binding.noOfVacanciesTv.text = numberOfVacancies
        binding.experienceTv.text = experience
    }
}