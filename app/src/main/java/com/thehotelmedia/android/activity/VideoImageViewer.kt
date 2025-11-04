package com.thehotelmedia.android.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.thehotelmedia.android.R
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.MediaItems
import com.thehotelmedia.android.adapters.MediaType
import com.thehotelmedia.android.adapters.VideoImageViewerAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.home.MediaActionCallback
import com.thehotelmedia.android.bottomSheets.CommentsBottomSheetFragment
import com.thehotelmedia.android.bottomSheets.YesOrNoBottomSheetFragment
import com.thehotelmedia.android.customClasses.Constants
import com.thehotelmedia.android.customClasses.Constants.VIDEO
import com.thehotelmedia.android.customClasses.MessageStore
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityVideoImageViewerBinding
import com.thehotelmedia.android.downloadManager.MediaDownloadManager
import com.thehotelmedia.android.extensions.sharePostWithDeepLink
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoImageViewer : DarkBaseActivity() {

    private lateinit var binding: ActivityVideoImageViewerBinding
    private lateinit var adapter: VideoImageViewerAdapter
    private lateinit var mediaList: List<MediaItems>
    private lateinit var individualViewModal: IndividualViewModal
    private var exoPlayer: ExoPlayer? = null

    private lateinit var preferenceManager: PreferenceManager
    private var isLikedByMe = false
    private var likeCount = 0
    private var commentCount = 0
    private var postId = ""
    private lateinit var mediaDownloadManager: MediaDownloadManager
    private var isLandscape = false // To track the current state of rotation



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUi()


        onBackPressedDispatcher.addCallback(this) {
            handelBackPress()
        }
    }

    private fun handelBackPress() {
        // Handle back press
        MediaActionCallback.onMediaAction?.invoke(isLikedByMe, likeCount, commentCount)
        finish() // or custom behavior
    }


    private fun initUi() {

        binding.rotateBtn.setOnClickListener {
            if (isLandscape) {
                isLandscape = false
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                isLandscape = true
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }

        exoPlayer = ExoPlayer.Builder(this).build()

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null, individualRepo, null))[IndividualViewModal::class.java]
        preferenceManager = PreferenceManager.getInstance(this)
        val ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
        mediaDownloadManager = MediaDownloadManager(this)

        binding.backBtn.setOnClickListener {
            handelBackPress()
        }

        val mediaType = intent.getStringExtra("MEDIA_TYPE")
        val mediaUrl = intent.getStringExtra("MEDIA_URL") ?: ""
        val mediaId = intent.getStringExtra("MEDIA_ID") ?: ""
        postId = intent.getStringExtra("POST_ID") ?: ""
        val from = intent.getStringExtra("FROM") ?: ""
        isLikedByMe = intent.getBooleanExtra("LIKED_BY_ME",false)
        likeCount = intent.getIntExtra("LIKE_COUNT",0)
        commentCount = intent.getIntExtra("COMMENT_COUNT",0)

        println("Afsafaskhkjasdk  IN_VIDEO_IMAGE_VIEWER $isLikedByMe")

        if (from == "CHAT"){
            binding.downloadNow.visibility = View.VISIBLE
        }

        binding.downloadNow.setOnClickListener {

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val fileName = "THM $mediaType $currentDate"


            val bottomSheet = YesOrNoBottomSheetFragment.newInstance(MessageStore.sureWantToDownloadMedia(this))
            bottomSheet.onYesClicked = {

                if(mediaType == VIDEO){
                    mediaDownloadManager.downloadM3U8Video(fileName, mediaUrl)
                }else{
                    mediaDownloadManager.downloadFileFromUrl(fileName, mediaUrl)
                }

            }
            bottomSheet.onNoClicked = {

            }
            bottomSheet.show(supportFragmentManager, "YesOrNoBottomSheet")

//            mediaDownloadManager.downloadFileFromUrl(fileName, mediaUrl)
        }


        updateLikeBtn(isLikedByMe, binding.likeIv)

        binding.likeTv.text = likeCount.toString()
        binding.commentTv.text = commentCount.toString()

        if (postId.isNotEmpty()){
            binding.postBtnLayout.visibility = View.VISIBLE
        }else{
            binding.postBtnLayout.visibility = View.GONE
        }


//        binding.likeBtn.setOnClickListener {
//            likeCount += 1
//            MediaActionCallback.onMediaAction?.invoke(isLikedByMe, likeCount, commentCount)
//        }
        binding.likeBtn.setOnClickListener {
            likePost(postId)
            isLikedByMe = !isLikedByMe
            binding.likeIv.setImageResource(if (isLikedByMe) R.drawable.ic_like_icon else R.drawable.ic_unlike_icon_white)
            likeCount = if (isLikedByMe) likeCount + 1 else likeCount - 1
            binding.likeTv.text = likeCount.toString()
        }


        // Comment button click
        binding.commentBtn.setOnClickListener {
            val bottomSheetFragment = CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", postId)
                    putInt("COMMENTS_COUNT", commentCount)
                }
            }
            bottomSheetFragment.onCommentSent = { comment ->
                if (comment.isNotEmpty()) {
                    commentCount += 1
                    binding.commentTv.text = commentCount.toString()
                }
            }
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }


        // Share button click
        binding.shareBtn.setOnClickListener {
            sharePostWithDeepLink(postId,ownerUserId)
        }



        mediaList = if (mediaType == VIDEO){
            listOf(MediaItems(MediaType.VIDEO, mediaUrl,mediaId))
        }else{
            listOf(MediaItems(MediaType.IMAGE, mediaUrl,mediaId))
        }

        adapter = VideoImageViewerAdapter(this, mediaList,exoPlayer!!,mediaId,postId,individualViewModal,::onControllerVisible,::onMediaTypeChanged)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

    }

    private fun onMediaTypeChanged(mediaType: String) {
        if (mediaType == Constants.IMAGE){
            binding.rotateBtn.visibility = View.GONE
        }else{
            binding.rotateBtn.visibility = View.VISIBLE
        }
    }

    private fun onControllerVisible(controllerVisible: Boolean) {
        if (controllerVisible){
            if (postId.isNotEmpty()){
                binding.titleLayout.visibility = View.VISIBLE
                binding.postBtnLayout.visibility = View.VISIBLE
            }
        }else{
            if (postId.isNotEmpty()){
                binding.titleLayout.visibility = View.GONE
                binding.postBtnLayout.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.pause()
    }

    private fun updateLikeBtn(postLiked: Boolean, likeIv: ImageView) {
        if (postLiked) {
            likeIv.setImageResource(R.drawable.ic_like_icon)
        } else {
            likeIv.setImageResource(R.drawable.ic_unlike_icon_white)
        }
    }

    private fun likePost(id: String) {
        individualViewModal.likePost(id)
    }


}