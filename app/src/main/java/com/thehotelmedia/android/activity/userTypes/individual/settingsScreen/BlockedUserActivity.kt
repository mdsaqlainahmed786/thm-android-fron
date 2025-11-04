package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.search.ProfilesAdapter
import com.thehotelmedia.android.bottomSheets.BlockUserBottomSheetFragment
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityBlockedUserBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class BlockedUserActivity : BaseActivity() , BlockUserBottomSheetFragment.BottomSheetListener {

    private lateinit var binding: ActivityBlockedUserBinding
    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var profileAdapter: ProfilesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using ViewBinding
        binding = ActivityBlockedUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        initUI()
    }

    private fun initUI() {
        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        // Load blocked user data
        getBlockedUserData()

        // Set up back button functionality
        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getBlockedUserData() {
        // Initialize the ProfilesAdapter with a menu click listener
        profileAdapter = ProfilesAdapter(this, ::onMenuClicked,"BlockScreen")

        // Set the adapter for the RecyclerView
        binding.blockUserRv.adapter = profileAdapter.withLoadStateFooter(
            footer = LoaderAdapter()
        )

        // Observe the follower data from the ViewModel
        individualViewModal.getBlockUserData().observe(this) { pagingData ->
            // Submit the PagingData to the adapter
            lifecycleScope.launch {
                isLoading()
                profileAdapter.submitData(pagingData)
            }
        }
    }

    private fun onMenuClicked(position: Int,userId: String,view: View) {
            val isBlocked = true // Example: initial boolean value
            val bottomSheetFragment = BlockUserBottomSheetFragment.newInstance(isBlocked,userId)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }





    private fun isLoading() {
        // Listen for changes in the loading state
        profileAdapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading && profileAdapter.itemCount == 0

            // Show or hide progress bar based on loading state
            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Show empty state if no data is loaded
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.hasDataLayout.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }

    // This is called when a boolean is returned from the BottomSheetFragment
    override fun onBooleanDataReceived(isUserBlocked: Boolean) {
        // Handle the boolean returned from the fragment
        getBlockedUserData()
    }


}
