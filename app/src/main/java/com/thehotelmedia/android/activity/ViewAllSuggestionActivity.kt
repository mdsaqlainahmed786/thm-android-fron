package com.thehotelmedia.android.activity

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.SuggestionAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityViewAllSuggestionBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class ViewAllSuggestionActivity : BaseActivity() {

    private lateinit var binding : ActivityViewAllSuggestionBinding
    private lateinit var progressBar : CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var ownerUserid = ""
    private lateinit var preferenceManager : PreferenceManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewAllSuggestionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()
    }

    private fun initUi() {
        preferenceManager = PreferenceManager.getInstance(this)
        ownerUserid = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        progressBar = CustomProgressBar(this)

        getSuggestionData()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }



    }


    private fun getSuggestionData() {
        suggestionAdapter = SuggestionAdapter(this,individualViewModal,supportFragmentManager,ownerUserid)
        binding.suggestionRv.adapter = suggestionAdapter

        binding.suggestionRv.adapter = suggestionAdapter
            .withLoadStateFooter(footer = LoaderAdapter())

        individualViewModal.getSuggestion().observe(this) {
            this.lifecycleScope.launch {
                isLoading()
                suggestionAdapter.submitData(it)
            }
        }


    }

    private fun isLoading() {
        suggestionAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && suggestionAdapter.itemCount == 0

            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

//            // Handle empty state (optional)
//            if (isEmpty) {
//                binding.noDataFoundLayout.visibility = View.VISIBLE
//                binding.hasDataLayout.visibility = View.GONE
//            } else {
//                binding.noDataFoundLayout.visibility = View.GONE
//                binding.hasDataLayout.visibility = View.VISIBLE
//            }
        }
    }


}