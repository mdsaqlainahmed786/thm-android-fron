package com.thehotelmedia.android.pagination.feed

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.extensions.isPostEmpty
import retrofit2.HttpException
import java.io.IOException

class FeedPagingSource(
    private val repository: IndividualRepo,
    private val lat: Double,
    private val lng: Double,

) : PagingSource<Int, Data>() {

    private val tag = "PAGING_ACTIVE_FEED"
    private var maxPageLimit: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Data> {
        val nextPageNumber = params.key ?: 1 // Default to page 1 if no key is provided
        return try {
            // Prevent loading beyond the max page limit
            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            }

            val response = repository.getFeed(nextPageNumber, params.loadSize,lat,lng)

            if (response.isSuccessful) {
                val body = response.body()
                val data = body?.data ?: emptyList()

                // Get current user ID to check if post belongs to the user
                val currentUserId = repository.getCurrentUserId()

                // Filter out posts from private accounts where user is not following
                // Also filter out empty posts (no content, feelings, or media)
                val filteredData = data.filter { post ->
                    // First, filter out empty posts
                    if (post.isPostEmpty()) {
                        return@filter false
                    }
                    
                    val postedBy = post.postedBy
                    if (postedBy == null) {
                        true // Keep posts without postedBy info
                    } else {
                        // Check if post belongs to current user
                        val isMyPost = post.userID == currentUserId || 
                                      postedBy.Id == currentUserId
                        
                        val isPrivateAccount = postedBy.privateAccount == true || 
                                               postedBy.businessProfileRef?.privateAccount == true
                        val isFollowed = postedBy.isFollowedByMe == true
                        
                        // Show post if:
                        // 1. It belongs to the current user, OR
                        // 2. Account is not private, OR
                        // 3. Account is private but user is following
                        isMyPost || !isPrivateAccount || isFollowed
                    }
                }

                // Next key calculation
                val nextKey = if (filteredData.isEmpty() || nextPageNumber == maxPageLimit) null else nextPageNumber + 1
                val prevKey = if (nextPageNumber > 1) nextPageNumber - 1 else null


                maxPageLimit = body?.totalPages

//                val nextKey = if (data.isEmpty()) null else nextPageNumber + 1
//                val prevKey = if (nextPageNumber > 1) nextPageNumber - 1 else null

                Log.d(tag, "Loaded page $nextPageNumber")
                LoadResult.Page(
                    data = filteredData,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } else {
                Log.e(tag, "HTTP error: ${response.code()}")
                LoadResult.Error(HttpException(response))
            }
        } catch (e: IOException) {
            Log.e(tag, "IO Exception: ${e.message}")
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(tag, "HTTP Exception: ${e.message}")
            LoadResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected Exception: ${e.message}")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Data>): Int? {
        // Return the key for the closest page to the anchor position (e.g., middle of the list)
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.let { closestPage ->
                closestPage.prevKey?.plus(1) ?: closestPage.nextKey?.minus(1)
            }
        }
    }
}
