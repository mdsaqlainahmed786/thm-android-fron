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
import com.thehotelmedia.android.adapters.userTypes.individual.profile.ProfilePhotosAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentProfilePhotosBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch


class ProfilePhotosFragment : Fragment() {

    private lateinit var binding : FragmentProfilePhotosBinding
    private var userId: String = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar : CustomProgressBar
    private lateinit var profilePhotosAdapter : ProfilePhotosAdapter

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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_photos, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        if (userId.isNotEmpty()){
            getImageData()
        }else{
            binding.privateAccLayout.root.visibility = View.VISIBLE
            binding.hasDataLayout.visibility = View.GONE
            binding.noDataFoundLayout.visibility = View.GONE
        }


//        val profilePhotosAdapter = ProfilePhotosAdapter(requireContext())
//        binding.photosRv.adapter = profilePhotosAdapter

    }

    private fun getImageData() {
        profilePhotosAdapter = ProfilePhotosAdapter(requireContext(), userId)

        // Optimize RecyclerView performance
        binding.photosRv.setItemViewCacheSize(15)
        binding.photosRv.itemAnimator = null
        binding.photosRv.recycledViewPool.setMaxRecycledViews(0, 10)
        
        binding.photosRv.adapter = profilePhotosAdapter
            .withLoadStateFooter(footer = LoaderAdapter())

        individualViewModal.getImages(userId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                profilePhotosAdapter.submitData(it)
            }
        }

    }

    private fun isLoading() {
        profilePhotosAdapter.addLoadStateListener { loadState ->

            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading &&
                    profilePhotosAdapter.itemCount == 0
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