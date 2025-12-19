package com.thehotelmedia.android.Socket

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CompletableDeferred
import java.net.URISyntaxException

class SocketManager private constructor(private val url: String, private val username: String) {

    private val TAG = "SocketManager"
    private var socket: Socket? = null
    private val connectionCallbacks = mutableListOf<() -> Unit>()

    // Companion object for Singleton pattern
    companion object {
        private var instance: SocketManager? = null

        // Create a singleton instance
        fun getInstance(url: String, username: String): SocketManager {
            if (instance == null) {
                instance = SocketManager(url, username)
            }
            return instance!!
        }
    }
    
    /**
     * Register a callback to be invoked when the socket connects.
     * If already connected, the callback is invoked immediately.
     */
    fun onConnected(callback: () -> Unit) {
        if (socket?.connected() == true) {
            callback()
        } else {
            connectionCallbacks.add(callback)
        }
    }

    // Initialize socket with authentication and options
    fun initializeSocket() {
        // Check if the socket is already initialized and connected
        if (socket != null && socket?.connected() == true) {
            Log.d(TAG, "Socket is already connected  $username")
            // Invoke any pending connection callbacks since we're already connected
            connectionCallbacks.forEach { it.invoke() }
            connectionCallbacks.clear()
            return
        }



        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                auth = mapOf("username" to username)
                reconnection = true
                reconnectionAttempts = 5  // Reduced number of attempts
                reconnectionDelay = 2000 // Reduced delay between attempts
                reconnectionDelayMax = 5000 // Max delay for reconnections
                timeout = 10000 // Timeout for the initial connection attempt

            }

            socket = IO.socket(url, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to socket  $username")
                // Emit username after connection
                socket?.emit("set_username", username)
                // Invoke all registered connection callbacks
                connectionCallbacks.forEach { it.invoke() }
                connectionCallbacks.clear()
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from socket")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connection error: ${args[0]}")
            }

            socket?.connect()

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket URI error: ${e.message}")
        }
    }

//    // Emit message to server
//    fun emitMessage(event: String, message: Any) {
//        socket?.emit(event, message) ?: Log.e(TAG, "Socket is not initialized")
//    }

    fun emitEvent(event: String, data: Any? = null) {
        if (socket?.connected() == true) {
            if (data != null) {
                socket?.emit(event, data)
                Log.d(TAG, "Emitted event: $event with data: $data")
            } else {
                socket?.emit(event)
                Log.d(TAG, "Emitted event: $event without data")
            }
        } else {
            Log.e(TAG, "Socket is not connected. Cannot emit event: $event")
        }
    }

    // Emit event and wait for response (suspending function)
    suspend fun emitEventAndAwaitResponse(event: String, vararg data: Any): String {
        val deferred = CompletableDeferred<String>()
        // Ensure socket is connected before emitting event
        if (socket?.connected() == true) {
            socket?.emit(event, data) { args ->
                if (args.isNotEmpty()) {
                    deferred.complete(args[0].toString())
                } else {
                    deferred.completeExceptionally(Exception("No response received"))
                }
            }
        } else {
            deferred.completeExceptionally(Exception("Socket is not connected"))
        }

        return deferred.await()
    }


    // Listen for events
    // This can be called before socket is initialized - listeners will be attached when socket is created
    fun on(event: String, listener: (args: Array<Any>) -> Unit) {
        if (socket != null) {
            socket?.on(event) { args -> listener(args) }
        } else {
            // If socket not yet initialized, we'll attach the listener after initialization
            // For now, log a warning but don't fail - the listener will be attached in initializeSocket
            // if needed, but typically listeners are attached after initializeSocket is called
            Log.w(TAG, "Socket not initialized when attaching listener for: $event. Ensure initializeSocket() is called first.")
        }
    }

    // Disconnect the socket
    fun disconnect() {
        if (socket != null) {
            socket?.disconnect()
            socket?.off() // Removes all listeners
            socket = null // Clear the socket instance
            Log.d(TAG, "Socket disconnected and cleared")
        } else {
            Log.e(TAG, "Socket is not initialized")
        }
    }

    fun reset() {
        disconnect() // Properly disconnect and cleanup
        instance = null // Reset the singleton instance
        Log.d(TAG, "SocketManager instance reset")
    }

    // Get socket status
    fun getSocketStatus(): String {
        return if (socket?.connected() == true) {
            "Connected"
        } else {
            "Disconnected"
        }
    }

    fun removeAllListeners() {
        socket?.off() // Removes all event listeners
        connectionCallbacks.clear()
    }
    
    /**
     * Check if socket is connected
     */
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
}
