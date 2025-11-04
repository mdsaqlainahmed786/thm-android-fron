package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.comments.CommentsAdapter
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentCommentsBottomSheetBinding
import com.thehotelmedia.android.modals.feeds.getComments.CommentedBy
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch
import com.thehotelmedia.android.modals.feeds.getComments.Data
import com.thehotelmedia.android.modals.feeds.getComments.ProfilePic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {
    var onCommentSent: ((String) -> Unit)? = null
    private lateinit var binding: FragmentCommentsBottomSheetBinding
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var progressBar : CustomProgressBar
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var postId: String
    private  var commentsCount: Int = 0
    private var parentId = ""


    private var fullName = ""
    private var profilePic = ""
    private var businessType = ""
    private var ownerUserId = ""

    private lateinit var preferenceManager: PreferenceManager

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

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_comments_bottom_sheet, container, false)

        initUI()
        return binding.root
    }

    private fun initUI() {
        binding.replyLayout.visibility = View.GONE
        postId = arguments?.getString("POST_ID") ?: ""
        commentsCount = arguments?.getInt("COMMENTS_COUNT") ?: 0
        if (commentsCount == 0){
            binding.commentsTv.text = getString(R.string.comments)
        }else{
            binding.commentsTv.text = "$commentsCount ${getString(R.string.comments)}"
        }
        preferenceManager = PreferenceManager.getInstance(requireContext())
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(requireActivity(), ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]
        progressBar = CustomProgressBar(requireContext())

        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
        businessType = preferenceManager.getString(PreferenceManager.Keys.BUSINESS_TYPE, "").toString()
        profilePic = preferenceManager.getString(PreferenceManager.Keys.USER_LARGE_PROFILE_PIC, "").toString()
        fullName = preferenceManager.getString(PreferenceManager.Keys.USER_FULL_NAME, "").toString()

        getCommentsData()


        binding.cancelReplyingBtn.setOnClickListener {
            binding.replyLayout.visibility = View.GONE
            parentId = ""
        }


        binding.sendCommentBtn.setOnClickListener {
            val comment =  binding.commentEt.text.toString().trim()
            if(comment.isNotEmpty()){
                sendComment(comment)
            }
        }


        openKeyboard()


//        individualViewModal.createCommentResult.observe(viewLifecycleOwner){result->
//            if (result.status==true){
//                binding.commentEt.text?.clear()
//
//                val isParent = result.data?.isParent ?: false
//                val id = result.data?.id ?: ""
//                val userID = result.data?.userID ?: ""
//                val businessProfileID = result.data?.businessProfileID ?: ""
//                val postID = result.data?.postID ?: ""
//                val message = result.data?.message ?: ""
//                val parentID = result.data?.parentID ?: ""
//                val createdAt = result.data?.createdAt ?: ""
//                val updatedAt = result.data?.updatedAt ?: ""
//                if (isParent){
//
//                    addOuterStaticComment(id,isParent,userID,businessProfileID,postID,message,parentID,createdAt,updatedAt)
//                }else{
//                    dismiss()
//                }
//
//                println("fasdjkhgjk   $result")
//
//            }else{
//                val msg = result.message
////                Toast.makeText(activity,msg, Toast.LENGTH_SHORT).show()
//            }
//        }


//        individualViewModal.reportToast.observe(viewLifecycleOwner){
////            Toast.makeText(activity,it, Toast.LENGTH_SHORT).show()
//            CustomSnackBar.showSnackBar(binding.root,it)
//        }
    }



    private fun sendComment(comment: String) {

        individualViewModal.createComment(postId,comment,parentId)
        // Call the lambda function and pass the comment
        onCommentSent?.invoke(comment)
        dismiss()
    }

    private fun getCommentsData() {

        commentsAdapter = CommentsAdapter(requireContext(),::onReplyClick,individualViewModal,childFragmentManager,ownerUserId)

        if (binding.commentsRv.adapter == null) {
            binding.commentsRv.adapter = commentsAdapter.withLoadStateFooter(footer = LoaderAdapter())
        }

        individualViewModal.getComments(postId).observe(viewLifecycleOwner) {
            this.lifecycleScope.launch {
                isLoading()
                commentsAdapter.submitData(it)
            }
        }
    }

    private fun onReplyClick(id: String, name: String, profilePic: String) {
        openKeyboard()
        parentId = id
        binding.replyLayout.visibility = View.VISIBLE
        Glide.with(requireContext()).load(profilePic).placeholder(R.drawable.ic_profile_placeholder).into(binding.replyingIv)
        binding.replyingTv.text = "${getString(R.string.replying_to)} ${name}"

    }

    private fun isLoading() {
        commentsAdapter.addLoadStateListener {

            val isLoading = it.refresh is LoadState.Loading

//            val itemCount = commentsAdapter.itemCount
//            binding.commentsTv.text = "$itemCount Comments"

            val isEmpty = it.refresh is LoadState.NotLoading &&
                    commentsAdapter.itemCount == 0
            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.commentsRv.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.commentsRv.visibility = View.VISIBLE
            }

        }
    }
//    private fun setupCommentEditText() {
//        binding.commentEt.setOnFocusChangeListener { v, hasFocus ->
//            if (hasFocus) {
//                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                inputMethodManager.showSoftInput(binding.commentEt, InputMethodManager.SHOW_IMPLICIT)
//            }
//        }
//    }
    private fun openKeyboard() {
        // Request focus on the EditText
        binding.commentEt.requestFocus()

        // Show the soft keyboard
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.commentEt, InputMethodManager.SHOW_IMPLICIT)
    }

    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return sdf.format(Date())
    }


    private fun addOuterStaticComment(
        id: String,
        isParent: Boolean,
        userID: String,
        businessProfileID: String,
        postID: String,
        message: String,
        parentID: String,
        createdAt: String,
        updatedAt: String
    ) {
        val newComment = Data(
            Id = id,  // Replace this with the actual ID you get from the server after creating the comment
            isParent = isParent,  // Set it to true or false based on your logic
            userID = userID,  // Set the user ID of the commenter
            businessProfileID = businessProfileID,  // Optional: Set if necessary
            postID = postID,  // The post ID this comment belongs to
            message = message,  // The actual comment message
            createdAt = createdAt,  // Set the current date and time of the comment creation
            repliesRef = arrayListOf(),  // Initialize the replies if any
            commentedBy = CommentedBy(
                Id = "", // Commenter ID
                accountType = businessType, // Account type, e.g., 'user', 'business'
                businessProfileID = "", // Business profile ID if it's a business user
                name = fullName, // Name of the commenter
                profilePic = ProfilePic(
                    small = profilePic, // Small profile image URL
                    medium = profilePic, // Medium profile image URL
                    large = profilePic // Large profile image URL
                ),
                businessProfileRef = null
            ),


            likes = 0,  // Initial likes, you can update this as needed
            likedByMe = false // Indicates whether the current user has liked this comment
        )

        lifecycleScope.launch {
            // Get the current list snapshot from the PagingDataAdapter
            val currentPagingData = commentsAdapter.snapshot().filterNotNull().toMutableList()

            // Add the new message at the 0th index (top of the list)
            currentPagingData.add(0, newComment)

            // Convert the updated list back into PagingData
            val newPagingData = PagingData.from(currentPagingData)

            // Submit the updated PagingData to the adapter
            commentsAdapter.submitData(newPagingData)
            binding.commentsRv.scrollToPosition(0)
        }

    }


}