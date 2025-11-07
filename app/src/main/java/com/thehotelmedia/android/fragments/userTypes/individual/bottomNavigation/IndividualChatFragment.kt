package com.thehotelmedia.android.fragments.userTypes.individual.bottomNavigation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.thehotelmedia.android.R
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.SocketPagination.ChatScreenPagingSource
import com.thehotelmedia.android.activity.NotificationActivity
import com.thehotelmedia.android.adapters.LoaderAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.chat.ChatListAdapter
import com.thehotelmedia.android.adapters.userTypes.individual.chat.RecentChatProfileAdapter
import com.thehotelmedia.android.customClasses.CustomProgressBar
import com.thehotelmedia.android.customClasses.PreferenceManager
import com.thehotelmedia.android.databinding.FragmentIndividualChatBinding
import com.thehotelmedia.android.extensions.NotificationDotUtil
import com.thehotelmedia.android.extensions.setOnSwipeListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IndividualChatFragment : Fragment() {

    private lateinit var binding: FragmentIndividualChatBinding
    private lateinit var preferenceManager: PreferenceManager
    private val socketViewModel: SocketViewModel by viewModels()
    private lateinit var recentChatProfileAdapter: RecentChatProfileAdapter
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var progressBar: CustomProgressBar
    private var query: String = ""
    private var userName: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_individual_chat, container, false)
        initUI()
        initializeAndUpdateNotificationDot()
        setupSwipeGestures()
        return binding.root
    }

    private fun initializeAndUpdateNotificationDot() {
        NotificationDotUtil.initializeAndUpdateNotificationDot(
            requireContext(),
            binding.redDotView,
            preferenceManager
        )
    }

    override fun onResume() {
        super.onResume()
//        socketViewModel.connectSocket(userName)
        // Ensure fragment is attached before proceeding
        if (isAdded) {
            userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
            socketViewModel.connectSocket(userName)
            fetchSocketData()
        }
//        userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
//        socketViewModel.connectSocket(userName)
//        fetchSocketData()
    }

    override fun onPause() {
        super.onPause()
        socketViewModel.leaveChat()
        socketViewModel.removeAllListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationDotUtil.unregisterReceiver(requireContext())
    }

    private fun initUI() {
        preferenceManager = PreferenceManager.getInstance(requireContext())
        progressBar = CustomProgressBar(requireContext())

        binding.notificationBtn.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

//        binding.searchEt.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {}
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                query = s.toString().trim()
////                fetchSocketData()
//                fetchChatData()
//            }
//        })

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s.toString().trim()
                // Remove any previous callbacks
                searchRunnable?.let { handler.removeCallbacks(it) }
                // Create a new delayed task
                searchRunnable = Runnable {
                    fetchChatData()
                }
                // Post with delay (e.g., 500ms)
                handler.postDelayed(searchRunnable!!, 300)
            }
        })



        setupRecyclerViews()
    }

    private fun setupRecyclerViews() {
        chatListAdapter = ChatListAdapter(requireContext())

        binding.chatListRv.adapter = chatListAdapter.withLoadStateFooter(footer = LoaderAdapter())
        binding.chatListRv.isNestedScrollingEnabled = false


    }

    private fun fetchSocketData() {
        fetchChatData()
        fetchUserData()

        userConnectDisconnect()
    }

    private fun userConnectDisconnect() {
        socketViewModel.connectUser.observe(viewLifecycleOwner) { message ->
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            fetchUserData()
        }
        socketViewModel.disconnectUser.observe(viewLifecycleOwner) { message ->
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            fetchUserData()
        }
    }

    private fun fetchChatData() {
        lifecycleScope.launch {
            val chatFlow = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                ChatScreenPagingSource(socketViewModel, query)
            }.flow

            chatFlow.collectLatest { pagingData ->
                handleLoadingState()
                chatListAdapter.submitData(pagingData)
            }
        }
    }

    private fun fetchUserData() {
        socketViewModel.fetchUsers()
        socketViewModel.inChat()

        socketViewModel.receivedMessage.observe(viewLifecycleOwner) { message ->
//            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            fetchChatData()
        }

        socketViewModel.usersList.observe(viewLifecycleOwner) { users ->
//            if (!users.isNullOrEmpty()) {
                recentChatProfileAdapter = RecentChatProfileAdapter(requireContext(), users)
                binding.recentChatRv.adapter = recentChatProfileAdapter
//            } else {
//                Log.d("IndividualChatFragment", "No users found.")
//            }
        }
    }

    private fun handleLoadingState() {
        chatListAdapter.addLoadStateListener { loadState ->
            val isLoading = loadState.refresh is LoadState.Loading
            val isEmpty = loadState.refresh is LoadState.NotLoading && chatListAdapter.itemCount == 0

//            if (isLoading) progressBar.show() else progressBar.hide()
            if (isEmpty){
                binding.noDataFoundLayout.visibility = View.VISIBLE
                binding.recentChatTv.visibility = View.GONE
                binding.chatListRv.visibility = View.GONE
            }else{
                binding.noDataFoundLayout.visibility = View.GONE
                binding.recentChatTv.visibility = View.VISIBLE
                binding.chatListRv.visibility = View.VISIBLE
            }

//            binding.noDataFoundLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
//            binding.chatListRv.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun setupSwipeGestures() {
        binding.root.setOnSwipeListener(
            onSwipeLeft = {
                // Swipe right -> left: Open story creation page
                val intent = Intent(requireContext(), com.thehotelmedia.android.activity.userTypes.forms.createStory.CreateStoryActivity::class.java)
                startActivity(intent)
            },
            onSwipeRight = null // No action for left->right swipe on messages tab
        )
    }
}
