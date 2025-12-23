package com.thehotelmedia.android.fragments.userTypes.individual.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.graphics.Rect
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
    private var viewerFollowsOwner: Boolean = false
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar: CustomProgressBar

    private var ownerUserId = ""
    private lateinit var preferenceManager : PreferenceManager
    private var activePosition = RecyclerView.NO_POSITION // No active position initially
    private var pendingStoryPostId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: ""
            from = it.getString("FROM") ?: ""
            viewerFollowsOwner = it.getBoolean("IS_CONNECTED", false)
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
        individualViewModal.toast.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank()) {
                return@observe
            }
            progressBar.hide()
            if (pendingStoryPostId != null && message.contains("already", ignoreCase = true)) {
                pendingStoryPostId?.let { postAdapter.markPostShared(it) }
            }
            pendingStoryPostId = null
            CustomSnackBar.showSnackBar(binding.root, message)
        }
        individualViewModal.publishStoryResult.observe(viewLifecycleOwner) { result ->
            progressBar.hide()
            if (result?.status == true) {
                val successMessage = result.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.story_publish_success)
                pendingStoryPostId?.let { postId ->
                    postAdapter.markPostShared(postId)
                }
                CustomSnackBar.showSnackBar(binding.root, successMessage)
            }
            pendingStoryPostId = null
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

        postAdapter = SavedFeedAdapter(
            requireContext(),
            individualViewModal,
            childFragmentManager,
            ownerUserId,
            from,
            this.lifecycleScope,
            enableStoryShare = true,
            viewerFollowsOwner = viewerFollowsOwner
        )
        postAdapter.onStoryShareRequested = { postId ->
            pendingStoryPostId = postId
            progressBar.show()
            individualViewModal.publishPostToStory(postId)
        }
        binding.postRecyclerView.adapter = postAdapter.withLoadStateFooter(footer = LoaderAdapter())
        binding.postRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Optimize RecyclerView performance
        binding.postRecyclerView.setItemViewCacheSize(20) // Increased cache size for smoother scrolling
        binding.postRecyclerView.setHasFixedSize(false) // Allow RecyclerView to optimize layout
        binding.postRecyclerView.itemAnimator = null // Disable animations for better performance
        binding.postRecyclerView.recycledViewPool.setMaxRecycledViews(0, 10) // Recycle views more efficiently

        individualViewModal.getPostsData(userId).observe(viewLifecycleOwner) { data ->
            this.lifecycleScope.launch {
                isLoading()
                // Use withContext to switch to the main thread for UI updates
                withContext(Dispatchers.Main) {
                    postAdapter.submitData(data)

                    // After data is submitted and the list is laid out, ensure that
                    // the most visible item is marked as active so that videos
                    // can auto‑play (instead of waiting for the user to scroll).
                    binding.postRecyclerView.post {
                        val initialPosition = findMostVisibleItemPosition(binding.postRecyclerView)
                        if (initialPosition != RecyclerView.NO_POSITION) {
                            updateActivePosition(initialPosition)
                        }
                    }
                }
            }
        }


        // Listener to track active item based on global scroll. Because this RecyclerView
        // is inside a larger scrolling container, its own onScrolled callback may not fire
        // when the parent scrolls. Instead, listen for global scroll changes and compute
        // which post is most visible on screen.
        binding.postRecyclerView.viewTreeObserver.addOnScrollChangedListener {
            val recyclerView = binding.postRecyclerView
            val candidatePosition = findMostVisibleItemPosition(recyclerView)
            if (candidatePosition != RecyclerView.NO_POSITION && candidatePosition != activePosition) {
                recyclerView.post {
                    if (candidatePosition != activePosition) {
                        updateActivePosition(candidatePosition)
                    }
                }
            }
        }

        // Ensure the initially visible item is marked active
        val initialPosition = findMostVisibleItemPosition(binding.postRecyclerView)
        if (initialPosition != RecyclerView.NO_POSITION) {
            updateActivePosition(initialPosition)
        }
    }

        /**
         * Returns the adapter position of the item that is mostly/fully visible
         * on screen (not just within the RecyclerView). Uses global coordinates
         * so it works even when the RecyclerView itself is inside a parent
         * scrolling container.
         */
    private fun findMostVisibleItemPosition(recyclerView: RecyclerView): Int {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        // Threshold of visible height required to consider an item "active".
        // If at least ~60% of a post is visible on screen, its media is
        // considered the primary focus and its video may auto‑play.
        val visibilityThreshold = 0.6f
        var bestPosition = RecyclerView.NO_POSITION
        var maxVisibleRatio = 0f

        val parentGlobalRect = Rect()
        recyclerView.getGlobalVisibleRect(parentGlobalRect)

        val childGlobalRect = Rect()

        for (position in firstVisible..lastVisible) {
            val child = layoutManager.findViewByPosition(position) ?: continue

            if (child.height <= 0) continue

            // Compute how much of this child is currently visible on screen by
            // intersecting its global rect with the RecyclerView's global rect.
            val hasVisibleRect = child.getGlobalVisibleRect(childGlobalRect)
            if (!hasVisibleRect) continue

            val visibleRect = Rect(childGlobalRect)
            val intersected = visibleRect.intersect(parentGlobalRect)
            if (!intersected) continue

            val visibleHeight = visibleRect.height().coerceAtLeast(0)
            if (visibleHeight <= 0) continue

            val ratio = visibleHeight.toFloat() / child.height.toFloat()
            if (ratio > maxVisibleRatio) {
                maxVisibleRatio = ratio
                bestPosition = position
            }
        }

        return if (maxVisibleRatio >= visibilityThreshold) bestPosition else RecyclerView.NO_POSITION
    }

    private fun updateActivePosition(newPosition: Int) {
        if (newPosition == activePosition) return
        if (newPosition < 0 || newPosition >= postAdapter.itemCount) return

        activePosition = newPosition
        // Delegate item update logic to the adapter so it can decide which post
        // should have its media (video/image) in the active state.
        postAdapter.setActivePosition(newPosition)
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

    fun updateViewerFollowState(isFollowing: Boolean) {
        viewerFollowsOwner = isFollowing
        if (this::postAdapter.isInitialized) {
            postAdapter.updateViewerFollowState(isFollowing)
        }
    }
}