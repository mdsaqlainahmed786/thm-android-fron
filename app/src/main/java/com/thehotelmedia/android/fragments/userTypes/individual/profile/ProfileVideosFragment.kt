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
import com.thehotelmedia.android.adapters.userTypes.individual.profile.ProfileVideosAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentProfileVideosBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch


class ProfileVideosFragment : Fragment() {

    private lateinit var binding: FragmentProfileVideosBinding

    private lateinit var individualViewModal: IndividualViewModal
    private var userId: String = ""
    private lateinit var videoAdapter: ProfileVideosAdapter

    private lateinit var progressBar : CustomProgressBar

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_videos, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {


        println("asdjhajkh   Video = $userId")
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        if (userId.isNotEmpty()){
            getVideoData()
        }else{
            binding.privateAccLayout.root.visibility = View.VISIBLE
            binding.hasDataLayout.visibility = View.GONE
            binding.noDataFoundLayout.visibility = View.GONE
        }

    }

    private fun getVideoData() {
        videoAdapter = ProfileVideosAdapter(requireContext(), userId)
        
        // Optimize RecyclerView performance
        binding.videosRv.setItemViewCacheSize(15)
        binding.videosRv.itemAnimator = null
        binding.videosRv.recycledViewPool.setMaxRecycledViews(0, 10)

        binding.videosRv.adapter = videoAdapter
            .withLoadStateFooter(footer = LoaderAdapter { videoAdapter.retry() })

        individualViewModal.getVideos(userId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()

                videoAdapter.submitData(it)
            }
        }
    }
    private fun isLoading() {
        videoAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading


            val isEmpty = it.refresh is LoadState.NotLoading &&
                    videoAdapter.itemCount == 0
//            if (isLoading) {
//                progressBar.show()
//                println("sdahfajkhfajk    show in videos")
//            } else {
//                progressBar.hide()
//                println("sdahfajkhfajk    hide in videos" )
//            }

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