package com.thehotelmedia.android.pagination.collaboration

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.modals.forms.taggedPeople.TaggedData
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessProfileRef
import com.thehotelmedia.android.modals.forms.taggedPeople.ProfilePic
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessProfilePic
import com.thehotelmedia.android.modals.forms.taggedPeople.BusinessTypeRef
import com.thehotelmedia.android.modals.forms.taggedPeople.Address
import com.thehotelmedia.android.repository.IndividualRepo
import retrofit2.HttpException
import java.io.IOException

/**
 * PagingSource for searching all users (both individual and business) for collaboration
 * Uses the search endpoint with type "users" to search across all users in the database
 */
class CollaborationUsersPagingSource(
    private val search: String,
    private val repository: IndividualRepo
) : PagingSource<Int, TaggedData>() {
    private val tag = "PAGING_COLLABORATION_USERS"
    private var maxPageLimit: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TaggedData> {
        try {
            val nextPageNumber = params.key ?: 1

            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {
                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }

            // Use search endpoint with type "users" to search all users (individual + business)
            val response = repository.getSearchData(
                query = search,
                type = "users", // Search all users
                pageNumber = nextPageNumber,
                documentLimit = 20,
                businessTypeID = emptyList(), // Empty list to get all business types
                initialKm = "0", // Not needed for user search
                lat = 0.0, // Not needed for user search
                lng = 0.0 // Not needed for user search
            )

            Log.d(tag + "KEY", nextPageNumber.toString())

            return if (response.isSuccessful) {
                Log.d(tag, "Response code: ${response.code()}")
                val searchDataList = response.body()?.searchData ?: emptyList()

                // Convert SearchData to TaggedData format
                val taggedDataList = searchDataList.mapNotNull { searchData ->
                    try {
                        TaggedData(
                            Id = searchData.Id,
                            accountType = searchData.accountType,
                            profilePic = searchData.profilePic?.let { pic ->
                                ProfilePic(
                                    small = pic.small,
                                    medium = pic.medium,
                                    large = pic.large
                                )
                            },
                            username = searchData.username,
                            name = searchData.name,
                            businessProfileRef = searchData.businessProfileRef?.let { businessRef ->
                                BusinessProfileRef(
                                    businessProfilePic = businessRef.profilePic?.let { bpPic ->
                                        BusinessProfilePic(
                                            small = bpPic.small,
                                            medium = bpPic.medium,
                                            large = bpPic.large
                                        )
                                    },
                                    name = businessRef.name,
                                    address = businessRef.address?.let { addr ->
                                        Address(
                                            street = addr.street,
                                            city = addr.city,
                                            state = addr.state,
                                            country = addr.country,
                                            zipCode = addr.zipCode
                                        )
                                    },
                                    businessTypeRef = businessRef.businessTypeRef?.let { btRef ->
                                        BusinessTypeRef(
                                            Id = btRef.Id,
                                            name = btRef.name,
                                            icon = btRef.icon
                                        )
                                    }
                                )
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error converting SearchData to TaggedData: ${e.message}", e)
                        null
                    }
                }

                val nextKey = if (taggedDataList.isEmpty()) null else nextPageNumber + 1

                if (maxPageLimit == null) {
                    maxPageLimit = response.body()?.totalPages
                }
                Log.d(tag, "Response: $nextKey, converted ${taggedDataList.size} users")
                LoadResult.Page(taggedDataList, null, nextKey)

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

    override fun getRefreshKey(state: PagingState<Int, TaggedData>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

