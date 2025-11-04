package com.thehotelmedia.android.SocketPagination

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.Socket.SocketViewModel
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

            // Fetch the chat messages from the server (simulate with a delay here)
            socketViewModel.fetchChatScreen(pageNumber, pageSize,query)

            // Simulating a delay for network request
            delay(1000)

            // Safely get chatScreenList, handle nullability
            val chatScreen = socketViewModel.chatScreenList.value
            // Check if chatScreen is null or if messages are empty
            if (chatScreen != null) {
                val messages = chatScreen.messages
                // Return the loaded data and key for the next page
                LoadResult.Page(
                    data = messages,
                    prevKey = null, // For non-paginated lists, set to null
                    nextKey = if (messages.size < pageSize) null else pageNumber + 1
                )
            } else {
                // Return empty data and nextKey as null if chatScreen is null
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
