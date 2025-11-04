package com.thehotelmedia.android.activity

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.SocketModals.fetchConversation.Messages
import com.thehotelmedia.android.SocketPagination.FetchConversationPagingSource
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.socket.ChatAdapter
import com.thehotelmedia.android.bottomSheets.BlockUserBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.ReportBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants.DEFAULT_PDF_MB
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityInboxScreenBinding
import com.thehotelmedia.android.downloadManager.FileDownloadManager
import com.thehotelmedia.android.extensions.toISO8601UTC
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID

class InboxScreenActivity : BaseActivity() , BlockUserBottomSheetFragment.BottomSheetListener{

    // View Binding
    private lateinit var binding: ActivityInboxScreenBinding
    // Variables
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var progressBar: CustomProgressBar
    private val socketViewModel: SocketViewModel by viewModels()

    private var myUserName: String = ""
    private var from: String = ""
    private var name: String = ""
    private var userName: String = ""
    private var profilePic: String = ""
    private var userId: String = ""
    private var pdfSizeLimit = 5
    private var scrollButtonAnimator: ObjectAnimator? = null
    private lateinit var individualViewModal: IndividualViewModal

    private lateinit var fetchConversationPagingSource: FetchConversationPagingSource
    private var isEmojiPickerVisible = false

    var isBlocked = false
    private lateinit var fileDownloadManager: FileDownloadManager

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Check the file type
            val mimeType = contentResolver.getType(uri)

