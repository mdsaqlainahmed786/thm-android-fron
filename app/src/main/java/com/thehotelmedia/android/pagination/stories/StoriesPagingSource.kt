package com.thehotelmedia.android.pagination.stories

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.Stories.Stories
import com.thehotelmedia.android.modals.Stories.StoriesData
import com.thehotelmedia.android.modals.Stories.StoriesRef
import com.thehotelmedia.android.modals.feeds.feed.Data
import com.thehotelmedia.android.repository.IndividualRepo
import retrofit2.HttpException
import java.io.IOException



class StoriesPagingSource(
    private val repository: IndividualRepo
) : PagingSource<Int, Stories>() {
    private val tag = "PAGING_ACTIVE_STORY"
    private var maxPageLimit: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Stories> {

        try {

            val nextPageNumber = params.key ?: 1

            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {

                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)

            }

            val response = repository
                .getStories( nextPageNumber, 20)

            Log.d(tag + "KEY", nextPageNumber.toString())

            return if (response.isSuccessful) {
                Log.d(tag, "Response code: ${response.code()}")

                // Get the myStories and stories from the response body
                val myStories = response.body()?.storiesData?.myStories ?: emptyList()
                val stories = response.body()?.storiesData?.stories ?: emptyList()

                // Create an updated list to hold the final stories data
                val updatedStories = mutableListOf<Stories>()

                updatedStories.add(
                    Stories(
                        id = "myStory.Id",  // Assuming you want to use _id for Id in Stories
                        accountType = "Owner",  // Set accountType to "Owner"
                        username = "myStory.username",
                        name = "myStory.name",
                        businessProfileRef = null,  // Set businessProfileRef to null as per requirement
                        storiesRef = ArrayList(myStories),  // Assuming no storiesRef data in myStories, leave it empty
                    )
                )

                // Add the rest of the stories to the updated list
                updatedStories.addAll(stories.map { story ->

                    Stories(
                        id = story.id,
                        accountType = story.accountType,
                        username = story.username,
                        name = story.name,
                        profilePic = story.profilePic,
                        businessProfileRef = story.businessProfileRef,  // Assuming you want to keep original businessProfileRef
                        storiesRef = story.storiesRef,  // Assuming you want to keep original storiesRef
                        seenByMe = story.seenByMe
                    )
                })

                // Calculate the next key for pagination
                val nextKey = if (stories.isEmpty()) null else nextPageNumber + 1

                // Check if maxPageLimit is set and update it if necessary
                if (maxPageLimit == null) {
                    maxPageLimit = response.body()?.totalPages
                }

                // Log the next key for debugging
                Log.d(tag, "Response: $nextKey")

                println("PAGING_ACTIVE_STORY    $updatedStories")

                // Return the updated list of stories in the paging result
                LoadResult.Page(updatedStories, null, nextKey)
            } else {
                // Handle unsuccessful response
                LoadResult.Error(Throwable("Failed to load data"))
            }




        } catch (e: IOException) {
            Log.e(tag, "IO Exception: ${e.message}")
            return LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.e(tag, "HTTP Exception: ${e.message}")
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Stories>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}