package com.thehotelmedia.android.fragments.userTypes.individual.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.search.SearchReviewsAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentProfileReviewsBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class ProfileReviewsFragment : Fragment() {

    private lateinit var binding: FragmentProfileReviewsBinding

    private lateinit var reviewAdapter: SearchReviewsAdapter

    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private var ownerUserId = ""
    private lateinit var preferenceManager : PreferenceManager

    private var userId: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_reviews, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {

        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        preferenceManager = PreferenceManager.getInstance(requireContext())
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()


        reviewAdapter = SearchReviewsAdapter(
            requireContext(),
            individualViewModal,
            childFragmentManager,
            ownerUserId
        )

        if (userId.isNotEmpty()){
            getReviewData()
        }else{
            binding.privateAccLayout.root.visibility = View.VISIBLE
            binding.hasDataLayout.visibility = View.GONE
            binding.noDataFoundLayout.visibility = View.GONE
        }

        println("asdjhajkh   Review = $userId")
//        val reviewAdapter = ReviewAdapter(
//            requireContext(),
//            onMenuClicked = { position->
//                onReviewMenu(position)
//            },
//            onLikeClicked = { position, isLiked ->
//                onReviewLiked(position, isLiked)
//            },
//            onCommentClicked = { position->
//                onReviewComment(position)
//            },
//            onShareClicked = { position->
//                onReviewShare(position)
//            },
//            onSaveClicked = { position, isSaved ->
//                onReviewSaved(position, isSaved)
//            }
//        )
//        binding.reviewRecyclerView.adapter = reviewAdapter

        individualViewModal.reportToast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }
    }

//    private fun getReviewData() {
//
//    }



    private fun getReviewData() {

        binding.reviewRecyclerView.adapter = reviewAdapter
            .withLoadStateFooter(footer = LoaderAdapter { reviewAdapter.retry() })

        individualViewModal.getReviewData(userId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                reviewAdapter.submitData(it)
            }
        }

    }

    private fun isLoading() {
        reviewAdapter.addLoadStateListener { loadState ->

            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading &&
                    reviewAdapter.itemCount == 0
            val isError = loadState.refresh is LoadState.Error

//            if (isLoading) {
//                progressBar.show()
//                println("Loading data...")
//            } else {
//                progressBar.hide()
//                println("Data loading completed")
//            }

            if (isEmpty || isError) {
                // Show "No Data Found" layout if there's an error or no data
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.hasDataLayout.visibility = View.GONE

                if (isError) {
                    val error = (loadState.refresh as LoadState.Error).error
                    println("Error occurred: ${error.message}")
                    // Optionally, show error message in UI if needed
                }

            } else {
                // Hide "No Data Found" layout if data is present
                binding.noDataFoundLayout.visibility = View.GONE
                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }



}