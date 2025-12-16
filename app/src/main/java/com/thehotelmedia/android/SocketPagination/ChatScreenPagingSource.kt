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

            // Ask the server for the current page
            socketViewModel.fetchChatScreen(pageNumber, pageSize, query)

            // Wait for the socket response instead of assuming it will always arrive
            // within a fixed 1s window. We poll the LiveData a few times with a
            // small delay to give the backend time to respond.
            var chatScreen: ChatScreenModal? = null
            repeat(10) { // Wait up to ~3 seconds (10 * 300ms)
                chatScreen = socketViewModel.chatScreenList.value
                if (chatScreen != null) return@repeat
                delay(300)
            }

            val result: ChatScreenModal? = chatScreen

            if (result != null) {
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
