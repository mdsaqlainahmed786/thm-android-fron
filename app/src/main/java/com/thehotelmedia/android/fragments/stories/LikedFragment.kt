package com.thehotelmedia.android.fragments.stories

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
import com.thehotelmedia.android.adapters.stories.LikedStoryAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentLikedBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch


class LikedFragment : Fragment() {

    private lateinit var binding : FragmentLikedBinding
    private lateinit var likedStoryAdapter: LikedStoryAdapter
    private lateinit var progressBar : CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal

    private var storyId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_liked, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        storyId = arguments?.getString("story_id") ?: ""
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        progressBar = CustomProgressBar(requireContext())

        likedStoryAdapter = LikedStoryAdapter(requireContext(),childFragmentManager,individualViewModal,viewLifecycleOwner.lifecycleScope)
//        binding.itemsRv.adapter = likedStoryAdapter

        getLikeData()

    }

    private fun getLikeData() {
        binding.itemsRv.adapter = likedStoryAdapter.withLoadStateFooter(footer = LoaderAdapter())
        individualViewModal.getLikes(storyId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                likedStoryAdapter.submitData(it)
            }
        }
    }
    private fun isLoading() {
        likedStoryAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    likedStoryAdapter.itemCount == 0

            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Show empty state if no data is loaded
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.itemsRv.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.itemsRv.visibility = View.VISIBLE
            }

        }
    }




}