package com.thehotelmedia.android.Socket


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thehotelmedia.android.BuildConfig
import com.thehotelmedia.android.SocketModals.chatScreen.ChatScreenModal
import com.thehotelmedia.android.SocketModals.fetchConversation.FetchConversationModal
import com.thehotelmedia.android.SocketModals.privateMessage.PrivateMessageModal
import com.thehotelmedia.android.SocketModals.privateMessage.EditMessageResponse
import com.thehotelmedia.android.SocketModals.privateMessage.DeleteMessageResponse
import com.thehotelmedia.android.SocketModals.privateMessage.SocketError
import com.thehotelmedia.android.SocketModals.users.UsersModal
import io.socket.client.Socket
import org.json.JSONObject

class SocketViewModel : ViewModel() {

    private val socketUrl = BuildConfig.DOMAIN

    private var userName: String = "" // Track the current username


    private val _socketStatus = MutableLiveData<String>()
    val socketStatus: LiveData<String> get() = _socketStatus


    private val _messageSeen = MutableLiveData<String>()
    val messageSeen: LiveData<String> get() = _messageSeen


    private val _connectUser = MutableLiveData<String>()
    val connectUser: LiveData<String> get() = _connectUser
    private val _disconnectUser = MutableLiveData<String>()
    val disconnectUser: LiveData<String> get() = _disconnectUser


    private val _receivedMessage = MutableLiveData<PrivateMessageModal>()
    val receivedMessage: LiveData<PrivateMessageModal> get() = _receivedMessage


    private val _usersList = MutableLiveData<ArrayList<UsersModal>>()
    val usersList: LiveData<ArrayList<UsersModal>> get() = _usersList

    private val _chatScreenList = MutableLiveData<ChatScreenModal>()
    val chatScreenList: LiveData<ChatScreenModal> get() = _chatScreenList

    private val _conversationList = MutableLiveData<FetchConversationModal>()
    val conversationList: LiveData<FetchConversationModal> get() = _conversationList

    private val _messageEdited = MutableLiveData<EditMessageResponse>()
    val messageEdited: LiveData<EditMessageResponse> get() = _messageEdited

    private val _messageDeleted = MutableLiveData<DeleteMessageResponse>()
    val messageDeleted: LiveData<DeleteMessageResponse> get() = _messageDeleted

    private val _socketError = MutableLiveData<SocketError>()
    val socketError: LiveData<SocketError> get() = _socketError

    private lateinit var socketManager: SocketManager
    private var listenersAttached = false
    private var shouldEmitChatScreenOnConnect = false



    fun connectSocket( username: String) {

//        // Purane connection ko reset karen
//        if (::socketManager.isInitialized) {
//            socketManager.reset()
//        }

        this.userName = username // Store the username

        // Initialize socket with URL and username
        socketManager = SocketManager.getInstance(socketUrl, userName)
        
        // Attach ALL listeners BEFORE initializing socket connection
        // This ensures listeners are ready when connection completes
        attachSocketListeners()
        
        // Now initialize the socket (this will trigger connection)
        socketManager.initializeSocket()
        
        // Register callback when connected - Fragment will handle triggering load
        socketManager.onConnected {
            _socketStatus.postValue("Connected")
        }
        
        // Also observe connection status for UI updates
        socketManager.on(Socket.EVENT_CONNECT) {
            _socketStatus.postValue("Connected")
        }
        socketManager.on(Socket.EVENT_DISCONNECT) {
            _socketStatus.postValue("Disconnected")
        }
        socketManager.on(Socket.EVENT_CONNECT_ERROR) { args ->
            _socketStatus.postValue("Error: ${args[0]}")
        }
    }

    /**
     * Attach all socket event listeners.
     * This should be called BEFORE initializeSocket() to ensure listeners are ready.
     * Can be called multiple times safely - Socket.IO handles duplicate listeners.
     */
    private fun attachSocketListeners() {
        // Check if socket manager is initialized
        if (!::socketManager.isInitialized) {
            return
        }
        
        // Note: We allow re-attaching listeners even if already attached
        // This is important when listeners are removed by other components
        // Socket.IO's on() method can handle multiple listeners for the same event
        
        // Listen for incoming messages
        socketManager.on("private message") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   private message ${args[0]}")
                try {
                    val gson = Gson()
                    val privateMsg: PrivateMessageModal = gson.fromJson(
                        json,
                        object : TypeToken<PrivateMessageModal>() {}.type
                    )
                    _receivedMessage.postValue(privateMsg)
                } catch (e: Exception) {
                    e.printStackTrace() // Handle any parsing errors
                }

            }
        }
//        // Listen for incoming messages
//        socketManager.on("message seen") { args ->
//            if (args.isNotEmpty()) {
//                val json = args[0].toString() // Convert to JSON string
//                println("dahfjhsadjkhas   message seen ${args[0]}")
//                try {
//                    val gson = Gson()
//                    val privateMsg: PrivateMessageModal = gson.fromJson(
//                        json,
//                        object : TypeToken<PrivateMessageModal>() {}.type
//                    )
//                    _receivedMessage.postValue(privateMsg)
//                } catch (e: Exception) {
//                    e.printStackTrace() // Handle any parsing errors
//                }
//
//            }
//        }

        // Listen for the 'chat screen' event
        socketManager.on("message seen") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   message seen ${args[0]}")
