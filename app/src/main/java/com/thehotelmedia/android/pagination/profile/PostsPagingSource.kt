package com.thehotelmedia.android.pagination.profile

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.modals.collaboration.CollaborationPostItem
import com.thehotelmedia.android.repository.IndividualRepo
import com.thehotelmedia.android.extensions.isPostEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException

//class PostsPagingSource {
//}

class PostsPagingSource(
    private val userId: String,
    private val repository: IndividualRepo
) : PagingSource<Int, Data>() {
    private val tag = "PAGING_ACTIVE_FOLLOWER"
    private var maxPageLimit: Int? = null

    /**
     * Collaboration posts are fetched via a separate endpoint (`GET collaboration`) and do not
     * show up in `GET user/posts/{id}`. To match expected UX (Instagram-style collabs), we
     * hydrate a small number of collaboration post IDs into full post objects and prepend them
     * to the first page of "my profile posts" only.
     */
    private val maxCollaborationHydration = 20

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Data> {

        try {

            val nextPageNumber = params.key ?: 1

            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {

                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)

            }

            val response = repository
                .getPostsData( userId,nextPageNumber, 20)

            Log.d(tag + "KEY", nextPageNumber.toString())
            return if (response.isSuccessful) {
                Log.d(tag, "Response code: ${response.code()}")
                println("sdajfksajfka    ${response.body()}")

                val basePosts = response.body()?.data?.filter { !it.isPostEmpty() } ?: emptyList()

                // Only include collaboration posts on "my own profile" and only on the first page
                val shouldIncludeCollaborationPosts =
                    nextPageNumber == 1 && userId.isNotBlank() && userId == repository.getCurrentUserId()

                val collaborationPosts: List<Data> = if (shouldIncludeCollaborationPosts) {
                    hydrateCollaborationPostsSafely()
                } else {
                    emptyList()
                }

                // Merge + dedupe by post ID. Keep newest first (createdAt is ISO8601 so string sort works).
                val services = (collaborationPosts + basePosts)
                    .distinctBy { it.Id }
                    .sortedByDescending { it.createdAt.orEmpty() }

                // Pagination should follow the base "user/posts" endpoint. Collaboration items are
                // extra and should not force additional paging requests when the base list is empty.
                val nextKey = if (basePosts.isEmpty()) null else nextPageNumber + 1

                if (maxPageLimit == null) {
                    maxPageLimit = response.body()?.totalPages
                }
                Log.d(tag, "Response: $nextKey")
                LoadResult.Page(services, null, nextKey)

            } else {
                Log.d(tag, "HTTP error: ${response.code()}")
                LoadResult.Error(Throwable("$tag error : ${response.code()}"))
            }


        } catch (e: IOException) {
            Log.e(tag, "IO Exception: ${e.message}")
            return LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(tag, "HTTP Exception: ${e.message}")
            return LoadResult.Error(e)
        }
    }

    private suspend fun hydrateCollaborationPostsSafely(): List<Data> = coroutineScope {
        try {
            val collabResponse = repository.getCollaborationPosts()
            if (!collabResponse.isSuccessful) {
                Log.w(tag, "Collaboration posts HTTP error: ${collabResponse.code()}")
                return@coroutineScope emptyList()
            }

            val collabItems: List<CollaborationPostItem> = collabResponse.body()?.data ?: emptyList()
            val postIds = collabItems.mapNotNull { it._id }.distinct().take(maxCollaborationHydration)

            if (postIds.isEmpty()) return@coroutineScope emptyList()

            // Hydrate into full feed Data objects in parallel (bounded by maxCollaborationHydration)
            val hydrated = postIds.map { postId ->
                async {
                    try {
                        val postRes = repository.getSinglePosts(postId)
                        if (postRes.isSuccessful) {
                            postRes.body()?.data
                        } else {
                            Log.w(tag, "Failed to hydrate collaboration post=$postId code=${postRes.code()}")
                            null
                        }
                    } catch (t: Throwable) {
                        Log.w(tag, "Error hydrating collaboration post=$postId: ${t.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull().filter { !it.isPostEmpty() }

            Log.d(tag, "Hydrated collaboration posts: requested=${postIds.size}, got=${hydrated.size}")
            hydrated
        } catch (t: Throwable) {
            Log.w(tag, "Error fetching collaboration posts: ${t.message}")
            emptyList()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Data>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}