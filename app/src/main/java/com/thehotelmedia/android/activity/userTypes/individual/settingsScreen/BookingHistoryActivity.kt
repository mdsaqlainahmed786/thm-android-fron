package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.NotificationAdapter
import com.thehotelmedia.android.adapters.booking.BookingHistoryAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityBookingHistoryBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class BookingHistoryActivity : BaseActivity() {

    private lateinit var binding : ActivityBookingHistoryBinding
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal

    private lateinit var bookingHistoryAdapter : BookingHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        // Initialize UI components
        initUi()
    }

    override fun onResume() {
        super.onResume()
        gteBookingHistory()
    }

    private fun initUi() {

        // Set up back button functionality
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun gteBookingHistory() {

        bookingHistoryAdapter = BookingHistoryAdapter(this)

        binding.bookingHistoryRv.adapter = bookingHistoryAdapter

        binding.bookingHistoryRv.adapter = bookingHistoryAdapter
            .withLoadStateFooter(footer = LoaderAdapter())

        individualViewModal.getBookingHistory().observe(this) {
            this.lifecycleScope.launch {
                isLoading()
                bookingHistoryAdapter.submitData(it)
            }
        }

    }

    private fun isLoading() {
        bookingHistoryAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && bookingHistoryAdapter.itemCount == 0

            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Handle empty state (optional)
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.hasDataLayout.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }


}