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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentProfilePostsBinding
import com.thehotelmedia.android.fragments.userTypes.SavedFeedAdapter
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePostsFragment : Fragment() {
    private lateinit var binding: FragmentProfilePostsBinding
//    private lateinit var postAdapter: SearchPostAdapter
    private lateinit var postAdapter: SavedFeedAdapter
    private var userId: String = ""
    private var from: String = ""
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private var ownerUserId = ""
    private lateinit var preferenceManager : PreferenceManager
    private var activePosition = 0 // No active position initially

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: ""
            from = it.getString("FROM") ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile_posts, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {

        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())
        preferenceManager = PreferenceManager.getInstance(requireContext())
        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()

//        postAdapter = SearchPostAdapter(
//            requireContext(),
//            individualViewModal,
//            childFragmentManager,
//            ownerUserId
//        )

        if (userId.isNotEmpty()){
            getPostsData()
        }else{
            binding.privateAccLayout.root.visibility = View.VISIBLE
            binding.hasDataLayout.visibility = View.GONE
            binding.noDataFoundLayout.visibility = View.GONE
        }


//        val postAdapter = PostAdapter(
//            requireContext(),
//            onMenuClicked = { position->
//                onPostMenu(position)
//            },
//            onLikeClicked = { position, isLiked ->
//                onPostLiked(position, isLiked)
//            },
//            onCommentClicked = { position->
//                onPostComment(position)
//            },
//            onShareClicked = { position->
//                onPostShare(position)
//            },
//            onSaveClicked = { position, isSaved ->
//                onPostSaved(position, isSaved)
//            }
//        )
//        binding.postRecyclerView.adapter = postAdapter



        individualViewModal.reportToast.observe(viewLifecycleOwner){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }


    private fun getPostsData() {

//        binding.postRecyclerView.adapter = postAdapter
//            .withLoadStateFooter(footer = LoaderAdapter())
//
//        individualViewModal.getPostsData(userId).observe(viewLifecycleOwner) {
//            this.lifecycleScope.launch {
//                isLoading()
//                postAdapter.submitData(it)
//            }
//        }

        postAdapter = SavedFeedAdapter(requireContext(), individualViewModal, childFragmentManager,ownerUserId,from,this.lifecycleScope)
        binding.postRecyclerView.adapter = postAdapter.withLoadStateFooter(footer = LoaderAdapter())

        binding.postRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        individualViewModal.getPostsData(userId).observe(viewLifecycleOwner) { data ->
            this.lifecycleScope.launch {
                isLoading()
                // Use withContext to switch to the main thread for UI updates
                withContext(Dispatchers.Main) {
                    postAdapter.submitData(data)
                }
            }
        }


        // Scroll listener to track active item
        binding.postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition != activePosition) {
            val previousActivePosition = activePosition
            activePosition = newPosition
            // Notify adapter to update views
            postAdapter.setActivePosition(activePosition)
            postAdapter.notifyItemChanged(previousActivePosition)
            postAdapter.notifyItemChanged(activePosition)
        }
    }

    private fun isLoading() {
        postAdapter.addLoadStateListener { loadState ->

            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading &&
                    postAdapter.itemCount == 0
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