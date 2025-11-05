package com.thehotelmedia.android.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.thehotelmedia.android.ViewModelFactory
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.NotificationAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.CustomSnackBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.ActivityNotificationBinding
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.viewModal.individualViewModal.IndividualViewModal
import kotlinx.coroutines.launch

class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var progressBar : CustomProgressBar
    private lateinit var individualViewModal: IndividualViewModal
    private lateinit var preferenceManager : PreferenceManager
    private lateinit var notificationAdapter: NotificationAdapter
    private var ownerUserId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
    }

    private fun initUI() {

        preferenceManager = PreferenceManager.getInstance(this)

        ownerUserId = preferenceManager.getString(PreferenceManager.Keys.USER_ID, "").toString()
        NotificationDotUtil.clearUnreadNotificationsAndBroadcast(this, preferenceManager)

        val individualRepo = IndividualRepo(this)
        individualViewModal = ViewModelProvider(this, ViewModelFactory(null,individualRepo,null))[IndividualViewModal::class.java]

        progressBar = CustomProgressBar(this)

        getNotificationData()

        binding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        individualViewModal.acceptFollowResult.observe(this){result->
            if (result.status == true){
                loadNotificationData()
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.declineRequestResult.observe(this){result->
            if (result.status == true){
                loadNotificationData()
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }
        individualViewModal.followBackResult.observe(this){result->
            if (result.status == true){
                loadNotificationData()
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.collaborationRespondResult.observe(this){result->
            if (result.status == true){
                loadNotificationData()
            }else{
                val msg = result.message.toString()
                CustomSnackBar.showSnackBar(binding.root,msg)
            }
        }

        individualViewModal.loading.observe(this){
            if (it == true){
                progressBar.show()
            }else{
                progressBar.hide()
            }
        }

        individualViewModal.toast.observe(this){
            CustomSnackBar.showSnackBar(binding.root,it)
        }

    }

    private fun loadNotificationData() {
        Handler(Looper.getMainLooper()).postDelayed({
            getNotificationData()
        }, 800)
    }

    private fun getNotificationData() {
        notificationAdapter = NotificationAdapter(
            this,
            ::onDeclineClick,
            ::onAcceptClick,
            ::onFollowClick,
            ::onCollaborationAcceptClick,
            ::onCollaborationDeclineClick,
            ownerUserId
        )
        binding.notificationRv.adapter = notificationAdapter

        binding.notificationRv.adapter = notificationAdapter
            .withLoadStateFooter(footer = LoaderAdapter())

        individualViewModal.getNotification().observe(this) {
            this.lifecycleScope.launch {
                isLoading()
                notificationAdapter.submitData(it)
            }
        }


    }

    private fun onDeclineClick(connectionId: String, position: Int) {
        if (connectionId.isNotEmpty()){
            individualViewModal.declineRequest(connectionId)
        }
    }



    private fun onAcceptClick(connectionId: String?) {
        if (!connectionId.isNullOrEmpty()){
            individualViewModal.acceptRequest(connectionId)
        }
    }

    private fun onFollowClick(connectionId: String?) {
        if (!connectionId.isNullOrEmpty()){
            individualViewModal.followBack(connectionId)
        }
    }

    private fun onCollaborationAcceptClick(postID: String, notificationId: String) {
        if (postID.isNotEmpty() && postID != "null") {
            individualViewModal.collaborationRespond(postID, "accept")
        }
    }

    private fun onCollaborationDeclineClick(postID: String, notificationId: String) {
        if (postID.isNotEmpty() && postID != "null") {
            individualViewModal.collaborationRespond(postID, "reject")
        }
    }

    private fun isLoading() {
        notificationAdapter.addLoadStateListener {
            val isLoading = it.refresh is LoadState.Loading
            val isEmpty = it.refresh is LoadState.NotLoading && notificationAdapter.itemCount == 0

            if (isLoading) {
                progressBar.show()
            } else {
                progressBar.hide()
            }

            // Handle empty state (optional)
            if (isEmpty) {
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.hasDataLayout.visibility = View.GONE
            } else {
                binding.noDataFoundLayout.visibility = View.GONE
                binding.hasDataLayout.visibility = View.VISIBLE
            }
        }
    }

}