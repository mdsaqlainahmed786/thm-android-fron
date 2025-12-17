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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.paging.PagingDataAdapter

class IndividualChatFragment : Fragment() {

    private lateinit var binding: FragmentIndividualChatBinding
    private lateinit var preferenceManager: PreferenceManager
    private val socketViewModel: SocketViewModel by viewModels()
    private lateinit var recentChatProfileAdapter: RecentChatProfileAdapter
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var progressBar: CustomProgressBar
    private var query: String = ""
    private var userName: String = ""
    private var chatPagingSource: ChatScreenPagingSource? = null
    private var pagerJob: Job? = null

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_individual_chat, container, false)
        initUI()
        initializeAndUpdateNotificationDot()
        setupSwipeGestures()
        // Set up socket observation and Pager
        observeSocketStatus()
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
        refreshData()
        // Ensure listeners are re-attached in case they were removed by another activity
        ensureListenersAttached()
    }
    
    /**
     * Ensure socket listeners are attached. This is important because
     * other activities might remove listeners, but the Fragment still needs them.
     */
    private fun ensureListenersAttached() {
        // Re-attach listeners in case they were removed by another activity
        // This ensures the Fragment can still receive socket events
        if (isAdded && userName.isNotEmpty()) {
            socketViewModel.reattachListeners()
            // Also ensure socket is connected
            socketViewModel.connectSocket(userName)
        }
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded) {
            // Fragment became visible, refresh data and ensure listeners
            ensureListenersAttached()
            refreshData()
        } else if (hidden) {
            // Fragment hidden, disable auto-fetch
            socketViewModel.disableAutoFetchChatScreen()
        }
    }
    
    fun refreshDataIfNeeded() {
        // Public method to refresh data when fragment becomes visible
        refreshData()
    }
    
    private fun refreshData() {
        // Ensure fragment is attached before proceeding
        if (isAdded) {
            userName = preferenceManager.getString(PreferenceManager.Keys.USER_USER_NAME, "").orEmpty()
            socketViewModel.connectSocket(userName)
            // Set up Pager FIRST so it's ready to receive data
            setupChatPager()
            // Enable auto-fetch so CHAT_SCREEN is emitted when socket connects
            socketViewModel.enableAutoFetchChatScreen()
        }
    }
    
    /**
     * Set up the chat Pager. This should be called when socket is connected
     * so that the PagingSource is ready to receive responses.
     */
    private fun setupChatPager() {
        // Cancel previous collection if exists
        pagerJob?.cancel()
        
        // Always set up a new Pager to ensure fresh data
        pagerJob = lifecycleScope.launch {
            // Invalidate previous if exists
            chatPagingSource?.invalidate()
            chatPagingSource = null
            
            val chatFlow = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                ChatScreenPagingSource(socketViewModel, query).also {
                    chatPagingSource = it
                }
            }.flow

            chatFlow.collectLatest { pagingData ->
                handleLoadingState()
                chatListAdapter.submitData(pagingData)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        socketViewModel.leaveChat()
        socketViewModel.disableAutoFetchChatScreen()
        // Don't remove all listeners - we want them to persist for reconnection
        // socketViewModel.removeAllListeners()
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
                val newQuery = s.toString().trim()
                // Only update if query actually changed
                if (newQuery != query) {
                    val oldQuery = query
                    query = newQuery
                    // Remove any previous callbacks
                    searchRunnable?.let { handler.removeCallbacks(it) }
                    // Create a new delayed task
                    searchRunnable = Runnable {
                        // If search was cleared (had text before, now empty), explicitly fetch all chats
                        if (oldQuery.isNotEmpty() && query.isEmpty()) {
                            // Search cleared - fetch all chats by emitting CHAT_SCREEN with empty query
                            lifecycleScope.launch {
                                socketViewModel.fetchChatScreen(1, 20, "")
                            }
                        } else {
                            // Normal search - refresh chat data with new query
                            refreshChatData()
                        }
                    }
                    // Post with delay (e.g., 300ms)
                    handler.postDelayed(searchRunnable!!, 300)
                }
            }
        })



        setupRecyclerViews()
    }

    /**
     * Observe socket connection status.
     * When socket connects, ensure Pager is set up and trigger initial load.
     */
    private fun observeSocketStatus() {
        // Check current status immediately in case socket is already connected
        val currentStatus = socketViewModel.socketStatus.value
        if (currentStatus == "Connected") {
            // Socket is already connected
            setupChatPager()
            // Trigger initial load by invalidating PagingSource
            chatPagingSource?.invalidate()
            fetchUserData()
        }
        
        // Also observe future status changes
        socketViewModel.socketStatus.observe(viewLifecycleOwner) { status ->
            if (status == "Connected") {
                // Socket is ready
                setupChatPager()
                // Trigger initial load by invalidating PagingSource
                chatPagingSource?.invalidate()
                fetchUserData()
            }
        }
    }

    private fun setupRecyclerViews() {
        chatListAdapter = ChatListAdapter(requireContext())

        binding.chatListRv.adapter = chatListAdapter.withLoadStateFooter(footer = LoaderAdapter())
        binding.chatListRv.isNestedScrollingEnabled = false


    }

    private fun fetchSocketData() {
        // Note: Chat data is now fetched automatically when socket connects
        // (via enableAutoFetchChatScreen() in refreshData())
        // We only need to fetch user data here
        handler.postDelayed({
            fetchUserData()
            userConnectDisconnect()
        }, 200)
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
            // Invalidate previous PagingSource if it exists to cancel any ongoing loads
            chatPagingSource?.invalidate()
            chatPagingSource = null

            // Create a new Pager flow with the current query
            val chatFlow = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                ChatScreenPagingSource(socketViewModel, query).also {
                    chatPagingSource = it
                }
            }.flow

            chatFlow.collectLatest { pagingData ->
                handleLoadingState()
                chatListAdapter.submitData(pagingData)
            }
        }
    }
    
    private fun refreshChatData() {
        // Simply call fetchChatData which will create a new Pager with updated query
        fetchChatData()
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