//                try {
//                    val gson = Gson()
//                    val chatArray: FetchConversationModal = gson.fromJson(
//                        json,
//                        object : TypeToken<FetchConversationModal>() {}.type
//                    )
                _messageSeen.postValue(json)
//                } catch (e: Exception) {
//                    e.printStackTrace() // Handle any parsing errors
//                }

            }
        }



        // Listen for the 'users' event
        socketManager.on("users") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string

                println("dahfjhsadjkhas   users ${args[0]}")
                try {
                    val gson = Gson()
                    val usersArray: List<UsersModal> = gson.fromJson(
                        json,
                        object : TypeToken<List<UsersModal>>() {}.type
                    )
                    _usersList.postValue(ArrayList(usersArray))
                } catch (e: Exception) {
                    e.printStackTrace() // Handle any parsing errors
                }
//                _usersList.postValue(args[0] as ArrayList<UsersModal>?)
            }
        }


        // Listen for the 'chat screen' event
        socketManager.on("chat screen") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   chat screen ${args[0]}")
                try {
                    val gson = Gson()
                    val chatArray: ChatScreenModal = gson.fromJson(
                        json,
                        object : TypeToken<ChatScreenModal>() {}.type
                    )
                    _chatScreenList.postValue(chatArray)
                } catch (e: Exception) {
                    e.printStackTrace() // Handle any parsing errors
                }
            }
        }
        // Listen for the 'chat screen' event
        socketManager.on("fetch conversations") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   fetch conversations ${args[0]}")
                try {
                    val gson = Gson()
                    val chatArray: FetchConversationModal = gson.fromJson(
                        json,
                        object : TypeToken<FetchConversationModal>() {}.type
                    )
                    _conversationList.postValue(chatArray)
                } catch (e: Exception) {
                    e.printStackTrace() // Handle any parsing errors
                }

            }
        }



        // Listen for the 'chat screen' event
        socketManager.on("user connected") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   user connected ${args[0]}")
//                try {
//                    val gson = Gson()
//                    val chatArray: FetchConversationModal = gson.fromJson(
//                        json,
//                        object : TypeToken<FetchConversationModal>() {}.type
//                    )
                    _connectUser.postValue(json)
//                } catch (e: Exception) {
//                    e.printStackTrace() // Handle any parsing errors
//                }

            }
        }

        // Listen for the 'chat screen' event
        socketManager.on("user disconnected") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString() // Convert to JSON string
                println("dahfjhsadjkhas   user disconnected ${args[0]}")
//                try {
//                    val gson = Gson()
//                    val chatArray: FetchConversationModal = gson.fromJson(
//                        json,
//                        object : TypeToken<FetchConversationModal>() {}.type
//                    )
                    _disconnectUser.postValue(json)
