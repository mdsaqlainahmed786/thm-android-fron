package com.thehotelmedia.android.pagination.feed

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.thehotelmedia.android.repository.IndividualRepo
import retrofit2.HttpException
import java.io.IOException
import com.thehotelmedia.android.modals.feeds.getComments.Data

class CommentsPagingSource(
    private val id: String,
    private val repository: IndividualRepo
) : PagingSource<Int, Data>() {
    private val tag = "PAGING_ACTIVE_FEED"
    private var maxPageLimit: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Data> {

        try {

            val nextPageNumber = params.key ?: 1

            if (maxPageLimit != null && nextPageNumber > maxPageLimit!!) {
                return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
            }

            val response = repository.getComments(id, nextPageNumber, 20)

            Log.d(tag + "KEY", nextPageNumber.toString())

            return if (response.isSuccessful) {

                val body = response.body()
                if (body == null) {
                    Log.e(tag, "Response body is null")
                    return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
                }

                val services = body.data ?: emptyList()
                val nextKey = if (services.isEmpty()) null else nextPageNumber + 1

                if (maxPageLimit == null) {
                    maxPageLimit = body.totalPages
                }

                Log.d(tag, "Loaded ${services.size} items. NextKey: $nextKey")

                LoadResult.Page(
                    data = services,
                    prevKey = if (nextPageNumber == 1) null else nextPageNumber - 1,
                    nextKey = nextKey
                )

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

    override fun getRefreshKey(state: PagingState<Int, Data>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}