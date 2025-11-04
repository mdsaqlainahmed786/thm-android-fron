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

    private lateinit var socketManager: SocketManager



    fun connectSocket( username: String) {

//        // Purane connection ko reset karen
//        if (::socketManager.isInitialized) {
//            socketManager.reset()
//        }

        this.userName = username // Store the username

        // Initialize socket with URL and username
        socketManager = SocketManager.getInstance(socketUrl, userName)
        socketManager.initializeSocket()

        // Observe connection status
        socketManager.on(Socket.EVENT_CONNECT) {
            _socketStatus.postValue("Connected")
        }
        socketManager.on(Socket.EVENT_DISCONNECT) {
            _socketStatus.postValue("Disconnected")
        }
        socketManager.on(Socket.EVENT_CONNECT_ERROR) { args ->
            _socketStatus.postValue("Error: ${args[0]}")
        }

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



    fun sendPrivateMessage(type: String, message: String, recipientUsername: String, recipientMediaUrl: String, thumbnailUrl: String, mediaID: String) {
        // Create the JSON object for the "private message" event
        val jsonData = JSONObject()

        // Create the nested "message" object

        val messageObject = JSONObject()
        messageObject.put("type", type)
        messageObject.put("message", message)
        messageObject.put("mediaUrl", recipientMediaUrl)
        messageObject.put("thumbnailUrl", thumbnailUrl)
        messageObject.put("mediaID", mediaID)
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

        val jsonData = JSONObject()

        // Add data to the JSON object
        jsonData.put("query", query)
        jsonData.put("pageNumber", pageNumber)
        // Handle server data fetching with pagination (You may need to update your SocketManager and server to support this)
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
}