            // Get the file size
            val fileSizeInBytes = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong() // Get file size in bytes
            } ?: 0L

            when {
                mimeType?.startsWith("image/") == true -> {
//                    Toast.makeText(this, "Selected Image", Toast.LENGTH_SHORT).show()
                    // You can now send the image file as multipart
                    sendFileToServer(uri, "image")
                }
                mimeType?.startsWith("video/") == true -> {
//                    Toast.makeText(this, "Selected Video", Toast.LENGTH_SHORT).show()
                    // You can now send the video file as multipart
                    sendFileToServer(uri, "video")
                }
                mimeType == "application/pdf" -> {
////                    Toast.makeText(this, "Selected PDF", Toast.LENGTH_SHORT).show()
//                    // You can now send the PDF file as multipart
//                    sendFileToServer(uri, "pdf")

                    if (fileSizeInBytes > pdfSizeLimit * 1024 * 1024) { // 5 MB in bytes
                        val errorMessage = getString(R.string.pdf_limit_in_mb, pdfSizeLimit)
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        sendFileToServer(uri, "pdf")
                    }
                }
                else -> {
                    Toast.makeText(this, MessageStore.fileNotSupported(this), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInboxScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
    }

    override fun onResume() {
        socketViewModel.connectSocket(myUserName)
        fetchConversationData()
        super.onResume()
    }

    override fun onDestroy() {
//        socketViewModel.leavePrivateChat(userName)
////        socketViewModel.leaveChat()
//        socketViewModel.removeAllListeners()
        super.onDestroy()
    }

    override fun onPause() {

        socketViewModel.leavePrivateChat(userName)
//        socketViewModel.leaveChat()
        socketViewModel.removeAllListeners()
        super.onPause()

    }

    /**
     * Initialize the UI components and set up listeners.
     */
    private fun initializeUI() {
        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Fetch intent data
        from = intent.getStringExtra("FROM") ?: ""
        name = intent.getStringExtra("NAME") ?: ""
        userName = intent.getStringExtra("USER_NAME") ?: ""
        profilePic = intent.getStringExtra("PROFILE_PIC") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: ""
        fileDownloadManager = FileDownloadManager(this)
        println("asfjsakjkl   $userId")
        // Set title and profile picture
        binding.titleTv.text = name
        Glide.with(this).load(profilePic)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(binding.profilePicIv)



        if (from == "job"){
            binding.messageEt.setText(getString(R.string.job_application_message))
        }


        socketViewModel.receivedMessage.observe(this) { message ->
            val msg = message.message?.message.toString().trim()
            val mediaUrl = message.message?.mediaUrl.orEmpty()
            val thumbnailUrl = message.message?.thumbnailUrl.orEmpty()
            val mediaId = message.message?.mediaID.orEmpty()
            val type = message.message?.type.orEmpty()
            val from = message.from.orEmpty()
            val time = message.time.orEmpty()
            val isSeen = message.isSeen ?: true




            if (from == userName) {
                val uniqueMessageId = UUID.randomUUID().toString().replace("-", "")
                val staticMessage = Messages(
                    Id = uniqueMessageId,
                    message = msg,
                    isSeen = isSeen,
                    type = type,
                    mediaUrl = mediaUrl,
                    thumbnailUrl = thumbnailUrl,
                    mediaID = mediaId,
                    createdAt = time,
                    _v = 0,
                    sentByMe = false
                )
                addStaticMessage(staticMessage, false)
            }
        }



        binding.profilePicIv.setOnClickListener {
            if (userId.isNotEmpty()){
                val intent = Intent(this, BusinessProfileDetailsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }

        binding.menuBtn.setOnClickListener { view ->
            showMenuDialog(view)
        }



        // Initialize utilities
        preferenceManager = PreferenceManager.getInstance(this)
        progressBar = CustomProgressBar(this)
        myUserName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
        pdfSizeLimit = preferenceManager.getInt(PreferenceManager.Keys.PDF_SIZE_INT, DEFAULT_PDF_MB)


        chatAdapter = ChatAdapter(this)

//        socketViewModel.connectSocket(myUserName)
//        fetchConversationData()

        // Button click listeners
        binding.backBtn.setOnClickListener { finish() }
        binding.sendMsgBtn.setOnClickListener { sendMessage() }
        binding.scrollDownBtn.setOnClickListener { scrollToBottom() }
        binding.galleryBtn.setOnClickListener {
            openFilePicker()
        }
// Set the click listener for the emoji button
        binding.emojiBtn.setOnClickListener {
            if (isEmojiPickerVisible) {
                // Hide Emoji Picker
                binding.emojiPicker.visibility = View.GONE
                binding.emojiBtn.setImageResource(R.drawable.ic_emoji_small) // Change to emoji icon

                // Show Keyboard
                binding.messageEt.requestFocus()
                imm.showSoftInput(binding.messageEt, InputMethodManager.SHOW_IMPLICIT)

                // Enable the EditText when Emoji Picker is hidden
                binding.messageEt.isEnabled = true
            } else {
                // Hide Keyboard
                imm.hideSoftInputFromWindow(binding.messageEt.windowToken, 0)

                // Show Emoji Picker
                binding.emojiPicker.visibility = View.VISIBLE
                binding.emojiBtn.setImageResource(R.drawable.ic_keyboard) // Change to keyboard icon

                // Disable the EditText when Emoji Picker is visible
                binding.messageEt.isEnabled = false
            }

            // Toggle visibility state
            isEmojiPickerVisible = !isEmojiPickerVisible
        }

// Handle emoji selection
        binding.emojiPicker.setOnEmojiPickedListener { emoji ->
            binding.messageEt.append(emoji.emoji)
        }

// Handle soft keyboard closing when the user taps outside of the EditText
        binding.root.setOnTouchListener { _, event ->
            // If the emoji picker is visible, hide it when tapping outside the EditText
            if (isEmojiPickerVisible && event.action == MotionEvent.ACTION_DOWN) {
                binding.emojiPicker.visibility = View.GONE
                binding.emojiBtn.setImageResource(R.drawable.ic_emoji_small) // Reset emoji icon
                isEmojiPickerVisible = false
            }
            false // Allow other touches to propagate
        }


        // Set up RecyclerView scroll listener
        setupScrollListener()


        individualViewModal.sendMediaResult.observe(this){result->
            if (result.status==true){

                val msg = result.data?.message?.message.toString().trim() ?: ""
                val type = result.data?.message?.type ?: ""
                val mediaUrl = result.data?.message?.mediaUrl ?: ""
                val thumbnailUrl = result.data?.message?.thumbnailUrl ?: ""
                val mediaId = result.data?.message?.mediaID ?: ""

                socketViewModel.sendPrivateMessage(type, msg, userName,mediaUrl,thumbnailUrl,mediaId)
                val uniqueMessageId = UUID.randomUUID().toString().replace("-", "")
                val currentTime = Date().toISO8601UTC()
                val staticMessage = Messages(
                    Id = uniqueMessageId,
                    message = msg,
                    isSeen = false,
                    mediaUrl = mediaUrl,
                    thumbnailUrl = thumbnailUrl,
                    mediaID = mediaId,
                    type = type,
                    createdAt = currentTime,
                    _v = 0,
                    sentByMe = true
                )
                addStaticMessage(staticMessage, true)

            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.exportChatResult.observe(this){result->
            if (result.status==true){
                val fileUrl = result.data?.filePath ?: ""
                val fileName = result.data?.filename ?: "ChatFile.txt"
                if (fileUrl.isNotEmpty()){

                    val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToExportChat(this))
                    bottomSheet.onYesClicked = {
                        fileDownloadManager.downloadFile(fileName, fileUrl)
                    }
                    bottomSheet.onNoClicked = {

                    }
                    bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")

//                    fileDownloadManager.downloadFileFromUrl(fileName, fileUrl)
                }
            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }

        individualViewModal.deleteChatResult.observe(this){result->
            if (result.status==true){
                finish()
            }else{
                val msg = result.message
                Toast.makeText(this,msg, Toast.LENGTH_SHORT).show()
            }
        }


        individualViewModal.loading.observe(this){
            if (it == true){
                progressBar.show() // To show the progress bar
            }else{
                progressBar.hide() // To hide the progress bar
            }
        }

        individualViewModal.toast.observe(this){
            Toast.makeText(this,it, Toast.LENGTH_SHORT).show()
        }




    }
    private fun openFilePicker() {
        // Open the file picker for images, videos, and PDFs
        selectFileLauncher.launch("*/*") // "*" means any file type
    }



    /**
     * Handle sending a message.
     */
    private fun sendMessage() {
        val msg = binding.messageEt.text?.trim().toString()
        val type = "text"

        if (msg.isNotEmpty()) {
            binding.messageEt.text?.clear()
            socketViewModel.sendPrivateMessage(type, msg, userName,"","","")

            val uniqueMessageId = UUID.randomUUID().toString().replace("-", "")
            val currentTime = Date().toISO8601UTC()
            val staticMessage = Messages(
                Id = uniqueMessageId,
                message = msg,
                isSeen = false,
                type = type,
                createdAt = currentTime,
                _v = 0,
                sentByMe = true
            )
            addStaticMessage(staticMessage, true)
        }
    }

    /**
     * Scroll to the bottom of the chat.
     */
    private fun scrollToBottom() {
        scrollButtonAnimator?.cancel()
        binding.scrollDownBtn.visibility = View.GONE
        val layoutManager = binding.inboxRv.layoutManager as LinearLayoutManager
        layoutManager.scrollToPosition(chatAdapter.itemCount - 1)
    }

    /**
     * Add a static message to the chat.
     */
    private fun addStaticMessage(message: Messages, sentByMe: Boolean) {

        println("fsahksadh    $message")
        lifecycleScope.launch {
            val currentPagingData = chatAdapter.snapshot().filterNotNull().toMutableList()
            currentPagingData.add(message)

            val newPagingData = PagingData.from(currentPagingData)
            chatAdapter.submitData(newPagingData)


            if (sentByMe) {
                binding.inboxRv.scrollToPosition(currentPagingData.size - 1)
            } else {
                handleIncomingMessageScroll(currentPagingData)
            }
        }
    }




    /**
     * Handle scrolling behavior for incoming messages.
     */
    private fun handleIncomingMessageScroll(updatedMessages: MutableList<Messages>) {
        val layoutManager = binding.inboxRv.layoutManager as LinearLayoutManager
        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        if (totalItemCount - lastVisibleItemPosition <= 8) {
            binding.inboxRv.scrollToPosition(updatedMessages.size - 1)
        } else {
            binding.scrollDownBtn.visibility = View.VISIBLE
            animateScrollButton()
        }
    }

    /**
     * Fetch conversation data using paging.
     */
    private fun fetchConversationData() {
//        socketViewModel.inChat()
        socketViewModel.inPrivateChat(userName)
        socketViewModel.messageSeen(userName)

        if (binding.inboxRv.adapter == null) {
            binding.inboxRv.adapter = chatAdapter.withLoadStateFooter(LoaderAdapter())
            binding.inboxRv.isNestedScrollingEnabled = false
            fetchConversationPagingSource = FetchConversationPagingSource(socketViewModel, userName)
        }
        // Initialize a fresh FetchConversationPagingSource each time
        lifecycleScope.launch {
            val chatFlow = Pager(PagingConfig(pageSize = 80, prefetchDistance = 40, enablePlaceholders = true)) {
                FetchConversationPagingSource(socketViewModel, userName)  // Create a new instance of PagingSource
            }.flow

            chatFlow.collectLatest { chatAdapter.submitData(it) }
        }

//        socketViewModel.receivedMessage.observe(this) { message ->
//            val msg = message.message?.message.orEmpty()
//            val mediaUrl = message.message?.mediaUrl.orEmpty()
//            val type = message.message?.type.orEmpty()
//            val from = message.from.orEmpty()
//            val time = message.time.orEmpty()
//            val isSeen = message.isSeen ?: true
//
//
//
//
//            if (from == userName) {
//                val uniqueMessageId = UUID.randomUUID().toString().replace("-", "")
//                val staticMessage = Messages(
//                    Id = uniqueMessageId,
//                    message = msg,
//                    isSeen = isSeen,
//                    type = type,
//                    mediaUrl = mediaUrl,
//                    createdAt = time,
//                    _v = 0,
//                    sentByMe = false
//                )
//                addStaticMessage(staticMessage, false)
//            }
//        }


    }

    /**
     * Set up scroll listener for the RecyclerView.
     */
    private fun setupScrollListener() {
        binding.inboxRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                    scrollButtonAnimator?.cancel()
                    binding.scrollDownBtn.visibility = View.GONE
                }
            }
        })
    }

    /**
     * Animate the scroll-to-bottom button.
     */
    private fun animateScrollButton() {
        val bounceAnimator = ObjectAnimator.ofFloat(
            binding.scrollDownBtn, "translationY", 0f, -20f, 0f
        ).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        bounceAnimator.start()
        scrollButtonAnimator = bounceAnimator
    }


    private fun sendFileToServer(uri: Uri, fileType: String) {
        val originalFileName = getFileNameFromUri(uri)

        val savedUri = saveMediaToStorageFromUri(uri,fileType).toString()

        val validPath = savedUri.replace("file:", "")
        val mediaFile = File(validPath)

        individualViewModal.sendMediaMessage(userName, originalFileName, fileType, mediaFile)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val column_index = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                return it.getString(column_index)
            }
        }
        return "unknown_file" // Fallback in case something goes wrong
    }

    private fun saveMediaToStorageFromUri(uri: Uri, fileType: String): Uri? {
        // Extract the file name from the URI
        val fileName = getFileNameFromUri(uri)

        // Use a directory based on the file type (images, videos, etc.)
        val mediaDir = when (fileType) {
            "image" -> File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMediaImages")
            "video" -> File(this.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "HotelMediaVideos")
            "pdf" -> File(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HotelMediaPDFs")
            else -> File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HotelMedia")
        }

        // Create directory if it doesn't exist
        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }

        // Generate a unique file name based on the file type
        val fileExtension = when (fileType) {
            "image" -> ".jpg"
            "video" -> ".mp4"
            "pdf" -> ".pdf"
            else -> ""
        }

//        val mediaFile = File(mediaDir, "media_${System.currentTimeMillis()}$fileExtension")
        val mediaFile = File(mediaDir, "${fileName}_${System.currentTimeMillis()}$fileExtension")


        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(mediaFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Return the URI of the saved file
            return Uri.fromFile(mediaFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    private fun showMenuDialog(view: View?) {
        // Inflate the dropdown menu layout
        val inflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropdownView = inflater.inflate(R.layout.chat_menu_dropdown_item, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            dropdownView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find TextViews and set click listeners
        val blockBtn: TextView = dropdownView.findViewById(R.id.blockBtn)
        val reportBtn: TextView = dropdownView.findViewById(R.id.reportBtn)
        val deleteChatBtn: TextView = dropdownView.findViewById(R.id.deleteChatBtn)
        val exportChatBtn: TextView = dropdownView.findViewById(R.id.exportChatBtn)


        if (isBlocked){
            blockBtn.text = getString(R.string.unblock)
        }else{
            blockBtn.text = getString(R.string.block)
        }


        blockBtn.setOnClickListener {
            blockUser()
            popupWindow.dismiss()
        }

        reportBtn.setOnClickListener {
            reportUser()
            popupWindow.dismiss()
        }

        deleteChatBtn.setOnClickListener {
            deleteChat()
            popupWindow.dismiss()
        }
        exportChatBtn.setOnClickListener {
            individualViewModal.exportChat(userId)
            popupWindow.dismiss()
        }

        // Set the background drawable to make the popup more visually appealing
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.popup_background))

        // Show the popup window
        popupWindow.showAsDropDown(view)

        // Optionally, dismiss the popup when clicking outside of it
        popupWindow.setOnDismissListener {
            // Handle any actions you want to perform when the popup is dismissed
        }
    }

    private fun deleteChat() {
        val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDeleteChat(this))
        bottomSheet.onYesClicked = {
            individualViewModal.deleteChat(userId)
        }
        bottomSheet.onNoClicked = {

        }
        bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")
    }

    private fun reportUser() {

        val bottomSheetFragment = ReportBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString("ID", userId)
                putString("TYPE", "user")
            }
            onReasonSelected = { selectedReason ->
                individualViewModal.reportUser(userId,selectedReason)
            }
        }
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

    }
    private fun blockUser() {
        val bottomSheetFragment = BlockUserBottomSheetFragment.newInstance(isBlocked,userId)
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onBooleanDataReceived(isUserBlocked: Boolean) {
        // Handle the boolean returned from the fragment
        isBlocked = isUserBlocked
//        getUserProfile()

    }


}
