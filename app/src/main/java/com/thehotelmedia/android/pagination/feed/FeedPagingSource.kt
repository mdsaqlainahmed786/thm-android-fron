package com.thehotelmedia.android.pagination.feed

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.repository.IndividualRepo
import retrofit2.HttpException
import java.io.IOException

class FeedPagingSource(
    private val repository: IndividualRepo,
    private val lat: Double,
    private val lng: Double,

) : PagingSource<Int, Data>() {

    private val tag = "PAGING_ACTIVE_FEED"
    private var maxPageLimit: Int? = null

    private var staticItemAdded = false // Flag to track if static item has been added

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

                // Next key calculation
                val nextKey = if (data.isEmpty() || nextPageNumber == maxPageLimit) null else nextPageNumber + 1
                val prevKey = if (nextPageNumber > 1) nextPageNumber - 1 else null
//                println("fjfkjfdk   $nextKey")
//                println("fjfkjfdk   $data")
//                println("fjfkjfdk   ${data.size}")
//                println("fjfkjfdk   ${params.loadSize}")

                val dataList = if (nextKey == null || data.size < params.loadSize) {
                    // Last page hai, to static item add karo
//                    data + listOf(
//                        Data(
//                            Id = "static_id",
//                            isPublished = true,
//                            feelings = "Excited",
//                            content = "This is a static post",
//                            name = "Static Name",
//                            postType = "post",
//                            createdAt = "2025-01-09T16:19:45.248Z",
//                            likes = 0,
//                            comments = 0,
//                            likedByMe = false,
//                            savedByMe = false,
//                            imJoining = false
//                        )
//                    )
                    if (!staticItemAdded) {
                        staticItemAdded = true // Mark that static item has been added
                        data + listOf(
                            Data(
                                Id = "static_id",
                                isPublished = true,
                                feelings = "Excited",
                                content = "This is a static post",
                                name = "Static Name",
                                postType = "post",
                                createdAt = "2025-01-09T16:19:45.248Z",
                                likes = 0,
                                comments = 0,
                                likedByMe = false,
                                savedByMe = false,
                                imJoining = false
                            )
                        )
                    } else {
                        data
                    }



                } else {
                    data
                }


                maxPageLimit = body?.totalPages

//                val nextKey = if (data.isEmpty()) null else nextPageNumber + 1
//                val prevKey = if (nextPageNumber > 1) nextPageNumber - 1 else null

                Log.d(tag, "Loaded page $nextPageNumber")
                LoadResult.Page(
                    data = dataList,
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
