package com.thehotelmedia.android.SocketPagination

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.SocketModals.chatScreen.ChatScreenModal
import com.thehotelmedia.android.SocketModals.chatScreen.Messages
import kotlinx.coroutines.delay

class ChatScreenPagingSource(
    private val socketViewModel: SocketViewModel,
    private val query: String = ""
) : PagingSource<Int, Messages>() {

    override fun getRefreshKey(state: PagingState<Int, Messages>): Int? {
        // If the data source doesn't support refreshing, return null
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Messages> {
        return try {
            // Get the page number
            val pageNumber = params.key ?: 1
            // You can adjust the number of items per page as needed
            val pageSize = params.loadSize

            // Wait for socket to be connected before emitting
            // Check socket status and wait if not connected
            var socketReady = false
            var waitAttempts = 0
            val maxWaitAttempts = 50 // Wait up to 10 seconds (50 * 200ms)
            
            while (!socketReady && waitAttempts < maxWaitAttempts) {
                val status = socketViewModel.socketStatus.value
                if (status == "Connected") {
                    socketReady = true
                } else {
                    delay(200)
                    waitAttempts++
                }
            }
            
            if (!socketReady) {
                // Socket didn't connect in time, return empty
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }
            
            // Small delay to ensure socket is fully ready
            delay(100)
            
            // Ask the server for the current page
            socketViewModel.fetchChatScreen(pageNumber, pageSize, query)

            // Wait for the socket response. We poll the LiveData multiple times
            // to give the backend time to respond. Check more frequently at first.
            var chatScreen: ChatScreenModal? = null
            var attempts = 0
            val maxAttempts = 20 // Wait up to ~4 seconds (20 * 200ms)
            
            while (chatScreen == null && attempts < maxAttempts) {
                delay(200)
                chatScreen = socketViewModel.chatScreenList.value
                attempts++
            }

            val result: ChatScreenModal? = chatScreen

            if (result != null && result.messages != null) {
                val messages = result.messages
                // Return the loaded data and key for the next page
                LoadResult.Page(
                    data = messages,
                    prevKey = null, // For non-paginated lists, set to null
                    nextKey = if (messages.size < pageSize) null else pageNumber + 1
                )
            } else {
                // No data received even after waiting – return an empty page
                LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
