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
 * Fetches both users and business profiles and merges them together
 */
class CollaborationUsersPagingSource(
    private val search: String,
    private val repository: IndividualRepo
) : PagingSource<Int, TaggedData>() {
    private val tag = "PAGING_COLLABORATION_USERS"
    private var maxPageLimitUsers: Int? = null
    private var maxPageLimitBusiness: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TaggedData> {
        try {
            val nextPageNumber = params.key ?: 1

            // Check if we've exceeded both page limits
            if (maxPageLimitUsers != null && maxPageLimitBusiness != null && 
                nextPageNumber > maxPageLimitUsers!! && nextPageNumber > maxPageLimitBusiness!!) {
                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }

            // Fetch both users and business profiles in parallel
            val usersResponse = repository.getSearchData(
                query = search,
                type = "users", // Search individual users
                pageNumber = nextPageNumber,
                documentLimit = 20,
                businessTypeID = emptyList(),
                initialKm = "0",
                lat = 0.0,
                lng = 0.0
            )

            val businessResponse = repository.getSearchData(
                query = search,
                type = "business", // Search business profiles
                pageNumber = nextPageNumber,
                documentLimit = 20,
                businessTypeID = emptyList(),
                initialKm = "0",
                lat = 0.0,
                lng = 0.0
            )

            Log.d(tag + "KEY", "Page: $nextPageNumber")

            // Process users response
            val usersList = if (usersResponse.isSuccessful) {
                if (maxPageLimitUsers == null) {
                    maxPageLimitUsers = usersResponse.body()?.totalPages
                }
                usersResponse.body()?.searchData ?: emptyList()
            } else {
                Log.w(tag, "Users response error: ${usersResponse.code()}")
                emptyList()
            }

            // Process business response
            val businessList = if (businessResponse.isSuccessful) {
                if (maxPageLimitBusiness == null) {
                    maxPageLimitBusiness = businessResponse.body()?.totalPages
                }
                businessResponse.body()?.searchData ?: emptyList()
            } else {
                Log.w(tag, "Business response error: ${businessResponse.code()}")
                emptyList()
            }

            // Merge both lists
            val allSearchData = (usersList + businessList)

            // Convert SearchData to TaggedData format
            val taggedDataList = allSearchData.mapNotNull { searchData ->
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

            // Determine if there's a next page (if either source has more pages)
            val hasMoreUsers = maxPageLimitUsers == null || nextPageNumber < maxPageLimitUsers!!
            val hasMoreBusiness = maxPageLimitBusiness == null || nextPageNumber < maxPageLimitBusiness!!
            val nextKey = if (taggedDataList.isEmpty() || (!hasMoreUsers && !hasMoreBusiness)) {
                null
            } else {
                nextPageNumber + 1
            }

            Log.d(tag, "Merged response: users=${usersList.size}, business=${businessList.size}, total=${taggedDataList.size}, nextKey=$nextKey")
            return LoadResult.Page(taggedDataList, null, nextKey)

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

