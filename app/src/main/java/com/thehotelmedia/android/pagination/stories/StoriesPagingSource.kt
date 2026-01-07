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
                
                // Log raw response body for debugging location field
                try {
                    val gson = com.google.gson.Gson()
                    val jsonResponse = gson.toJson(response.body())
                    Log.d(tag, "Raw API Response: $jsonResponse")
                } catch (e: Exception) {
                    Log.e(tag, "Error logging raw response: ${e.message}")
                }
                
                // Log location data and user tagging data for debugging
                Log.d(tag, "myStories count: ${myStories.size}")
                myStories.forEachIndexed { index, storyRef ->
                    Log.d(tag, "myStories[$index] _id: ${storyRef.Id}, location: ${storyRef.location}, placeName: ${storyRef.location?.placeName}, lat: ${storyRef.location?.lat}, lng: ${storyRef.location?.lng}, locationPositionX: ${storyRef.locationPositionX}, locationPositionY: ${storyRef.locationPositionY}")
                    // Log user tagging fields
                    Log.d(tag, "myStories[$index] userTaggedName: '${storyRef.userTaggedName}', userTaggedId: '${storyRef.userTaggedId}', userTaggedPositionX: ${storyRef.userTaggedPositionX}, userTaggedPositionY: ${storyRef.userTaggedPositionY}")
                    Log.d(tag, "myStories[$index] taggedUsers array size: ${storyRef.taggedUsers?.size ?: 0}, content: ${storyRef.taggedUsers}")
                    // Log all fields of the story to see what's available
                    Log.d(tag, "myStories[$index] full object: $storyRef")
                }
                Log.d(tag, "stories count: ${stories.size}")
                stories.forEachIndexed { index, story ->
                    Log.d(tag, "stories[$index] storiesRef count: ${story.storiesRef.size}")
                    story.storiesRef.forEachIndexed { refIndex, storyRef ->
                        Log.d(tag, "stories[$index].storiesRef[$refIndex] location: ${storyRef.location}, placeName: ${storyRef.location?.placeName}, lat: ${storyRef.location?.lat}, lng: ${storyRef.location?.lng}")
                    }
                }

                // Create an updated list to hold the final stories data
                val updatedStories = mutableListOf<Stories>()

                // Convert MyStories to StoriesRef to preserve location data and all other fields
                val myStoriesRef = myStories.map { myStory ->
                    StoriesRef(
                        Id = myStory.Id,
                        mediaID = myStory.mediaID,
                        createdAt = myStory.createdAt,
                        likedByMe = null,
                        mimeType = myStory.mimeType,
                        sourceUrl = myStory.sourceUrl,
                        likesRef = myStory.likesRef,
                        viewsRef = myStory.viewsRef,
                        likes = myStory.likes,
                        views = myStory.views,
                        taggedRef = myStory.taggedRef,
                        location = myStory.location,  // CRITICAL: Preserve location data from API response
                        locationPositionX = myStory.locationPositionX,  // Preserve x position
                        locationPositionY = myStory.locationPositionY,  // Preserve y position
                        taggedUsers = myStory.taggedUsers,  // Preserve tagged users array
                        userTaggedName = myStory.userTaggedName,  // Preserve user tag name (backward compatibility)
                        userTaggedId = myStory.userTaggedId,  // Preserve user tag ID (backward compatibility)
                        userTaggedPositionX = myStory.userTaggedPositionX,  // Preserve x position (backward compatibility)
                        userTaggedPositionY = myStory.userTaggedPositionY   // Preserve y position (backward compatibility)
                    ).also {
                        // Log to verify all data is being preserved, especially userTaggedId
                        Log.d(tag, "Converted MyStories to StoriesRef - Story ID: ${it.Id}")
                        Log.d(tag, "  Location: ${it.location}, placeName: ${it.location?.placeName}, lat: ${it.location?.lat}, lng: ${it.location?.lng}, x: ${it.locationPositionX}, y: ${it.locationPositionY}")
                        Log.d(tag, "  User Tagging - userTaggedName: '${it.userTaggedName}', userTaggedId: '${it.userTaggedId}', userTaggedX: ${it.userTaggedPositionX}, userTaggedY: ${it.userTaggedPositionY}")
                        Log.d(tag, "  taggedUsers count: ${it.taggedUsers?.size ?: 0}, content: ${it.taggedUsers}")
                        // Additional debug log specifically for userTaggedId
                        if (it.userTaggedName != null && it.userTaggedId == null) {
                            Log.w(tag, "WARNING: Story has userTaggedName '${it.userTaggedName}' but userTaggedId is NULL! This story may need to be re-created after backend update.")
                        }
                        if (it.userTaggedName != null && it.userTaggedId != null) {
                            Log.d(tag, "✓ Story has valid user tagging data - Name: '${it.userTaggedName}', ID: '${it.userTaggedId}', Position: (${it.userTaggedPositionX}, ${it.userTaggedPositionY})")
                        }
                    }
                }

                updatedStories.add(
                    Stories(
                        id = "myStory.Id",  // Assuming you want to use _id for Id in Stories
                        accountType = "Owner",  // Set accountType to "Owner"
                        username = "myStory.username",
                        name = "myStory.name",
                        businessProfileRef = null,  // Set businessProfileRef to null as per requirement
                        storiesRef = ArrayList(myStoriesRef),  // Convert MyStories to StoriesRef to preserve location
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