//                } catch (e: Exception) {
//                    e.printStackTrace() // Handle any parsing errors
//                }

            }
        }

        // Listen for 'edit message' event
        socketManager.on("edit message") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString()
                println("dahfjhsadjkhas   edit message ${args[0]}")
                try {
                    val gson = Gson()
                    val editResponse: EditMessageResponse = gson.fromJson(
                        json,
                        object : TypeToken<EditMessageResponse>() {}.type
                    )
                    _messageEdited.postValue(editResponse)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Listen for 'delete message' event
        socketManager.on("delete message") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString()
                println("dahfjhsadjkhas   delete message ${args[0]}")
                try {
                    val gson = Gson()
                    val deleteResponse: DeleteMessageResponse = gson.fromJson(
                        json,
                        object : TypeToken<DeleteMessageResponse>() {}.type
                    )
                    _messageDeleted.postValue(deleteResponse)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Listen for 'error' event
        socketManager.on("error") { args ->
            if (args.isNotEmpty()) {
                val json = args[0].toString()
                println("dahfjhsadjkhas   error ${args[0]}")
                try {
                    val gson = Gson()
                    val error: SocketError = gson.fromJson(
                        json,
                        object : TypeToken<SocketError>() {}.type
                    )
                    _socketError.postValue(error)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        listenersAttached = true
    }
    
    /**
     * Force re-attach listeners. Useful when listeners were removed
     * by other components but the Fragment still needs them.
     */
    fun reattachListeners() {
        if (::socketManager.isInitialized) {
            listenersAttached = false // Reset flag to force re-attachment
            attachSocketListeners()
        }
    }
    
    /**
     * Enable automatic CHAT_SCREEN emission on connection.
     * Call this when the Messages screen is opened.
     * Note: This doesn't actually emit - it just marks that we should.
     * The Fragment will handle triggering the PagingSource load.
     */
    fun enableAutoFetchChatScreen() {
        shouldEmitChatScreenOnConnect = true
    }
    
    /**
     * Disable automatic CHAT_SCREEN emission on connection.
     * Call this when leaving the Messages screen.
     */
    fun disableAutoFetchChatScreen() {
        shouldEmitChatScreenOnConnect = false
    }
    
    /**
     * Emit CHAT_SCREEN with empty query for initial load.
     * This fetches all chats.
     */
    private fun emitInitialChatScreen() {
        if (::socketManager.isInitialized && socketManager.isConnected()) {
            val jsonData = JSONObject()
            jsonData.put("pageNumber", 1)
            jsonData.put("pageSize", 20)
            // Don't include query - server returns all chats when query is missing/empty
            socketManager.emitEvent("chat screen", jsonData)
        }
    }


    // Request user list
    fun fetchUsers() {
        socketManager.emitEvent("users")
    }


    // Request user list
    fun inChat() {
        socketManager.emitEvent("in chat")
    }
    fun leaveChat() {
        socketManager.emitEvent("leave chat")
    }


    fun inPrivateChat(senderID: String) {
        socketManager.emitEvent("in private chat", senderID)
    }


    fun leavePrivateChat(senderID: String) {
        socketManager.emitEvent("leave private chat", senderID)
    }


    fun messageSeen(senderID: String) {
        socketManager.emitEvent("message seen", senderID)
    }



    fun sendPrivateMessage(type: String, message: String, recipientUsername: String, recipientMediaUrl: String, thumbnailUrl: String, mediaID: String, postID: String? = null, postOwnerUsername: String? = null) {
        // Create the JSON object for the "private message" event
        val jsonData = JSONObject()

        // Create the nested "message" object

        val messageObject = JSONObject()
        messageObject.put("type", type)
        messageObject.put("message", message)
        messageObject.put("mediaUrl", recipientMediaUrl)
        messageObject.put("thumbnailUrl", thumbnailUrl)
        messageObject.put("mediaID", mediaID)
        if (postID != null) {
            messageObject.put("postID", postID)
        }
        if (postOwnerUsername != null) {
            messageObject.put("postOwnerUsername", postOwnerUsername)
        }
        jsonData.put("to", recipientUsername)
        jsonData.put("message", messageObject)

        // Emit the "private message" event with the prepared JSON data
        socketManager.emitEvent("private message", jsonData)
    }


    fun sendStoryComment(type: String, message: String, recipientUsername: String, recipientMediaUrl: String, mediaId: String, storyId: String) {
        // Create the JSON object for the "private message" event
        val jsonData = JSONObject()

        // Create the nested "message" object

        val messageObject = JSONObject()
        messageObject.put("type", type)
        messageObject.put("message", message)
        messageObject.put("mediaUrl", recipientMediaUrl)
        messageObject.put("mediaID", mediaId)
        messageObject.put("storyID", storyId)
        jsonData.put("to", recipientUsername)
        jsonData.put("message", messageObject)

        // Emit the "private message" event with the prepared JSON data
        socketManager.emitEvent("private message", jsonData)
    }
    // Fetch chat screen data with pagination support
    suspend fun fetchChatScreen(pageNumber: Int, pageSize: Int, query: String) {
        // Clear previous data to ensure PagingSource waits for fresh response
        _chatScreenList.postValue(null)
        
        val jsonData = JSONObject()

        // Only send the query when it's not blank so that the server
        // returns the full chat list on initial load / when search is cleared.
        if (query.isNotBlank()) {
            jsonData.put("query", query)
        }

        jsonData.put("pageNumber", pageNumber)
        jsonData.put("pageSize", pageSize)

        socketManager.emitEvent("chat screen", jsonData)
    }


    // Fetch chat screen data with pagination support
    fun fetchConversation(pageNumber: Int, pageSize: Int, senderID: String) {

        // Create the JSON object
        val jsonData = JSONObject()

        // Add data to the JSON object
        jsonData.put("username", senderID)
        jsonData.put("pageNumber", pageNumber)


        socketManager.emitEvent("fetch conversations", jsonData)
    }


    fun removeAllListeners() {
        if (::socketManager.isInitialized) {
            socketManager.removeAllListeners() // Delegates to SocketManager for cleanup
            listenersAttached = false // Reset flag so listeners can be re-attached
        }
    }

    // Disconnect the socket
    fun disconnectSocket() {
        if (::socketManager.isInitialized) {
            socketManager.disconnect() // Disconnect the socket
            socketManager.reset() // Reset the SocketManager instance
            clearUsername()
        }
    }

    private fun clearUsername() {
        userName = ""
    }

    /**
     * Edit a message by sending edit message event to server
     */
    fun editMessage(messageID: String, newMessage: String) {
        val jsonData = JSONObject()
        jsonData.put("messageID", messageID)
        jsonData.put("message", newMessage)
        socketManager.emitEvent("edit message", jsonData)
    }

    /**
     * Delete a message by sending delete message event to server
     */
    fun deleteMessage(messageID: String) {
        val jsonData = JSONObject()
        jsonData.put("messageID", messageID)
        socketManager.emitEvent("delete message", jsonData)
    }
}
