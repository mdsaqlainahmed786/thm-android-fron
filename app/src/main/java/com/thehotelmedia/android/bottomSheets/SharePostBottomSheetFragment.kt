package com.thehotelmedia.android.bottomSheets

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.SocketModals.chatScreen.Messages
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.socket.ShareChatListAdapter
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.BottomSheetSharePostBinding
import com.thehotelmedia.android.extensions.EncryptionHelper
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class SharePostBottomSheetFragment : BottomSheetDialogFragment() {

    private val socketViewModel: SocketViewModel by viewModels()
    private lateinit var binding: BottomSheetSharePostBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var chatAdapter: ShareChatListAdapter
    private lateinit var individualViewModal: IndividualViewModal

    private var postId: String = ""
    private var ownerUserId: String = ""
    private var shareLink: String = ""
    private var sharedMediaType: String? = null
    private var sharedMediaUrl: String? = null
    private var sharedThumbnailUrl: String? = null
    private var sharedMediaId: String? = null
    private var userName: String = ""
    private var currentRecipientUsername: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme).also {
            it.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_share_post, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initArgs()
        setupRecycler()
        setupListeners()
        observeChatData()
        observeStoryPublishResult()
        observeSharePostMessageResult()
        connectAndFetchChats()
    }

    private fun initViewModel() {
        val individualRepo = IndividualRepo(requireContext())
        individualViewModal = ViewModelProvider(
            requireActivity(),
            ViewModelFactory(null, individualRepo, null)
        )[IndividualViewModal::class.java]
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { handler.removeCallbacks(it) }
        socketViewModel.leaveChat()
        socketViewModel.removeAllListeners()
    }

    private fun initArgs() {
        preferenceManager = PreferenceManager.getInstance(requireContext())
        postId = arguments?.getString(ARG_POST_ID).orEmpty()
        ownerUserId = arguments?.getString(ARG_OWNER_USER_ID).orEmpty()
        sharedMediaType = arguments?.getString(ARG_MEDIA_TYPE)
        sharedMediaUrl = arguments?.getString(ARG_MEDIA_URL)
        sharedThumbnailUrl = arguments?.getString(ARG_THUMBNAIL_URL)
        sharedMediaId = arguments?.getString(ARG_MEDIA_ID)

        userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
        shareLink = if (postId.isNotBlank() && ownerUserId.isNotBlank()) {
            buildDeepLink(postId, ownerUserId)
        } else {
            ""
        }

        if (shareLink.isBlank() && sharedMediaUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }
    }

    private fun setupRecycler() {
        chatAdapter = ShareChatListAdapter(requireContext()) { chat ->
            onChatSelected(chat)
        }
        binding.chatListRv.adapter = chatAdapter
    }

    private fun setupListeners() {
        binding.shareExternallyBtn.apply {
            isVisible = shareLink.isNotBlank()
            setOnClickListener {
                if (shareLink.isNotBlank()) {
                    requireContext().sharePostWithDeepLink(postId, ownerUserId)
                }
                dismissAllowingStateLoss()
            }
        }

        // Show/hide share to story button based on media presence
        val hasMedia = hasShareableMedia()
        binding.shareToStoryBtn.apply {
            isVisible = hasMedia
            setOnClickListener {
                if (postId.isNotBlank()) {
                    individualViewModal.publishPostToStory(postId)
                }
            }
        }

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable { fetchChats(s.toString()) }
                handler.postDelayed(searchRunnable!!, SEARCH_DEBOUNCE_MS)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun hasShareableMedia(): Boolean {
        if (sharedMediaUrl.isNullOrBlank()) return false
        val normalizedType = when (sharedMediaType?.lowercase(Locale.getDefault())) {
            Constants.IMAGE -> Constants.IMAGE
            Constants.VIDEO -> Constants.VIDEO
            else -> null
        }
        return !normalizedType.isNullOrBlank()
    }

    private fun observeSharePostMessageResult() {
        individualViewModal.sharePostMessageResult.observe(viewLifecycleOwner) { result ->
            val recipientUsername = currentRecipientUsername ?: return@observe
            currentRecipientUsername = null // Reset after use
            
            if (result?.status == true && result.data != null) {
                val messageData = result.data?.message
                if (messageData != null) {
                    // Send the message via socket using the response from the API
                    val messageID = UUID.randomUUID().toString().replace("-", "")
                    socketViewModel.sendPrivateMessage(
                        messageData.type ?: "",
                        messageData.message ?: "",
                        recipientUsername,
                        messageData.mediaUrl ?: "",
                        messageData.thumbnailUrl ?: "",
                        messageData.mediaID ?: "",
                        messageID,
                        messageData.postID,
                        messageData.postOwnerUsername
                    )
                    val displayName = recipientUsername
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.share_message_sent, displayName),
                        Toast.LENGTH_SHORT
                    ).show()
                    dismissAllowingStateLoss()
                } else {
                    Toast.makeText(requireContext(), R.string.share_message_failed, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), R.string.share_message_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeStoryPublishResult() {
        individualViewModal.publishStoryResult.observe(viewLifecycleOwner) { result ->
            if (result?.status == true) {
                val message = result.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.story_publish_success)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                dismissAllowingStateLoss()
            } else if (result != null) {
                // Error case - result is not null but status is false
                val message = result.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.something_went_wrong)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeChatData() {
        socketViewModel.chatScreenList.observe(viewLifecycleOwner) { chatScreen ->
            binding.loadingIndicator.isVisible = false
            val messages = chatScreen?.messages ?: emptyList()
            chatAdapter.submitList(messages)
            binding.emptyStateTv.isVisible = messages.isEmpty()
        }
    }

    private fun connectAndFetchChats() {
        if (userName.isBlank()) {
            Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
            return
        }
        socketViewModel.connectSocket(userName)
        fetchChats(binding.searchEt.text?.toString().orEmpty())
    }

    private fun fetchChats(query: String) {
        binding.loadingIndicator.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                socketViewModel.fetchChatScreen(1, PAGE_SIZE, query.trim())
            } catch (e: Exception) {
                binding.loadingIndicator.isVisible = false
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onChatSelected(chat: Messages) {
        val recipientUsername = chat.username.orEmpty()
        if (recipientUsername.isBlank()) {
            Toast.makeText(requireContext(), R.string.share_message_failed, Toast.LENGTH_SHORT).show()
            return
        }

        val displayName = chat.name.orEmpty().ifBlank { recipientUsername }
        val normalizedType = when (sharedMediaType?.lowercase(Locale.getDefault())) {
            Constants.IMAGE -> Constants.IMAGE
            Constants.VIDEO -> Constants.VIDEO
            else -> null
        }

        val hasMediaAttachment = !sharedMediaUrl.isNullOrBlank() && !normalizedType.isNullOrBlank()

        try {
            if (hasMediaAttachment && postId.isNotBlank()) {
                // Use the new HTTP endpoint to share post with media
                currentRecipientUsername = recipientUsername // Store for observer
                individualViewModal.sharePostMessage(
                    recipientUsername,
                    normalizedType!!,
                    "", // Optional message text
                    postId,
                    sharedMediaUrl.orEmpty()
                )
            } else if (hasMediaAttachment) {
                // Fallback: direct socket send if no postId (shouldn't happen for post sharing)
                val messageID = UUID.randomUUID().toString().replace("-", "")
                socketViewModel.sendPrivateMessage(
                    normalizedType!!,
                    "",
                    recipientUsername,
                    sharedMediaUrl.orEmpty(),
                    sharedThumbnailUrl.orEmpty(),
                    sharedMediaId.orEmpty(),
                    messageID,
                    null
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.share_message_sent, displayName),
                    Toast.LENGTH_SHORT
                ).show()
                dismissAllowingStateLoss()
            } else {
                // Text-only share (with deep link)
                val messageID = UUID.randomUUID().toString().replace("-", "")
                val message = getString(R.string.post_share_message_template, shareLink)
                socketViewModel.sendPrivateMessage("text", message, recipientUsername, "", "", "", messageID, postId.takeIf { it.isNotBlank() })
                Toast.makeText(
                    requireContext(),
                    getString(R.string.share_message_sent, displayName),
                    Toast.LENGTH_SHORT
                ).show()
                dismissAllowingStateLoss()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.share_message_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildDeepLink(postID: String, userID: String): String {
        val encryptedPostID = EncryptionHelper.encrypt(postID)
        val encryptedUserID = EncryptionHelper.encrypt(userID)
        val baseUrl = "${BuildConfig.SHARE_DEEP_LINK_HOST}/share/posts"
        return "$baseUrl?postID=$encryptedPostID&userID=$encryptedUserID"
    }

    companion object {
        private const val ARG_POST_ID = "ARG_POST_ID"
        private const val ARG_OWNER_USER_ID = "ARG_OWNER_USER_ID"
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val ARG_MEDIA_TYPE = "ARG_MEDIA_TYPE"
        private const val ARG_MEDIA_URL = "ARG_MEDIA_URL"
        private const val ARG_THUMBNAIL_URL = "ARG_THUMBNAIL_URL"
        private const val ARG_MEDIA_ID = "ARG_MEDIA_ID"

        fun newInstance(
            postId: String,
            ownerUserId: String,
            mediaType: String? = null,
            mediaUrl: String? = null,
            thumbnailUrl: String? = null,
            mediaId: String? = null
        ): SharePostBottomSheetFragment {
            return SharePostBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                    putString(ARG_OWNER_USER_ID, ownerUserId)
                    putString(ARG_MEDIA_TYPE, mediaType)
                    putString(ARG_MEDIA_URL, mediaUrl)
                    putString(ARG_THUMBNAIL_URL, thumbnailUrl)
                    putString(ARG_MEDIA_ID, mediaId)
                }
            }
        }
    }
}

