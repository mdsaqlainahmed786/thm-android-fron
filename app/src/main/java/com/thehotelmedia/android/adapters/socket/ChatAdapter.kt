package com.thehotelmedia.android.adapters.socket

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thehotelmedia.android.R
import com.thehotelmedia.android.SocketModals.fetchConversation.Messages
import com.thehotelmedia.android.activity.PostPreviewActivity
import com.thehotelmedia.android.activity.VideoImageViewer
import com.thehotelmedia.android.customClasses.Constants.IMAGE
import com.thehotelmedia.android.customClasses.Constants.URL_PATTERN
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.databinding.ChatItemLayoutBinding
import com.thehotelmedia.android.extensions.censorAbusiveWords
import com.thehotelmedia.android.extensions.formatDateTime
import com.thehotelmedia.android.extensions.loadAbusiveWordsFromJson
import com.thehotelmedia.android.extensions.toFormattedDate

// ChatAdapter.kt
class ChatAdapter(
    private val context: Context,
    private val onStoryClick: ((storyId: String, sentByMe: Boolean, storyCreatedAt: String?) -> Unit)? = null,
    private val onMessageAction: ((message: Messages, action: String) -> Unit)? = null
) : PagingDataAdapter<Messages, ChatAdapter.ViewHolder>(ChatMessagesComparator) {

    inner class ViewHolder(val binding: ChatItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ChatItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val message = getItem(position)

        // Setup view depending on the message type
        setupMessageView(binding, message)

        // Show or hide date layout based on the createdAt value
        if (position == 0 || isDifferentDate(position)) {
            binding.dateLayout.visibility = View.VISIBLE
            binding.dateTv.text = message?.createdAt.toFormattedDate()
        } else {
            binding.dateLayout.visibility = View.GONE
        }

    }

    // Function to check if the current message date is different from the previous message
    private fun isDifferentDate(position: Int): Boolean {
        val currentMessage = getItem(position)
        val previousMessage = getItem(position - 1)

        // If either message is null, consider them as having different dates
        if (currentMessage == null || previousMessage == null) return true

        val currentDate = currentMessage.createdAt.toFormattedDate()
        val previousDate = previousMessage.createdAt.toFormattedDate()

        // Compare the formatted dates
        return currentDate != previousDate
    }

//     Function to format the date
//    private fun formatDate(createdAt: String?): String {
//        if (createdAt.isNullOrEmpty()) return ""
//
//        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
//        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
//
//        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
//
//        return try {
//            val date = inputFormat.parse(createdAt)
//            outputFormat.format(date ?: Date())
//        } catch (e: Exception) {
//            ""
//        }
//    }

    private fun setupMessageView(binding: ChatItemLayoutBinding, message: Messages?) {
        if (message == null) return

        val type = message.type
        val mediaUrl = message.mediaUrl ?: ""
        val thumbnailUrl = message.thumbnailUrl ?: ""
        val msg = message.message.toString().trim()

        val abusiveWords = context.loadAbusiveWordsFromJson() // Load from JSON
        val messageText = msg.censorAbusiveWords(abusiveWords)

        // Handle deleted messages
        val isDeleted = message.isDeleted ?: false
        if (isDeleted) {
            binding.chatMessageText.visibility = View.GONE
            binding.deletedMessageText.visibility = View.VISIBLE
            val deletedText = if (message.sentByMe == true) {
                "You deleted this message"
            } else {
                "This message was deleted"
            }
            binding.deletedMessageText.text = deletedText
            binding.deletedMessageText.setTypeface(null, Typeface.ITALIC)
        } else {
            binding.chatMessageText.visibility = View.VISIBLE
            binding.deletedMessageText.visibility = View.GONE
            makeLinksClickable(binding.chatMessageText, messageText, Color.WHITE, Color.TRANSPARENT)
        }

        // Handle edited label logic (Moved outside bubble as per user request)
        binding.editedLabel.visibility = View.GONE
        val isEdited = message.isEdited ?: false
        val editedAt = message.editedAt

        val timeStr = if (isEdited && !isDeleted && !editedAt.isNullOrEmpty()) {
            "Edited · ${formatDateTime(editedAt)}"
        } else {
            formatDateTime(message.createdAt.toString())
        }

        binding.timeText.text = timeStr
        binding.pdfNameTv.text  = if (isDeleted) "" else message.message

        // Handle different message types (text, image, video, pdf)
        when (type) {
            "text" -> {
                binding.messageLayout.visibility = View.VISIBLE
                binding.storyLayout.visibility = View.GONE
                binding.mediaLayout.visibility = View.GONE
                binding.pdfLayout.visibility = View.GONE
            }
            "video", "image" -> {
                binding.pdfLayout.visibility = View.GONE
                binding.messageLayout.visibility = if (isDeleted) View.VISIBLE else View.GONE
                binding.storyLayout.visibility = View.GONE
                binding.mediaLayout.visibility = if (isDeleted) View.GONE else View.VISIBLE
//                Glide.with(context).load(mediaUrl).placeholder(R.drawable.ic_post_placeholder).into(binding.chatMediaIv)
                if (type == "video" ){
                    Glide.with(context).load(thumbnailUrl).placeholder(R.drawable.ic_post_placeholder).into(binding.chatMediaIv)
                }else{
                    Glide.with(context).load(mediaUrl).placeholder(R.drawable.ic_post_placeholder).into(binding.chatMediaIv)
                }

                binding.playIcon.visibility = if (type == "video") View.VISIBLE else View.GONE
            }
            "pdf" -> {
                binding.messageLayout.visibility = if (isDeleted) View.VISIBLE else View.GONE
                binding.mediaLayout.visibility = View.GONE
                binding.storyLayout.visibility = View.GONE
                binding.pdfLayout.visibility = if (isDeleted) View.GONE else View.VISIBLE
            }
            "story-comment" -> {
                binding.messageLayout.visibility = View.VISIBLE
                binding.mediaLayout.visibility = View.GONE
                binding.pdfLayout.visibility = View.GONE
                binding.storyLayout.visibility = View.VISIBLE

                val isStoryAvailable = message.isStoryAvailable ?: false
                val sentByMe = message.sentByMe ?: false
                val storyId = message.storyID ?: ""
                val storyCreatedAt = message.createdAt // Use message createdAt as fallback, but ideally should be story's createdAt
                
                android.util.Log.d("ChatAdapter", "Setting up story-comment - storyId: $storyId, isStoryAvailable: $isStoryAvailable, onStoryClick null: ${onStoryClick == null}")
                
                if (sentByMe){
                    binding.replyTv.text = "You replied to story"
                }else{
                    binding.replyTv.text = "Replied to your story"
                }
                if (isStoryAvailable){

                    binding.storyAvailableTv.visibility = View.GONE
                    binding.storyIv.visibility = View.VISIBLE
                    if (mediaUrl.endsWith(".m3u8")) {
                        // Generate thumbnail from video URL
                        val thumbnail = getVideoThumbnail(mediaUrl)
                        // If thumbnail is not null, load it
                        if (thumbnail != null) {
                            Glide.with(context).load(thumbnail).placeholder(R.drawable.ic_post_placeholder).into(binding.storyIv)
                        }
                    } else {
                        // Load image normally if it's not a video
                        Glide.with(context).load(mediaUrl).placeholder(R.drawable.ic_post_placeholder).into(binding.storyIv)
                    }
                    
                    // Make story layout clickable - clear any previous listener first
                    binding.storyLayout.setOnClickListener(null)
                    binding.storyLayout.setOnClickListener {
                        android.util.Log.d("ChatAdapter", "Story layout clicked - storyId: $storyId, sentByMe: $sentByMe")
                        // Pass storyId, sentByMe flag, and createdAt to the callback
                        // userId will be determined in InboxScreenActivity based on sentByMe
                        if (onStoryClick != null) {
                            onStoryClick.invoke(storyId, sentByMe, storyCreatedAt)
                        } else {
                            android.util.Log.e("ChatAdapter", "onStoryClick callback is null!")
                        }
                    }
                    binding.storyLayout.isClickable = true
                    binding.storyLayout.isFocusable = true
                }else{
                    binding.storyIv.visibility = View.GONE
                    binding.storyAvailableTv.visibility = if (isDeleted) View.GONE else View.VISIBLE
                    binding.storyAvailableTv.text = mediaUrl
                    // Clear click listener if story is not available
                    binding.storyLayout.setOnClickListener(null)
                }

                if (isDeleted) {
                    binding.storyLayout.visibility = View.GONE
                }
            }
        }

        // Check if the message is sent by the user
        if (message.sentByMe == true) {
            binding.root.gravity = Gravity.END
            val layoutParams = binding.storyIv.layoutParams as LinearLayout.LayoutParams
            layoutParams.gravity = Gravity.END
            binding.storyIv.layoutParams = layoutParams

            binding.messageLayout.setBackgroundResource(R.drawable.msg_sender_background)
            binding.mediaLayout.setBackgroundResource(R.drawable.msg_sender_background)
            binding.pdfLayout.setBackgroundResource(R.drawable.msg_sender_background)
            binding.timeText.gravity = Gravity.END
        } else {
            binding.root.gravity = Gravity.START

            val layoutParams = binding.storyIv.layoutParams as LinearLayout.LayoutParams
            layoutParams.gravity = Gravity.START
            binding.storyIv.layoutParams = layoutParams

            binding.messageLayout.setBackgroundResource(R.drawable.msg_receiver_background)
            binding.mediaLayout.setBackgroundResource(R.drawable.msg_receiver_background)
            binding.pdfLayout.setBackgroundResource(R.drawable.msg_receiver_background)
            binding.timeText.gravity = Gravity.START
        }

        // Add long-press listener for text messages to show context menu
        val isTextMessage = type == "text"
        val isMediaMessage = type == IMAGE || type == VIDEO
        
        if (!isDeleted && isTextMessage) {
            val longClickListener = View.OnLongClickListener {
                // Show context menu for text messages
                if (onMessageAction != null) {
                    onMessageAction.invoke(message, "show_menu")
                }
                true
            }
            binding.root.setOnLongClickListener(longClickListener)
            binding.messageLayout.setOnLongClickListener(longClickListener)
            binding.chatMessageText.setOnLongClickListener(longClickListener)
        } else if (!isDeleted && isMediaMessage) {
            // Add long-press listener for image/video messages to show delete-only menu
            val longClickListener = View.OnLongClickListener {
                // Show delete-only menu for media messages
                if (onMessageAction != null) {
                    onMessageAction.invoke(message, "show_delete_menu")
                }
                true
            }
            binding.root.setOnLongClickListener(longClickListener)
            binding.mediaLayout.setOnLongClickListener(longClickListener)
            binding.chatMediaIv.setOnLongClickListener(longClickListener)
        } else {
            binding.root.setOnLongClickListener(null)
            binding.messageLayout.setOnLongClickListener(null)
            binding.chatMessageText.setOnLongClickListener(null)
            binding.mediaLayout.setOnLongClickListener(null)
            binding.chatMediaIv.setOnLongClickListener(null)
        }

        binding.root.setOnClickListener {
            // Don't handle clicks for deleted messages
            if (isDeleted) {
                return@setOnClickListener
            }
            
            // Don't handle story-comment clicks here - let storyLayout handle them
            if (type == "story-comment") {
                return@setOnClickListener
            }
            if (type == "pdf") {
                openPdfInDevice(mediaUrl)
            } else if (type == IMAGE || type == VIDEO) {
                // Check if this is a shared post - if postID exists, navigate to feed page
                val postID = message.postID
                if (!postID.isNullOrBlank()) {
                    // Navigate to the main feed activity and scroll to the post
                    val intent = Intent(context, com.thehotelmedia.android.activity.userTypes.individual.bottomNavigation.BottomNavigationIndividualMainActivity::class.java).apply {
                        putExtra("SCROLL_TO_POST_ID", postID)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } else {
                    // Regular media message - open in video/image viewer
                    val intent = Intent(context, VideoImageViewer::class.java).apply {
                        putExtra("MEDIA_URL", mediaUrl)
                        putExtra("MEDIA_TYPE", type)
                        putExtra("FROM", "CHAT")
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    fun getVideoThumbnail(videoUrl: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoUrl, HashMap<String, String>())
            retriever.frameAtTime // Retrieve the first frame as thumbnail
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    // Function to open a PDF from the given URL
    private fun openPdfInDevice(pdfUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(pdfUrl), "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        context.startActivity(intent)
    }

    object ChatMessagesComparator : DiffUtil.ItemCallback<Messages>() {
        override fun areItemsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem.Id == newItem.Id
        }

        override fun areContentsTheSame(oldItem: Messages, newItem: Messages): Boolean {
            return oldItem.Id == newItem.Id &&
                    oldItem.message == newItem.message &&
                    oldItem.isEdited == newItem.isEdited &&
                    oldItem.editedAt == newItem.editedAt &&
                    oldItem.isDeleted == newItem.isDeleted &&
                    oldItem.messageID == newItem.messageID
        }
    }

    private fun makeLinksClickable(textView: TextView, message: String, textColor: Int, bgColor: Int) {
        val spannableString = SpannableString(message)
        URL_PATTERN.findAll(message).forEach { matchResult ->
            val url = matchResult.value
            val start = matchResult.range.first
            val end = matchResult.range.last + 1

            spannableString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    widget.context.startActivity(intent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = textColor
                    ds.isUnderlineText = true
                    ds.bgColor = bgColor
                    ds.textSkewX = -0.2f      // Makes text italic
                    ds.isFakeBoldText = false  // Optional: Bold + Italic
                }
            }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

}
