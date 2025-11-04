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
import com.thehotelmedia.android.adapters.stories.ViewerStoryAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.databinding.FragmentViewerBinding
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class ViewerFragment : Fragment() {

    private lateinit var binding : FragmentViewerBinding
    private lateinit var viewerStoryAdapter: ViewerStoryAdapter
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_viewer, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        storyId = arguments?.getString("story_id") ?: ""
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        progressBar = CustomProgressBar(requireContext())



        viewerStoryAdapter = ViewerStoryAdapter(requireContext(),childFragmentManager,individualViewModal,viewLifecycleOwner.lifecycleScope)
//        binding.itemsRv.adapter = viewerStoryAdapter
        getViewerData()


//        individualViewModal.blockUserResult.observe(viewLifecycleOwner){result->
//            if (result.status == true){
//                val msg = result.message.toString()
//                CustomSnackBar.showSnackBar(binding.root,msg)
//
////                storyPagerAdapter.moveToMainScreen()
////               onBackPressedDispatcher.onBackPressed()
////                moveToMainScreen(businessType)
//
//
//            }else{
//                val msg = result.message.toString()
//                CustomSnackBar.showSnackBar(binding.root,msg)
//            }
//        }
//
//
//        individualViewModal.loading.observe(viewLifecycleOwner){
//            if (it == true){
//                progressBar.show() // To show the giff progress bar
//            }else{
//                progressBar.hide() // To hide the giff progress bar
//            }
//        }
//
//        individualViewModal.toast.observe(viewLifecycleOwner){
////            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
//            CustomSnackBar.showSnackBar(binding.root,it)
//        }



    }

    private fun getViewerData() {
        println("asdfioasdjfa entre $storyId")
        binding.itemsRv.adapter = viewerStoryAdapter.withLoadStateFooter(footer = LoaderAdapter())
        individualViewModal.getViewers(storyId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                viewerStoryAdapter.submitData(it)
            }
        }
    }
    private fun isLoading() {
        viewerStoryAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    viewerStoryAdapter.itemCount == 0

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