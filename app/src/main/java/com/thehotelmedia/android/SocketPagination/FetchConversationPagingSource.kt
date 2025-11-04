package com.thehotelmedia.android.SocketPagination

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.Socket.SocketViewModel
import com.thehotelmedia.android.SocketModals.fetchConversation.Messages
import kotlinx.coroutines.delay

class FetchConversationPagingSource(
    private val socketViewModel: SocketViewModel,
    private val userName: String = ""
) : PagingSource<Int, Messages>() {

    override fun getRefreshKey(state: PagingState<Int, Messages>): Int? {
        // If the data source doesn't support refreshing, return null
        return null
    }




    // Adjust your FetchConversationPagingSource to correctly handle reverse pagination
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Messages> {
        return try {
            val pageNumber = params.key ?: 1
            val pageSize = params.loadSize

            // Fetch messages (this should be in reverse order based on pagination)
            socketViewModel.fetchConversation(pageNumber, pageSize, userName)

            delay(1000) // Simulate network delay

            val conversationMessages = socketViewModel.conversationList.value
            if (conversationMessages != null) {
                val messages = conversationMessages.messages

                val reversedMessages = messages.reversed()  // Reverse messages if they're fetched in ascending order
                // Combine static messages with dynamically fetched messages
                // LoadResult.Page, prevKey is null since we're only loading older messages (no next pages)
                LoadResult.Page(
                    data = reversedMessages,
                    prevKey = if (reversedMessages.isEmpty()) null else pageNumber + 1, // Move up for previous page
                    nextKey = if (reversedMessages.size < pageSize) null else pageNumber + 1
                )
            } else {
                LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }



}