package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userTypes.business.TransactionsAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.ActivityTransactionsBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class TransactionsActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionsBinding


    private lateinit var progressBar: CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var transactionAdapter: TransactionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        // Set up repository and view model
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        // Initialize progress bar
        progressBar = CustomProgressBar(this)

        // Load transaction user data
        getTransactionData()


        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }


    private fun getTransactionData() {
        // Initialize the TransactionsAdapter with a menu click listener
        transactionAdapter = TransactionsAdapter(this)

        // Set the adapter for the RecyclerView
        binding.transactionRv.adapter = transactionAdapter.withLoadStateFooter(
            footer = LoaderAdapter()
        )

        // Observe the follower data from the ViewModel
        individualViewModal.getTransactionData().observe(this) { pagingData ->
            // Submit the PagingData to the adapter
            lifecycleScope.launch {
                isLoading()
                transactionAdapter.submitData(pagingData)
            }
        }
    }


    private fun isLoading() {
        // Listen for changes in the loading state
        transactionAdapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading && transactionAdapter.itemCount == 0

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


}