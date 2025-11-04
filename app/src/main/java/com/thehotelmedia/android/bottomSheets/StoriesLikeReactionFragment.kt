package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.databinding.FragmentStoriesLikeReactionBinding
import com.thehotelmedia.android.extensions.toggleEnable
import com.thehotelmedia.android.fragments.stories.LikedFragment
import com.thehotelmedia.android.fragments.stories.ViewerFragment


class StoriesLikeReactionFragment : BottomSheetDialogFragment() {
    var onDismissCallback: (() -> Unit)? = null

    private lateinit var binding: FragmentStoriesLikeReactionBinding
    private lateinit var storyId: String
    private lateinit var likesCount: String
    private lateinit var viewsCount: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        // You can also set a custom enter animation here if needed
        bottomSheetDialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        return bottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onStart() {
        super.onStart()
        // Ensure the bottom sheet opens to 75% of the screen height
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {

                val behavior = BottomSheetBehavior.from(it)
                // Get the screen height
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val maxHeight = (screenHeight * 0.75).toInt() // 75% of the screen height

                // Set the bottom sheet height to 75% and restrict it from expanding more
                bottomSheet.layoutParams.height = maxHeight
                behavior.peekHeight = maxHeight

                // Optional: Set a fixed state to stop expanding more than the max height
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED

                // Disable expanding beyond 75%
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Optional: you can control the sliding here
                    }
                })

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stories_like_reaction, container, false)
        initUI()
        return binding.root
    }

    private fun initUI() {
        storyId = arguments?.getString(ARG_STORY_ID) ?: ""
        likesCount = arguments?.getString(ARG_LIKES) ?: ""
        viewsCount = arguments?.getString(ARG_VIEWS) ?: ""

        if (likesCount == "0"){
            binding.likeCountTv.visibility = View.INVISIBLE
        }
        if (viewsCount == "0"){
            binding.viewerCountTv.visibility = View.INVISIBLE
        }
        binding.likeCountTv.text = likesCount
        binding.viewerCountTv.text = viewsCount



        disableLikeLayout()
        enableViewerLayout()
        loadFragment(ViewerFragment(),storyId)

        binding.viewerLayout.setOnClickListener {
            disableLikeLayout()
            enableViewerLayout()
            loadFragment(ViewerFragment(),storyId)
        }

        binding.likedLayout.setOnClickListener {
            disableViewerLayout()
            enableLikeLayout()
            loadFragment(LikedFragment(),storyId)
        }



    }

//    private fun loadFragment(fragment: Fragment) {
//        childFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, fragment)
//            .commit()
//    }

    private fun loadFragment(fragment: Fragment, storyId: String) {
        val bundle = Bundle()
        bundle.putString("story_id", storyId)
        fragment.arguments = bundle

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun disableLikeLayout() {
        binding.likeIv.toggleEnable(false)
        binding.likeView.visibility = View.GONE
        binding.likeCountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color_60))

    }
    private fun enableLikeLayout() {
        binding.likeIv.toggleEnable(true)
        binding.likeView.visibility = View.VISIBLE
        binding.likeCountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))

    }
    private fun enableViewerLayout() {
        binding.viewerIv.toggleEnable(true)
        binding.viewerView.visibility = View.VISIBLE
        binding.viewerCountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
    }


    private fun disableViewerLayout() {
        binding.viewerIv.toggleEnable(false)
        binding.viewerView.visibility = View.GONE
        binding.viewerCountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_color_60))

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    companion object {
        private const val ARG_STORY_ID = "story_id"
        private const val ARG_LIKES = "ARG_LIKES"
        private const val ARG_VIEWS = "ARG_VIEWS"

        // Factory method to create an instance of the fragment with the story ID as an argument
        fun newInstance(storyId: String,likeCount: String,viewsCount: String,): StoriesLikeReactionFragment {
            val fragment = StoriesLikeReactionFragment()
            val args = Bundle()
            args.putString(ARG_STORY_ID, storyId)
            args.putString(ARG_LIKES, likeCount)
            args.putString(ARG_VIEWS, viewsCount)
            fragment.arguments = args
            return fragment
        }
    }

}