package com.thehotelmedia.android.activity.userTypes.individual.settingsScreen

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.activity.BaseActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivitySavedPostBinding
import com.thehotelmedia.android.fragments.userTypes.SavedFeedAdapter
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedPostActivity : BaseActivity() {

    private lateinit var binding: ActivitySavedPostBinding
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var savedFeedAdapter: SavedFeedAdapter
    private lateinit var progressBar: CustomProgressBar
    private lateinit var preferenceManager: PreferenceManager
    private var ownerUserId = ""
    private var activePosition = 0 // No active position initially

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(this)
        preferenceManager = PreferenceManager.getInstance(this)
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()

        }


        getSavedPostData()



        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

        individualViewModal.reportToast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }





    }
    private fun updateActivePosition(newPosition: Int) {
        if (newPosition != activePosition) {
            val previousActivePosition = activePosition
            activePosition = newPosition
            // Notify adapter to update views
            savedFeedAdapter.setActivePosition(activePosition)
            savedFeedAdapter.notifyItemChanged(previousActivePosition)
            savedFeedAdapter.notifyItemChanged(activePosition)
        }
    }


    private fun getSavedPostData() {
        savedFeedAdapter = SavedFeedAdapter(
            this,
            individualViewModal,
            supportFragmentManager,
            ownerUserId,
            "",
            this.lifecycleScope,
            enableStoryShare = false,
            viewerFollowsOwner = false
        )
        binding.savedPostRv.adapter = savedFeedAdapter.withLoadStateFooter(
            footer = LoaderAdapter { savedFeedAdapter.retry() }
        )

        binding.savedPostRv.layoutManager = LinearLayoutManager(this)
        binding.savedPostRv.itemAnimator = null

        individualViewModal.getSavedPost().observe(this) { data ->
            this.lifecycleScope.launch {
                isLoading()
                // Use withContext to switch to the main thread for UI updates
                withContext(Dispatchers.Main) {
                    savedFeedAdapter.submitData(data)
                }
            }
        }


        // Scroll listener to track active item
        binding.savedPostRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItem = layoutManager.findFirstCompletelyVisibleItemPosition()

                if (firstVisibleItem != RecyclerView.NO_POSITION && firstVisibleItem != activePosition) {
                    updateActivePosition(firstVisibleItem)
                }
            }
        })

    }


    private fun isLoading() {
        savedFeedAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    savedFeedAdapter.itemCount == 0
            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            if (isEmpty) {
                binding.hasDataLayout.visibility = View.GONE
                binding.noDataFoundLayout.visibility = View.VISIBLE
            }else{
                binding.hasDataLayout.visibility = View.VISIBLE
                binding.noDataFoundLayout.visibility = View.GONE
            }


        }
    }






}